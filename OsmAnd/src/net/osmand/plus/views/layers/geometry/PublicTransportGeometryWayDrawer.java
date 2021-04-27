package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportStop;
import net.osmand.plus.views.OsmandMapLayer;
import net.osmand.plus.views.OsmandMapLayer.RenderingLineAttributes;
import net.osmand.plus.views.layers.geometry.PublicTransportGeometryWay.GeometryAnchorWayStyle;
import net.osmand.plus.views.layers.geometry.PublicTransportGeometryWay.GeometryTransportWayStyle;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;

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
			attrsPT.customColor = style.getStrokeColor();
			attrsPT.customColorPaint.setStrokeWidth(attrsPT.paint2.getStrokeWidth());
			attrsPT.drawPath(canvas, path);
			attrsPT.customColorPaint.setStrokeWidth(attrsPT.paint.getStrokeWidth());
			attrsPT.customColor = style.getColor();
			attrsPT.drawPath(canvas, path);
		} else {
			super.drawPath(canvas, pathData);
		}
	}

	@Override
	public void drawArrowsOverPath(Canvas canvas, RotatedTileBox tb, List<Float> tx, List<Float> ty,
								   List<Double> angles, List<Double> distances, double distPixToFinish, List<GeometryWayStyle<?>> styles) {
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

		boolean hasStyles = styles != null && styles.size() == tx.size();
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
		for (int i = tx.size() - 2; i >= 0; i --) {
			GeometryWayStyle<?> style = hasStyles ? styles.get(i) : null;
			float px = tx.get(i);
			float py = ty.get(i);
			float x = tx.get(i + 1);
			float y = ty.get(i + 1);
			double distSegment = distances.get(i + 1);
			double angle = angles.get(i + 1);
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
				if (GeometryWay.isIn(iconx, icony, left, top, right, bottom)) {
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
				if (GeometryWay.isIn(x, y, left, top, right, bottom)) {
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


		@Override
		void draw(Canvas canvas, GeometryWayContext context) {
			Bitmap stopBitmap = smallPoint ?
					getTransportWayStyle().getStopSmallBitmap() : getTransportWayStyle().getStopBitmap();
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
