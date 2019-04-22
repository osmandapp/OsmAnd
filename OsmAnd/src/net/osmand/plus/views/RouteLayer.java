package net.osmand.plus.views;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Cap;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffColorFilter;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.Pair;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.PointDescription;
import net.osmand.data.QuadRect;
import net.osmand.data.QuadTree;
import net.osmand.data.RotatedTileBox;
import net.osmand.data.TransportRoute;
import net.osmand.data.TransportStop;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OSMSettings;
import net.osmand.osm.edit.Way;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu;
import net.osmand.plus.mapcontextmenu.other.TrackDetailsMenu.TrackChartPoints;
import net.osmand.plus.render.RenderingIcons;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteDirectionInfo;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.transport.TransportStopRoute;
import net.osmand.plus.transport.TransportStopType;
import net.osmand.router.TransportRoutePlanner;
import net.osmand.router.TransportRoutePlanner.TransportRouteResult;
import net.osmand.router.TransportRoutePlanner.TransportRouteResultSegment;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import gnu.trove.list.array.TByteArrayList;

public class RouteLayer extends OsmandMapLayer implements ContextMenuLayer.IContextMenuProvider {
	
	private static final float EPSILON_IN_DPI = 2;

	private OsmandMapTileView view;
	
	private final RoutingHelper helper;
	private final TransportRoutingHelper transportHelper;
	// keep array lists created
	private List<Location> actionPoints = new ArrayList<Location>();
	private List<TransportStop> routeTransportStops = new ArrayList<>();

	// cache
	private Bitmap actionArrow;

	private Paint paintIconAction;
	private Paint paintGridOuterCircle;
	private Paint paintGridCircle;

	private Paint paintIconSelected;
	private Bitmap selectedPoint;
	private TrackChartPoints trackChartPoints;

	private RenderingLineAttributes attrs;
	private RenderingLineAttributes attrsPT;
	private RenderingLineAttributes attrsW;
	private boolean nightMode;

	private GeometryWayContext wayContext;

	public RouteLayer(RoutingHelper helper) {
		this.helper = helper;
		this.transportHelper = helper.getTransportRoutingHelper();
	}

	public void setTrackChartPoints(TrackDetailsMenu.TrackChartPoints trackChartPoints) {
		this.trackChartPoints = trackChartPoints;
	}

	private void initUI() {
		float density = view.getDensity();

		actionArrow = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_action_arrow, null);

		paintIconAction = new Paint();
		paintIconAction.setFilterBitmap(true);
		paintIconAction.setAntiAlias(true);

		attrs = new RenderingLineAttributes("route");
		attrs.defaultWidth = (int) (12 * density);
		attrs.defaultWidth3 = (int) (7 * density);
		attrs.defaultColor = view.getResources().getColor(R.color.nav_track);
		attrs.paint3.setStrokeCap(Cap.BUTT);
		attrs.paint3.setColor(Color.WHITE);
		attrs.paint2.setStrokeCap(Cap.BUTT);
		attrs.paint2.setColor(Color.BLACK);

		attrsPT = new RenderingLineAttributes("publicTransportLine");
		attrsPT.defaultWidth = (int) (12 * density);
		attrsPT.defaultWidth3 = (int) (7 * density);
		attrsPT.defaultColor = view.getResources().getColor(R.color.nav_track);
		attrsPT.paint3.setStrokeCap(Cap.BUTT);
		attrsPT.paint3.setColor(Color.WHITE);
		attrsPT.paint2.setStrokeCap(Cap.BUTT);
		attrsPT.paint2.setColor(Color.BLACK);

		attrsW = new RenderingLineAttributes("walkingRouteLine");
		attrsW.defaultWidth = (int) (12 * density);
		attrsW.defaultWidth3 = (int) (7 * density);
		attrsW.defaultColor = view.getResources().getColor(R.color.nav_track_walk_fill);
		attrsW.paint3.setStrokeCap(Cap.BUTT);
		attrsW.paint3.setColor(Color.WHITE);
		attrsW.paint2.setStrokeCap(Cap.BUTT);
		attrsW.paint2.setColor(Color.BLACK);

		wayContext = new GeometryWayContext(view.getContext(), density);

		paintIconSelected = new Paint();
		selectedPoint = BitmapFactory.decodeResource(view.getResources(), R.drawable.map_default_location);

