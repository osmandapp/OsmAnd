package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.Location;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.router.RouteColorize;
import net.osmand.router.RouteColorize.RouteColorizationPoint;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gnu.trove.list.array.TByteArrayList;

public class RouteGeometryWay extends GeometryWay<RouteGeometryWayContext, RouteGeometryWayDrawer> {

	private final RoutingHelper helper;
	private RouteCalculationResult route;

	private Integer customColor;
	private Float customWidth;
	private Integer customPointColor;
	private GradientScaleType scaleType;

	private boolean needUpdate;

	public RouteGeometryWay(RouteGeometryWayContext context) {
		super(context, new RouteGeometryWayDrawer(context, true));
		this.helper = context.getApp().getRoutingHelper();
	}

	public void setRouteStyleParams(@Nullable @ColorInt Integer color,
	                                @Nullable Float width,
	                                @Nullable @ColorInt Integer pointColor,
									@Nullable GradientScaleType scaleType) {
		this.needUpdate = this.scaleType != scaleType;

		if (scaleType != null && !Algorithms.objectEquals(customWidth, width)) {
			for (GeometryWayStyle<?> style : styleMap.values()) {
				style.width = width;
			}
		}
		this.customColor = color;
		this.customWidth = width;
		this.customPointColor = pointColor;
		this.scaleType = scaleType;
		if (width != null) {
			getContext().getAttrs().shadowPaint.setStrokeWidth(width + getContext().getDensity() * 2);
		}
		getContext().getAttrs().customColorPaint.setStrokeCap(scaleType != null ? Paint.Cap.ROUND : Paint.Cap.BUTT);
	}

	public void drawPreviewRouteLine(Canvas canvas, RotatedTileBox tileBox, List<Float> tx, List<Float> ty) {
		List<Double> angles = new ArrayList<>();
		List<Double> distances = new ArrayList<>();
		angles.add(0d);
		distances.add(0d);
		for (int i = 1; i < tx.size(); i++) {
			float x = tx.get(i);
			float y = ty.get(i);
			float px = tx.get(i - 1);
			float py = ty.get(i - 1);
			double angleRad = Math.atan2(y - py, x - px);
			Double angle = (angleRad * 180 / Math.PI) + 90f;
			angles.add(angle);
			double dist = Math.sqrt((y - py) * (y - py) + (x - px) * (x - px));
			distances.add(dist);
		}

		List<GeometryWayStyle<?>> styles = new ArrayList<>();
		if (scaleType == null) {
			for (int i = 0; i < tx.size(); i++) {
				styles.add(getDefaultWayStyle());
			}
		} else {
			for (int i = 1; i < tx.size(); i++) {
				GeometryGradientWayStyle style = getGradientWayStyle();
				styles.add(style);
				style.startXY = new PointF(tx.get(i - 1), ty.get(i - 1));
				style.endXY = new PointF(tx.get(i), ty.get(i));
				double prevDist = distances.get(i - 1);
				double currDist = distances.get(i);
				double nextDist = i + 1 == distances.size() ? 0 : distances.get(i + 1);
				style.startColor = getPreviewColor(i - 1, (prevDist + currDist / 2) / (prevDist + currDist));
				style.endColor = getPreviewColor(i, (currDist + nextDist / 2) / (currDist + nextDist));
			}
			styles.add(styles.get(styles.size() - 1));
		}

		drawRouteSegment(tileBox, canvas, tx, ty, angles, distances, 0, styles);
	}

