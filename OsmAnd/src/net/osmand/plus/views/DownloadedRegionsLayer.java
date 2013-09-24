package net.osmand.plus.views;

import android.content.Context;
import android.content.Intent;
import android.graphics.*;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.os.AsyncTask;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import net.osmand.IndexConstants;
import net.osmand.binary.BinaryMapDataObject;
import net.osmand.map.OsmandRegions;
import net.osmand.plus.GPXUtilities.GPXFile;
import net.osmand.plus.GPXUtilities.WptPt;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.DownloadIndexActivity;
import net.osmand.plus.activities.OsmandIntents;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.io.IOException;
import java.util.*;

public class DownloadedRegionsLayer extends OsmandMapLayer {

	private OsmandMapTileView view;

	private Paint paint;

	private Path path;

	private OsmandSettings settings;

	private OsmandRegions osmandRegions;



	private List<BinaryMapDataObject> objectsToDraw = new ArrayList<BinaryMapDataObject>();
	private RectF queriedBBox = new RectF();
	private int queriedZoom = 0;
	private boolean basemapExists = true;
	private boolean noMapsPresent = false;
	private TextPaint textPaint;
	private AsyncTask<?, ?, ?> currentTask = null;
	private AsyncTask<?, ?, ?> pendingTask = null;
	private ResourceManager rm;
	private Button downloadBtn;
	private StringBuilder filter = new StringBuilder();

	@Override
	public void initLayer(final OsmandMapTileView view) {
		this.view = view;
		settings = view.getSettings();
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
				final Intent intent = new Intent(view.getContext(), OsmandIntents.getDownloadIndexActivity());
				intent.putExtra(DownloadIndexActivity.FILTER_KEY, filter.toString());
				view.getContext().startActivity(intent);
			}
		});


		final FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
				ViewGroup.LayoutParams.WRAP_CONTENT);
		lp.gravity = Gravity.CENTER;
		fl.addView(downloadBtn, lp);


		path = new Path();

	}


	private static int ZOOM_TO_SHOW_BORDERS_ST = 5;
	private static int ZOOM_TO_SHOW_BORDERS = 7;
	private static int ZOOM_TO_SHOW_MAP_NAMES = 12;

	@Override
	public void onDraw(Canvas canvas, RectF latLonBox, RectF tilesRect, DrawSettings nightMode) {
		final int zoom = view.getZoom();
		if(downloadBtn.getVisibility() == View.VISIBLE) {
			downloadBtn.setVisibility(View.GONE);
		}
		if (zoom >= ZOOM_TO_SHOW_BORDERS_ST && (zoom < ZOOM_TO_SHOW_BORDERS || zoom >= ZOOM_TO_SHOW_MAP_NAMES) &&
				osmandRegions.isInitialized()) {
			if (!queriedBBox.contains(latLonBox) || Math.abs(queriedZoom - zoom) > 2) {
				float w = Math.abs(latLonBox.width() / 2);
				float h = Math.abs(latLonBox.height() / 2);
				final RectF rf = new RectF(latLonBox.left - w, latLonBox.top + h, latLonBox.right + w, latLonBox.bottom - h);
				AsyncTask<Object, Object, List<BinaryMapDataObject>> task = new AsyncTask<Object, Object, List<BinaryMapDataObject>>() {
					@Override
					protected List<BinaryMapDataObject> doInBackground(Object... params) {
						if (queriedBBox.contains(rf)) {
							return null;
						}
						if(zoom < ZOOM_TO_SHOW_MAP_NAMES) {
							basemapExists = rm.getRenderer().basemapExists();
						}
						List<BinaryMapDataObject> result = null;
						int left = MapUtils.get31TileNumberX(rf.left);
						int right = MapUtils.get31TileNumberX(rf.right);
						int top = MapUtils.get31TileNumberY(rf.top);
						int bottom = MapUtils.get31TileNumberY(rf.bottom);
						final boolean empty = rm.getRenderer().checkIfMapIsEmpty(left, right, top, bottom, zoom);
						noMapsPresent = empty;
						if(!empty && zoom >= ZOOM_TO_SHOW_MAP_NAMES) {
							return Collections.emptyList();
						}
						try {
							result = osmandRegions.queryBbox(left, right, top, bottom);
						} catch (IOException e) {
							return result;
						}
						Iterator<BinaryMapDataObject> it = result.iterator();
						while (it.hasNext()) {
							BinaryMapDataObject o = it.next();
							if (zoom < ZOOM_TO_SHOW_BORDERS) {
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
					protected void onPreExecute() {
						currentTask = this;
					}

					@Override
					protected void onPostExecute(List<BinaryMapDataObject> result) {
						if (result != null) {
							queriedBBox = rf;
							objectsToDraw = result;
							queriedZoom = zoom;
						}
						currentTask = null;
						if (pendingTask != null) {
							pendingTask.execute();
							pendingTask = null;
						}
					}
				};
				if (currentTask == null) {
					task.execute();
				} else {
					pendingTask = task;
				}
			}
			final List<BinaryMapDataObject> currentObjects = objectsToDraw;
			if ((currentObjects != null && currentObjects.size() > 0) || noMapsPresent) {
				if (zoom >= ZOOM_TO_SHOW_MAP_NAMES) {
					StringBuilder s = new StringBuilder(view.getResources().getString(R.string.download_files));
					filter.setLength(0);
					if ((currentObjects != null && currentObjects.size() > 0)) {
						for (int i = 0; i < currentObjects.size(); i++) {
							if (i > 0) {
								s.append(" & ");
							} else {
								s.append(" ");
							}
							final BinaryMapDataObject o = currentObjects.get(i);
							String string = Algorithms.capitalizeFirstLetterAndLowercase(o.getName());
							filter.append(string + " ");
							if (osmandRegions.getPrefix(o) != null) {
								string = Algorithms.capitalizeFirstLetterAndLowercase(osmandRegions.getPrefix(o)) + " " + string;
							}
							s.append(string);
						}
					}
					downloadBtn.setVisibility(View.VISIBLE);
					downloadBtn.setText(s.toString());
				} else {
					if(!basemapExists) {
						filter.setLength(0);
						filter.append("basemap");
						downloadBtn.setVisibility(View.VISIBLE);
						downloadBtn.setText(view.getResources().getString(R.string.download_files) + " " +
								view.getResources().getString(R.string.base_world_map));
					}
					for (BinaryMapDataObject o : currentObjects) {
						final String key = Algorithms.capitalizeFirstLetterAndLowercase(osmandRegions.getDownloadName(o)) +
								IndexConstants.BINARY_MAP_INDEX_EXT;
						if (!rm.getIndexFileNames().containsKey(key)) {
							continue;
						}
						path.reset();
						double lat = MapUtils.get31LatitudeY(o.getPoint31YTile(0));
						double lon = MapUtils.get31LongitudeX(o.getPoint31XTile(0));
						path.moveTo(view.getRotatedMapXForPoint(lat, lon), view.getRotatedMapYForPoint(lat, lon));
						for(int j = 1 ; j < o.getPointsLength(); j++) {
							lat = MapUtils.get31LatitudeY(o.getPoint31YTile(j));
							lon = MapUtils.get31LongitudeX(o.getPoint31XTile(j));
							path.lineTo(view.getRotatedMapXForPoint(lat, lon),
									view.getRotatedMapYForPoint(lat, lon));
						}
						canvas.drawPath(path, paint);
					}
				}

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
	public boolean onLongPressEvent(PointF point) {
		return false;
	}

	@Override
	public boolean onSingleTap(PointF point) {
		return false;
	}


}
