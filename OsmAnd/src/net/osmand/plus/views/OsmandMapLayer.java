package net.osmand.plus.views;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Pair;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class OsmandMapLayer {

	protected List<LatLon> fullObjectsLatLon;
	protected List<LatLon> smallObjectsLatLon;

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

	protected boolean isIn(float x, float y, int lx, int ty, int rx, int by) {
		return x >= lx && x <= rx && y >= ty && y <= by;
	}

	public int calculatePath(RotatedTileBox tb, List<Float> xs, List<Float> ys, Path path) {
		List<Pair<Path, GeometryWayStyle>> paths = new ArrayList<>();
		int res = calculatePath(tb, xs, ys, null, paths);
		if (paths.size() > 0) {
			path.addPath(paths.get(0).first);
		}
		return res;
	}

	public static class GeometryWayContext {

		private Context ctx;
		private float density;
		private boolean nightMode;

		private Paint paintIcon;
		private Paint paintIconCustom;

		private RenderingLineAttributes attrs;
		private RenderingLineAttributes attrsPT;
		private RenderingLineAttributes attrsW;

		private Bitmap arrowBitmap;
		private Bitmap walkArrowBitmap;
		private Bitmap anchorBitmap;
		private Map<Pair<Integer, Drawable>, Bitmap> stopBitmapsCache = new HashMap<>();
		private Map<Integer, Bitmap> stopSmallBitmapsCache = new HashMap<>();

		public GeometryWayContext(Context ctx, float density) {
			this.ctx = ctx;
			this.density = density;

			paintIcon = new Paint();
			paintIcon.setFilterBitmap(true);
			paintIcon.setAntiAlias(true);
			paintIcon.setColor(Color.BLACK);
			paintIcon.setStrokeWidth(1f * density);

			paintIconCustom = new Paint();
			paintIconCustom.setFilterBitmap(true);
			paintIconCustom.setAntiAlias(true);
			paintIconCustom.setColor(Color.BLACK);
			paintIconCustom.setStrokeWidth(1f * density);

			attrsW = new RenderingLineAttributes("walkingRouteLine");
			attrsW.defaultWidth = (int) (12 * density);
			attrsW.defaultWidth3 = (int) (7 * density);
			attrsW.defaultColor = ctx.getResources().getColor(R.color.nav_track_walk_fill);
			attrsW.paint3.setStrokeCap(Cap.BUTT);
			attrsW.paint3.setColor(Color.WHITE);
			attrsW.paint2.setStrokeCap(Cap.BUTT);
			attrsW.paint2.setColor(Color.BLACK);

			arrowBitmap = BitmapFactory.decodeResource(ctx.getResources(), R.drawable.map_route_direction_arrow, null);
		}

		public OsmandApplication getApp() {
			return (OsmandApplication) ctx.getApplicationContext();
		}

		public float getDensity() {
			return density;
		}

		public boolean isNightMode() {
			return nightMode;
		}

		private int calculateHash(Object... o) {
			return Arrays.hashCode(o);
		}

		public void updatePaints(boolean nightMode,
									@NonNull RenderingLineAttributes attrs,
									@NonNull RenderingLineAttributes attrsPT,
									@NonNull RenderingLineAttributes attrsW) {
			this.attrs = attrs;
			this.attrsPT = attrsPT;
			this.attrsW = attrsW;
			paintIcon.setColorFilter(new PorterDuffColorFilter(attrs.paint2.getColor(), Mode.MULTIPLY));

			this.nightMode = nightMode;
			recreateBitmaps();
		}

		private boolean hasAttrs() {
			return attrs != null && attrsPT != null && attrsW != null;
		}

		public int getStrokeColor(int sourceColor) {
			return ColorUtils.blendARGB(sourceColor, Color.BLACK, 0.6f);
		}

		private void recreateBitmaps() {
			if (hasAttrs()) {
				float walkCircleH = attrsW.paint.getStrokeWidth() * 1.33f;
				float walkCircleW = attrsW.paint.getStrokeWidth();
				float walkCircleRadius = attrsW.paint.getStrokeWidth() / 2f;
				float routeShieldRadius = attrsPT.paint3.getStrokeWidth() / 2f;

				// create anchor bitmap
				float margin = 2f * density;
				float width = routeShieldRadius * 2 + margin * 2;
				float height = routeShieldRadius * 2 + margin * 2;
				Bitmap bitmap = Bitmap.createBitmap((int)width, (int)height, Bitmap.Config.ARGB_8888);

				Canvas canvas = new Canvas(bitmap);
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				paint.setStrokeWidth(1f * density);

				float x = width / 2;
				float y = height / 2;
				paint.setColor(Color.WHITE);
				paint.setStyle(Paint.Style.FILL);
				canvas.drawCircle(x, y, routeShieldRadius, paint);
				paint.setColor(Color.BLACK);
				paint.setStyle(Paint.Style.STROKE);
				canvas.drawCircle(x, y, routeShieldRadius, paint);

				anchorBitmap = bitmap;

				// create walk arrow bitmap
				width = walkCircleW + margin * 2;
				height = walkCircleH + margin * 2;
				bitmap = Bitmap.createBitmap((int)width, (int)height, Bitmap.Config.ARGB_8888);

				canvas = new Canvas(bitmap);
				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setStrokeWidth(1f * density);

				RectF rect = new RectF(margin, margin, width - margin, height - margin);
				paint.setColor(attrsW.paint.getColor());
				paint.setStyle(Paint.Style.FILL);
				canvas.drawRoundRect(rect, walkCircleRadius, walkCircleRadius, paint);
				paint.setColor(getStrokeColor(paint.getColor()));
				paint.setStyle(Paint.Style.STROKE);
				canvas.drawRoundRect(rect, walkCircleRadius, walkCircleRadius, paint);

				paint = new Paint();
				paint.setAntiAlias(true);
				paint.setColor(Color.WHITE);
				paint.setStrokeWidth(1f * density);
				paint.setAlpha(200);
				canvas.drawBitmap(arrowBitmap, width / 2 - arrowBitmap.getWidth() / 2f, height / 2 - arrowBitmap.getHeight() / 2f, paint);

				walkArrowBitmap = bitmap;
				stopBitmapsCache = new HashMap<>();
			}
		}

		public Paint getPaintIcon() {
			return paintIcon;
		}

		public Paint getPaintIconCustom() {
			return paintIconCustom;
		}

		public Bitmap getArrowBitmap() {
			return arrowBitmap;
		}

		public Bitmap getWalkArrowBitmap() {
			return walkArrowBitmap;
		}

		public Bitmap getAnchorBitmap() {
			return anchorBitmap;
		}

		public Bitmap getStopShieldBitmap(int color, Drawable stopDrawable) {
			Bitmap bmp = stopBitmapsCache.get(new Pair<>(color, stopDrawable));
			if (bmp == null) {
				int fillColor = UiUtilities.getContrastColor(getApp(), color, true);
				int strokeColor = getStrokeColor(color);

				float routeShieldRadius = attrsPT.paint3.getStrokeWidth() / 2;
				float routeShieldCornerRadius = 3 * density;

				float margin = 2f * density;
				float width = routeShieldRadius * 2 + margin * 2;
				float height = routeShieldRadius * 2 + margin * 2;
				bmp = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);

				Canvas canvas = new Canvas(bmp);
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				paint.setStrokeWidth(1f * density);

				RectF rect = new RectF(margin, margin, width - margin, height - margin);
				paint.setColor(fillColor);
				paint.setStyle(Paint.Style.FILL);
				canvas.drawRoundRect(rect, routeShieldCornerRadius, routeShieldCornerRadius, paint);
				paint.setColor(strokeColor);
				paint.setStyle(Paint.Style.STROKE);
				canvas.drawRoundRect(rect, routeShieldCornerRadius, routeShieldCornerRadius, paint);

				if (stopDrawable != null) {
					stopDrawable.setColorFilter(new PorterDuffColorFilter(strokeColor, Mode.SRC_IN));
					float marginBitmap = 1f * density;
					rect.inset(marginBitmap, marginBitmap);
					stopDrawable.setBounds(0, 0, (int) rect.width(), (int) rect.height());
					canvas.translate(rect.left, rect.top);
					stopDrawable.draw(canvas);
				}
				stopBitmapsCache.put(new Pair<>(color, stopDrawable), bmp);
			}
			return bmp;
		}

		public Bitmap getStopSmallShieldBitmap(int color) {
			Bitmap bmp = stopSmallBitmapsCache.get(color);
			if (bmp == null) {
				int fillColor = UiUtilities.getContrastColor(getApp(), color, true);
				int strokeColor = getStrokeColor(color);

				float routeShieldRadius = attrsPT.paint3.getStrokeWidth() / 4;

				float margin = 3f * density;
				float width = routeShieldRadius * 2 + margin * 2;
				float height = routeShieldRadius * 2 + margin * 2;
				bmp = Bitmap.createBitmap((int) width, (int) height, Bitmap.Config.ARGB_8888);

				Canvas canvas = new Canvas(bmp);
				Paint paint = new Paint();
				paint.setAntiAlias(true);
				paint.setStrokeWidth(1f * density);
				paint.setColor(fillColor);
				paint.setStyle(Paint.Style.FILL);
				canvas.drawCircle(width / 2, height / 2, routeShieldRadius, paint);
				paint.setColor(strokeColor);
				paint.setStyle(Paint.Style.STROKE);
				canvas.drawCircle(width / 2, height / 2, routeShieldRadius, paint);

				stopSmallBitmapsCache.put(color, bmp);
			}
			return bmp;
		}
	}

	public abstract static class GeometryWayStyle {

		private GeometryWayContext context;
		protected Integer color;

		public GeometryWayStyle(GeometryWayContext context) {
			this.context = context;
		}

		public GeometryWayStyle(GeometryWayContext context, Integer color) {
			this.context = context;
			this.color = color;
		}

		public GeometryWayContext getContext() {
			return context;
		}

		public Context getCtx() {
			return context.ctx;
		}

		public Integer getColor() {
			return color;
		}

		public Integer getStrokeColor() {
			return context.getStrokeColor(color);
		}

		public Integer getPointColor() {
			return null;
		}

		public boolean isNightMode() {
			return context.nightMode;
		}

		public boolean hasAnchors() {
			return false;
		}

		public boolean hasPathLine() {
			return true;
		}

		public boolean isTransportLine() {
			return false;
		}

		public boolean isWalkLine() {
			return false;
		}

		public abstract Bitmap getPointBitmap();

		public boolean hasPaintedPointBitmap() {
			return false;
		}

		@Override
		public int hashCode() {
			return (color != null ? color.hashCode() : 0) + (context.nightMode ? 1231 : 1237) + (hasAnchors() ? 12310 : 12370);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!(other instanceof GeometryWayStyle)) {
				return false;
			}
			GeometryWayStyle o = (GeometryWayStyle) other;
			if (color != null && o.color != null) {
				return color.equals(o.color);
			}
			return color == null && o.color == null;
		}
	}

	public int calculatePath(RotatedTileBox tb, List<Float> xs, List<Float> ys, List<GeometryWayStyle> styles, List<Pair<Path, GeometryWayStyle>> paths) {
		boolean segmentStarted = false;
		float prevX = xs.get(0);
		float prevY = ys.get(0);
		int height = tb.getPixHeight();
		int width = tb.getPixWidth();
		int cnt = 0;
		boolean hasStyles = styles != null && styles.size() == xs.size();
		GeometryWayStyle style = hasStyles ? styles.get(0) : null;
		Path path = new Path();
		boolean prevIn = isIn(prevX, prevY, 0, 0, width, height);
		for (int i = 1; i < xs.size(); i++) {
			float currX = xs.get(i);
			float currY = ys.get(i);
			boolean currIn = isIn(currX, currY, 0, 0, width, height);
			boolean draw = false;
			if (prevIn && currIn) {
				draw = true;
			} else {
				long intersection = MapAlgorithms.calculateIntersection((int)currX, (int)currY, (int)prevX, (int)prevY, 0, width, height, 0);
				if (intersection != -1) {
					if (prevIn && (i == 1)) {
						cnt++;
						path.moveTo(prevX, prevY);
						segmentStarted = true;
					}
					prevX = (int) (intersection >> 32);
					prevY = (int) (intersection & 0xffffffff);
					draw = true;
				}
				if (i == xs.size() - 1 && !currIn) {
					long inter = MapAlgorithms.calculateIntersection((int)prevX, (int)prevY, (int)currX, (int)currY, 0, width, height, 0);
					if (inter != -1) {
						currX = (int) (inter >> 32);
						currY = (int) (inter & 0xffffffff);
					}
				}
			}
			if (draw) {
				if (!segmentStarted) {
					cnt++;
					path.moveTo(prevX, prevY);
					segmentStarted = true;
				}
				path.lineTo(currX, currY);
			} else {
				segmentStarted = false;
			}
			prevIn = currIn;
			prevX = currX;
			prevY = currY;

			if (hasStyles) {
				GeometryWayStyle newStyle = styles.get(i);
				if (!style.equals(newStyle)) {
					paths.add(new Pair<>(path, style));
					path = new Path();
					if (segmentStarted) {
						path.moveTo(currX, currY);
					}
					style = newStyle;
				}
			}
		}
		if (!path.isEmpty()) {
			paths.add(new Pair<>(path, style));
		}
		return cnt;
	}

	@NonNull
	public QuadTree<QuadRect> initBoundIntersections(RotatedTileBox tileBox) {
		QuadRect bounds = new QuadRect(0, 0, tileBox.getPixWidth(), tileBox.getPixHeight());
		bounds.inset(-bounds.width() / 4, -bounds.height() / 4);
		return new QuadTree<>(bounds, 4, 0.6f);
	}

	public boolean intersects(QuadTree<QuadRect> boundIntersections, float x, float y, float width, float height) {
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

	public QuadRect calculateRect(float x, float y, float width, float height) {
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

	protected static class RenderingLineAttributes {
		protected int cachedHash;
		public Paint paint;
		public Paint customColorPaint;
		public int customColor = 0;
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
			if (customColor != 0) {
				customColorPaint.setColor(customColor);
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