	public void updateRoute(RotatedTileBox tb, RouteCalculationResult route, OsmandApplication app) {
		if (needUpdate || tb.getMapDensity() != getMapDensity() || this.route != route) {
			this.route = route;
			List<Location> locations = route != null ? route.getImmutableAllLocations() : Collections.<Location>emptyList();

			needUpdate = false;
			if (scaleType == null || locations.size() < 2) {
				updateWay(locations, tb);
				return;
			}
			GPXFile gpxFile = GpxUiHelper.makeGpxFromLocations(locations, app);
			if (!gpxFile.hasAltitude) {
				updateWay(locations, tb);
				return;
			}

			// Start point can have wrong zero altitude
			List<GPXUtilities.WptPt> pts = gpxFile.tracks.get(0).segments.get(0).points;
			GPXUtilities.WptPt firstPt = pts.get(0);
			if (firstPt.ele == 0) {
				firstPt.ele = pts.get(1).ele;
			}

			RouteColorize routeColorize = new RouteColorize(tb.getZoom(), gpxFile, null, scaleType.toColorizationType(), 0);
			List<RouteColorizationPoint> points = routeColorize.getResult(false);

			updateWay(new GradientGeometryWayProvider(routeColorize, points), createGradientStyles(points), tb);
		}
	}

	private Map<Integer, GeometryWayStyle<?>> createGradientStyles(List<RouteColorizationPoint> points) {
		Map<Integer, GeometryWayStyle<?>> styleMap = new TreeMap<>();
		for (int i = 1; i < points.size(); i++) {
			GeometryGradientWayStyle style = getGradientWayStyle();
			style.startColor = points.get(i - 1).color;
			style.endColor = points.get(i).color;
			styleMap.put(i, style);
		}
		return styleMap;
	}

	@Override
	protected boolean addInitialPoint(RotatedTileBox tb, double topLatitude, double leftLongitude, double bottomLatitude,
									  double rightLongitude, GeometryWayStyle<?> style, boolean previousVisible,
									  Location lastPoint, int startLocationIndex) {
		previousVisible = super.addInitialPoint(tb, topLatitude, leftLongitude, bottomLatitude, rightLongitude,
				style, previousVisible, lastPoint, startLocationIndex);
		if (style instanceof GeometryGradientWayStyle) {
			GeometryGradientWayStyle gradientWayStyle = (GeometryGradientWayStyle) style;
			GradientGeometryWayProvider locationProvider = getGradientLocationProvider();
			if (startLocationIndex == 0) {
				int startColor = locationProvider.getColor(0);
				gradientWayStyle.startColor = startColor;
				gradientWayStyle.endColor = startColor;
			} else {
				GeometryGradientWayStyle prevStyle = (GeometryGradientWayStyle) getStyle(startLocationIndex - 1, null);
				GeometryGradientWayStyle nextStyle = (GeometryGradientWayStyle) getStyle(startLocationIndex, null);
				if (prevStyle != null && nextStyle != null) {
					//TODO: calculate color delta
					gradientWayStyle.startColor = prevStyle.startColor;
					gradientWayStyle.endColor = prevStyle.startColor;
				}
			}
		}
		return previousVisible;
	}

	@Override
	protected boolean shouldSkipLocation(TByteArrayList simplification, Map<Integer, GeometryWayStyle<?>> styleMap, int locationIdx) {
		return scaleType == null ?
				super.shouldSkipLocation(simplification, styleMap, locationIdx) :
				simplification.getQuick(locationIdx) == 0;
	}

	public void clearRoute() {
		if (route != null) {
			route = null;
			clearWay();
		}
	}

	@NonNull
	@Override
	public GeometryWayStyle<RouteGeometryWayContext> getDefaultWayStyle() {
		Paint paint = getContext().getAttrs().paint;
		int color = customColor != null ? customColor : paint.getColor();
		float width = customWidth != null ? customWidth : paint.getStrokeWidth();
		return scaleType == null
				? new GeometrySolidWayStyle(getContext(), color, width, customPointColor)
				: new GeometryGradientWayStyle(getContext(), color, width);
	}

	private GeometryGradientWayStyle getGradientWayStyle() {
		return (GeometryGradientWayStyle) getDefaultWayStyle();
	}

	@Override
	public Location getNextVisiblePoint() {
		return helper.getRoute().getCurrentStraightAnglePoint();
	}

	private GradientGeometryWayProvider getGradientLocationProvider() {
		return (GradientGeometryWayProvider) getLocationProvider();
	}

