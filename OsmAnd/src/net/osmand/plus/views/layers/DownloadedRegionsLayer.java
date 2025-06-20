package net.osmand.plus.views.layers;

import static net.osmand.plus.AppInitEvents.INDEX_REGION_BOUNDARIES;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.FColorARGB;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.PolygonBuilder;
import net.osmand.core.jni.PolygonsCollection;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.core.jni.ZoomLevel;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.AppInitEvents;
import net.osmand.plus.AppInitializeListener;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.local.LocalIndexHelper;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.resources.ResourceManager.ResourceListener;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProviderSelection;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class DownloadedRegionsLayer extends OsmandMapLayer implements IContextMenuProvider,
		IContextMenuProviderSelection, ResourceListener {

	private static final int ZOOM_THRESHOLD = 2;

	private OsmandApplication app;
	private Paint paintDownloaded;
	private Path pathDownloaded;
	private Paint paintSelected;
	private Path pathSelected;
	private Paint paintBackuped;
	private Path pathBackuped;
	private OsmandRegions osmandRegions;
	private LocalIndexHelper helper;

	private TextPaint textPaint;
	private ResourceManager rm;

	private MapLayerData<List<BinaryMapDataObject>> data;
	private WorldRegion selectedRegion;

	private static final int ZOOM_TO_SHOW_MAP_NAMES = 6;
	private static final int ZOOM_AFTER_BASEMAP = 12;

	private static final int ZOOM_TO_SHOW_BORDERS_ST = 4;
	private static final int ZOOM_TO_SHOW_BORDERS = 8;
	private static final int ZOOM_TO_SHOW_SELECTION_ST = 3;
	private static final int ZOOM_TO_SHOW_SELECTION = 8;

	//OpenGL
	private PolygonsCollection polygonsCollection;
	private int downloadedSize;
	private int backupedSize;
	private boolean hasSelectedRegion;
	private int polygonId = 1;
	private boolean needRedrawOpenGL;
	private boolean indexRegionBoundaries;
	private boolean onMapsChanged;
	private boolean cachedShowDownloadedMaps;
	List<WorldRegion> downloadedRegions;
	List<WorldRegion> backupedRegions;
	private MapSuggestionController mapSuggestionController;

	public static class DownloadMapObject {
		private final BinaryMapDataObject dataObject;
		private final WorldRegion worldRegion;
		private final IndexItem indexItem;
		private final LocalItem localItem;

		@NonNull
		public BinaryMapDataObject getDataObject() {
			return dataObject;
		}

		@NonNull
		public WorldRegion getWorldRegion() {
			return worldRegion;
		}

		@Nullable
		public IndexItem getIndexItem() {
			return indexItem;
		}

		@Nullable
		public LocalItem getLocalItem() {
			return localItem;
		}

		public DownloadMapObject(@NonNull BinaryMapDataObject dataObject,
		                         @NonNull WorldRegion worldRegion,
		                         @Nullable IndexItem indexItem,
		                         @Nullable LocalItem localItem) {
			this.dataObject = dataObject;
			this.worldRegion = worldRegion;
			this.indexItem = indexItem;
			this.localItem = localItem;
		}
	}

	public DownloadedRegionsLayer(@NonNull Context context) {
		super(context);
	}

	@Override
	public void initLayer(@NonNull OsmandMapTileView view) {
		super.initLayer(view);

		app = view.getApplication();
		rm = app.getResourceManager();
		rm.addResourceListener(this);
		osmandRegions = rm.getOsmandRegions();
		helper = new LocalIndexHelper(app);
		cachedShowDownloadedMaps = isShowDownloadedMaps();
		mapSuggestionController = new MapSuggestionController(view);

		paintDownloaded = getPaint(getColor(R.color.region_uptodate));
		paintSelected = getPaint(getColor(R.color.region_selected));
		paintBackuped = getPaint(getColor(R.color.region_backuped));

		textPaint = new TextPaint();

		updatePaints();

		pathDownloaded = new Path();
		pathSelected = new Path();
		pathBackuped = new Path();


		data = new MapLayerData<>() {

			@Override
			public void layerOnPostExecute() {
				view.refreshMap();
			}

			public boolean queriedBoxContains(RotatedTileBox queriedData, RotatedTileBox newBox) {
				if (newBox.getZoom() < ZOOM_TO_SHOW_SELECTION) {
					if (queriedData != null && queriedData.getZoom() < ZOOM_TO_SHOW_SELECTION) {
						return queriedData.containsTileBox(newBox);
					} else {
						return false;
					}
				}
				List<BinaryMapDataObject> queriedResults = getResults();
				if (queriedData != null && queriedData.containsTileBox(newBox) && queriedData.getZoom() >= ZOOM_TO_SHOW_MAP_NAMES) {
					return queriedResults != null && (queriedResults.isEmpty() || Math.abs(queriedData.getZoom() - newBox.getZoom()) <= 1);
				}
				return false;
			}

			@Override
			protected Pair<List<BinaryMapDataObject>, List<BinaryMapDataObject>> calculateResult(@NonNull QuadRect latLonBounds, int zoom) {
				List<BinaryMapDataObject> dataObjects = queryData(latLonBounds, zoom);
				return new Pair<>(dataObjects, dataObjects);
			}
		};
		addMapsInitializedListener();
	}

	@Override
	protected void updateResources() {
		super.updateResources();
		updatePaints();
	}

	private void updatePaints() {
		DisplayMetrics metrics = new DisplayMetrics();
		AndroidUtils.getDisplay(app).getMetrics(metrics);
		textPaint.setStrokeWidth(21 * metrics.scaledDensity);
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Paint.Align.CENTER);
	}

	private Paint getPaint(int color) {
		Paint paint = new Paint();
		paint.setStyle(Style.FILL_AND_STROKE);
		paint.setStrokeWidth(1);
		paint.setColor(color);
		paint.setAntiAlias(true);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);
		return paint;
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		super.onPrepareBufferImage(canvas, tileBox, settings);
		int zoom = tileBox.getZoom();
		if (zoom < ZOOM_TO_SHOW_SELECTION_ST || !indexRegionBoundaries) {
			return;
		}
		// Check whether there are no active or downloaded maps for the location
		mapSuggestionController.updateSuggestion(tileBox, data.getResults());
		// draw objects
		if (osmandRegions.isInitialized() && zoom >= ZOOM_TO_SHOW_SELECTION_ST && zoom < ZOOM_TO_SHOW_SELECTION) {
			MapRendererView mapRenderer = getMapRenderer();
			if (mapRenderer != null) {
				drawMapPolygons(zoom);
				return;
			}

			List<BinaryMapDataObject> currentObjects = new LinkedList<>();
			if (data.getResults() != null) {
				currentObjects.addAll(data.getResults());
			}

			WorldRegion selectedRegion = this.selectedRegion;
			if (selectedRegion != null) {
				removeRegionObjectsFromList(currentObjects, selectedRegion);
				drawPolygons(canvas, tileBox, selectedRegion.getPolygons(), pathSelected, paintSelected);
			}

			boolean isZoomToShowBorders = zoom >= ZOOM_TO_SHOW_BORDERS_ST && zoom < ZOOM_TO_SHOW_BORDERS;
			if (isShowDownloadedMaps() && isZoomToShowBorders) {
				if (!currentObjects.isEmpty()) {
					List<BinaryMapDataObject> downloadedObjects = new ArrayList<>();
					List<BinaryMapDataObject> backupedObjects = new ArrayList<>();
					for (BinaryMapDataObject o : currentObjects) {
						boolean downloaded = rm.checkIfObjectDownloaded(osmandRegions.getDownloadName(o));
						boolean backuped = rm.checkIfObjectBackuped(osmandRegions.getDownloadName(o));
						if (downloaded) {
							downloadedObjects.add(o);
						} else if (backuped) {
							backupedObjects.add(o);
						}
					}
					if (!backupedObjects.isEmpty()) {
						drawMapObjectsPolygons(canvas, tileBox, backupedObjects, pathBackuped, paintBackuped);
					}
					if (!downloadedObjects.isEmpty()) {
						drawMapObjectsPolygons(canvas, tileBox, downloadedObjects, pathDownloaded, paintDownloaded);
					}
				}
			}
		} else {
			clearPolygonsCollections();
		}
	}

	private void removeRegionObjectsFromList(@NonNull List<BinaryMapDataObject> list, @NonNull WorldRegion region) {
		OsmandRegions osmandRegions = app.getRegions();
		Iterator<BinaryMapDataObject> it = list.iterator();
		while (it.hasNext()) {
			BinaryMapDataObject mapObject = it.next();
			String regionName = osmandRegions.getFullName(mapObject);
			if (!Algorithms.isEmpty(regionName) && regionName.equals(region.getRegionId())) {
				it.remove();
			}
		}
	}

	private void drawMapObjectsPolygons(@NonNull Canvas canvas,
	                                    @NonNull RotatedTileBox tileBox,
	                                    @NonNull List<BinaryMapDataObject> mapObjects,
	                                    @NonNull Path path,
	                                    @NonNull Paint paint) {
		List<List<LatLon>> polygons = new ArrayList<>();
		for (BinaryMapDataObject mapObject : mapObjects) {
			List<LatLon> polygon = new ArrayList<>();
			for (int i = 0; i < mapObject.getPointsLength(); i++) {
				double lat = MapUtils.get31LatitudeY(mapObject.getPoint31YTile(i));
				double lon = MapUtils.get31LongitudeX(mapObject.getPoint31XTile(i));
				polygon.add(new LatLon(lat, lon));
			}
			polygons.add(polygon);
		}
		drawPolygons(canvas, tileBox, polygons, path, paint);
	}

	private void drawPolygons(@NonNull Canvas canvas,
	                          @NonNull RotatedTileBox tileBox,
	                          @NonNull List<List<LatLon>> polygons,
	                          @NonNull Path path,
	                          @NonNull Paint paint) {
		path.reset();
		for (List<LatLon> polygon : polygons) {
			for (int i = 0; i < polygon.size(); i++) {
				LatLon latLon = polygon.get(i);
				int pixX = tileBox.getPixXFromLonNoRot(latLon.getLongitude());
				int pixY = tileBox.getPixYFromLatNoRot(latLon.getLatitude());
				if (i == 0) {
					path.moveTo(pixX, pixY);
				} else {
					path.lineTo(pixX, pixY);
				}
			}
		}
		canvas.drawPath(path, paint);
	}

	private List<BinaryMapDataObject> queryData(@NonNull QuadRect latLonBounds, int zoom) {
		if (zoom >= ZOOM_AFTER_BASEMAP) {
			if (!checkIfMapEmpty(zoom)) {
				return Collections.emptyList();
			}
		}

		List<BinaryMapDataObject> result;
		int left = MapUtils.get31TileNumberX(latLonBounds.left);
		int right = MapUtils.get31TileNumberX(latLonBounds.right);
		int top = MapUtils.get31TileNumberY(latLonBounds.top);
		int bottom = MapUtils.get31TileNumberY(latLonBounds.bottom);

		try {
			result = osmandRegions.query(left, right, top, bottom, false);
		} catch (IOException e) {
			return null;
		}

		Iterator<BinaryMapDataObject> it = result.iterator();
		while (it.hasNext()) {
			BinaryMapDataObject o = it.next();
			if (zoom >= ZOOM_TO_SHOW_SELECTION) {
				if (!osmandRegions.contain(o, left / 2 + right / 2, top / 2 + bottom / 2)) {
					it.remove();
				}
			}
		}

		return result;
	}

	private boolean checkIfMapEmpty(int zoom) {
		int cState = rm.getRenderer().getCheckedRenderedState();
		boolean empty;
		if (zoom < ZOOM_AFTER_BASEMAP) {
			empty = cState == 0;
		} else {
			empty = cState <= 1;
		}
		return empty;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		if (view.getMainLayer() instanceof MapTileLayer) {
			return;
		}
		// query from UI thread because of Android AsyncTask bug (Handler init)
		data.queryNewData(tileBox);
	}

	public String getFilter(StringBuilder btnName) {
		StringBuilder filter = new StringBuilder();
		int zoom = view.getZoom();
		RotatedTileBox queriedBox = data.getQueriedBox();
		List<BinaryMapDataObject> currentObjects = data.getResults();
		if (osmandRegions.isInitialized() && queriedBox != null) {
			if (zoom >= ZOOM_TO_SHOW_MAP_NAMES && Math.abs(queriedBox.getZoom() - zoom) <= ZOOM_THRESHOLD &&
					currentObjects != null) {
				btnName.setLength(0);
				btnName.append(view.getResources().getString(R.string.shared_string_download));
				filter.setLength(0);
				Set<String> set = new TreeSet<>();
				int cx = view.getCurrentRotatedTileBox().getCenter31X();
				int cy = view.getCurrentRotatedTileBox().getCenter31Y();
				if ((!currentObjects.isEmpty())) {
					for (int i = 0; i < currentObjects.size(); i++) {
						BinaryMapDataObject o = currentObjects.get(i);
						if (!osmandRegions.contain(o, cx, cy)) {
							continue;
						}
						String fullName = osmandRegions.getFullName(o);
						WorldRegion rd = osmandRegions.getRegionData(fullName);
						if (rd != null && rd.isRegionMapDownload() && rd.getRegionDownloadName() != null) {
							String name = rd.getLocaleName();
							if (rm.checkIfObjectDownloaded(rd.getRegionDownloadName())) {
								return null;
							}
							if (!set.add(name)) {
								continue;
							}
							if (set.size() > 1) {
								btnName.append(" ").append(view.getResources().getString(R.string.shared_string_or))
										.append(" ");
								filter.append(", ");
							} else {
								btnName.append(" ");
							}
							filter.append(name);
							btnName.append(name);
						}
					}
				}
			}
		}
		if (filter.length() == 0) {
			return null;
		}
		return filter.toString();
	}

	@Override
	public boolean drawInScreenPixels() {
		return false;
	}

	@Override
	public boolean onLongPressEvent(@NonNull PointF point, @NonNull RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public void destroyLayer() {
		super.destroyLayer();
		rm.removeResourceListener(this);
	}

	@Override
	protected void cleanupResources() {
		super.cleanupResources();
		clearPolygonsCollections();
	}

	// IContextMenuProvider
	@Override
	public void collectObjectsFromPoint(@NonNull MapSelectionResult result,
	                                    boolean unknownLocation, boolean excludeUntouchableObjects) {
		if (excludeUntouchableObjects) {
			return;
		}

		boolean isMenuVisible = false;
		MapActivity mapActivity = view.getMapActivity();
		if (mapActivity != null) {
			MapContextMenu menu = mapActivity.getContextMenu();
			MapMultiSelectionMenu multiMenu = menu.getMultiSelectionMenu();
			isMenuVisible = menu.isVisible() || (multiMenu != null && multiMenu.isVisible());
		}
		if (!isMenuVisible) {
			collectRegionsFromPoint(result);
		}
	}

	@Override
	public boolean isSecondaryProvider() {
		return true;
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof DownloadMapObject mapObject) {
			return mapObject.worldRegion.getRegionCenter();
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof DownloadMapObject mapObject) {
			return new PointDescription(PointDescription.POINT_TYPE_WORLD_REGION,
					getContext().getString(R.string.shared_string_map), mapObject.worldRegion.getLocaleName());
		}
		return new PointDescription(PointDescription.POINT_TYPE_WORLD_REGION,
				getContext().getString(R.string.shared_string_map), "");
	}

	private void collectRegionsFromPoint(@NonNull MapSelectionResult result) {
		PointF point = result.getPoint();
		RotatedTileBox tb = result.getTileBox();
		int zoom = tb.getZoom();
		if (zoom >= ZOOM_TO_SHOW_SELECTION_ST && zoom < ZOOM_TO_SHOW_SELECTION
				&& data.getResults() != null && osmandRegions.isInitialized()) {
			LatLon pointLatLon = NativeUtilities.getLatLonFromPixel(getMapRenderer(), tb, point.x, point.y);
			int point31x = MapUtils.get31TileNumberX(pointLatLon.getLongitude());
			int point31y = MapUtils.get31TileNumberY(pointLatLon.getLatitude());

			List<BinaryMapDataObject> objects = new LinkedList<>(data.getResults());
			Iterator<BinaryMapDataObject> it = objects.iterator();
			while (it.hasNext()) {
				BinaryMapDataObject o = it.next();
				if (!osmandRegions.contain(o, point31x, point31y)) {
					it.remove();
				}
			}
			OsmandRegions osmandRegions = app.getRegions();
			for (BinaryMapDataObject o : objects) {
				String fullName = osmandRegions.getFullName(o);
				WorldRegion region = osmandRegions.getRegionData(fullName);
				if (region != null && region.isRegionMapDownload()) {
					List<IndexItem> indexItems = app.getDownloadThread().getIndexes().getIndexItems(region);
					List<IndexItem> dataItems = new LinkedList<>();
					IndexItem regularMapItem = null;
					for (IndexItem item : indexItems) {
						if (item.isDownloaded() || app.getDownloadThread().isDownloading(item)) {
							dataItems.add(item);
							if (item.getType() == DownloadActivityType.NORMAL_FILE) {
								regularMapItem = item;
							}
						}
					}
					if (dataItems.isEmpty() && regularMapItem != null) {
						dataItems.add(regularMapItem);
					}

					if (!dataItems.isEmpty()) {
						for (IndexItem item : dataItems) {
							result.collect(new DownloadMapObject(o, region, item, null), this);
						}
					} else {
						String downloadName = osmandRegions.getDownloadName(o);
						List<LocalItem> infos = helper.getLocalItems(downloadName);
						if (Algorithms.isEmpty(infos)) {
							result.collect(new DownloadMapObject(o, region, null, null), this);
						} else {
							for (LocalItem info : infos) {
								result.collect(new DownloadMapObject(o, region, null, info), this);
							}
						}
					}
				}
			}
		}
	}

	@Override
	public int getOrder(Object o) {
		int order = 0;
		if (o instanceof DownloadMapObject mapObject) {
			order = mapObject.worldRegion.getLevel() * 1000 - 100000;
			if (mapObject.indexItem != null) {
				order += mapObject.indexItem.getType().getOrderIndex();
			} else if (mapObject.localItem != null) {
				order += mapObject.localItem.getType().ordinal();
			}
		}
		return order;
	}

	@Override
	public void setSelectedObject(Object o) {
		if (o instanceof DownloadMapObject downloadMapObject) {
			selectedRegion = downloadMapObject.getWorldRegion();
		}
	}

	@Override
	public void clearSelectedObject() {
		selectedRegion = null;
	}

	/**OpenGL*/
	private void drawMapPolygons(int zoom) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		boolean showDownloadedMaps = isShowDownloadedMaps();
		boolean showDownloadedMapsChanged = cachedShowDownloadedMaps != showDownloadedMaps;
		cachedShowDownloadedMaps = showDownloadedMaps;
		if (onMapsChanged || showDownloadedMapsChanged) {
			clearPolygonsCollections();
			onMapsChanged = false;
			downloadedRegions = null;
			backupedRegions = null;
		}
		if (polygonsCollection != null
				&& hasSelectedRegion == (selectedRegion != null)
				&& !showDownloadedMapsChanged && !mapActivityInvalidated) {
			return;
		}
		List<WorldRegion> downloadedRegions = new ArrayList<>();
		List<WorldRegion> backupedRegions = new ArrayList<>();
		if (showDownloadedMaps && zoom >= ZOOM_TO_SHOW_BORDERS_ST && zoom < ZOOM_TO_SHOW_BORDERS) {
			if (this.downloadedRegions == null || this.backupedRegions == null) {
				List<WorldRegion> worldRegions = osmandRegions.getAllRegionData();
				for (WorldRegion wr : worldRegions) {
					String n = wr.getRegionDownloadName();
					if (rm.checkIfObjectDownloaded(n)) {
						downloadedRegions.add(wr);
					} else if (rm.checkIfObjectBackuped(n)) {
						backupedRegions.add(wr);
					}
				}
				this.downloadedRegions = new ArrayList<>(downloadedRegions);
				this.backupedRegions = new ArrayList<>(backupedRegions);
			} else {
				downloadedRegions = new ArrayList<>(this.downloadedRegions);
				backupedRegions = new ArrayList<>(this.backupedRegions);
			}
		}

		WorldRegion selectedRegion = this.selectedRegion;
		if (zoom >= ZOOM_TO_SHOW_SELECTION_ST && zoom < ZOOM_TO_SHOW_SELECTION) {
			if (selectedRegion != null) {
				downloadedRegions.remove(selectedRegion);
				backupedRegions.remove(selectedRegion);
			}
		}
		if (backupedSize != backupedRegions.size()
				|| downloadedSize != downloadedRegions.size()
				|| hasSelectedRegion == (selectedRegion == null)) {
			clearPolygonsCollections();
			backupedSize = backupedRegions.size();
			downloadedSize = downloadedRegions.size();
			hasSelectedRegion = selectedRegion != null;
		}
		int baseOrder = getBaseOrder();
		if (zoom >= ZOOM_TO_SHOW_BORDERS_ST && zoom < ZOOM_TO_SHOW_BORDERS) {
			baseOrder = addToPolygonsCollection(downloadedRegions, paintDownloaded, baseOrder);
			baseOrder = addToPolygonsCollection(backupedRegions, paintBackuped, baseOrder);
		}
		if (zoom >= ZOOM_TO_SHOW_SELECTION_ST && zoom < ZOOM_TO_SHOW_SELECTION && selectedRegion != null) {
			addToPolygonsCollection(Collections.singletonList(selectedRegion), paintSelected, baseOrder);
		}
		if ((needRedrawOpenGL || mapActivityInvalidated) && polygonsCollection != null) {
			mapRenderer.addSymbolsProvider(polygonsCollection);
			needRedrawOpenGL = false;
		}
		mapActivityInvalidated = false;
	}

	/**OpenGL*/
	private int addToPolygonsCollection(List<WorldRegion> regionList, Paint paint, int baseOrder) {
		MapRendererView mapRenderer = getMapView().getMapRenderer();
		if (mapRenderer == null || regionList.isEmpty() || !needRedrawOpenGL) {
			return baseOrder;
		}
		if (polygonsCollection == null) {
			polygonsCollection = new PolygonsCollection(ZoomLevel.ZoomLevel3, ZoomLevel.ZoomLevel7);
		}
		for (WorldRegion region : regionList) {
			for (List<LatLon> polygon : region.getPolygons()) {
				QVectorPointI points = new QVectorPointI();
				for (LatLon latLon : polygon) {
					int x = MapUtils.get31TileNumberX(latLon.getLongitude());
					int y = MapUtils.get31TileNumberY(latLon.getLatitude());
					points.add(new PointI(x, y));
				}
				FColorARGB colorARGB = NativeUtilities.createFColorARGB(paint.getColor());
				PolygonBuilder polygonBuilder = new PolygonBuilder();
				polygonBuilder.setBaseOrder(baseOrder--)
						.setIsHidden(points.size() < 3)
						.setPolygonId(++polygonId)
						.setPoints(points)
						.setFillColor(colorARGB)
						.buildAndAddToCollection(polygonsCollection);
			}
		}
		return baseOrder;
	}

	/**OpenGL*/
	private void clearPolygonsCollections() {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		if (polygonsCollection != null) {
			mapRenderer.removeSymbolsProvider(polygonsCollection);
			polygonsCollection = null;
		}
		needRedrawOpenGL = true;
		hasSelectedRegion = false;
		polygonId = 1;
	}

	private void addMapsInitializedListener() {
		OsmandApplication app = getApplication();
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializeListener() {
				@Override
				public void onProgress(@NonNull AppInitializer init, @NonNull AppInitEvents event) {
					if (event == INDEX_REGION_BOUNDARIES) {
						indexRegionBoundaries = true;
					}
				}
			});
		} else {
			indexRegionBoundaries = true;
		}
	}

	private boolean isShowDownloadedMaps() {
		return app.getSettings().SHOW_BORDERS_OF_DOWNLOADED_MAPS.get() &&
				!PluginsHelper.layerShouldBeDisabled(this);
	}

	@Override
	public void onMapsIndexed() {
		onMapsChanged = true;
	}

	@Override
	public void onMapClosed(String fileName) {
		onMapsChanged = true;
	}
}
