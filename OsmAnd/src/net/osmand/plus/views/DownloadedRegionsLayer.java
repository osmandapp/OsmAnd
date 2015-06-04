package net.osmand.plus.views;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.osmand.IndexConstants;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.R;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class DownloadedRegionsLayer extends OsmandMapLayer {

	private static final int ZOOM_THRESHOLD = 2;

	private OsmandMapTileView view;
	private Paint paint;
	private Path path;
	private OsmandRegions osmandRegions;

	
	private TextPaint textPaint;
	private ResourceManager rm;

	private MapLayerData<List<BinaryMapDataObject>> data;
	
	private static int ZOOM_TO_SHOW_MAP_NAMES = 6;
	private static int ZOOM_AFTER_BASEMAP = 12;

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		rm = view.getApplication().getResourceManager();
		osmandRegions = rm.getOsmandRegions();

		paint = new Paint();
		paint.setStyle(Style.FILL_AND_STROKE);
		paint.setStrokeWidth(1);
		paint.setColor(Color.argb(100, 50, 200, 50));
		paint.setAntiAlias(true);
		paint.setStrokeCap(Cap.ROUND);
		paint.setStrokeJoin(Join.ROUND);

		textPaint = new TextPaint();
		final WindowManager wmgr = (WindowManager) view.getApplication().getSystemService(Context.WINDOW_SERVICE);
		DisplayMetrics dm = new DisplayMetrics();
		wmgr.getDefaultDisplay().getMetrics(dm);
		textPaint.setStrokeWidth(21 * dm.scaledDensity);
		textPaint.setAntiAlias(true);
		textPaint.setTextAlign(Paint.Align.CENTER);

		path = new Path();
		data = new MapLayerData<List<BinaryMapDataObject>>() {
			
			@Override
			public void layerOnPostExecute() {
				view.refreshMap();
			}
			public boolean queriedBoxContains(final RotatedTileBox queriedData, final RotatedTileBox newBox) {
				if (newBox.getZoom() < ZOOM_TO_SHOW_BORDERS) {
					if (queriedData != null && queriedData.getZoom() < ZOOM_TO_SHOW_BORDERS) {
						return queriedData != null && queriedData.containsTileBox(newBox);
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
	private static int ZOOM_TO_SHOW_BORDERS_ST = 5;
	private static int ZOOM_TO_SHOW_BORDERS = 7;
	
	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		final int zoom = tileBox.getZoom();
		if(zoom < ZOOM_TO_SHOW_BORDERS_ST) {
			return;
		}
		// draw objects
		final List<BinaryMapDataObject> currentObjects = data.results;
		if (zoom >= ZOOM_TO_SHOW_BORDERS_ST && zoom < ZOOM_TO_SHOW_BORDERS && osmandRegions.isInitialized() &&
				currentObjects != null) {
			path.reset();
			for (BinaryMapDataObject o : currentObjects) {
				boolean downloaded = checkIfObjectDownloaded(o);
				if (!downloaded) {
					continue;
				}
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
	}



	private boolean checkIfObjectDownloaded(BinaryMapDataObject o) {
		final String regionName = Algorithms.capitalizeFirstLetterAndLowercase(osmandRegions.getDownloadName(o))
				+ IndexConstants.BINARY_MAP_INDEX_EXT;
		final String roadsRegionName = Algorithms.capitalizeFirstLetterAndLowercase(osmandRegions.getDownloadName(o)) + "-roads"
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
			if (tileBox.getZoom() < ZOOM_TO_SHOW_BORDERS) {
				//
			} else {
				if (!osmandRegions.contain(o, left / 2 + right / 2, top / 2 + bottom / 2)) {
					it.remove();
				}
			}
		}
		return result;
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
				int cx = queriedBox.getCenter31X();
				int cy = queriedBox.getCenter31Y();
				if ((currentObjects != null && currentObjects.size() > 0)) {
					for (int i = 0; i < currentObjects.size(); i++) {
						// TODO regions=boundary
						final BinaryMapDataObject o = currentObjects.get(i);
						if (!osmandRegions.contain(o, cx, cy)) {
							continue;
						}
						if(checkIfObjectDownloaded(o)) {
							return null;
						}
						String name =  osmandRegions.getLocaleName(o); //Algorithms.capitalizeFirstLetterAndLowercase(o.getName());
						if (!set.add(name)) {
							continue;
						}
						if (set.size() > 1) {
							btnName.append(" ").append(view.getResources().getString(R.string.shared_string_or)).append(" ");
							filter.append(", ");
						} else {
							btnName.append(" ");
						}
						filter.append(name);
//						String parent = osmandRegions.getParentFullName(o);
//						if (osmandRegions.getParentFullName(o) != null) {
//							name = Algorithms.capitalizeFirstLetterAndLowercase(osmandRegions.getPrefix(o)) + " "
//									+ name;
//						}
						btnName.append(name);
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
	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return false;
	}



	@Override
	public void destroyLayer() {
	}


}
