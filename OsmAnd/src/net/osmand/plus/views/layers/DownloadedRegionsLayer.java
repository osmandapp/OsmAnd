package net.osmand.plus.views.layers;

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
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.binary.BinaryMapDataObject;
import net.osmand.binary.BinaryMapIndexReader.TagValuePair;
import net.osmand.core.android.MapRendererView;
import net.osmand.core.jni.FColorARGB;
import net.osmand.core.jni.PointI;
import net.osmand.core.jni.PolygonBuilder;
import net.osmand.core.jni.PolygonsCollection;
import net.osmand.core.jni.QVectorPointI;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.AppInitializer;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadResources;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.download.LocalIndexHelper;
import net.osmand.plus.download.LocalIndexInfo;
import net.osmand.plus.download.ui.DownloadMapToolbarController;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.resources.ResourceManager.ResourceListener;
import net.osmand.plus.utils.NativeUtilities;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.layers.ContextMenuLayer.IContextMenuProviderSelection;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.mapwidgets.TopToolbarController;
import net.osmand.plus.views.mapwidgets.TopToolbarController.TopToolbarControllerType;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class DownloadedRegionsLayer extends OsmandMapLayer implements IContextMenuProvider, IContextMenuProviderSelection,
		ResourceListener {

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
	private List<BinaryMapDataObject> selectedObjects = new LinkedList<>();

	private int lastCheckMapCx;
	private int lastCheckMapCy;
	private int lastCheckMapZoom;

	private static final int ZOOM_TO_SHOW_MAP_NAMES = 6;
	private static final int ZOOM_AFTER_BASEMAP = 12;

	private static final int ZOOM_TO_SHOW_BORDERS_ST = 4;
	private static final int ZOOM_TO_SHOW_BORDERS = 8;
	private static final int ZOOM_TO_SHOW_SELECTION_ST = 3;
	private static final int ZOOM_TO_SHOW_SELECTION = 8;
	private static final int ZOOM_MIN_TO_SHOW_DOWNLOAD_DIALOG = 9;
	private static final int ZOOM_MAX_TO_SHOW_DOWNLOAD_DIALOG = 11;

	//OpenGL
	private PolygonsCollection polygonsCollection;
	private int downloadedSize;
	private int selectedSize;
	private int backupedSize;
	private int polygonId = 1;
	private boolean needRedrawOpenGL;
	private boolean indexRegionBoundaries;
	private boolean onMapsChanged;
	List<WorldRegion> downloadedRegions;
	List<WorldRegion> backupedRegions;

	public static class DownloadMapObject {
		private final BinaryMapDataObject dataObject;
		private final WorldRegion worldRegion;
		private final IndexItem indexItem;
		private final LocalIndexInfo localIndexInfo;

		public BinaryMapDataObject getDataObject() {
			return dataObject;
		}

		public WorldRegion getWorldRegion() {
			return worldRegion;
		}

		public IndexItem getIndexItem() {
			return indexItem;
		}

		public LocalIndexInfo getLocalIndexInfo() {
			return localIndexInfo;
		}

		public DownloadMapObject(BinaryMapDataObject dataObject, WorldRegion worldRegion,
								 IndexItem indexItem, LocalIndexInfo localIndexInfo) {
			this.dataObject = dataObject;
			this.worldRegion = worldRegion;
			this.indexItem = indexItem;
			this.localIndexInfo = localIndexInfo;
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

		paintDownloaded = getPaint(getColor(R.color.region_uptodate));
		paintSelected = getPaint(getColor(R.color.region_selected));
		paintBackuped = getPaint(getColor(R.color.region_backuped));

		textPaint = new TextPaint();
		WindowManager wmgr = (WindowManager) view.getApplication().getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		wmgr.getDefaultDisplay().getMetrics(dm);
		textPaint.setStrokeWidth(21 * dm.scaledDensity);
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Paint.Align.CENTER);

		pathDownloaded = new Path();
		pathSelected = new Path();
		pathBackuped = new Path();

		data = new MapLayerData<List<BinaryMapDataObject>>() {

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
			protected List<BinaryMapDataObject> calculateResult(@NonNull QuadRect latLonBounds, int zoom) {
				return queryData(latLonBounds, zoom);
			}
		};
		addMapsInitializedListener();
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
		int zoom = tileBox.getZoom();
		if (zoom < ZOOM_TO_SHOW_SELECTION_ST || !indexRegionBoundaries) {
			return;
		}
		//make sure no maps are loaded for the location
		checkMapToDownload(tileBox, data.getResults());
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
			List<BinaryMapDataObject> selectedObjects = new LinkedList<>(this.selectedObjects);

			if (selectedObjects.size() > 0) {
				removeObjectsFromList(currentObjects, selectedObjects);
				drawBorders(canvas, tileBox, selectedObjects, pathSelected, paintSelected);
			}

			if (zoom >= ZOOM_TO_SHOW_BORDERS_ST && zoom < ZOOM_TO_SHOW_BORDERS) {
				if (currentObjects.size() > 0) {
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
					if (backupedObjects.size() > 0) {
						drawBorders(canvas, tileBox, backupedObjects, pathBackuped, paintBackuped);
					}
					if (downloadedObjects.size() > 0) {
						drawBorders(canvas, tileBox, downloadedObjects, pathDownloaded, paintDownloaded);
					}
				}
			}
		} else {
			clearPolygonsCollections();
		}
	}

	private void checkMapToDownload(RotatedTileBox tileBox, List<BinaryMapDataObject> currentObjects) {
		int zoom = tileBox.getZoom();
		int cx = tileBox.getCenter31X();
		int cy = tileBox.getCenter31Y();
		if (lastCheckMapCx == cx && lastCheckMapCy == cy && lastCheckMapZoom == zoom) {
			return;
		}
		lastCheckMapCx = cx;
		lastCheckMapCy = cy;
		lastCheckMapZoom = zoom;

		MapActivity mapActivity = getMapActivity();
		if (app.getSettings().SHOW_DOWNLOAD_MAP_DIALOG.get()
				&& mapActivity != null && mapActivity.getWidgetsVisibilityHelper().shouldShowDownloadMapWidget()
				&& zoom >= ZOOM_MIN_TO_SHOW_DOWNLOAD_DIALOG
				&& zoom <= ZOOM_MAX_TO_SHOW_DOWNLOAD_DIALOG
				&& !view.isAnimatingMapMove()
				&& currentObjects != null) {

			DownloadIndexesThread downloadThread = app.getDownloadThread();
			DownloadResources indexes = downloadThread.getIndexes();
			if (!indexes.getExternalMapFileNamesAt(cx, cy, zoom, false).isEmpty()) {
				hideDownloadMapToolbar();
				return;
			}
			Map<WorldRegion, BinaryMapDataObject> selectedObjects = new LinkedHashMap<>();
			for (int i = 0; i < currentObjects.size(); i++) {
				BinaryMapDataObject o = currentObjects.get(i);
				String fullName = osmandRegions.getFullName(o);
				WorldRegion regionData = osmandRegions.getRegionData(fullName);
				if (regionData != null && regionData.isRegionMapDownload()) {
					String regionDownloadName = regionData.getRegionDownloadName();
					if (regionDownloadName != null) {
						if (rm.checkIfObjectDownloaded(regionDownloadName)) {
							hideDownloadMapToolbar();
							return;
						} else {
							selectedObjects.put(regionData, o);
						}
					}
				}
			}

			IndexItem indexItem = null;
			String name = null;
			Map.Entry<WorldRegion, BinaryMapDataObject> res = app.getRegions().getSmallestBinaryMapDataObjectAt(selectedObjects);
			if (res != null && res.getKey() != null) {
				WorldRegion regionData  = res.getKey();
				List<IndexItem> indexItems = indexes.getIndexItems(regionData);
				if (indexItems.size() == 0) {
					if (!indexes.isDownloadedFromInternet && app.getSettings().isInternetConnectionAvailable()) {
						downloadThread.runReloadIndexFilesSilent();
					}
				} else {
					for (IndexItem item : indexItems) {
						if (item.getType() == DownloadActivityType.NORMAL_FILE
								&& !(item.isDownloaded() || downloadThread.isDownloading(item))) {
							indexItem = item;
							name = regionData.getLocaleName();
							break;
						}
					}
				}
			}
			if (indexItem != null && !Algorithms.isEmpty(name)) {
				showDownloadMapToolbar(indexItem, name);
			} else {
				hideDownloadMapToolbar();
			}
		} else {
			hideDownloadMapToolbar();
		}
	}

	private void showDownloadMapToolbar(@NonNull IndexItem indexItem, @NonNull String regionName) {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null && !regionName.equals(DownloadMapToolbarController.getLastProcessedRegionName())) {
			app.runInUIThread(() -> {
				if (!regionName.equals(DownloadMapToolbarController.getLastProcessedRegionName())) {
					TopToolbarController controller = mapActivity.getTopToolbarController(TopToolbarControllerType.DOWNLOAD_MAP);
					if (controller == null || !((DownloadMapToolbarController) controller).getRegionName().equals(regionName)) {
						controller = new DownloadMapToolbarController(mapActivity, indexItem, regionName);
						mapActivity.showTopToolbar(controller);
					}
				}
			});
		}
	}

	private void hideDownloadMapToolbar() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			app.runInUIThread(() -> mapActivity.hideTopToolbar(TopToolbarControllerType.DOWNLOAD_MAP));
		}
	}

	private void removeObjectsFromList(List<BinaryMapDataObject> list, List<BinaryMapDataObject> objects) {
		Iterator<BinaryMapDataObject> it = list.iterator();
		while (it.hasNext()) {
			BinaryMapDataObject o = it.next();
			for (BinaryMapDataObject obj : objects) {
				if (o.getId() == obj.getId()) {
					it.remove();
					break;
				}
			}
		}
	}

	private void drawBorders(Canvas canvas, RotatedTileBox tileBox, List<BinaryMapDataObject> objects, Path path, Paint paint) {
		path.reset();
		for (BinaryMapDataObject o : objects) {
			double lat = MapUtils.get31LatitudeY(o.getPoint31YTile(0));
			double lon = MapUtils.get31LongitudeX(o.getPoint31XTile(0));
			path.moveTo(tileBox.getPixXFromLonNoRot(lon), tileBox.getPixYFromLatNoRot(lat));
			for (int j = 1; j < o.getPointsLength(); j++) {
				lat = MapUtils.get31LatitudeY(o.getPoint31YTile(j));
				lon = MapUtils.get31LongitudeX(o.getPoint31XTile(j));
				path.lineTo(tileBox.getPixXFromLonNoRot(lon), tileBox.getPixYFromLatNoRot(lat));
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
			if(zoom >= ZOOM_TO_SHOW_MAP_NAMES && Math.abs(queriedBox.getZoom() - zoom) <= ZOOM_THRESHOLD &&
					currentObjects != null){
				btnName.setLength(0);
				btnName.append(view.getResources().getString(R.string.shared_string_download));
				filter.setLength(0);
				Set<String> set = new TreeSet<>();
				int cx = view.getCurrentRotatedTileBox().getCenter31X();
				int cy = view.getCurrentRotatedTileBox().getCenter31Y();
				if ((currentObjects.size() > 0)) {
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
		if(filter.length() == 0) {
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
		clearPolygonsCollections();
	}


	// IContextMenuProvider
	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects, boolean unknownLocation) {
		boolean isMenuVisible = false;
		MapActivity mapActivity = view.getMapActivity();
		if (mapActivity != null) {
			MapContextMenu menu = mapActivity.getContextMenu();
			MapMultiSelectionMenu multiMenu = menu.getMultiSelectionMenu();
			isMenuVisible = menu.isVisible() || (multiMenu != null && multiMenu.isVisible());
		}
		if (!isMenuVisible) {
			getWorldRegionFromPoint(tileBox, point, objects);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof DownloadMapObject) {
			DownloadMapObject mapObject = ((DownloadMapObject) o);
			return mapObject.worldRegion.getRegionCenter();
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof DownloadMapObject) {
			DownloadMapObject mapObject = ((DownloadMapObject) o);
			return new PointDescription(PointDescription.POINT_TYPE_WORLD_REGION,
					getContext().getString(R.string.shared_string_map), mapObject.worldRegion.getLocaleName());
		}
		return new PointDescription(PointDescription.POINT_TYPE_WORLD_REGION,
				getContext().getString(R.string.shared_string_map), "");
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return false;
	}

	@Override
	public boolean runExclusiveAction(Object o, boolean unknownLocation) {
		return false;
	}

	@Override
	public boolean showMenuAction(@Nullable Object o) {
		return false;
	}

	private void getWorldRegionFromPoint(RotatedTileBox tb, PointF point, List<? super DownloadMapObject> dataObjects) {
		int zoom = tb.getZoom();
		if (zoom >= ZOOM_TO_SHOW_SELECTION_ST && zoom < ZOOM_TO_SHOW_SELECTION
				&& data.getResults() != null && osmandRegions.isInitialized()) {
			LatLon pointLatLon = NativeUtilities.getLatLonFromPixel(getMapRenderer(), tb, point.x, point.y);
			int point31x = MapUtils.get31TileNumberX(pointLatLon.getLongitude());
			int point31y = MapUtils.get31TileNumberY(pointLatLon.getLatitude());

			List<BinaryMapDataObject> result = new LinkedList<>(data.getResults());
			Iterator<BinaryMapDataObject> it = result.iterator();
			while (it.hasNext()) {
				BinaryMapDataObject o = it.next();
				boolean isRegion = true;
				for (int i = 0; i < o.getTypes().length; i++) {
					TagValuePair tp = o.getMapIndex().decodeType(o.getTypes()[i]);
					if ("boundary".equals(tp.value)) {
						isRegion = false;
						break;
					}
				}
				if (!isRegion || !osmandRegions.contain(o, point31x, point31y) ) {
					it.remove();
				}
			}
			OsmandRegions osmandRegions = app.getRegions();
			for (BinaryMapDataObject o : result) {
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
							dataObjects.add(new DownloadMapObject(o, region, item, null));
						}
					} else {
						String downloadName = osmandRegions.getDownloadName(o);
						List<LocalIndexInfo> infos = helper.getLocalIndexInfos(downloadName);
						if (infos.size() == 0) {
							dataObjects.add(new DownloadMapObject(o, region, null, null));
						} else {
							for (LocalIndexInfo info : infos) {
								dataObjects.add(new DownloadMapObject(o, region, null, info));
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
		if (o instanceof DownloadMapObject) {
			DownloadMapObject mapObject = ((DownloadMapObject) o);
			order = mapObject.worldRegion.getLevel() * 1000 - 100000;
			if (mapObject.indexItem != null) {
				order += mapObject.indexItem.getType().getOrderIndex();
			} else if (mapObject.localIndexInfo != null) {
				order += mapObject.localIndexInfo.getType().getOrderIndex(mapObject.localIndexInfo);
			}
		}
		return order;
	}

	@Override
	public void setSelectedObject(Object o) {
		if (o instanceof DownloadMapObject) {
			DownloadMapObject mapObject = ((DownloadMapObject) o);
			List<BinaryMapDataObject> list = new LinkedList<>();
			list.add(mapObject.dataObject);
			selectedObjects = list;
		}
	}

	@Override
	public void clearSelectedObject() {
		selectedObjects = new LinkedList<>();
	}

	/**OpenGL*/
	private void drawMapPolygons(int zoom) {
		MapRendererView mapRenderer = getMapRenderer();
		if (mapRenderer == null) {
			return;
		}
		if (onMapsChanged) {
			clearPolygonsCollections();
			onMapsChanged = false;
			downloadedRegions = null;
			backupedRegions = null;
		}
		if (polygonsCollection != null && selectedSize == selectedObjects.size()) {
			return;
		}
		List<WorldRegion> downloadedRegions = new ArrayList<>();
		List<WorldRegion> backupedRegions = new ArrayList<>();
		if (zoom >= ZOOM_TO_SHOW_BORDERS_ST && zoom < ZOOM_TO_SHOW_BORDERS) {
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
		List<WorldRegion> selectedRegions = new ArrayList<>();
		if (zoom >= ZOOM_TO_SHOW_SELECTION_ST && zoom < ZOOM_TO_SHOW_SELECTION) {
			if (selectedObjects.size() > 0) {
				for (BinaryMapDataObject o : selectedObjects) {
					String fullName = osmandRegions.getFullName(o);
					WorldRegion wr = osmandRegions.getRegionData(fullName);
					if (wr != null) {
						selectedRegions.add(wr);
						downloadedRegions.remove(wr);
						backupedRegions.remove(wr);
					}
				}
			}
		}
		if (backupedSize != backupedRegions.size()
				|| downloadedSize != downloadedRegions.size()
				|| selectedSize != selectedRegions.size()) {
			clearPolygonsCollections();
			backupedSize = backupedRegions.size();
			downloadedSize = downloadedRegions.size();
			selectedSize = selectedRegions.size();
		}
		int baseOrder = getBaseOrder();
		if (zoom >= ZOOM_TO_SHOW_BORDERS_ST && zoom < ZOOM_TO_SHOW_BORDERS) {
			baseOrder = addToPolygonsCollection(downloadedRegions, paintDownloaded, baseOrder);
			baseOrder = addToPolygonsCollection(backupedRegions, paintBackuped, baseOrder);
		}
		if (zoom >= ZOOM_TO_SHOW_SELECTION_ST && zoom < ZOOM_TO_SHOW_SELECTION) {
			addToPolygonsCollection(selectedRegions, paintSelected, baseOrder);
		}
		if (needRedrawOpenGL && polygonsCollection != null) {
			mapRenderer.addSymbolsProvider(polygonsCollection);
			needRedrawOpenGL = false;
		}
	}

	/**OpenGL*/
	private int addToPolygonsCollection(List<WorldRegion> regionList, Paint paint, int baseOrder) {
		MapRendererView mapRenderer = getMapView().getMapRenderer();
		if (mapRenderer == null || regionList.size() == 0 || !needRedrawOpenGL) {
			return baseOrder;
		}
		if (polygonsCollection == null) {
			polygonsCollection = new PolygonsCollection();
		}
		for (WorldRegion wr : regionList) {
			List<LatLon> polygon = wr.getPolygon();
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
		selectedSize = 0;
		polygonId = 1;
	}

	private void addMapsInitializedListener() {
		OsmandApplication app = getApplication();
		if (app.isApplicationInitializing()) {
			app.getAppInitializer().addListener(new AppInitializer.AppInitializeListener() {
				@Override
				public void onStart(AppInitializer init) {
				}

				@Override
				public void onProgress(AppInitializer init, AppInitializer.InitEvents event) {
					if (event == AppInitializer.InitEvents.INDEX_REGION_BOUNDARIES) {
						indexRegionBoundaries = true;
					}
				}

				@Override
				public void onFinish(AppInitializer init) {
				}
			});
		} else {
			indexRegionBoundaries = true;
		}
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
