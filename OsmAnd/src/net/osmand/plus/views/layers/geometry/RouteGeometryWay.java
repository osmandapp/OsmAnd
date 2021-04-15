package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.PointF;

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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import gnu.trove.list.array.TByteArrayList;

public class RouteGeometryWay extends GeometryWay<RouteGeometryWayContext, RouteGeometryWayDrawer> {

	private RoutingHelper helper;
	private RouteCalculationResult route;

	private Integer customColor;
	private Float customWidth;
	private Integer customPointColor;
	private GradientScaleType scaleType;

	public RouteGeometryWay(RouteGeometryWayContext context) {
		super(context, new RouteGeometryWayDrawer(context, true));
		this.helper = context.getApp().getRoutingHelper();
	}

	public void setRouteStyleParams(@Nullable @ColorInt Integer color,
	                                @Nullable Float width,
	                                @Nullable @ColorInt Integer pointColor,
									@Nullable GradientScaleType scaleType) {
		this.customColor = color;
		this.customWidth = width;
		this.customPointColor = pointColor;
		this.scaleType = GradientScaleType.ALTITUDE;
		if (width != null) {
			getContext().getAttrs().shadowPaint.setStrokeWidth(width + getContext().getDensity() * 2);
		}
	}

	public void updateRoute(RotatedTileBox tb, RouteCalculationResult route, OsmandApplication app) {
		if (tb.getMapDensity() == getMapDensity() && this.route == route) {
			return;
		}

		this.route = route;
		List<Location> locations = route != null ? route.getImmutableAllLocations() : Collections.<Location>emptyList();

		if (scaleType == null || locations.size() < 2) {
			updateWay(locations, tb);
			return;
		}

		GPXFile gpxFile = GpxUiHelper.makeGpxFromRoute(route, app);
		if (!gpxFile.hasAltitude) {
			updateWay(locations, tb);
		}

		RouteColorize routeColorize = new RouteColorize(tb.getZoom(), gpxFile, null, scaleType.toColorizationType(), 0);
		List<RouteColorizationPoint> points = routeColorize.getResult(false);

		updateWay(new GradientGeometryWayProvider(routeColorize), createStyles(points), tb);
	}

	private Map<Integer, GeometryWayStyle<?>> createStyles(List<RouteColorizationPoint> points) {
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
	protected boolean shouldSkipLocation(TByteArrayList simplification, Map<Integer, GeometryWayStyle<?>> styleMap, int locationIdx) {
		return scaleType == null ?
				super.shouldSkipLocation(simplification, styleMap, locationIdx) :
				simplification.getQuick(locationIdx) == 0;
	}

	@Override
	protected void addLocation(RotatedTileBox tb, int locationIdx, GeometryWayStyle<?> style, List<Float> tx, List<Float> ty, List<Double> angles, List<Double> distances, double dist, List<GeometryWayStyle<?>> styles) {
		super.addLocation(tb, getLocationProvider().getLatitude(locationIdx),
				getLocationProvider().getLongitude(locationIdx), style, tx, ty, angles, distances, dist, styles);
		if (scaleType != null && tx.size() - 1 > 0) {
			int idx = tx.size() - 1;
			((GeometryGradientWayStyle) style).startXY = new PointF(tx.get(idx - 1), ty.get(idx - 1));
			((GeometryGradientWayStyle) style).endXY = new PointF(tx.get(idx), ty.get(idx));
 		}
	}

	@Override
	protected void addLocation(RotatedTileBox tb, double latitude, double longitude, GeometryWayStyle<?> style, List<Float> tx, List<Float> ty, List<Double> angles, List<Double> distances, double dist, List<GeometryWayStyle<?>> styles) {
		super.addLocation(tb, latitude, longitude, style, tx, ty, angles, distances, dist, styles);
		if (scaleType != null) {
			int lastIdx = tx.size() - 1;
			((GeometryGradientWayStyle) style).startXY = new PointF(tx.get(lastIdx), ty.get(lastIdx));
			((GeometryGradientWayStyle) style).startColor = getGradientLocationProvider().getColor(0);
			((GeometryGradientWayStyle) style).endColor = getGradientLocationProvider().getColor(0);
			if (lastIdx != 0) {
				((GeometryGradientWayStyle) styles.get(lastIdx - 1)).endXY = new PointF(tx.get(lastIdx - 1), ty.get(lastIdx - 1));
			}
		}
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
		return scaleType == null ?
				new GeometrySolidWayStyle(getContext(), color, width, customPointColor) :
				new GeometryGradientWayStyle(getContext(), width);
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
	protected PathGeometryZoom getGeometryZoom(RotatedTileBox tb, Map<Integer, PathGeometryZoom> zooms) {
		if (scaleType == null) {
			return super.getGeometryZoom(tb, zooms);
		}
		int zoom = tb.getZoom();
		PathGeometryZoom zm = zooms.get(zoom);
		if (zm == null) {
			zm = new GradientPathGeometryZoom(getLocationProvider(), tb, true);
			zooms.put(zoom, zm);
		}
		return zm;
	}

	private static class GradientGeometryWayProvider implements GeometryWayProvider {

		private final RouteColorize routeColorize;
		private final List<RouteColorizationPoint> locations;

		public GradientGeometryWayProvider(RouteColorize routeColorize) {
			this.routeColorize = routeColorize;
			locations = routeColorize.getResult(false);
		}

		public List<RouteColorizationPoint> simplify(int zoom) {
			return routeColorize.simplify(zoom);
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

		public PointF startXY;
		public PointF endXY;

		public GeometryGradientWayStyle(RouteGeometryWayContext context, Float width) {
			super(context, 0xFFFFFFFF, width);
		}

		@Override
		public Bitmap getPointBitmap() {
			return getContext().getArrowBitmap();
		}

		@Override
		public boolean equals(Object other) {
			return this == other;
		}
	}

	private static class GeometrySolidWayStyle extends GeometryWayStyle<RouteGeometryWayContext> {

		private Integer pointColor;

		GeometrySolidWayStyle(RouteGeometryWayContext context, Integer color, Float width,
							  Integer pointColor) {
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
			return other instanceof GeometrySolidWayStyle;
		}
	}
}