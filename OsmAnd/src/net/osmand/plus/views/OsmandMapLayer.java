package net.osmand.plus.views;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Paint.Join;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.view.MotionEvent;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.FileUtils;
import net.osmand.GPXUtilities;
import net.osmand.PlatformUtil;
import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.mapcontextmenu.controllers.SelectedGpxMenuController;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.TrackMenuFragment;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public abstract class OsmandMapLayer {

	public static final float ICON_VISIBLE_PART_RATIO = 0.45f;
	protected List<LatLon> fullObjectsLatLon;
	protected List<LatLon> smallObjectsLatLon;
	private static final Log log = PlatformUtil.getLog(OsmandMapLayer.class);

	public enum MapGestureType {
		DOUBLE_TAP_ZOOM_IN,
		DOUBLE_TAP_ZOOM_CHANGE,
		TWO_POINTERS_ZOOM_OUT
	}

	public boolean isMapGestureAllowed(MapGestureType type) {
		return true;
	}

	public abstract void initLayer(OsmandMapTileView view);

	public abstract void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings);

	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
	}

	public abstract void destroyLayer();

	public void onRetainNonConfigurationInstance(Map<String, Object> map) {
	}

	public void populateObjectContextMenu(LatLon latLon, Object o, ContextMenuAdapter adapter, MapActivity mapActivity) {
	}

	public boolean onSingleTap(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	public boolean onLongPressEvent(PointF point, RotatedTileBox tileBox) {
		return false;
	}

	public boolean onTouchEvent(MotionEvent event, RotatedTileBox tileBox) {
		return false;
	}


	public <Params> void executeTaskInBackground(AsyncTask<Params, ?, ?> task, Params... params) {
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
	}

	public boolean isPresentInFullObjects(LatLon latLon) {
		if (fullObjectsLatLon == null) {
			return true;
		} else if (latLon != null) {
			return fullObjectsLatLon.contains(latLon);
		}
		return false;
	}

	public boolean isPresentInSmallObjects(LatLon latLon) {
		if (smallObjectsLatLon != null && latLon != null) {
			return smallObjectsLatLon.contains(latLon);
		}
		return false;
	}

	/**
	 * This method returns whether canvas should be rotated as
	 * map rotated before {@link #onDraw(android.graphics.Canvas, net.osmand.data.RotatedTileBox, net.osmand.plus.views.OsmandMapLayer.DrawSettings)}.
	 * If the layer draws simply layer over screen (not over map)
	 * it should return true.
	 */
	public abstract boolean drawInScreenPixels();

	public static class DrawSettings {

		public long mapRefreshTimestamp;

		private final boolean nightMode;
		private final boolean updateVectorRendering;

		public DrawSettings(boolean nightMode) {
			this(nightMode, false);
		}

		public DrawSettings(boolean nightMode, boolean updateVectorRendering) {
			this.nightMode = nightMode;
			this.updateVectorRendering = updateVectorRendering;
		}

		public boolean isUpdateVectorRendering() {
			return updateVectorRendering;
		}

		public boolean isNightMode() {
			return nightMode;
		}
	}

	@NonNull
	public static QuadTree<QuadRect> initBoundIntersections(RotatedTileBox tileBox) {
		QuadRect bounds = new QuadRect(0, 0, tileBox.getPixWidth(), tileBox.getPixHeight());
		bounds.inset(-bounds.width() / 4, -bounds.height() / 4);
		return new QuadTree<>(bounds, 4, 0.6f);
	}

	public static boolean intersects(QuadTree<QuadRect> boundIntersections, float x, float y, float width, float height) {
		List<QuadRect> result = new ArrayList<>();
		QuadRect visibleRect = calculateRect(x, y, width, height);
		boundIntersections.queryInBox(new QuadRect(visibleRect.left, visibleRect.top, visibleRect.right, visibleRect.bottom), result);
		for (QuadRect r : result) {
			if (QuadRect.intersects(r, visibleRect)) {
				return true;
			}
		}
		boundIntersections.insert(visibleRect,
				new QuadRect(visibleRect.left, visibleRect.top, visibleRect.right, visibleRect.bottom));
		return false;
	}

	public QuadRect getCorrectedQuadRect(QuadRect latlonRect) {
		double topLatitude = latlonRect.top;
		double leftLongitude = latlonRect.left;
		double bottomLatitude = latlonRect.bottom;
		double rightLongitude = latlonRect.right;
		// double lat = 0;
		// double lon = 0;
		// this is buggy lat/lon should be 0 but in that case
		// it needs to be fixed in case there is no route points in the view bbox
		double lat = topLatitude - bottomLatitude + 0.1;
		double lon = rightLongitude - leftLongitude + 0.1;
		return new QuadRect(leftLongitude - lon, topLatitude + lat, rightLongitude + lon, bottomLatitude - lat);
	}

	public static QuadRect calculateRect(float x, float y, float width, float height) {
		QuadRect rf;
		double left = x - width / 2.0d;
		double top = y - height / 2.0d;
		double right = left + width;
		double bottom = top + height;
		rf = new QuadRect(left, top, right, bottom);
		return rf;
	}

	public Amenity findAmenity(OsmandApplication app, long id, List<String> names, LatLon latLon, int radius) {
		QuadRect rect = MapUtils.calculateLatLonBbox(latLon.getLatitude(), latLon.getLongitude(), radius);
		List<Amenity> amenities = app.getResourceManager().searchAmenities(
				new BinaryMapIndexReader.SearchPoiTypeFilter() {
					@Override
					public boolean accept(PoiCategory type, String subcategory) {
						return true;
					}

					@Override
					public boolean isEmpty() {
						return false;
					}
				}, rect.top, rect.left, rect.bottom, rect.right, -1, null);

		Amenity res = null;
		for (Amenity amenity : amenities) {
			Long amenityId = amenity.getId() >> 1;
			if (amenityId == id && !amenity.isClosed()) {
				res = amenity;
				break;
			}
		}
		if (res == null && names != null && names.size() > 0) {
			for (Amenity amenity : amenities) {
				for (String name : names) {
					if (name.equals(amenity.getName()) && !amenity.isClosed()) {
						res = amenity;
						break;
					}
				}
				if (res != null) {
					break;
				}
			}
		}

		return res;
	}

	public int getDefaultRadiusPoi(RotatedTileBox tb) {
		int r;
		final double zoom = tb.getZoom();
		if (zoom <= 15) {
			r = 10;
		} else if (zoom <= 16) {
			r = 14;
		} else if (zoom <= 17) {
			r = 16;
		} else {
			r = 18;
		}
		return (int) (r * tb.getDensity());
	}

	protected float getIconSize(OsmandApplication app) {
		return app.getResources().getDimensionPixelSize(R.dimen.favorites_icon_outline_size) * ICON_VISIBLE_PART_RATIO * app.getSettings().TEXT_SCALE.get();
	}

	public Rect getIconDestinationRect(float x, float y, int width, int height, float scale) {
		int scaledWidth = width;
		int scaledHeight = height;
		if (scale != 1.0f) {
			scaledWidth = (int) (width * scale);
			scaledHeight = (int) (height * scale);
		}
		Rect rect = new Rect(0, 0, scaledWidth, scaledHeight);
		rect.offset((int) x - scaledWidth / 2, (int) y - scaledHeight / 2);
		return rect;
	}

	public int getScaledTouchRadius(OsmandApplication app, int radiusPoi) {
		float textScale = app.getSettings().TEXT_SCALE.get();
		if (textScale < 1.0f) {
			textScale = 1.0f;
		}
		return (int) textScale * radiusPoi;
	}

	public void setMapButtonIcon(ImageView imageView, Drawable icon) {
		int btnSizePx = imageView.getLayoutParams().height;
		int iconSizePx = imageView.getContext().getResources().getDimensionPixelSize(R.dimen.map_widget_icon);
		int iconPadding = (btnSizePx - iconSizePx) / 2;
		imageView.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
		imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
		imageView.setImageDrawable(icon);
	}

	public TravelHelper.GpxReadCallback gpxReadListener(MapActivity mapActivity, String gpxFileName, LatLon latLon) {
		return new TravelHelper.GpxReadCallback() {
			@Override
			public void onGpxFileReading() {
			}

			@Override
			public void onGpxFileRead(@Nullable GPXUtilities.GPXFile gpxFile) {
				if (gpxFile != null) {
					saveGpx(mapActivity, gpxFile, gpxFileName, latLon);
				}
			}
		};
	}

	private void saveGpx(MapActivity mapActivity, @NonNull GPXUtilities.GPXFile gpxFile, String gpxFileName, LatLon latLon) {
		OsmandApplication app = mapActivity.getMyApplication();
		File file = new File(FileUtils.getTempDir(app), gpxFileName);
		new SaveGpxAsyncTask(file, gpxFile, new SaveGpxAsyncTask.SaveGpxListener() {
			@Override
			public void gpxSavingStarted() {

			}

			@Override
			public void gpxSavingFinished(Exception errorMessage) {
				if (errorMessage == null) {
					GPXUtilities.WptPt selectedPoint = new GPXUtilities.WptPt();
					selectedPoint.lat = latLon.getLatitude();
					selectedPoint.lon = latLon.getLongitude();
					app.getSelectedGpxHelper().selectGpxFile(gpxFile, true, false);
					GpxSelectionHelper.SelectedGpxFile selectedGpxFile = app.getSelectedGpxHelper().selectGpxFile(gpxFile, true, false);
					SelectedGpxMenuController.SelectedGpxPoint selectedGpxPoint = new SelectedGpxMenuController.SelectedGpxPoint(selectedGpxFile, selectedPoint, null, null, Float.NaN);
					TrackMenuFragment.showInstance(mapActivity, selectedGpxFile, selectedGpxPoint);
				} else {
					log.error(errorMessage);
				}
			}
		}).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public abstract class MapLayerData<T> {
		public int ZOOM_THRESHOLD = 1;
		public RotatedTileBox queriedBox;
		protected T results;
		protected Task currentTask;
		protected Task pendingTask;

		public RotatedTileBox getQueriedBox() {
			return queriedBox;
		}

		public T getResults() {
			return results;
		}

		public boolean queriedBoxContains(final RotatedTileBox queriedData, final RotatedTileBox newBox) {
			return queriedData != null && queriedData.containsTileBox(newBox) && Math.abs(queriedData.getZoom() - newBox.getZoom()) <= ZOOM_THRESHOLD;
		}

		public void queryNewData(RotatedTileBox tileBox) {
			if (!queriedBoxContains(queriedBox, tileBox)) {
				Task ct = currentTask;
				if (ct == null || !queriedBoxContains(ct.getDataBox(), tileBox)) {
					RotatedTileBox original = tileBox.copy();
					RotatedTileBox extended = original.copy();
					extended.increasePixelDimensions(tileBox.getPixWidth() / 2, tileBox.getPixHeight() / 2);
					Task task = new Task(original, extended);
					if (currentTask == null) {
						executeTaskInBackground(task);
					} else {
						pendingTask = task;
					}
				}

			}
		}

		public void layerOnPreExecute() {
		}

		public void layerOnPostExecute() {
		}

		public boolean isInterrupted() {
			return pendingTask != null;
		}

		protected abstract T calculateResult(RotatedTileBox tileBox);

		public class Task extends AsyncTask<Object, Object, T> {
			private RotatedTileBox dataBox;
			private RotatedTileBox requestedBox;

			public Task(RotatedTileBox requestedBox, RotatedTileBox dataBox) {
				this.requestedBox = requestedBox;
				this.dataBox = dataBox;
			}

			public RotatedTileBox getOriginalBox() {
				return requestedBox;
			}

			public RotatedTileBox getDataBox() {
				return dataBox;
			}

			@Override
			protected T doInBackground(Object... params) {
				if (queriedBoxContains(queriedBox, dataBox)) {
					return null;
				}
				return calculateResult(dataBox);
			}

			@Override
			protected void onPreExecute() {
				currentTask = this;
				layerOnPreExecute();
			}

			@Override
			protected void onPostExecute(T result) {
				if (result != null) {
					queriedBox = dataBox;
					results = result;
				}
				currentTask = null;
				if (pendingTask != null) {
					executeTaskInBackground(pendingTask);
					pendingTask = null;
				} else {
					layerOnPostExecute();
				}
			}
		}

		public void clearCache() {
			results = null;
			queriedBox = null;

		}

	}

	public static class RenderingLineAttributes {
		protected int cachedHash;
		public Paint paint;
		public Paint customColorPaint;
		public int customColor = 0;
		public float customWidth = 0;
		public int defaultWidth = 0;
		public int defaultColor = 0;
		public boolean isPaint2;
		public Paint paint2;
		public int defaultWidth2 = 0;
		public boolean isPaint3;
		public Paint paint3;
		public int defaultWidth3 = 0;
		public Paint shadowPaint;
		public boolean isShadowPaint;
		public int defaultShadowWidthExtent = 2;
		public Paint paint_1;
		public boolean isPaint_1;
		public int defaultWidth_1 = 0;
		private String renderingAttribute;

		public RenderingLineAttributes(String renderingAttribute) {
			this.renderingAttribute = renderingAttribute;
			paint = initPaint();
			customColorPaint = new Paint(paint);
			paint2 = initPaint();
			paint3 = initPaint();
			paint_1 = initPaint();
			shadowPaint = initPaint();
		}


		private Paint initPaint() {
			Paint paint = new Paint();
			paint.setStyle(Style.STROKE);
			paint.setAntiAlias(true);
			paint.setStrokeCap(Cap.ROUND);
			paint.setStrokeJoin(Join.ROUND);
			return paint;
		}


		public boolean updatePaints(OsmandApplication app, DrawSettings settings, RotatedTileBox tileBox) {
			OsmandRenderer renderer = app.getResourceManager().getRenderer().getRenderer();
			RenderingRulesStorage rrs = app.getRendererRegistry().getCurrentSelectedRenderer();
			final boolean isNight = settings != null && settings.isNightMode();
			int hsh = calculateHash(rrs, isNight, tileBox.getDensity());
			if (hsh != cachedHash) {
				cachedHash = hsh;
				if (rrs != null) {
					RenderingRuleSearchRequest req = new RenderingRuleSearchRequest(rrs);
					req.setBooleanFilter(rrs.PROPS.R_NIGHT_MODE, isNight);
					if (req.searchRenderingAttribute(renderingAttribute)) {
						RenderingContext rc = new OsmandRenderer.RenderingContext(app);
						rc.setDensityValue((float) tileBox.getDensity());
						// cachedColor = req.getIntPropertyValue(rrs.PROPS.R_COLOR);
						renderer.updatePaint(req, paint, 0, false, rc);
						isPaint2 = renderer.updatePaint(req, paint2, 1, false, rc);
						if (paint2.getStrokeWidth() == 0 && defaultWidth2 != 0) {
							paint2.setStrokeWidth(defaultWidth2);
						}
						isPaint3 = renderer.updatePaint(req, paint3, 2, false, rc);
						if (paint3.getStrokeWidth() == 0 && defaultWidth3 != 0) {
							paint3.setStrokeWidth(defaultWidth3);
						}
						isPaint_1 = renderer.updatePaint(req, paint_1, -1, false, rc);
						if (paint_1.getStrokeWidth() == 0 && defaultWidth_1 != 0) {
							paint_1.setStrokeWidth(defaultWidth_1);
						}
						isShadowPaint = req.isSpecified(rrs.PROPS.R_SHADOW_RADIUS);
						if (isShadowPaint) {
							ColorFilter cf = new PorterDuffColorFilter(
									req.getIntPropertyValue(rrs.PROPS.R_SHADOW_COLOR), Mode.SRC_IN);
							shadowPaint.setColorFilter(cf);
							shadowPaint.setStrokeWidth(paint.getStrokeWidth() + defaultShadowWidthExtent
									* rc.getComplexValue(req, rrs.PROPS.R_SHADOW_RADIUS));
						}
					} else {
						System.err.println("Rendering attribute route is not found !");
					}
					updateDefaultColor(paint, defaultColor);
					if (paint.getStrokeWidth() == 0 && defaultWidth != 0) {
						paint.setStrokeWidth(defaultWidth);
					}
					customColorPaint = new Paint(paint);
				}
				return true;
			}
			return false;
		}


		private void updateDefaultColor(Paint paint, int defaultColor) {
			if ((paint.getColor() == 0 || paint.getColor() == Color.BLACK) && defaultColor != 0) {
				paint.setColor(defaultColor);
			}
		}

		private int calculateHash(Object... o) {
			return Arrays.hashCode(o);
		}

		public void drawPath(Canvas canvas, Path path) {
			if (isPaint_1) {
				canvas.drawPath(path, paint_1);
			}
			if (isShadowPaint) {
				canvas.drawPath(path, shadowPaint);
			}
			if (customColor != 0 || customWidth != 0) {
				if (customColor != 0) {
					customColorPaint.setColor(customColor);
				}
				if (customWidth != 0) {
					customColorPaint.setStrokeWidth(customWidth);
				}
				canvas.drawPath(path, customColorPaint);
			} else {
				canvas.drawPath(path, paint);
			}
			if (isPaint2) {
				canvas.drawPath(path, paint2);
			}
			if (isPaint3) {
				canvas.drawPath(path, paint3);
			}
		}
	}
}
