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
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.view.MotionEvent;

import net.osmand.binary.BinaryMapIndexReader;
import net.osmand.data.Amenity;
import net.osmand.data.LatLon;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.osm.PoiCategory;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.render.OsmandRenderer;
import net.osmand.plus.render.OsmandRenderer.RenderingContext;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.util.MapAlgorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import gnu.trove.list.array.TIntArrayList;

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


	protected boolean isIn(int x, int y, int lx, int ty, int rx, int by) {
		return x >= lx && x <= rx && y >= ty && y <= by;
	}


	public int calculatePath(RotatedTileBox tb, TIntArrayList xs, TIntArrayList ys, Path path) {
		boolean segmentStarted = false;
		int prevX = xs.get(0);
		int prevY = ys.get(0);
		int height = tb.getPixHeight();
		int width = tb.getPixWidth();
		int cnt = 0;
		boolean prevIn = isIn(prevX, prevY, 0, 0, width, height);
		for (int i = 1; i < xs.size(); i++) {
			int currX = xs.get(i);
			int currY = ys.get(i);
			boolean currIn = isIn(currX, currY, 0, 0, width, height);
			boolean draw = false;
			if (prevIn && currIn) {
				draw = true;
			} else {
				long intersection = MapAlgorithms.calculateIntersection(currX, currY, prevX, prevY, 0, width, height, 0);
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
					long inter = MapAlgorithms.calculateIntersection(prevX, prevY, currX, currY, 0, width, height, 0);
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
			if (amenityId == id) {
				res = amenity;
				break;
			}
		}
		if (res == null && names != null && names.size() > 0) {
			for (Amenity amenity : amenities) {
				for (String name : names) {
					if (name.equals(amenity.getName())) {
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


		public boolean updatePaints(OsmandMapTileView view, DrawSettings settings, RotatedTileBox tileBox) {
			OsmandApplication app = view.getApplication();
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
			canvas.drawPath(path, paint);
			if (isPaint2) {
				canvas.drawPath(path, paint2);
			}
			if (isPaint3) {
				canvas.drawPath(path, paint3);
			}
		}
	}
}
