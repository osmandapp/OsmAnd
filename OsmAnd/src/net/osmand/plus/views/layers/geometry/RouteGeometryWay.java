package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.plus.routing.RouteColoringType;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteColorize;
import net.osmand.router.RouteColorize.ColorizationType;
import net.osmand.router.RouteColorize.RouteColorizationPoint;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RouteStatisticsHelper;
import net.osmand.router.RouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.router.RouteStatisticsHelper.RouteStatistics;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import gnu.trove.list.array.TByteArrayList;

public class RouteGeometryWay extends GeometryWay<RouteGeometryWayContext, RouteGeometryWayDrawer> {

	private final RoutingHelper helper;
	private RouteCalculationResult route;

	private Integer customColor;
	private Float customWidth;
	private Integer customPointColor;
	private RouteColoringType routeColoringType;

	private boolean needUpdate;

	public RouteGeometryWay(RouteGeometryWayContext context) {
		super(context, new RouteGeometryWayDrawer(context, true));
		this.helper = context.getApp().getRoutingHelper();
	}

	public void setRouteStyleParams(@Nullable @ColorInt Integer color,
	                                @Nullable Float width,
	                                @Nullable @ColorInt Integer pointColor,
	                                @NonNull RouteColoringType routeColoringType) {
		this.needUpdate = this.routeColoringType != routeColoringType;

		if (!Algorithms.objectEquals(customWidth, width)) {
			for (GeometryWayStyle<?> style : styleMap.values()) {
				style.width = width;
			}
		}
		this.customColor = color;
		this.customWidth = width;
		this.customPointColor = pointColor;
		this.routeColoringType = routeColoringType;
		if (width != null) {
			getContext().getAttrs().shadowPaint.setStrokeWidth(width + getContext().getDensity() * 2);
		}

		Paint.Cap cap = routeColoringType.isGradient() || routeColoringType.isRouteInfoAttribute() ?
				Paint.Cap.ROUND : getContext().getAttrs().paint.getStrokeCap();
		getContext().getAttrs().customColorPaint.setStrokeCap(cap);
	}

	public void updateRoute(@NonNull RotatedTileBox tb, @NonNull RouteCalculationResult route) {
		needUpdate = true;
		if (needUpdate || tb.getMapDensity() != getMapDensity() || this.route != route) {
			this.route = route;
			needUpdate = false;
			List<Location> locations = route.getImmutableAllLocations();

			if (routeColoringType.isGradient()) {
				updateGradientRoute(tb, locations);
			} else if (routeColoringType.isRouteInfoAttribute()) {
				updateSolidMultiColorRoute(tb, route);
			} else {
				updateWay(locations, tb);
			}
		}
	}

	private void updateGradientRoute(RotatedTileBox tb, List<Location> locations) {
		GPXFile gpxFile = GpxUiHelper.makeGpxFromLocations(locations, getContext().getApp());
		// Start point can have wrong zero altitude
		List<GPXUtilities.WptPt> pts = gpxFile.tracks.get(0).segments.get(0).points;
		GPXUtilities.WptPt firstPt = pts.get(0);
		if (firstPt.ele == 0) {
			firstPt.ele = pts.get(1).ele;
		}

		ColorizationType colorizationType = routeColoringType.toGradientScaleType().toColorizationType();
		RouteColorize routeColorize = new RouteColorize(tb.getZoom(), gpxFile, null, colorizationType, 0);
		List<RouteColorizationPoint> points = routeColorize.getResult(false);

		updateWay(new GradientGeometryWayProvider(routeColorize, points), createGradientStyles(points), tb);
	}

	private Map<Integer, GeometryWayStyle<?>> createGradientStyles(List<RouteColorizationPoint> points) {
		Map<Integer, GeometryWayStyle<?>> styleMap = new TreeMap<>();
		for (int i = 0; i < points.size() - 1; i++) {
			GeometryGradientWayStyle style = getGradientWayStyle();
			style.currColor = points.get(i).color;
			style.nextColor = points.get(i + 1).color;
			styleMap.put(i, style);
		}
		return styleMap;
	}

	private void updateSolidMultiColorRoute(RotatedTileBox tileBox, RouteCalculationResult route) {
		OsmandApplication app = getContext().getApp();
		boolean night = app.getDaynightHelper().isNightModeForMapControls();
		RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		RenderingRulesStorage defaultRenderer = app.getRendererRegistry().defaultRender();
		MapRenderRepositories maps = app.getResourceManager().getRenderer();
		RenderingRuleSearchRequest currentSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(currentRenderer, night);
		RenderingRuleSearchRequest defaultSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(defaultRenderer, night);

		List<RouteSegmentResult> routeSegments = route.getOriginalRoute();
		List<RouteStatistics> routeStatisticsList = RouteStatisticsHelper.calculateRouteStatistic(routeSegments,
				Collections.singletonList(routeColoringType.getAttrName()), currentRenderer,
				defaultRenderer, currentSearchRequest, defaultSearchRequest);

		List<Location> srcLocations = route.getImmutableAllLocations();
		List<Location> locations = new ArrayList<>();
		RouteStatistics routeStatistics = routeStatisticsList.get(0);
		Map<Integer, GeometryWayStyle<?>> styleMap = new HashMap<>();

		int start = getIdxOfFirstSegmentsLocation(srcLocations, routeSegments);
		if (start != 0) {
			locations.add(srcLocations.get(0));
		}
		int attrsIdx = 0;
		for (int i = start; i < srcLocations.size(); i++) {
			locations.add(srcLocations.get(i));
			RouteSegmentAttribute attr;
			if (attrsIdx + 1 <=	 routeStatistics.elements.size()) {
				attr = routeStatistics.elements.get(attrsIdx);
			} else {
				continue;
			}
			if (attr.getStartLocation() == null || attr.getEndLocation() == null) {
				attrsIdx++;
				continue;
			}
			if (attr.getStartLocationIdx() == i) {
				GeometrySolidWayStyle style = getSolidWayStyle(attr.getColor());
				styleMap.put(locations.size(), style);
				locations.add(attr.getStartLocation());
			}
			if (attr.getEndLocationIdx() == i) {
				locations.add(attr.getEndLocation());
				attrsIdx++;
			}
		}

		updateWay(locations, styleMap, tileBox);
	}

