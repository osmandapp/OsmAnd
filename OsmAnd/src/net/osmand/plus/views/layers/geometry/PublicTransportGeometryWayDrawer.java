package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.core.jni.VectorLinesCollection;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.plus.views.layers.base.OsmandMapLayer;
import net.osmand.plus.views.layers.base.OsmandMapLayer.RenderingLineAttributes;
import net.osmand.plus.views.layers.geometry.PublicTransportGeometryWay.GeometryAnchorWayStyle;
import net.osmand.plus.views.layers.geometry.PublicTransportGeometryWay.GeometryTransportWayStyle;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PublicTransportGeometryWayDrawer extends GeometryWayDrawer<PublicTransportGeometryWayContext> {

	private List<TransportStop> routeTransportStops = new ArrayList<>();

	public PublicTransportGeometryWayDrawer(PublicTransportGeometryWayContext context) {
		super(context);
	}

	public List<TransportStop> getRouteTransportStops() {
		return routeTransportStops;
	}

	@Override
	public void drawPath(Canvas canvas, DrawPathData pathData) {
		Path path = pathData.path;
		GeometryWayStyle<?> style = pathData.style;
		if (style instanceof GeometryTransportWayStyle) {
			RenderingLineAttributes attrsPT = getContext().getAttrsPT();
			attrsPT.customColor = style.getStrokeColor(0);
			attrsPT.customColorPaint.setStrokeWidth(attrsPT.paint2.getStrokeWidth());
			attrsPT.drawPath(canvas, path);
			attrsPT.customColorPaint.setStrokeWidth(attrsPT.paint.getStrokeWidth());
			attrsPT.customColor = style.getColor(0);
			attrsPT.drawPath(canvas, path);
		} else {
			super.drawPath(canvas, pathData);
		}
	}

	@Override
	public void drawPath(@NonNull VectorLinesCollection collection, int baseOrder,
	                     boolean shouldDrawArrows, @NonNull List<DrawPathData31> pathsData) {
		int lineId = LINE_ID;
		GeometryWayStyle<?> prevStyle = null;
		List<DrawPathData31> dataArr = new ArrayList<>();
		RenderingLineAttributes attrsPT = getContext().getAttrsPT();
		float width = attrsPT.paint.getStrokeWidth();
		float outlineWidth = attrsPT.paint2.getStrokeWidth();
		for (DrawPathData31 data : pathsData) {
			if (prevStyle != null && (!Algorithms.objectEquals(data.style, prevStyle) || data.style.isUnique())) {
				if (prevStyle instanceof GeometryTransportWayStyle) {
					int outlineColor = prevStyle.getStrokeColor(0);
					drawVectorLine(collection, lineId++, baseOrder--, shouldDrawArrows, prevStyle,
							prevStyle.getColor(0), width, outlineColor, outlineWidth, null, false, dataArr);
				} else {
					drawVectorLine(collection, lineId++, baseOrder--, shouldDrawArrows, true, prevStyle, dataArr);
				}
				dataArr.clear();
			}
			prevStyle = data.style;
			dataArr.add(data);
		}
		if (!dataArr.isEmpty() && prevStyle != null) {
			if (prevStyle instanceof GeometryTransportWayStyle) {
				int outlineColor = prevStyle.getStrokeColor(0);
				drawVectorLine(collection, lineId, baseOrder, shouldDrawArrows, prevStyle,
						prevStyle.getColor(0), width, outlineColor, outlineWidth, null, false, dataArr);
			} else {
				drawVectorLine(collection, lineId, baseOrder, shouldDrawArrows, true, prevStyle, dataArr);
			}
		}
	}

	@Override
	public void drawArrowsOverPath(@NonNull Canvas canvas, @NonNull RotatedTileBox tb, List<GeometryWayPoint> points, double distPixToFinish) {
		PublicTransportGeometryWayContext context = getContext();

		List<PathPoint> arrows = new ArrayList<>();
		List<PathAnchor> anchors = new ArrayList<>();
		List<PathTransportStop> stops = new ArrayList<>();
		Set<GeometryTransportWayStyle> transportWaysStyles = new HashSet<>();
		GeometryAnchorWayStyle anchorWayStyle = new GeometryAnchorWayStyle(context);

		int h = tb.getPixHeight();
		int w = tb.getPixWidth();
		int left =  -w / 4;
		int right = w + w / 4;
		int top = - h/4;
		int bottom = h + h/4;

		double zoomCoef = tb.getZoomAnimation() > 0 ? (Math.pow(2, tb.getZoomAnimation() + tb.getZoomFloatPart())) : 1f;

		Bitmap arrow = context.getArrowBitmap();
		int arrowHeight = arrow.getHeight();
		double pxStep = arrowHeight * 4f * zoomCoef;
		double pxStepRegular = arrowHeight * 4f * zoomCoef;
		double dist = 0;
		if (distPixToFinish != 0) {
			dist = distPixToFinish - pxStep * ((int) (distPixToFinish / pxStep)); // dist < 1
		}

		GeometryWayStyle<?> prevStyle = null;
		for (int i = points.size() - 2; i >= 0; i --) {
			GeometryWayPoint pnt = points.get(i);
			GeometryWayStyle<?> style = pnt.style;
			float px = points.get(i).tx;
			float py = points.get(i).ty;
			float x = points.get(i + 1).tx;
			float y = points.get(i + 1).ty;
			double distSegment = points.get(i + 1).distance;
			double angle = points.get(i + 1).angle;
			if (distSegment == 0) {
				continue;
			}
			pxStep = style != null ? style.getPointStepPx(zoomCoef) : pxStepRegular;
			boolean transportStyle = style instanceof GeometryTransportWayStyle;
			if (style != null && !style.equals(prevStyle) && (prevStyle != null || transportStyle)) {
				prevStyle = style;
				anchors.add(new PathAnchor(x, y, anchorWayStyle));
				dist = 0;
			}
			if (transportStyle) {
				transportWaysStyles.add((GeometryTransportWayStyle) style);
			}
			if (dist >= pxStep) {
				dist = 0;
			}
			double percent = 1 - (pxStep - dist) / distSegment;
			dist += distSegment;
			while (dist >= pxStep) {
				double pdx = (x - px) * percent;
				double pdy = (y - py) * percent;
				float iconx = (float) (px + pdx);
				float icony = (float) (py + pdy);
				if (GeometryWayPathAlgorithms.isIn(iconx, icony, left, top, right, bottom)) {
					arrows.add(new PathPoint(iconx, icony, angle, style));
				}
				dist -= pxStep;
				percent -= pxStep / distSegment;
			}
		}
		List<TransportStop> routeTransportStops = new ArrayList<>();
		for (GeometryTransportWayStyle style : transportWaysStyles) {
			List<TransportStop> transportStops = style.getRoute().getForwardStops();
			TransportRouteResultSegment segment = style.getSegment();
			int start = segment.start;
			int end = segment.end;
			for (int i = start; i <= end; i++) {
				TransportStop stop = transportStops.get(i);
				double lat = stop.getLocation().getLatitude();
				double lon = stop.getLocation().getLongitude();
				float x = tb.getPixXFromLatLon(lat, lon);
				float y = tb.getPixYFromLatLon(lat, lon);
				if (GeometryWayPathAlgorithms.isIn(x, y, left, top, right, bottom)) {
					if (i != start && i != end) {
						stops.add(new PathTransportStop(x, y, style));
					}
					routeTransportStops.add(transportStops.get(i));
				}
			}
		}
		this.routeTransportStops = routeTransportStops;

		for (int i = arrows.size() - 1; i >= 0; i--) {
			PathPoint a = arrows.get(i);
			if (!tb.isZoomAnimated() || a.style.isVisibleWhileZooming()) {
				a.draw(canvas, context);
			}
		}
		for (int i = anchors.size() - 1; i >= 0; i--) {
			PathAnchor anchor = anchors.get(i);
			anchor.draw(canvas, context);
		}
		if (stops.size() > 0) {
			QuadTree<QuadRect> boundIntersections = OsmandMapLayer.initBoundIntersections(tb);
			List<PathTransportStop> fullObjects = new ArrayList<>();
			Bitmap stopBitmap = null;
			float iconSize = 1f;
			for (int i = stops.size() - 1; i >= 0; i--) {
				PathTransportStop stop = stops.get(i);
				if (stopBitmap == null) {
					stopBitmap = stop.getTransportWayStyle().getStopBitmap();
					iconSize = stopBitmap.getWidth() * 3 / 2.5f;
				}
				float x = stop.x;
				float y = stop.y;
				if (OsmandMapLayer.intersects(boundIntersections, x, y, iconSize, iconSize)) {
					stop.setSmallPoint(true);
					stop.draw(canvas, context);
				} else {
					stop.setSmallPoint(false);
					fullObjects.add(stop);
				}
			}
			for (PathTransportStop stop : fullObjects) {
				stop.draw(canvas, context);
			}
		}
	}

	public static class PathAnchor extends PathPoint {
		PathAnchor(float x, float y, GeometryAnchorWayStyle style) {
			super(x, y, 0, style);
		}
	}

	public static class PathTransportStop extends PathPoint {

		private boolean smallPoint;

		public boolean isSmallPoint() {
			return smallPoint;
		}

		public void setSmallPoint(boolean smallPoint) {
			this.smallPoint = smallPoint;
		}

		PathTransportStop(float x, float y, GeometryTransportWayStyle style) {
			super(x, y, 0, style);
		}

		GeometryTransportWayStyle getTransportWayStyle() {
			return (GeometryTransportWayStyle) style;
		}

		@Nullable
		@Override
		protected Bitmap getPointBitmap() {
			return smallPoint
					? getTransportWayStyle().getStopSmallBitmap()
					: getTransportWayStyle().getStopBitmap();
		}

		@Override
		protected void draw(@NonNull Canvas canvas, @NonNull GeometryWayContext context) {
			Bitmap stopBitmap = getPointBitmap();
			if (stopBitmap != null) {
				float paintH2 = stopBitmap.getHeight() / 2f;
				float paintW2 = stopBitmap.getWidth() / 2f;

				Matrix matrix = getMatrix();
				matrix.reset();
				matrix.postRotate(0f, paintW2, paintH2);
				matrix.postTranslate(x - paintW2, y - paintH2);
				Paint paint = context.getPaintIconCustom();
				paint.setColorFilter(null);
				canvas.drawBitmap(stopBitmap, matrix, paint);
			}
		}
	}
}
