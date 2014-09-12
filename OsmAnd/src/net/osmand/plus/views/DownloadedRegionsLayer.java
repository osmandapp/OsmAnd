package net.osmand.plus.views;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.osmand.IndexConstants;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.data.RotatedTileBox;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.R;
import net.osmand.plus.download.DownloadActivity;
import net.osmand.plus.download.DownloadIndexFragment;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;
import android.content.Context;
import android.content.Intent;
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
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;

public class DownloadedRegionsLayer extends OsmandMapLayer {

	private static final int ZOOM_THRESHOLD = 2;

	private OsmandMapTileView view;

	private Paint paint;

	private Path path;

	private OsmandRegions osmandRegions;


	private boolean basemapExists = true;
	private boolean noMapsPresent = false;
	
	private TextPaint textPaint;
	private ResourceManager rm;
	private Button downloadBtn;
	private StringBuilder filter = new StringBuilder();

	private MapLayerData<List<BinaryMapDataObject>> data;

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

		FrameLayout fl = (FrameLayout) view.getParent();
		downloadBtn = new Button(view.getContext());
		downloadBtn.setVisibility(View.GONE);
		downloadBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent intent = new Intent(view.getContext(), view.getApplication().getAppCustomization().getDownloadIndexActivity());
				intent.putExtra(DownloadActivity.FILTER_KEY, filter.toString());
				view.getContext().startActivity(intent);
			}
		});


		final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER;
		fl.addView(downloadBtn, lp);


		path = new Path();
		
		data = new MapLayerData<List<BinaryMapDataObject>>() {
			{
				ZOOM_THRESHOLD = 2;
			}
			
			@Override
			public void layerOnPostExecute() {
				view.refreshMap();
			}
			public boolean queriedBoxContains(final RotatedTileBox queriedData, final RotatedTileBox newBox) {
				if (newBox.getZoom() < ZOOM_TO_SHOW_MAP_NAMES) {
					if (queriedData != null && queriedData.getZoom() < ZOOM_TO_SHOW_MAP_NAMES) {
						if (newBox.getZoom() >= ZOOM_TO_SHOW_BORDERS_ST && newBox.getZoom() < ZOOM_TO_SHOW_BORDERS) {
							return queriedData != null && queriedData.containsTileBox(newBox);
						}
						return true;
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
	private static int ZOOM_TO_SHOW_MAP_NAMES = 12;
	
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
				final String regionName = Algorithms.capitalizeFirstLetterAndLowercase(osmandRegions.getDownloadName(o))
						+ IndexConstants.BINARY_MAP_INDEX_EXT;
				final String roadsRegionName = Algorithms.capitalizeFirstLetterAndLowercase(osmandRegions.getDownloadName(o)) + "-roads"
						+ IndexConstants.BINARY_MAP_INDEX_EXT;
				if (!rm.getIndexFileNames().containsKey(regionName) && !rm.getIndexFileNames().containsKey(roadsRegionName)) {
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


	
	private List<BinaryMapDataObject> queryData(RotatedTileBox tileBox) {
		if (tileBox.getZoom() < ZOOM_TO_SHOW_MAP_NAMES) {
			basemapExists = rm.getRenderer().basemapExists();
		}
		// wait for image to be rendered
		int count = 0;
		RotatedTileBox cb = rm.getRenderer().getCheckedBox();
		while (cb == null || cb.getZoom() != tileBox.getZoom() || 
				!cb.containsLatLon(tileBox.getCenterLatLon())) {
			if (count++ > 7) {
				return null;
			}
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return null;
			}
			cb = rm.getRenderer().getCheckedBox();
		}
		int cState = rm.getRenderer().getCheckedRenderedState();
		final boolean empty;
		if (tileBox.getZoom() < ZOOM_TO_SHOW_MAP_NAMES) {
			empty = cState == 0;
		} else {
			empty = cState <= 1;
		}
		noMapsPresent = empty;
		if (!empty && tileBox.getZoom() >= ZOOM_TO_SHOW_MAP_NAMES) {
			return Collections.emptyList();
		}

		List<BinaryMapDataObject> result = null;
		int left = MapUtils.get31TileNumberX(tileBox.getLeftTopLatLon().getLongitude());
		int right = MapUtils.get31TileNumberX(tileBox.getRightBottomLatLon().getLongitude());
		int top = MapUtils.get31TileNumberY(tileBox.getLeftTopLatLon().getLatitude());
		int bottom = MapUtils.get31TileNumberY(tileBox.getRightBottomLatLon().getLatitude());

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

	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings nightMode) {
		final int zoom = tileBox.getZoom();
		if (downloadBtn.getVisibility() == View.VISIBLE) {
			downloadBtn.setVisibility(View.GONE);
		}
		if(view.getMainLayer() instanceof MapTileLayer) {
			return;
		}
		// query from UI thread because of Android AsyncTask bug (Handler init)
		data.queryNewData(tileBox);
		RotatedTileBox queriedBox = data.getQueriedBox();
		final List<BinaryMapDataObject> currentObjects = data.results;
		if (osmandRegions.isInitialized() && queriedBox != null) {
			if (zoom < ZOOM_TO_SHOW_MAP_NAMES && !basemapExists) {
				filter.setLength(0);
				filter.append("basemap");
				downloadBtn.setVisibility(View.VISIBLE);
				downloadBtn.setText(view.getResources().getString(R.string.download_files) + " "
						+ view.getResources().getString(R.string.base_world_map));
			} else if(zoom >= ZOOM_TO_SHOW_MAP_NAMES && noMapsPresent && Math.abs(queriedBox.getZoom() - zoom) <= ZOOM_THRESHOLD &&
					currentObjects != null){
				StringBuilder s = new StringBuilder(view.getResources().getString(R.string.download_files));
				filter.setLength(0);
				Set<String> set = new TreeSet<String>();
				if ((currentObjects != null && currentObjects.size() > 0)) {
					for (int i = 0; i < currentObjects.size(); i++) {
						final BinaryMapDataObject o = currentObjects.get(i);
						String name =  osmandRegions.getLocaleName(o); //Algorithms.capitalizeFirstLetterAndLowercase(o.getName());
						if (!set.add(name)) {
							continue;
						}
						if (set.size() > 1) {
							s.append(" ").append(view.getResources().getString(R.string.default_or)).append(" ");
							filter.append(", ");
						} else {
							s.append(" ");
						}
						filter.append(name);
						if (osmandRegions.getPrefix(o) != null) {
							name = Algorithms.capitalizeFirstLetterAndLowercase(osmandRegions.getPrefix(o)) + " "
									+ name;
						}
						s.append(name);
					}
				}
				downloadBtn.setVisibility(View.VISIBLE);
				downloadBtn.setText(s.toString());
			}
		}
	}

	


	@Override
	public void destroyLayer() {
		((FrameLayout)downloadBtn.getParent()).removeView(downloadBtn);
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


}
