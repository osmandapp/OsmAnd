package net.osmand.plus.views;

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

import net.osmand.IndexConstants;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.download.DownloadActivityType;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.mapcontextmenu.MapContextMenu;
import net.osmand.plus.mapcontextmenu.other.MapMultiSelectionMenu;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProvider;
import net.osmand.plus.views.ContextMenuLayer.IContextMenuProviderSelection;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class DownloadedRegionsLayer extends OsmandMapLayer implements IContextMenuProvider, IContextMenuProviderSelection {

	private static final int ZOOM_THRESHOLD = 2;

	private OsmandApplication app;
	private OsmandMapTileView view;
	private Paint paintDownloaded;
	private Path pathDownloaded;
	private Paint paintSelected;
	private Path pathSelected;
	private Paint paintDownloading;
	private Path pathDownloading;
	private Paint paintOutdated;
	private Path pathOutdated;
	private OsmandRegions osmandRegions;

	private TextPaint textPaint;
	private ResourceManager rm;

	private MapLayerData<List<BinaryMapDataObject>> data;
	private List<BinaryMapDataObject> outdatedObjects = new LinkedList<>();
	private List<BinaryMapDataObject> downloadingObjects = new LinkedList<>();
	private List<BinaryMapDataObject> selectedObjects = new LinkedList<>();

	private static int ZOOM_TO_SHOW_MAP_NAMES = 6;
	private static int ZOOM_AFTER_BASEMAP = 12;

	private static int ZOOM_TO_SHOW_BORDERS_ST = 4;
	private static int ZOOM_TO_SHOW_BORDERS = 7;
	private static int ZOOM_TO_SHOW_SELECTION_ST = 3;
	private static int ZOOM_TO_SHOW_SELECTION = 10;

	public static class DownloadMapObject {
		private BinaryMapDataObject dataObject;
		private WorldRegion worldRegion;
		private IndexItem indexItem;

		public BinaryMapDataObject getDataObject() {
			return dataObject;
		}

		public WorldRegion getWorldRegion() {
			return worldRegion;
		}

		public IndexItem getIndexItem() {
			return indexItem;
		}

		public DownloadMapObject(BinaryMapDataObject dataObject, WorldRegion worldRegion, IndexItem indexItem) {
			this.dataObject = dataObject;
			this.worldRegion = worldRegion;
			this.indexItem = indexItem;
		}
	}

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		app = view.getApplication();
		rm = app.getResourceManager();
		osmandRegions = rm.getOsmandRegions();

		paintDownloaded = getPaint(view.getResources().getColor(R.color.region_uptodate));
		paintOutdated = getPaint(view.getResources().getColor(R.color.region_updateable));
		paintSelected = getPaint(view.getResources().getColor(R.color.region_selected));
		paintDownloading = getPaint(view.getResources().getColor(R.color.region_downloading));

		textPaint = new TextPaint();
		final WindowManager wmgr = (WindowManager) view.getApplication().getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		wmgr.getDefaultDisplay().getMetrics(dm);
		textPaint.setStrokeWidth(21 * dm.scaledDensity);
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Paint.Align.CENTER);

		pathDownloaded = new Path();
		pathSelected = new Path();
		pathDownloading = new Path();
		pathOutdated = new Path();

		data = new MapLayerData<List<BinaryMapDataObject>>() {
			
			@Override
			public void layerOnPostExecute() {
				view.refreshMap();
			}
			
			public boolean queriedBoxContains(final RotatedTileBox queriedData, final RotatedTileBox newBox) {
				if (newBox.getZoom() < ZOOM_TO_SHOW_SELECTION) {
					if (queriedData != null && queriedData.getZoom() < ZOOM_TO_SHOW_SELECTION) {
						return queriedData.containsTileBox(newBox);
					} else {
						return false;
					}
				}
				List<BinaryMapDataObject> queriedResults = getResults();
				if(queriedData != null && queriedData.containsTileBox(newBox) && queriedData.getZoom() >= ZOOM_TO_SHOW_MAP_NAMES) {
					if(queriedResults != null && ( queriedResults.isEmpty() || 
							Math.abs(queriedData.getZoom() - newBox.getZoom()) <= 1)) {
						return true;
					}
				}
				return false;
			}
			
			@Override
			protected List<BinaryMapDataObject> calculateResult(RotatedTileBox tileBox) {
				return queryData(tileBox);
			}
			
		};

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
		final int zoom = tileBox.getZoom();
		if(zoom < ZOOM_TO_SHOW_SELECTION_ST) {
			return;
		}
		// draw objects
		if (osmandRegions.isInitialized() && zoom >= ZOOM_TO_SHOW_SELECTION_ST && zoom < ZOOM_TO_SHOW_SELECTION) {
			final List<BinaryMapDataObject> currentObjects = new LinkedList<>();
			if (data.results != null) {
				currentObjects.addAll(data.results);
			}
			final List<BinaryMapDataObject> downloadingObjects = new LinkedList<>(this.downloadingObjects);
			final List<BinaryMapDataObject> outdatedObjects = new LinkedList<>(this.outdatedObjects);
			final List<BinaryMapDataObject> selectedObjects = new LinkedList<>(this.selectedObjects);

			if (selectedObjects.size() > 0) {
				removeObjectsFromList(currentObjects, selectedObjects);
				drawBorders(canvas, tileBox, selectedObjects, pathSelected, paintSelected);
			}

			if (zoom >= ZOOM_TO_SHOW_BORDERS_ST && zoom < ZOOM_TO_SHOW_BORDERS) {
				removeObjectsFromList(downloadingObjects, selectedObjects);
				if (downloadingObjects.size() > 0) {
					removeObjectsFromList(currentObjects, downloadingObjects);
					drawBorders(canvas, tileBox, downloadingObjects, pathDownloading, paintDownloading);
				}
				removeObjectsFromList(outdatedObjects, selectedObjects);
				if (outdatedObjects.size() > 0) {
					removeObjectsFromList(currentObjects, outdatedObjects);
					drawBorders(canvas, tileBox, outdatedObjects, pathOutdated, paintOutdated);
				}
				if (currentObjects.size() > 0) {
					Iterator<BinaryMapDataObject> it = currentObjects.iterator();
					while (it.hasNext()) {
						BinaryMapDataObject o = it.next();
						boolean downloaded = checkIfObjectDownloaded(osmandRegions.getDownloadName(o));
						if (!downloaded) {
							it.remove();
						}
					}
					if (currentObjects.size() > 0) {
						drawBorders(canvas, tileBox, currentObjects, pathDownloaded, paintDownloaded);
					}
				}
			}
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

	private void drawBorders(Canvas canvas, RotatedTileBox tileBox, final List<BinaryMapDataObject> objects, Path path, Paint paint) {
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

	private boolean checkIfObjectDownloaded(String downloadName) {
		final String regionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName)
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		final String roadsRegionName = Algorithms.capitalizeFirstLetterAndLowercase(downloadName) + ".road"
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		return rm.getIndexFileNames().containsKey(regionName) || rm.getIndexFileNames().containsKey(roadsRegionName);
	}


	
	private List<BinaryMapDataObject> queryData(RotatedTileBox tileBox) {
		if (tileBox.getZoom() >= ZOOM_AFTER_BASEMAP) {
			if(!checkIfMapEmpty(tileBox)) {
				return Collections.emptyList();	
			}
		}
		LatLon lt = tileBox.getLeftTopLatLon();
		LatLon rb = tileBox.getRightBottomLatLon();
//		if (tileBox.getZoom() >= ZOOM_TO_SHOW_MAP_NAMES) {
//			lt = rb = tileBox.getCenterLatLon();
//		}
		
		List<BinaryMapDataObject> result = null;
		int left = MapUtils.get31TileNumberX(lt.getLongitude());
		int right = MapUtils.get31TileNumberX(rb.getLongitude());
		int top = MapUtils.get31TileNumberY(lt.getLatitude());
		int bottom = MapUtils.get31TileNumberY(rb.getLatitude());

		try {
			result = osmandRegions.queryBbox(left, right, top, bottom);
		} catch (IOException e) {
			return result;
		}

		Iterator<BinaryMapDataObject> it = result.iterator();
		while (it.hasNext()) {
			BinaryMapDataObject o = it.next();
			if (tileBox.getZoom() < ZOOM_TO_SHOW_SELECTION) {
				//
			} else {
				if (!osmandRegions.contain(o, left / 2 + right / 2, top / 2 + bottom / 2)) {
					it.remove();
					continue;
				}
			}
		}

		updateObjects(result);

		return result;
	}

	public boolean updateObjects() {
		int zoom = view.getZoom();
		if (osmandRegions.isInitialized() && data.results != null
				&& zoom >= ZOOM_TO_SHOW_SELECTION_ST && zoom < ZOOM_TO_SHOW_SELECTION) {
			return updateObjects(data.results);
		}
		return false;
	}

	private boolean updateObjects(List<BinaryMapDataObject> objects) {
		List<BinaryMapDataObject> outdatedObjects = new LinkedList<>();
		List<BinaryMapDataObject> downloadingObjects = new LinkedList<>();
		for (BinaryMapDataObject o : objects) {
			String fullName = osmandRegions.getFullName(o);
			WorldRegion region = osmandRegions.getRegionData(fullName);
			if (region != null && region.getRegionDownloadName() != null) {
				List<IndexItem> indexItems = app.getDownloadThread().getIndexes().getIndexItems(region);
				for (IndexItem item : indexItems) {
					if (item.getType() == DownloadActivityType.NORMAL_FILE
							|| item.getType() == DownloadActivityType.ROADS_FILE
							|| item.getType() == DownloadActivityType.SRTM_COUNTRY_FILE
							|| item.getType() == DownloadActivityType.HILLSHADE_FILE
							|| item.getType() == DownloadActivityType.WIKIPEDIA_FILE) {
						if (app.getDownloadThread().isDownloading(item)) {
							downloadingObjects.add(o);
						} else if (item.isOutdated()) {
							outdatedObjects.add(o);
						}
					}
				}
			}
		}

		boolean res = !this.downloadingObjects.equals(downloadingObjects)
				|| !this.outdatedObjects.equals(outdatedObjects);

		this.downloadingObjects = downloadingObjects;
		this.outdatedObjects = outdatedObjects;

		return res;
	}

	private boolean checkIfMapEmpty(RotatedTileBox tileBox) {
//		RotatedTileBox cb = rm.getRenderer().getCheckedBox();
		// old code to wait
		// wait for image to be rendered
//		int count = 0;
//		while (cb == null || cb.getZoom() != tileBox.getZoom() || 
//				!cb.containsLatLon(tileBox.getCenterLatLon())) {
//			if (count++ > 5) {
//				return false;
//			}
//			try {
//				Thread.sleep(1000);
//			} catch (InterruptedException e) {
//				return false;
//			}
//			cb = rm.getRenderer().getCheckedBox();
//		}
//		if (cb == null || cb.getZoom() != tileBox.getZoom() || 
//				!cb.containsLatLon(tileBox.getCenterLatLon())) {
//			return false;
//		}
		int cState = rm.getRenderer().getCheckedRenderedState();
		final boolean empty;
		if (tileBox.getZoom() < ZOOM_AFTER_BASEMAP) {
			empty = cState == 0;
		} else {
			empty = cState <= 1;
		}
		return empty;
	}

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		if(view.getMainLayer() instanceof MapTileLayer) {
			return;
		}
		// query from UI thread because of Android AsyncTask bug (Handler init)
		data.queryNewData(tileBox);
	}

	public String getFilter(StringBuilder btnName) {
		StringBuilder filter = new StringBuilder();
		int zoom = view.getZoom();
		RotatedTileBox queriedBox = data.getQueriedBox();
		final List<BinaryMapDataObject> currentObjects = data.results;
		if (osmandRegions.isInitialized() && queriedBox != null) {
			if(zoom >= ZOOM_TO_SHOW_MAP_NAMES && Math.abs(queriedBox.getZoom() - zoom) <= ZOOM_THRESHOLD &&
					currentObjects != null){
				btnName.setLength(0);
				btnName.append(view.getResources().getString(R.string.shared_string_download));
				filter.setLength(0);
				Set<String> set = new TreeSet<String>();
				int cx = view.getCurrentRotatedTileBox().getCenter31X();
				int cy = view.getCurrentRotatedTileBox().getCenter31Y();
				if ((currentObjects != null && currentObjects.size() > 0)) {
					for (int i = 0; i < currentObjects.size(); i++) {
						final BinaryMapDataObject o = currentObjects.get(i);
						if (!osmandRegions.contain(o, cx, cy)) {
							continue;
						}
						String fullName = osmandRegions.getFullName(o);
						WorldRegion rd = osmandRegions.getRegionData(fullName);
						if (rd != null && rd.isRegionMapDownload() && rd.getRegionDownloadName() != null) {
							String name = rd.getLocaleName();
							if (checkIfObjectDownloaded(rd.getRegionDownloadName())) {
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
	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}



	@Override
	public void destroyLayer() {
	}


	// IContextMenuProvider
	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> objects) {
		boolean isMenuVisible = false;
		if (view.getContext() instanceof MapActivity) {
			MapActivity mapActivity = (MapActivity) view.getContext();
			MapContextMenu menu = mapActivity.getContextMenu();
			MapMultiSelectionMenu multiMenu = menu.getMultiSelectionMenu();
			isMenuVisible = menu.isVisible() || multiMenu.isVisible();
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
	public String getObjectDescription(Object o) {
		return view.getContext().getString(R.string.shared_string_map);
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof DownloadMapObject) {
			DownloadMapObject mapObject = ((DownloadMapObject) o);
			return new PointDescription(PointDescription.POINT_TYPE_WORLD_REGION,
					view.getContext().getString(R.string.shared_string_map), mapObject.worldRegion.getLocaleName());
		}
		return new PointDescription(PointDescription.POINT_TYPE_WORLD_REGION,
				view.getContext().getString(R.string.shared_string_map), "");
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	private void getWorldRegionFromPoint(RotatedTileBox tb, PointF point, List<? super DownloadMapObject> dataObjects) {
		int zoom = tb.getZoom();
		if (zoom >= ZOOM_TO_SHOW_SELECTION_ST && zoom < ZOOM_TO_SHOW_SELECTION && osmandRegions.isInitialized()) {
			LatLon pointLatLon = tb.getLatLonFromPixel(point.x, point.y);
			int point31x = MapUtils.get31TileNumberX(pointLatLon.getLongitude());
			int point31y = MapUtils.get31TileNumberY(pointLatLon.getLatitude());

			List<BinaryMapDataObject> result = new LinkedList<>(data.results);
			Iterator<BinaryMapDataObject> it = result.iterator();
			while (it.hasNext()) {
				BinaryMapDataObject o = it.next();
				if (!osmandRegions.contain(o, point31x, point31y) ) {
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
							dataObjects.add(new DownloadMapObject(o, region, item));
						}
					} else {
						dataObjects.add(new DownloadMapObject(o, region, null));
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
}