	private int getIdxOfFirstSegmentsLocation(List<Location> locations, List<RouteSegmentResult> segments) {
		LatLon firstSegmentsLocation = segments.get(0).getStartPoint();
		Location firstLocation = locations.get(0);
		if (firstSegmentsLocation.getLatitude() == firstLocation.getLatitude()
				&& firstSegmentsLocation.getLongitude() == firstLocation.getLongitude()) {
			return 0;
		} else {
			return 1;
		}
	}

	@Override
	protected void addLocation(RotatedTileBox tb, int locationIdx, GeometryWayStyle<?> style, List<Float> tx, List<Float> ty, List<Double> angles, List<Double> distances, double dist, List<GeometryWayStyle<?>> styles) {
		super.addLocation(tb, locationIdx, style, tx, ty, angles, distances, dist, styles);
		if (style instanceof GeometryGradientWayStyle && styles.size() > 1) {
			GeometryGradientWayStyle prevStyle = (GeometryGradientWayStyle) styles.get(styles.size() - 2);
			GeometryGradientWayStyle currStyle = (GeometryGradientWayStyle) style;
			prevStyle.nextColor = currStyle.currColor;
		}
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
				gradientWayStyle.currColor = startColor;
				gradientWayStyle.nextColor = startColor;
			} else {
				double currLat = lastPoint.getLatitude();
				double currLon = lastPoint.getLongitude();
				double prevLat = locationProvider.getLatitude(startLocationIndex - 1);
				double prevLon = locationProvider.getLongitude(startLocationIndex - 1);
				double nextLat = locationProvider.getLatitude(startLocationIndex);
				double nextLon = locationProvider.getLongitude(startLocationIndex);
				double percent = MapUtils.getProjectionCoeff(currLat, currLon, prevLat, prevLon, nextLat, nextLon);
				int prevColor = locationProvider.getColor(startLocationIndex - 1);
				int nextColor = locationProvider.getColor(startLocationIndex);
				gradientWayStyle.currColor = RouteColorize.getGradientColor(prevColor, nextColor, percent);
				gradientWayStyle.nextColor = nextColor;
			}
		} else if (routeColoringType.isRouteInfoAttribute() && style instanceof GeometrySolidWayStyle) {
			GeometrySolidWayStyle solidWayStyle = (GeometrySolidWayStyle) style;
			GeometrySolidWayStyle transparentWayStyle = getSolidWayStyle(Color.TRANSPARENT);
			solidWayStyle.color = getStyle(startLocationIndex, transparentWayStyle).color;
		}
		return previousVisible;
	}

	@Override
	protected boolean shouldSkipLocation(TByteArrayList simplification, Map<Integer, GeometryWayStyle<?>> styleMap, int locationIdx) {
		return routeColoringType.isGradient()
				? simplification.getQuick(locationIdx) == 0
				: super.shouldSkipLocation(simplification, styleMap, locationIdx);
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
		if (routeColoringType.isGradient()) {
			return new GeometryGradientWayStyle(getContext(), color, width);
		}
		return new GeometrySolidWayStyle(getContext(), color, width, customPointColor);
	}

	@NonNull
	public GeometrySolidWayStyle getSolidWayStyle(int lineColor) {
		Paint paint = getContext().getAttrs().paint;
		float width = customWidth != null ? customWidth : paint.getStrokeWidth();
		return new GeometrySolidWayStyle(getContext(), lineColor, width, customPointColor);
	}

	@NonNull
	public GeometryGradientWayStyle getGradientWayStyle() {
		Paint paint = getContext().getAttrs().paint;
		int color = customColor != null ? customColor : paint.getColor();
		float width = customWidth != null ? customWidth : paint.getStrokeWidth();
		return new GeometryGradientWayStyle(getContext(), color, width);
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
		if (routeColoringType.isGradient()) {
			int zoom = tb.getZoom();
			PathGeometryZoom zm = zooms.get(zoom);
			if (zm == null) {
				zm = new GradientPathGeometryZoom(getLocationProvider(), tb, true);
				zooms.put(zoom, zm);
			}
			return zm;
		}
		return super.getGeometryZoom(tb);
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

		public int currColor;
		public int nextColor;

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
			return currColor == o.currColor && nextColor == o.nextColor;
		}
	}

	private static class GeometrySolidWayStyle extends GeometryWayStyle<RouteGeometryWayContext> {

		private final Integer directionArrowsColor;

		GeometrySolidWayStyle(RouteGeometryWayContext context, Integer color, Float width, Integer directionArrowsColor) {
			super(context, color, width);
			this.directionArrowsColor = directionArrowsColor;
		}

		@Override
		public Bitmap getPointBitmap() {
			return getContext().getArrowBitmap();
		}

		@Override
		public Integer getPointColor() {
			return directionArrowsColor;
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
			return Algorithms.objectEquals(directionArrowsColor, o.directionArrowsColor);
		}
	}
}