	@Override
	protected PathGeometryZoom getGeometryZoom(RotatedTileBox tb) {
		if (scaleType == null) {
			return super.getGeometryZoom(tb);
		}
		int zoom = tb.getZoom();
		PathGeometryZoom zm = zooms.get(zoom);
		if (zm == null) {
			zm = new GradientPathGeometryZoom(getLocationProvider(), tb, true);
			zooms.put(zoom, zm);
		}
		return zm;
	}

	private int getPreviewColor(int index, double offset) {
		if (index == 0) {
			return RouteColorize.GREEN;
		} else if (index == 1) {
			return RouteColorize.getGradientColor(RouteColorize.GREEN, RouteColorize.YELLOW, offset);
		} else if (index == 2) {
			return RouteColorize.getGradientColor(RouteColorize.YELLOW, RouteColorize.RED, offset);
		} else {
			return RouteColorize.RED;
		}
	}

	private static class GradientGeometryWayProvider implements GeometryWayProvider {

		private final RouteColorize routeColorize;
		private final List<RouteColorizationPoint> locations;

		public GradientGeometryWayProvider(RouteColorize routeColorize, List<RouteColorizationPoint> locations) {
			this.routeColorize = routeColorize;
			this.locations = locations;
		}

		public List<RouteColorizationPoint> simplify(int zoom) {
			routeColorize.setZoom(zoom);
			return routeColorize.simplify();
		}

		public int getColor(int index) {
			return locations.get(index).color;
		}

		@Override
		public double getLatitude(int index) {
			return locations.get(index).lat;
		}

		@Override
		public double getLongitude(int index) {
			return locations.get(index).lon;
		}

		@Override
		public int getSize() {
			return locations.size();
		}
	}

	private static class GradientPathGeometryZoom extends PathGeometryZoom {

		public GradientPathGeometryZoom(GeometryWayProvider locationProvider, RotatedTileBox tb, boolean simplify) {
			super(locationProvider, tb, simplify);
		}

		@Override
		protected void simplify(RotatedTileBox tb, GeometryWayProvider locationProvider, TByteArrayList simplifyPoints) {
			if (locationProvider instanceof GradientGeometryWayProvider) {
				GradientGeometryWayProvider provider = (GradientGeometryWayProvider) locationProvider;
				List<RouteColorizationPoint> simplified = provider.simplify(tb.getZoom());
				for (RouteColorizationPoint location : simplified) {
					simplifyPoints.set(location.id, (byte) 1);
				}
			}
		}
	}

	public static class GeometryGradientWayStyle extends GeometryWayStyle<RouteGeometryWayContext> {

		public int startColor;
		public int endColor;

		public GeometryGradientWayStyle(RouteGeometryWayContext context, Integer color, Float width) {
			super(context, color, width);
		}

		@Override
		public Bitmap getPointBitmap() {
			return getContext().getArrowBitmap();
		}

		@Override
		public boolean isUnique() {
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
			if (!(other instanceof GeometryGradientWayStyle)) {
				return false;
			}
			GeometryGradientWayStyle o = (GeometryGradientWayStyle) other;
			return startColor == o.startColor && endColor == o.endColor;
		}
	}

	private static class GeometrySolidWayStyle extends GeometryWayStyle<RouteGeometryWayContext> {

		private final Integer pointColor;

		GeometrySolidWayStyle(RouteGeometryWayContext context, Integer color, Float width, Integer pointColor) {
			super(context, color, width);
			this.pointColor = pointColor;
		}

		@Override
		public Bitmap getPointBitmap() {
			return getContext().getArrowBitmap();
		}

		@Override
		public Integer getPointColor() {
			return pointColor;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			if (!(other instanceof GeometrySolidWayStyle)) {
				return false;
			}
			GeometrySolidWayStyle o = (GeometrySolidWayStyle) other;
			return Algorithms.objectEquals(pointColor, o.pointColor);
		}
	}
}