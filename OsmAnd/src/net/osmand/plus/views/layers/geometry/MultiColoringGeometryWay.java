package net.osmand.plus.views.layers.geometry;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.helpers.ColorPaletteHelper;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.shared.routing.Gpx3DWallColorType;
import net.osmand.plus.track.Track3DStyle;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer.DrawPathData31;
import net.osmand.render.RenderingRuleSearchRequest;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RouteSegmentResult;
import net.osmand.router.RouteStatisticsHelper.RouteSegmentAttribute;
import net.osmand.router.RouteStatisticsHelper.RouteStatisticComputer;
import net.osmand.shared.ColorPalette;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.shared.gpx.GradientScaleType;
import net.osmand.shared.routing.ColoringType;
import net.osmand.shared.routing.RouteColorize;
import net.osmand.shared.routing.RouteColorize.ColorizationType;
import net.osmand.shared.routing.RouteColorize.RouteColorizationPoint;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import gnu.trove.list.array.TByteArrayList;

public abstract class MultiColoringGeometryWay<C extends MultiColoringGeometryWayContext,
		D extends MultiColoringGeometryWayDrawer<C>> extends GeometryWay<C, D> {

	protected int customColor;
	protected float customWidth;
	protected float[] dashPattern;
	@NonNull
	protected ColoringType coloringType;
	protected String routeInfoAttribute;
	protected String gradientPalette;

	protected boolean coloringChanged;

	@Nullable
	private Track3DStyle track3DStyle;

	public MultiColoringGeometryWay(C context, D drawer) {
		super(context, drawer);
		coloringType = context.getDefaultColoringType();
		gradientPalette = context.getDefaultGradientPalette();
	}

	protected void updateStylesWidth(@Nullable Float newWidth) {
		if (!styleMap.isEmpty()) {
			for (GeometryWayStyle<?> style : styleMap.values()) {
				style.width = newWidth;
			}
		} else {
			for (List<DrawPathData31> pathDataList : pathsData31Cache) {
				for (DrawPathData31 pathData : pathDataList) {
					if (pathData.style != null) {
						pathData.style.width = newWidth;
					}
				}
			}
		}
		resetArrowsProvider();
	}

	protected void updateTrack3DStyle(@NonNull GeometryWayStyle<?> style, @Nullable Track3DStyle track3DStyle) {
		if (track3DStyle != null) {
			style.trackVisualizationType = track3DStyle.getVisualizationType();
			style.trackWallColorType = track3DStyle.getWallColorType();
			style.trackLinePositionType = track3DStyle.getLinePositionType();
			style.additionalExaggeration = track3DStyle.getExaggeration();
			style.elevationMeters = track3DStyle.getElevation();
		} else {
			style.trackVisualizationType = Gpx3DVisualizationType.NONE;
			style.trackWallColorType = Gpx3DWallColorType.NONE;
			style.trackLinePositionType = Gpx3DLinePositionType.TOP;
			style.additionalExaggeration = 1f;
			style.elevationMeters = 1000f;
		}
	}

	protected void updateTrack3DStyle(@Nullable Track3DStyle track3DStyle) {
		this.track3DStyle = track3DStyle;
		if (!styleMap.isEmpty()) {
			for (GeometryWayStyle<?> style : styleMap.values()) {
				updateTrack3DStyle(style, track3DStyle);
			}
		} else {
			for (List<DrawPathData31> pathDataList : pathsData31Cache) {
				for (DrawPathData31 pathData : pathDataList) {
					if (pathData.style != null) {
						updateTrack3DStyle(pathData.style, track3DStyle);
					}
				}
			}
		}
	}

	protected void updateStylesDashPattern(@Nullable float[] dashPattern) {
		for (GeometryWayStyle<?> style : styleMap.values()) {
			style.dashPattern = dashPattern;
		}
		resetSymbolProviders();
	}

	protected void updatePaints(@Nullable Float width, @NonNull ColoringType routeColoringType) {
		if (width != null) {
			getContext().updateBorderWidth(width);
		}
	}

	protected void updateGradientWay(RotatedTileBox tb, List<Location> locations) {
		GpxFile gpxFile = GpxUiHelper.makeGpxFromLocations(locations, getContext().getApp());
		GradientScaleType gradientScaleType = coloringType.toGradientScaleType();
		if (gradientScaleType != null) {
			ColorizationType colorizationType = gradientScaleType.toColorizationType();
			ColorPaletteHelper paletteHelper = getContext().getApp().getColorPaletteHelper();
			ColorPalette colorPalette = paletteHelper.getGradientColorPaletteSync(colorizationType, gradientPalette);

			RouteColorize routeColorize = new RouteColorize(gpxFile, null, colorizationType, colorPalette, 0);
			List<RouteColorizationPoint> points = routeColorize.getResult();
			updateWay(new GradientGeometryWayProvider(routeColorize, points, null), createGradientStyles(points), tb);
		}
	}

	@NonNull
	protected Map<Integer, GeometryWayStyle<?>> createGradientStyles(@NonNull List<RouteColorizationPoint> points) {
		Map<Integer, GeometryWayStyle<?>> styleMap = new TreeMap<>();
		updateTrack3DStyle(getTrack3DStyle());
		Track3DStyle track3DStyle = getTrack3DStyle();
		for (int i = 0; i < points.size() - 1; i++) {
			GeometryGradientWayStyle<?> style = getGradientWayStyle();
			style.currColor = points.get(i).getPrimaryColor();
			style.nextColor = points.get(i + 1).getPrimaryColor();
			styleMap.put(i, style);
			updateTrack3DStyle(style, track3DStyle);
		}
		return styleMap;
	}

	@NonNull
	protected Map<Integer, GeometryWayStyle<?>> createGradient3DStyles(@NonNull List<RouteColorizationPoint> points) {
		Map<Integer, GeometryWayStyle<?>> styleMap = new TreeMap<>();
		updateTrack3DStyle(getTrack3DStyle());
		Track3DStyle track3DStyle = getTrack3DStyle();
		for (int i = 0; i < points.size() - 1; i++) {
			RouteColorizationPoint currentPoint = points.get(i);
			RouteColorizationPoint nextPoint = points.get(i + 1);

			GeometryGradient3DWayStyle<?> style = getGradient3DWayStyle();
			style.currColor = currentPoint.getPrimaryColor();
			style.nextColor = nextPoint.getPrimaryColor();
			style.currOutlineColor = currentPoint.getSecondaryColor();
			style.nextOutlineColor = nextPoint.getSecondaryColor();

			styleMap.put(i, style);
			updateTrack3DStyle(style, track3DStyle);
		}
		return styleMap;
	}

	protected void updateSolidMultiColorRoute(RotatedTileBox tileBox, List<Location> locations,
	                                          List<RouteSegmentResult> routeSegments) {
		List<Integer> colors = getRouteInfoAttributesColors(locations, routeSegments);
		if (Algorithms.isEmpty(colors)) {
			updateWay(Collections.emptyList(), Collections.emptyMap(), tileBox);
			return;
		}
		Map<Integer, GeometryWayStyle<?>> styleMap = new TreeMap<>();
		for (int i = 0; i < colors.size(); ) {
			int color = colors.get(i);
			GeometrySolidWayStyle<?> style = getSolidWayStyle(color);
			styleMap.put(i, style);
			i++;
			while (i < colors.size() && colors.get(i) == color) {
				i++;
			}
		}
		updateWay(locations, styleMap, tileBox);
	}

	protected List<Integer> getRouteInfoAttributesColors(List<Location> locations, List<RouteSegmentResult> routeSegments) {
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
			color = color == 0 ? ColorPalette.Companion.getLIGHT_GREY() : color;

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

	protected int getIdxOfFirstSegmentLocation(List<Location> locations, List<RouteSegmentResult> routeSegments) {
		int locationsIdx = 0;
		LatLon segmentStartPoint = routeSegments.get(0).getStartPoint();
		while (locationsIdx < locations.size()) {
			Location location = locations.get(locationsIdx);
			if (location.getLatitude() == segmentStartPoint.getLatitude()
					&& location.getLongitude() == segmentStartPoint.getLongitude()) {
				break;
			}
			locationsIdx++;
		}
		return locationsIdx == locations.size() ? 0 : locationsIdx;
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
	protected void addLocation(RotatedTileBox tb, int locationIdx, double dist,
	                           GeometryWayStyle<?> style, List<GeometryWayPoint> points) {
		super.addLocation(tb, locationIdx, dist, style, points);

		if (points.size() > 1) {
			if (style instanceof GeometryGradientWayStyle<?>) {
				GeometryGradientWayStyle<?> prevStyle = (GeometryGradientWayStyle<?>) points.get(points.size() - 2).style;
				GeometryGradientWayStyle<?> currStyle = (GeometryGradientWayStyle<?>) style;
				if (!prevStyle.equals(currStyle)) {
					prevStyle.nextColor = currStyle.currColor;
				}
			}
			if (style instanceof GeometryGradient3DWayStyle<?>) {
				GeometryGradient3DWayStyle<?> prevStyle = (GeometryGradient3DWayStyle<?>) points.get(points.size() - 2).style;
				GeometryGradient3DWayStyle<?> currStyle = (GeometryGradient3DWayStyle<?>) style;
				if (!prevStyle.equals(currStyle)) {
					prevStyle.nextOutlineColor = currStyle.currOutlineColor;
				}
			}
		}
	}

	@Override
	protected boolean addInitialPoint(RotatedTileBox tb, double topLatitude, double leftLongitude,
	                                  double bottomLatitude, double rightLongitude, GeometryWayStyle<?> style,
	                                  boolean previousVisible, Location lastPoint, int startLocationIndex) {
		boolean added = super.addInitialPoint(tb, topLatitude, leftLongitude, bottomLatitude, rightLongitude,
				style, previousVisible, lastPoint, startLocationIndex);
		if (!added) {
			return false;
		}
		if (style instanceof GeometryGradient3DWayStyle) {
			GeometryGradient3DWayStyle<?> gradientStyle = (GeometryGradient3DWayStyle<?>) style;
			Geometry3DWayProvider provider = get3DLocationProvider();
			if (startLocationIndex == 0) {
				int startColor = provider.getColor(0);
				int startOutlineColor = provider.getOutlineColor(0);
				gradientStyle.currColor = startColor;
				gradientStyle.nextColor = startColor;
				gradientStyle.currOutlineColor = startOutlineColor;
				gradientStyle.nextOutlineColor = startOutlineColor;
			} else {
				double percent = getProjectionCoeff(provider, lastPoint, startLocationIndex);
				int prevColor = provider.getColor(startLocationIndex - 1);
				int nextColor = provider.getColor(startLocationIndex);
				int prevOutlineColor = provider.getColor(startLocationIndex - 1);
				int nextOutlineColor = provider.getColor(startLocationIndex);

				gradientStyle.currColor = ColorPalette.Companion.getIntermediateColor(prevColor, nextColor, percent);
				gradientStyle.nextColor = nextColor;
				gradientStyle.currOutlineColor = ColorPalette.Companion.getIntermediateColor(prevOutlineColor, nextOutlineColor, percent);
				gradientStyle.nextOutlineColor = nextOutlineColor;
			}
		} else if (style instanceof GeometryGradientWayStyle) {
			GeometryGradientWayStyle<?> gradientStyle = (GeometryGradientWayStyle<?>) style;
			GradientGeometryWayProvider provider = getGradientLocationProvider();
			if (startLocationIndex == 0) {
				int startColor = provider.getColor(0);
				gradientStyle.currColor = startColor;
				gradientStyle.nextColor = startColor;
			} else {
				double percent = getProjectionCoeff(provider, lastPoint, startLocationIndex);
				int prevColor = provider.getColor(startLocationIndex - 1);
				int nextColor = provider.getColor(startLocationIndex);
				gradientStyle.currColor = ColorPalette.Companion.getIntermediateColor(prevColor, nextColor, percent);
				gradientStyle.nextColor = nextColor;
			}
		} else if (coloringType.isRouteInfoAttribute() && style instanceof GeometrySolidWayStyle<?>) {
			GeometrySolidWayStyle<?> prevStyle = (GeometrySolidWayStyle<?>) style;
			GeometrySolidWayStyle<?> transparentWayStyle = getSolidWayStyle(Color.TRANSPARENT);
			int prevStyleIdx = startLocationIndex > 0 ? startLocationIndex - 1 : 0;
			prevStyle.color = getStyle(prevStyleIdx, transparentWayStyle).color;
		}
		return true;
	}

	private double getProjectionCoeff(@NonNull GeometryWayProvider provider, @NonNull Location location, int startIndex) {
		double currLat = location.getLatitude();
		double currLon = location.getLongitude();
		double prevLat = provider.getLatitude(startIndex - 1);
		double prevLon = provider.getLongitude(startIndex - 1);
		double nextLat = provider.getLatitude(startIndex);
		double nextLon = provider.getLongitude(startIndex);
		return MapUtils.getProjectionCoeff(currLat, currLon, prevLat, prevLon, nextLat, nextLon);
	}

	@Override
	protected boolean shouldSkipLocation(@Nullable TByteArrayList simplification,
	                                     Map<Integer, GeometryWayStyle<?>> styleMap, int locationIdx) {
		return coloringType.isGradient()
				? simplification != null && simplification.getQuick(locationIdx) == 0
				: super.shouldSkipLocation(simplification, styleMap, locationIdx);
	}

	@NonNull
	@Override
	public abstract GeometryWayStyle<?> getDefaultWayStyle();

	@NonNull
	public abstract GeometrySolidWayStyle<?> getSolidWayStyle(int lineColor);

	@NonNull
	public GeometryGradientWayStyle<?> getGradientWayStyle() {
		return new GeometryGradientWayStyle<>(getContext(), customColor, customWidth);
	}

	@NonNull
	public GeometryGradient3DWayStyle<?> getGradient3DWayStyle() {
		return new GeometryGradient3DWayStyle<>(getContext(), customColor, customWidth);
	}

	private Geometry3DWayProvider get3DLocationProvider() {
		return (Geometry3DWayProvider) getLocationProvider();
	}

	private GradientGeometryWayProvider getGradientLocationProvider() {
		return (GradientGeometryWayProvider) getLocationProvider();
	}

	@ColorInt
	protected int getContrastLineColor(@ColorInt int lineColor) {
		return ColorUtilities.getContrastColor(getContext().getCtx(), lineColor, false);
	}

	@Nullable
	protected Track3DStyle getTrack3DStyle() {
		return track3DStyle;
	}

	@Nullable
	protected ColoringType getOutlineColoringType() {
		return ColoringType.Companion.valueOf(getGpx3DWallColorType());
	}

	@NonNull
	protected Gpx3DWallColorType getGpx3DWallColorType() {
		return getGpx3DWallColorType(getTrack3DStyle());
	}

	@NonNull
	protected Gpx3DWallColorType getGpx3DWallColorType(@Nullable Track3DStyle style) {
		return style != null && style.getVisualizationType().is3dType() ? style.getWallColorType() : Gpx3DWallColorType.NONE;
	}
}