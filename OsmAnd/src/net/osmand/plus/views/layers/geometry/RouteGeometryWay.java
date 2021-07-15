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
import net.osmand.router.RouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.router.RouteStatisticsHelper.RouteStatisticComputer;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
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
	private String routeInfoAttribute;

	private boolean needUpdate;

	public RouteGeometryWay(RouteGeometryWayContext context) {
		super(context, new RouteGeometryWayDrawer(context, true));
		this.helper = context.getApp().getRoutingHelper();
	}

	public void setRouteStyleParams(@Nullable @ColorInt Integer color,
	                                @Nullable Float width,
	                                @Nullable @ColorInt Integer pointColor,
	                                @NonNull RouteColoringType routeColoringType,
	                                @Nullable String routeInfoAttribute) {
		this.needUpdate = this.routeColoringType != routeColoringType
				|| routeColoringType == RouteColoringType.ATTRIBUTE
				&& !Algorithms.objectEquals(this.routeInfoAttribute, routeInfoAttribute);

		boolean widthChanged = !Algorithms.objectEquals(customWidth, width);
		if (widthChanged) {
			updateStylesWidth(width);
		}
		updatePaints(width, routeColoringType);
		getDrawer().setRouteColoringType(routeColoringType);

		this.customColor = color;
		this.customWidth = width;
		this.customPointColor = pointColor;
		this.routeColoringType = routeColoringType;
		this.routeInfoAttribute = routeInfoAttribute;
	}

	private void updateStylesWidth(@Nullable Float newWidth) {
		for (GeometryWayStyle<?> style : styleMap.values()) {
			style.width = newWidth;
		}
	}

	private void updatePaints(@Nullable Float width, @NonNull RouteColoringType routeColoringType) {
		if (width != null) {
			getContext().updateBorderWidth(width);
		}
		Paint.Cap cap = routeColoringType.isGradient() || routeColoringType.isRouteInfoAttribute() ?
				Paint.Cap.ROUND : getContext().getAttrs().paint.getStrokeCap();
		getContext().getAttrs().customColorPaint.setStrokeCap(cap);
	}

	public void updateRoute(@NonNull RotatedTileBox tb, @NonNull RouteCalculationResult route) {
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
		List<Integer> colors = getRouteInfoAttributesColors(route);
		if (Algorithms.isEmpty(colors)) {
			updateWay(Collections.emptyList(), Collections.emptyMap(), tileBox);
			return;
		}

		Map<Integer, GeometryWayStyle<?>> styleMap = new TreeMap<>();

		for (int i = 0; i < colors.size(); ) {
			int color = colors.get(i);
			GeometrySolidWayStyle style = getSolidWayStyle(color);
			styleMap.put(i, style);

			i++;
			while (i < colors.size() && colors.get(i) == color) {
				i++;
			}
		}

		updateWay(route.getImmutableAllLocations(), styleMap, tileBox);
	}

	private List<Integer> getRouteInfoAttributesColors(RouteCalculationResult route) {
		List<Location> locations = route.getImmutableAllLocations();
		List<RouteSegmentResult> routeSegments = route.getOriginalRoute();
		if (Algorithms.isEmpty(routeSegments)) {
			return Collections.emptyList();
		}

		int firstSegmentLocationIdx = getIdxOfFirstSegmentLocation(locations, routeSegments);

		RouteStatisticComputer statisticComputer = createRouteStatisticsComputer();
		List<Integer> colors = new ArrayList<>(locations.size());
		for (int i = 0; i < routeSegments.size(); i++) {
			RouteSegmentResult segment = routeSegments.get(i);
			RouteSegmentAttribute attribute =
					statisticComputer.classifySegment(routeInfoAttribute, -1, segment.getObject());
			int color = attribute.getColor();

			if (i == 0) {
				for (int j = 0; j < firstSegmentLocationIdx; j++) {
					colors.add(color);
				}
			}

			int pointsSize = Math.abs(segment.getStartPointIndex() - segment.getEndPointIndex());
			for (int j = 0; j < pointsSize; j++) {
				colors.add(color);
			}

			if (i == routeSegments.size() - 1) {
				int start = colors.size();
				for (int j = start; j < locations.size(); j++) {
					colors.add(color);
				}
			}
		}

		return colors;
	}

	private int getIdxOfFirstSegmentLocation(List<Location> locations, List<RouteSegmentResult> routeSegments) {
		int locationsIdx = 0;
		LatLon segmentStartPoint = routeSegments.get(0).getStartPoint();
		while (true) {
			Location location = locations.get(locationsIdx);
			if (location.getLatitude() == segmentStartPoint.getLatitude()
					&& location.getLongitude() == segmentStartPoint.getLongitude()) {
				break;
			}
			locationsIdx++;
		}
		return locationsIdx;
	}

	private RouteStatisticComputer createRouteStatisticsComputer() {
		OsmandApplication app = getContext().getApp();
		boolean night = app.getDaynightHelper().isNightModeForMapControls();
		RenderingRulesStorage currentRenderer = app.getRendererRegistry().getCurrentSelectedRenderer();
		RenderingRulesStorage defaultRenderer = app.getRendererRegistry().defaultRender();
		MapRenderRepositories maps = app.getResourceManager().getRenderer();
		RenderingRuleSearchRequest currentSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(currentRenderer, night);
		RenderingRuleSearchRequest defaultSearchRequest =
				maps.getSearchRequestWithAppliedCustomRules(defaultRenderer, night);

		return new RouteStatisticComputer(currentRenderer, defaultRenderer,
				currentSearchRequest, defaultSearchRequest);
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
				gradientWayStyle.currColor = RouteColorize.getIntermediateColor(prevColor, nextColor, percent);
				gradientWayStyle.nextColor = nextColor;
			}
		} else if (routeColoringType.isRouteInfoAttribute() && style instanceof GeometrySolidWayStyle) {
			GeometrySolidWayStyle prevStyle = (GeometrySolidWayStyle) style;
			GeometrySolidWayStyle transparentWayStyle = getSolidWayStyle(Color.TRANSPARENT);
			int prevStyleIdx = startLocationIndex > 0 ? startLocationIndex - 1 : 0;
			prevStyle.color = getStyle(prevStyleIdx, transparentWayStyle).color;
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

	public static class GeometrySolidWayStyle extends GeometryWayStyle<RouteGeometryWayContext> {

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