		paintGridCircle = new Paint();
		paintGridCircle.setStyle(Paint.Style.FILL_AND_STROKE);
		paintGridCircle.setAntiAlias(true);
		paintGridCircle.setColor(attrs.defaultColor);
		paintGridCircle.setAlpha(255);
		paintGridOuterCircle = new Paint();
		paintGridOuterCircle.setStyle(Paint.Style.FILL_AND_STROKE);
		paintGridOuterCircle.setAntiAlias(true);
		paintGridOuterCircle.setColor(Color.WHITE);
		paintGridOuterCircle.setAlpha(204);
	}

	@Override
	public void initLayer(OsmandMapTileView view) {
		this.view = view;
		initUI();
	}

	@Override
	public void onPrepareBufferImage(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {
		if ((helper.isPublicTransportMode() && transportHelper.getRoutes() != null) ||
				(helper.getFinalLocation() != null && helper.getRoute().isCalculated())) {

			updateAttrs(settings, tileBox);
			
			int w = tileBox.getPixWidth();
			int h = tileBox.getPixHeight();
			Location lastProjection = helper.getLastProjection();
			final RotatedTileBox cp ;
			if(lastProjection != null &&
					tileBox.containsLatLon(lastProjection.getLatitude(), lastProjection.getLongitude())){
				cp = tileBox.copy();
				cp.increasePixelDimensions(w /2, h);
			} else {
				cp = tileBox;
			}

			final QuadRect latlonRect = cp.getLatLonBounds();
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
			drawLocations(tileBox, canvas, topLatitude + lat, leftLongitude - lon, bottomLatitude - lat, rightLongitude + lon);

			if (trackChartPoints != null) {
				drawXAxisPoints(canvas, tileBox);
				LatLon highlightedPoint = trackChartPoints.getHighlightedPoint();
				if (highlightedPoint != null
						&& highlightedPoint.getLatitude() >= latlonRect.bottom
						&& highlightedPoint.getLatitude() <= latlonRect.top
						&& highlightedPoint.getLongitude() >= latlonRect.left
						&& highlightedPoint.getLongitude() <= latlonRect.right) {
					float x = tileBox.getPixXFromLatLon(highlightedPoint.getLatitude(), highlightedPoint.getLongitude());
					float y = tileBox.getPixYFromLatLon(highlightedPoint.getLatitude(), highlightedPoint.getLongitude());
					canvas.drawBitmap(selectedPoint, x - selectedPoint.getWidth() / 2f, y - selectedPoint.getHeight() / 2f, paintIconSelected);
				}
			}
		}
	
	}

	private void updateAttrs(DrawSettings settings, RotatedTileBox tileBox) {
		boolean updatePaints = attrs.updatePaints(view.getApplication(), settings, tileBox);
		attrs.isPaint3 = false;
		attrs.isPaint2 = false;
		attrsPT.updatePaints(view.getApplication(), settings, tileBox);
		attrsPT.isPaint3 = false;
		attrsPT.isPaint2 = false;
		attrsW.updatePaints(view.getApplication(), settings, tileBox);
		attrsPT.isPaint3 = false;
		attrsPT.isPaint2 = false;

		nightMode = settings != null && settings.isNightMode();

		if (updatePaints) {
			paintIconAction.setColorFilter(new PorterDuffColorFilter(attrs.paint3.getColor(), Mode.MULTIPLY));
			wayContext.updatePaints(nightMode, attrs, attrsPT, attrsW);
		}
	}

	private void drawXAxisPoints(Canvas canvas, RotatedTileBox tileBox) {
		QuadRect latLonBounds = tileBox.getLatLonBounds();
		List<LatLon> xAxisPoints = trackChartPoints.getXAxisPoints();
		float r = 3 * tileBox.getDensity();
		float density = (float) Math.ceil(tileBox.getDensity());
		float outerRadius = r + 2 * density;
		float innerRadius = r + density;
		QuadRect prevPointRect = null;
		for (int i = 0; i < xAxisPoints.size(); i++) {
			LatLon axisPoint = xAxisPoints.get(i);
			if (axisPoint.getLatitude() >= latLonBounds.bottom
					&& axisPoint.getLatitude() <= latLonBounds.top
					&& axisPoint.getLongitude() >= latLonBounds.left
					&& axisPoint.getLongitude() <= latLonBounds.right) {
				float x = tileBox.getPixXFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
				float y = tileBox.getPixYFromLatLon(axisPoint.getLatitude(), axisPoint.getLongitude());
				QuadRect pointRect = new QuadRect(x - outerRadius, y - outerRadius, x + outerRadius, y + outerRadius);
				if (prevPointRect == null || !QuadRect.intersects(prevPointRect, pointRect)) {
					canvas.drawCircle(x, y, outerRadius, paintGridOuterCircle);
					canvas.drawCircle(x, y, innerRadius, paintGridCircle);
					prevPointRect = pointRect;
				}
			}
		}
	}
	
	@Override
	public void onDraw(Canvas canvas, RotatedTileBox tileBox, DrawSettings settings) {}

	private void drawAction(RotatedTileBox tb, Canvas canvas, List<Location> actionPoints) {
		if (actionPoints.size() > 0) {
			canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			try {
				Path pth = new Path();
				Matrix matrix = new Matrix();
				boolean first = true;
				int x = 0, px = 0, py = 0, y = 0;
				for (int i = 0; i < actionPoints.size(); i++) {
					Location o = actionPoints.get(i);
					if (o == null) {
						first = true;
						canvas.drawPath(pth, attrs.paint3);
						double angleRad = Math.atan2(y - py, x - px);
						double angle = (angleRad * 180 / Math.PI) + 90f;
						double distSegment = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
						if (distSegment == 0) {
							continue;
						}
						// int len = (int) (distSegment / pxStep);
						float pdx = x - px;
						float pdy = y - py;
						matrix.reset();
						matrix.postTranslate(0, -actionArrow.getHeight() / 2f);
						matrix.postRotate((float) angle, actionArrow.getWidth() / 2f, 0);
						matrix.postTranslate(px + pdx - actionArrow.getWidth() / 2f, py + pdy);
						canvas.drawBitmap(actionArrow, matrix, paintIconAction);
					} else {
						px = x;
						py = y;
						x = (int) tb.getPixXFromLatLon(o.getLatitude(), o.getLongitude());
						y = (int) tb.getPixYFromLatLon(o.getLatitude(), o.getLongitude());
						if (first) {
							pth.reset();
							pth.moveTo(x, y);
							first = false;
						} else {
							pth.lineTo(x, y);
						}
					}
				}

			} finally {
				canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			}
		}
	}

	@ColorInt
	public int getRouteLineColor(boolean night) {
		updateAttrs(new DrawSettings(night), view.getCurrentRotatedTileBox());
		return attrs.paint.getColor();
	}
	
	private void cullRamerDouglasPeucker(TByteArrayList survivor, List<Location> points,
			int start, int end, double epsillon) {
        double dmax = Double.NEGATIVE_INFINITY;
        int index = -1;
        Location startPt = points.get(start);
        Location endPt = points.get(end);

        for (int i = start + 1; i < end; i++) {
            Location pt = points.get(i);
            double d = MapUtils.getOrthogonalDistance(pt.getLatitude(), pt.getLongitude(), 
            		startPt.getLatitude(), startPt.getLongitude(), endPt.getLatitude(), endPt.getLongitude());
            if (d > dmax) {
                dmax = d;
                index = i;
            }
        }
        if (dmax > epsillon) {
        	cullRamerDouglasPeucker(survivor, points, start, index, epsillon);
        	cullRamerDouglasPeucker(survivor, points, index, end, epsillon);
        } else {
            survivor.set(end, (byte) 1);
        }
    }

    private static class PathPoint {
		float x;
		float y;
		double angle;
		GeometryWayStyle style;

		private Matrix matrix = new Matrix();

		PathPoint(float x, float y, double angle, GeometryWayStyle style) {
			this.x = x;
			this.y = y;
			this.angle = angle;
			this.style = style;
		}

		protected Matrix getMatrix() {
			return matrix;
		}

		void draw(Canvas canvas, GeometryWayContext context) {
			if (style != null && style.getPointBitmap() != null) {
				Bitmap bitmap = style.getPointBitmap();
				Integer pointColor = style.getPointColor();
				float paintH2 = bitmap.getHeight() / 2f;
				float paintW2 = bitmap.getWidth() / 2f;

				matrix.reset();
				matrix.postRotate((float) angle, paintW2, paintH2);
				matrix.postTranslate(x - paintW2, y - paintH2);
				if (pointColor != null) {
					Paint paint = context.getPaintIconCustom();
					paint.setColorFilter(new PorterDuffColorFilter(pointColor, Mode.SRC_IN));
					canvas.drawBitmap(bitmap, matrix, paint);
				} else {
					if (style.hasPaintedPointBitmap()) {
						Paint paint = context.getPaintIconCustom();
						paint.setColorFilter(null);
						canvas.drawBitmap(bitmap, matrix, paint);
					} else {
						canvas.drawBitmap(bitmap, matrix, context.getPaintIcon());
					}
				}
			}
		}
	}

	private static class PathAnchor extends PathPoint {
		PathAnchor(float x, float y, GeometryAnchorWayStyle style) {
			super(x, y, 0, style);
		}
	}

	private static class PathTransportStop extends PathPoint {

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

	private static class GeometryWalkWayStyle extends GeometryWayStyle {

		GeometryWalkWayStyle(GeometryWayContext context) {
			super(context);
		}

		@Override
		public boolean hasPathLine() {
			return false;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			return other instanceof GeometryWalkWayStyle;
		}

		public Bitmap getPointBitmap() {
			return getContext().getWalkArrowBitmap();
		}

		@Override
		public boolean isWalkLine() {
			return true;
		}

		@Override
		public boolean hasPaintedPointBitmap() {
			return true;
		}
	}

	private static class GeometryAnchorWayStyle extends GeometryWayStyle {

		GeometryAnchorWayStyle(GeometryWayContext context) {
			super(context);
		}

		@Override
		public boolean hasPathLine() {
			return false;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			return other instanceof GeometryAnchorWayStyle;
		}

		public Bitmap getPointBitmap() {
			return getContext().getAnchorBitmap();
		}

		@Override
		public boolean hasPaintedPointBitmap() {
			return true;
		}
	}

	private static class GeometrySolidWayStyle extends GeometryWayStyle {

		GeometrySolidWayStyle(GeometryWayContext context, Integer color) {
			super(context, color);
		}

		@Override
		public Bitmap getPointBitmap() {
			return getContext().getArrowBitmap();
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			return other instanceof GeometrySolidWayStyle;
		}
	}

	private static class TransportStopsWayStyle extends GeometryTransportWayStyle {
		TransportStopsWayStyle(GeometryWayContext context, TransportRouteResultSegment segment) {
			super(context, segment);
			OsmandApplication app = (OsmandApplication) getCtx().getApplicationContext();
			this.color = ContextCompat.getColor(app, R.color.icon_color);
			this.pointColor = UiUtilities.getContrastColor(app, color, true);
		}
	}

	private static class GeometryTransportWayStyle extends GeometryWayStyle {

		private TransportRouteResultSegment segment;
		private Bitmap stopBitmap;
		protected Integer pointColor;

		GeometryTransportWayStyle(GeometryWayContext context, TransportRouteResultSegment segment) {
			super(context);
			this.segment = segment;

			TransportStopRoute r = new TransportStopRoute();
			TransportRoute route = segment.route;
			r.type = TransportStopType.findType(route.getType());
			r.route = route;
			OsmandApplication app = (OsmandApplication) getCtx().getApplicationContext();
			this.color = r.getRouteColor(app, isNightMode());
			this.pointColor = UiUtilities.getContrastColor(app, color, true);

			TransportStopType type = TransportStopType.findType(route.getType());
			if (type == null) {
				type = TransportStopType.findType("bus");
			}
			if (type != null) {
				stopBitmap = RenderingIcons.getIcon(getCtx(), type.getResName(), false);
			}
		}

		public TransportRouteResultSegment getSegment() {
			return segment;
		}

		public TransportRoute getRoute() {
			return segment.route;
		}

		@Override
		public boolean hasAnchors() {
			return true;
		}

		@Override
		public Bitmap getPointBitmap() {
			return getContext().getArrowBitmap();
		}

		@Override
		public Integer getPointColor() {
			return pointColor;
		}

		public Bitmap getStopBitmap() {
			return getContext().getStopShieldBitmap(color, stopBitmap);
		}

		public Bitmap getStopSmallBitmap() {
			return getContext().getStopSmallShieldBitmap(color);
		}

		@Override
		public boolean isTransportLine() {
			return true;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			if (!(other instanceof GeometryTransportWayStyle)) {
				return false;
			}
			return getRoute() == ((GeometryTransportWayStyle) other).getRoute();
		}
	}

	private void drawArrowsOverPath(Canvas canvas, RotatedTileBox tb, List<Float> tx, List<Float> ty,
			List<Double> angles, List<Double> distances, double distPixToFinish, List<GeometryWayStyle> styles) {
		int h = tb.getPixHeight();
		int w = tb.getPixWidth();
		int left =  -w / 4;
		int right = w + w / 4;
		int top = - h/4;
		int bottom = h + h/4;

		boolean hasStyles = styles != null && styles.size() == tx.size();
		double zoomCoef = tb.getZoomAnimation() > 0 ? (Math.pow(2, tb.getZoomAnimation() + tb.getZoomFloatPart())) : 1f;

		Bitmap arrow = wayContext.getArrowBitmap();
		int arrowHeight = arrow.getHeight();
		double pxStep = arrowHeight * 4f * zoomCoef;
		double dist = 0;
		if (distPixToFinish != 0) {
			dist = distPixToFinish - pxStep * ((int) (distPixToFinish / pxStep)); // dist < 1
		}

		List<PathPoint> arrows = new ArrayList<>();
		List<PathAnchor> anchors = new ArrayList<>();
		List<PathTransportStop> stops = new ArrayList<>();
		Set<GeometryTransportWayStyle> transportWaysStyles = new HashSet<>();

		GeometryAnchorWayStyle anchorWayStyle = new GeometryAnchorWayStyle(wayContext);

		GeometryWalkWayStyle walkWayStyle = new GeometryWalkWayStyle(wayContext);
		Bitmap walkArrow = walkWayStyle.getPointBitmap();
		int walkArrowHeight = walkArrow.getHeight();
		double pxStepWalk = walkArrowHeight * 1.2f * zoomCoef;
		double pxStepRegular = arrowHeight * 4f * zoomCoef;

		GeometryWayStyle prevStyle = null;
		for (int i = tx.size() - 2; i >= 0; i --) {
			GeometryWayStyle style = hasStyles ? styles.get(i) : null;
			float px = tx.get(i);
			float py = ty.get(i);
			float x = tx.get(i + 1);
			float y = ty.get(i + 1);
			double distSegment = distances.get(i + 1);
			double angle = angles.get(i + 1);
			if (distSegment == 0) {
				continue;
			}
			if (style != null && style.isWalkLine()) {
				pxStep = pxStepWalk;
			} else {
				pxStep = pxStepRegular;
			}
			if (style != null && !style.equals(prevStyle) && (prevStyle != null || style.hasAnchors())) {
				prevStyle = style;
				anchors.add(new PathAnchor(x, y, anchorWayStyle));
				dist = 0;
			}
			if (style != null && style.isTransportLine()) {
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
				if (isIn(iconx, icony, left, top, right, bottom)) {
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
				if (isIn(x, y, left, top, right, bottom)) {
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
			if (!tb.isZoomAnimated() || a.style.isWalkLine()) {
				a.draw(canvas, wayContext);
			}
		}
		for (int i = anchors.size() - 1; i >= 0; i--) {
			PathAnchor anchor = anchors.get(i);
			anchor.draw(canvas, wayContext);
		}
		if (stops.size() > 0) {
			QuadTree<QuadRect> boundIntersections = initBoundIntersections(tb);
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
				if (intersects(boundIntersections, x, y, iconSize, iconSize)) {
					stop.setSmallPoint(true);
					stop.draw(canvas, wayContext);
				} else {
					stop.setSmallPoint(false);
					fullObjects.add(stop);
				}
			}
			for (PathTransportStop stop : fullObjects) {
				stop.draw(canvas, wayContext);
			}
		}
	}
	
	private static class RouteGeometryZoom {
		final TByteArrayList simplifyPoints;
		List<Double> distances;
		List<Double> angles;
		
		public RouteGeometryZoom(List<Location> locations, RotatedTileBox tb) {
			//  this.locations = locations;
			tb = new RotatedTileBox(tb);
			tb.setZoomAndAnimation(tb.getZoom(), 0, tb.getZoomFloatPart());
			simplifyPoints = new TByteArrayList(locations.size());
			distances = new ArrayList<>(locations.size());
			angles = new ArrayList<>(locations.size());
			simplifyPoints.fill(0, locations.size(), (byte)0);
			if(locations.size() > 0) {
				simplifyPoints.set(0, (byte) 1);
			}
			double distInPix = (tb.getDistance(0, 0, tb.getPixWidth(), 0) / tb.getPixWidth());
			double cullDistance = (distInPix * (EPSILON_IN_DPI * Math.max(1, tb.getDensity())));
			cullRamerDouglasPeucker(simplifyPoints, locations, 0, locations.size() - 1, cullDistance);
			
			int previousIndex = -1;
			for(int i = 0; i < locations.size(); i++) {
				double d = 0;
				double angle = 0;
				if(simplifyPoints.get(i) > 0) {
					if(previousIndex > -1) {
						Location loc = locations.get(i);
						Location pr = locations.get(previousIndex);
						float x = tb.getPixXFromLatLon(loc.getLatitude(), loc.getLongitude());
						float y = tb.getPixYFromLatLon(loc.getLatitude(), loc.getLongitude());
						float px = tb.getPixXFromLatLon(pr.getLatitude(), pr.getLongitude());
						float py = tb.getPixYFromLatLon(pr.getLatitude(), pr.getLongitude());
						d = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
						if(px != x || py != y) {
							double angleRad = Math.atan2(y - py, x - px);
							angle = (angleRad * 180 / Math.PI) + 90f;
						}
					}
					previousIndex = i;
				}
				distances.add(d);
				angles.add(angle);
			}
		}
		
		
		public List<Double> getDistances() {
			return distances;
		}
		
		private void cullRamerDouglasPeucker(TByteArrayList survivor, List<Location> points,
				int start, int end, double epsillon) {
	        double dmax = Double.NEGATIVE_INFINITY;
	        int index = -1;
	        Location startPt = points.get(start);
	        Location endPt = points.get(end);

	        for (int i = start + 1; i < end; i++) {
	            Location pt = points.get(i);
	            double d = MapUtils.getOrthogonalDistance(pt.getLatitude(), pt.getLongitude(), 
	            		startPt.getLatitude(), startPt.getLongitude(), endPt.getLatitude(), endPt.getLongitude());
	            if (d > dmax) {
	                dmax = d;
	                index = i;
	            }
	        }
	        if (dmax > epsillon) {
	        	cullRamerDouglasPeucker(survivor, points, start, index, epsillon);
	        	cullRamerDouglasPeucker(survivor, points, index, end, epsillon);
	        } else {
	            survivor.set(end, (byte) 1);
	        }
	    }
		
		public TByteArrayList getSimplifyPoints() {
			return simplifyPoints;
		}
	}
	
	private class RouteSimplificationGeometry {
		RouteCalculationResult route;
		TransportRouteResult transportRoute;
		double mapDensity;
		TreeMap<Integer, RouteGeometryZoom> zooms = new TreeMap<>();
		List<Location> locations = Collections.emptyList();
		Map<Integer, GeometryWayStyle> styleMap = Collections.emptyMap();

		// cache arrays
		List<Float> tx = new ArrayList<>();
		List<Float> ty = new ArrayList<>();
		List<Double> angles = new ArrayList<>();
		List<Double> distances = new ArrayList<>();
		List<GeometryWayStyle> styles = new ArrayList<>();

		private GeometryWayStyle getStyle(int index, GeometryWayStyle defaultWayStyle) {
			List<Integer> list = new ArrayList<>(styleMap.keySet());
			for (int i = list.size() - 1; i >= 0; i--) {
				int c = list.get(i);
				if (c <= index) {
					return styleMap.get(c);
				}
			}
			return defaultWayStyle;
		}

		private boolean isTransportRoute() {
			return transportRoute != null;
		}

		public void updateRoute(RotatedTileBox tb, RouteCalculationResult route) {
			if(tb.getMapDensity() != mapDensity || this.route != route) {
				this.route = route;
				if (route == null) {
					locations = Collections.emptyList();
				} else {
					locations = route.getImmutableAllLocations();
				}
				styleMap = Collections.emptyMap();
				this.mapDensity = tb.getMapDensity();
				zooms.clear();
			}
		}

		public void clearRoute() {
			if (route != null) {
				route = null;
				locations = Collections.emptyList();
				styleMap = Collections.emptyMap();
				zooms.clear();
			}
		}

		public void updateTransportRoute(RotatedTileBox tb, TransportRouteResult route) {
			if (tb.getMapDensity() != mapDensity || this.transportRoute != route) {
				this.transportRoute = route;
				if (route == null) {
					locations = Collections.emptyList();
					styleMap = Collections.emptyMap();
				} else {
					LatLon start = transportHelper.getStartLocation();
					LatLon end = transportHelper.getEndLocation();
					List<Way> list = new ArrayList<>();
					List<GeometryWayStyle> styles = new ArrayList<>();
					calculateTransportResult(start, end, route, list, styles);
					List<Location> locs = new ArrayList<>();
					Map<Integer, GeometryWayStyle> stlMap = new TreeMap<>();
					int i = 0;
					int k = 0;
					if (list.size() > 0) {
						for (Way w : list) {
							stlMap.put(k, styles.get(i++));
							for (Node n : w.getNodes()) {
								Location ln = new Location("");
								ln.setLatitude(n.getLatitude());
								ln.setLongitude(n.getLongitude());
								locs.add(ln);
								k++;
							}
						}
					}
					locations = locs;
					styleMap = stlMap;
				}
				this.mapDensity = tb.getMapDensity();
				zooms.clear();
			}
		}

		public void clearTransportRoute() {
			if (transportRoute != null) {
				transportRoute = null;
				locations = Collections.emptyList();
				styleMap = Collections.emptyMap();
				zooms.clear();
			}
		}

		private RouteGeometryZoom getGeometryZoom(RotatedTileBox tb) {
			int zoom = tb.getZoom();
			RouteGeometryZoom zm = zooms.size() > zoom ? zooms.get(zoom) : null;
			if (zm == null) {
				zm = new RouteGeometryZoom(locations, tb);
				zooms.put(zoom, zm);
			}
			return zm;
		}
		
		private void drawSegments(RotatedTileBox tb, Canvas canvas, double topLatitude, double leftLongitude,
				double bottomLatitude, double rightLongitude, Location lastProjection, int currentRoute) {
			if (locations.size() == 0) {
				return;
			}
			RouteGeometryZoom geometryZoom = getGeometryZoom(tb);
			TByteArrayList simplification = geometryZoom.getSimplifyPoints();
			List<Double> odistances = geometryZoom.getDistances();
			
			clearArrays();
			GeometryWayStyle defaultWayStyle = isTransportRoute() ?
					new GeometryWalkWayStyle(wayContext) :
					new GeometrySolidWayStyle(wayContext, attrs.paint.getColor());
			GeometryWayStyle style = defaultWayStyle;
			boolean previousVisible = false;
			if (lastProjection != null) {
				if (leftLongitude <= lastProjection.getLongitude() && lastProjection.getLongitude() <= rightLongitude
						&& bottomLatitude <= lastProjection.getLatitude() && lastProjection.getLatitude() <= topLatitude) {
					addLocation(tb, lastProjection, style, tx, ty, angles, distances, 0, styles);
					previousVisible = true;
				}
			}
			List<Location> routeNodes = locations;
			int previous = -1;
			for (int i = currentRoute; i < routeNodes.size(); i++) {
				Location ls = routeNodes.get(i);
				style = getStyle(i, defaultWayStyle);
				if (simplification.getQuick(i) == 0 && !styleMap.containsKey(i)) {
					continue;
				}
				if (leftLongitude <= ls.getLongitude() && ls.getLongitude() <= rightLongitude && bottomLatitude <= ls.getLatitude()
						&& ls.getLatitude() <= topLatitude) {
					double dist = 0;
					if (!previousVisible) {
						Location lt = null;
						if (previous != -1) {
							lt = routeNodes.get(previous);
							dist = odistances.get(i);
						} else if (lastProjection != null) {
							lt = lastProjection;
						}
						addLocation(tb, lt, style, tx, ty, angles, distances, 0, styles); // first point
					}
					addLocation(tb, ls, style, tx, ty, angles, distances, dist, styles);
					previousVisible = true;
				} else if (previousVisible) {
					addLocation(tb, ls, style, tx, ty, angles, distances, previous == -1 ? 0 : odistances.get(i), styles);
					double distToFinish = 0;
					for(int ki = i + 1; ki < odistances.size(); ki++) {
						distToFinish += odistances.get(ki);
					}
					drawRouteSegment(tb, canvas, tx, ty, angles, distances, distToFinish, styles);
					previousVisible = false;
					clearArrays();
				}
				previous = i;
			}
			drawRouteSegment(tb, canvas, tx, ty, angles, distances, 0, styles);
		}

		private void clearArrays() {
			tx.clear();
			ty.clear();
			distances.clear();
			angles.clear();
			styles.clear();
		}

		private void addLocation(RotatedTileBox tb, Location ls, GeometryWayStyle style, List<Float> tx, List<Float> ty,
				List<Double> angles, List<Double> distances, double dist, List<GeometryWayStyle> styles) {
			float x = tb.getPixXFromLatLon(ls.getLatitude(), ls.getLongitude());
			float y = tb.getPixYFromLatLon(ls.getLatitude(), ls.getLongitude());
			float px = x;
			float py = y;
			int previous = tx.size() - 1;
			if (previous >= 0 && previous < tx.size()) {
				px = tx.get(previous);
				py = ty.get(previous);
			}
			double angle = 0;
			if (px != x || py != y) {
				double angleRad = Math.atan2(y - py, x - px);
				angle = (angleRad * 180 / Math.PI) + 90f;
			}
			double distSegment = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
			if(dist != 0) {
				distSegment = dist;
			}
			tx.add(x);
			ty.add(y);
			angles.add(angle);
			distances.add(distSegment);
			styles.add(style);
		}
	}
	
	private RouteSimplificationGeometry routeGeometry  = new RouteSimplificationGeometry();
	
	private void drawRouteSegment(RotatedTileBox tb, Canvas canvas, List<Float> tx, List<Float> ty,
			List<Double> angles, List<Double> distances, double distToFinish, List<GeometryWayStyle> styles) {
		if (tx.size() < 2) {
			return;
		}
		try {
			List<Pair<Path, GeometryWayStyle>> paths = new ArrayList<>();
			canvas.rotate(-tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
			calculatePath(tb, tx, ty, styles, paths);
			for (Pair<Path, GeometryWayStyle> pc : paths) {
				GeometryWayStyle style = pc.second;
				if (style.hasPathLine()) {
					if (style.isTransportLine()) {
						attrsPT.customColor = style.getStrokeColor();
						attrsPT.customColorPaint.setStrokeWidth(attrsPT.paint2.getStrokeWidth());
						attrsPT.drawPath(canvas, pc.first);
						attrsPT.customColorPaint.setStrokeWidth(attrsPT.paint.getStrokeWidth());
						attrsPT.customColor = style.getColor();
						attrsPT.drawPath(canvas, pc.first);
					} else {
						attrs.customColor = style.getColor();
						attrs.drawPath(canvas, pc.first);
					}
				}
			}
			attrs.customColor = 0;
			attrsPT.customColor = 0;
			drawArrowsOverPath(canvas, tb, tx, ty, angles, distances, distToFinish, styles);
		} finally {
			canvas.rotate(tb.getRotate(), tb.getCenterPixelX(), tb.getCenterPixelY());
		}
	}
	
	public void drawLocations(RotatedTileBox tb, Canvas canvas, double topLatitude, double leftLongitude, double bottomLatitude, double rightLongitude) {
		if (helper.isPublicTransportMode()) {
			int currentRoute = transportHelper.getCurrentRoute();
			List<TransportRouteResult> routes = transportHelper.getRoutes();
			TransportRouteResult route = routes != null && routes.size() > currentRoute ? routes.get(currentRoute) : null;
			routeGeometry.clearRoute();
			routeGeometry.updateTransportRoute(tb, route);
			if (route != null) {
				LatLon start = transportHelper.getStartLocation();
				Location startLocation = new Location("transport");
				startLocation.setLatitude(start.getLatitude());
				startLocation.setLongitude(start.getLongitude());
				routeGeometry.drawSegments(tb, canvas, topLatitude, leftLongitude, bottomLatitude, rightLongitude, startLocation, 0);
			}
		} else {
			RouteCalculationResult route = helper.getRoute();
			routeGeometry.clearTransportRoute();
			routeGeometry.updateRoute(tb, route);
			routeGeometry.drawSegments(tb, canvas, topLatitude, leftLongitude, bottomLatitude, rightLongitude,
					helper.getLastProjection(), route == null ? 0 : route.getCurrentRoute());
			List<RouteDirectionInfo> rd = helper.getRouteDirections();
			Iterator<RouteDirectionInfo> it = rd.iterator();
			if (tb.getZoom() >= 14) {
				List<Location> actionPoints = calculateActionPoints(topLatitude, leftLongitude, bottomLatitude, rightLongitude, helper.getLastProjection(),
						helper.getRoute().getRouteLocations(), helper.getRoute().getCurrentRoute(), it, tb.getZoom());
				drawAction(tb, canvas, actionPoints);
			}
		}
	}
	
	

	private List<Location> calculateActionPoints(double topLatitude, double leftLongitude, double bottomLatitude,
			double rightLongitude, Location lastProjection, List<Location> routeNodes, int cd,
			Iterator<RouteDirectionInfo> it, int zoom) {
		RouteDirectionInfo nf = null;
		
		double DISTANCE_ACTION = 35;
		if(zoom >= 17) {
			DISTANCE_ACTION = 15;
		} else if (zoom == 15) {
			DISTANCE_ACTION = 70;
		} else if (zoom < 15) {
			DISTANCE_ACTION = 110;
		}
		double actionDist = 0;
		Location previousAction = null; 
		List<Location> actionPoints = this.actionPoints;
		actionPoints.clear();
		int prevFinishPoint = -1;
		for (int routePoint = 0; routePoint < routeNodes.size(); routePoint++) {
			Location loc = routeNodes.get(routePoint);
			if(nf != null) {
				int pnt = nf.routeEndPointOffset == 0 ? nf.routePointOffset : nf.routeEndPointOffset;
				if(pnt < routePoint + cd ) {
					nf = null;
				}
			}
			while (nf == null && it.hasNext()) {
				nf = it.next();
				int pnt = nf.routeEndPointOffset == 0 ? nf.routePointOffset : nf.routeEndPointOffset;
				if (pnt < routePoint + cd) {
					nf = null;
				}
			}
			boolean action = nf != null && (nf.routePointOffset == routePoint + cd ||
					(nf.routePointOffset <= routePoint + cd && routePoint + cd  <= nf.routeEndPointOffset));
			if(!action && previousAction == null) {
				// no need to check
				continue;
			}
			boolean visible = leftLongitude <= loc.getLongitude() && loc.getLongitude() <= rightLongitude && bottomLatitude <= loc.getLatitude()
					&& loc.getLatitude() <= topLatitude;
			if(action && !visible && previousAction == null) {
				continue;
			}
			if (!action) {
				// previousAction != null
				float dist = loc.distanceTo(previousAction);
				actionDist += dist;
				if (actionDist >= DISTANCE_ACTION) {
					actionPoints.add(calculateProjection(1 - (actionDist - DISTANCE_ACTION) / dist, previousAction, loc));
					actionPoints.add(null);
					prevFinishPoint = routePoint;
					previousAction = null;
					actionDist = 0;
				} else {
					actionPoints.add(loc);
					previousAction = loc;
				}
			} else {
				// action point
				if (previousAction == null) {
					addPreviousToActionPoints(actionPoints, lastProjection, routeNodes, DISTANCE_ACTION,
							prevFinishPoint, routePoint, loc);
				}
				actionPoints.add(loc);
				previousAction = loc;
				prevFinishPoint = -1;
				actionDist = 0;
			}
		}
		if(previousAction != null) {
			actionPoints.add(null);
		}
		return actionPoints;
	}


	private void addPreviousToActionPoints(List<Location> actionPoints, Location lastProjection, List<Location> routeNodes, double DISTANCE_ACTION,
			int prevFinishPoint, int routePoint, Location loc) {
		// put some points in front
		int ind = actionPoints.size();
		Location lprevious = loc;
		double dist = 0;
		for (int k = routePoint - 1; k >= -1; k--) {
			Location l = k == -1 ? lastProjection : routeNodes.get(k);
			float locDist = lprevious.distanceTo(l);
			dist += locDist;
			if (dist >= DISTANCE_ACTION) {
				if (locDist > 1) {
					actionPoints.add(ind,
							calculateProjection(1 - (dist - DISTANCE_ACTION) / locDist, lprevious, l));
				}
				break;
			} else {
				actionPoints.add(ind, l);
				lprevious = l;
			}
			if (prevFinishPoint == k) {
				if (ind >= 2) {
					actionPoints.remove(ind - 2);
					actionPoints.remove(ind - 2);
				}
				break;
			}
		}
	}
	
	private Location calculateProjection(double part, Location lp, Location l) {
		Location p = new Location(l);
		p.setLatitude(lp.getLatitude() + part * (l.getLatitude() - lp.getLatitude()));
		p.setLongitude(lp.getLongitude() + part * (l.getLongitude() - lp.getLongitude()));
		return p;
	}


	public RoutingHelper getHelper() {
		return helper;
	}

	
	
	@Override
	public void destroyLayer() {
		
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

	private void calculateTransportResult(LatLon start, LatLon end, TransportRouteResult r, List<Way> res, List<GeometryWayStyle> styles) {
		if (r != null) {
			LatLon p = start;
			TransportRouteResultSegment prev = null;
			for (TransportRouteResultSegment s : r.getSegments()) {
				LatLon floc = s.getStart().getLocation();
				addRouteWalk(prev, s, p, floc, res, styles);
				List<Way> geometry = s.getGeometry();
				res.addAll(geometry);
				addStyle(s, geometry, styles);
				p = s.getEnd().getLocation();
				prev = s;
			}
			addRouteWalk(prev, null, p, end, res, styles);
		}
	}

	private void addRouteWalk(TransportRouteResultSegment s1, TransportRouteResultSegment s2,
							  LatLon start, LatLon end, List<Way> res, List<GeometryWayStyle> styles) {
		final RouteCalculationResult walkingRouteSegment = transportHelper.getWalkingRouteSegment(s1, s2);
		if (walkingRouteSegment != null && walkingRouteSegment.getRouteLocations().size() > 0) {
			final List<Location> routeLocations = walkingRouteSegment.getRouteLocations();
			Way way = new Way(TransportRoutePlanner.GEOMETRY_WAY_ID);
			way.putTag(OSMSettings.OSMTagKey.NAME.getValue(), String.format(Locale.US, "Walk %d m", walkingRouteSegment.getWholeDistance()));
			for (Location l : routeLocations) {
				way.addNode(new Node(l.getLatitude(), l.getLongitude(), -1));
			}
			res.add(way);
			addStyle(null, Collections.singletonList(way), styles);
		} else {
			double dist = MapUtils.getDistance(start, end);
			Way way = new Way(TransportRoutePlanner.GEOMETRY_WAY_ID);
			way.putTag(OSMSettings.OSMTagKey.NAME.getValue(), String.format(Locale.US, "Walk %.1f m", dist));
			way.addNode(new Node(start.getLatitude(), start.getLongitude(), -1));
			way.addNode(new Node(end.getLatitude(), end.getLongitude(), -1));
			res.add(way);
			addStyle(null, Collections.singletonList(way), styles);
		}
	}

	private void addStyle(TransportRouteResultSegment segment, List<Way> geometry, List<GeometryWayStyle> styles) {
		GeometryWayStyle style;
		Way w = geometry.get(0);
		if (segment == null || segment.route == null) {
			style = new GeometryWalkWayStyle(wayContext);
		} else if (w.getId() == TransportRoutePlanner.GEOMETRY_WAY_ID) {
			style = new GeometryTransportWayStyle(wayContext, segment);
		} else {
			style = new TransportStopsWayStyle(wayContext, segment);
		}
		for (int i = 0; i < geometry.size(); i++) {
			styles.add(style);
		}
	}

	private int getRadiusPoi(RotatedTileBox tb){
		final double zoom = tb.getZoom();
		int r;
		if(zoom <= 15) {
			r = 8;
		} else if(zoom <= 16) {
			r = 10;
		} else if(zoom <= 17) {
			r = 14;
		} else {
			r = 18;
		}
		return (int) (r * tb.getDensity());
	}

	private void getFromPoint(RotatedTileBox tb, PointF point, List<? super TransportStop> res) {
		int ex = (int) point.x;
		int ey = (int) point.y;
		final int rp = getRadiusPoi(tb);
		int radius = rp * 3 / 2;
		try {
			for (int i = 0; i < routeTransportStops.size(); i++) {
				TransportStop n = routeTransportStops.get(i);
				if (n.getLocation() == null) {
					continue;
				}
				int x = (int) tb.getPixXFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
				int y = (int) tb.getPixYFromLatLon(n.getLocation().getLatitude(), n.getLocation().getLongitude());
				if (Math.abs(x - ex) <= radius && Math.abs(y - ey) <= radius) {
					radius = rp;
					res.add(n);
				}
			}
		} catch (IndexOutOfBoundsException e) {
			// ignore
		}
	}

	@Override
	public void collectObjectsFromPoint(PointF point, RotatedTileBox tileBox, List<Object> res, boolean unknownLocation) {
		if (routeTransportStops.size() > 0) {
			getFromPoint(tileBox, point, res);
		}
	}

	@Override
	public LatLon getObjectLocation(Object o) {
		if (o instanceof TransportStop){
			return ((TransportStop)o).getLocation();
		}
		return null;
	}

	@Override
	public PointDescription getObjectName(Object o) {
		if (o instanceof TransportStop){
			return new PointDescription(PointDescription.POINT_TYPE_TRANSPORT_STOP, view.getContext().getString(R.string.transport_Stop),
					((TransportStop)o).getName());
		}
		return null;
	}

	@Override
	public boolean disableSingleTap() {
		return false;
	}

	@Override
	public boolean disableLongPressOnMap() {
		return false;
	}

	@Override
	public boolean isObjectClickable(Object o) {
		return false;
	}

	@Override
	public boolean runExclusiveAction(@Nullable Object o, boolean unknownLocation) {
		return false;
	}
}
