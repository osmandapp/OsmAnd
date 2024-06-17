package net.osmand.plus.views.layers.geometry;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.ColorPalette;
import net.osmand.IndexConstants;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.data.RotatedTileBox;
import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.render.MapRenderRepositories;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.track.Gpx3DLinePositionType;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.plus.track.Gpx3DWallColorType;
import net.osmand.plus.track.GradientScaleType;
import net.osmand.plus.track.Track3DStyle;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.views.layers.geometry.GeometryWayDrawer.DrawPathData31;
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

	protected boolean coloringChanged;

	@Nullable
	private Track3DStyle track3DStyle;

	public MultiColoringGeometryWay(C context, D drawer) {
		super(context, drawer);
		coloringType = context.getDefaultColoringType();
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
		Gpx3DVisualizationType trackVisualizationType = track3DStyle == null ? Gpx3DVisualizationType.NONE : track3DStyle.getVisualizationType();
		Gpx3DWallColorType trackWallColorType = track3DStyle == null ? Gpx3DWallColorType.NONE : track3DStyle.getWallColorType();
		Gpx3DLinePositionType trackLinePositionType = track3DStyle == null ? Gpx3DLinePositionType.TOP : track3DStyle.getLinePositionType();
		float exaggeration = track3DStyle == null ? 1f : track3DStyle.getExaggeration();
		float elevationMeters = track3DStyle == null ? 1000f : track3DStyle.getElevation();
		style.trackVisualizationType = trackVisualizationType;
		style.trackWallColorType = trackWallColorType;
		style.trackLinePositionType = trackLinePositionType;
		style.additionalExaggeration = exaggeration;
		style.elevationMeters = elevationMeters;
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
		GPXFile gpxFile = GpxUiHelper.makeGpxFromLocations(locations, getContext().getApp());
		GradientScaleType gradientScaleType = coloringType.toGradientScaleType();
		if (gradientScaleType != null) {
			ColorizationType colorizationType = gradientScaleType.toColorizationType();
			File filePalette = getContext().getApp().getAppPath(IndexConstants.CLR_PALETTE_DIR +
					"route_" + colorizationType.name().toLowerCase() + "_default.txt");
			ColorPalette colorPalette = null;
			try {
				if (filePalette.exists()) {
					colorPalette = ColorPalette.parseColorPalette(new FileReader(filePalette));
				}
			} catch (IOException e) {
				PlatformUtil.getLog(MultiColoringGeometryWay.class).error("Error reading color file ", e);
			}
			RouteColorize routeColorize = new RouteColorize(gpxFile, null, colorizationType, colorPalette, 0);
			List<RouteColorizationPoint> points = routeColorize.getResult();
			updateWay(new GradientGeometryWayProvider(routeColorize, points, null), createGradientStyles(points), tb);
		}
	}

	protected Map<Integer, GeometryWayStyle<?>> createGradientStyles(List<RouteColorizationPoint> points) {
		Map<Integer, GeometryWayStyle<?>> styleMap = new TreeMap<>();
		updateTrack3DStyle(getTrack3DStyle());
		Track3DStyle track3DStyle = getTrack3DStyle();
		for (int i = 0; i < points.size() - 1; i++) {
			GeometryGradientWayStyle<?> style = getGradientWayStyle();
			style.currColor = points.get(i).color;
			style.nextColor = points.get(i + 1).color;
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

	private List<Integer> getRouteInfoAttributesColors(List<Location> locations, List<RouteSegmentResult> routeSegments) {
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
			color = color == 0 ? net.osmand.ColorPalette.LIGHT_GREY : color;

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
		if (style instanceof GeometryGradientWayStyle<?> && points.size() > 1) {
			GeometryGradientWayStyle<?> prevStyle = (GeometryGradientWayStyle<?>) points.get(points.size() - 2).style;
			GeometryGradientWayStyle<?> currStyle = (GeometryGradientWayStyle<?>) style;
			if (!prevStyle.equals(currStyle)) {
				prevStyle.nextColor = currStyle.currColor;
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

		if (style instanceof GeometryGradientWayStyle) {
			GeometryGradientWayStyle<?> gradientWayStyle = (GeometryGradientWayStyle<?>) style;
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
				gradientWayStyle.currColor = ColorPalette.getIntermediateColor(prevColor, nextColor, percent);
				gradientWayStyle.nextColor = nextColor;
			}
		} else if (coloringType.isRouteInfoAttribute() && style instanceof GeometrySolidWayStyle<?>) {
			GeometrySolidWayStyle<?> prevStyle = (GeometrySolidWayStyle<?>) style;
			GeometrySolidWayStyle<?> transparentWayStyle = getSolidWayStyle(Color.TRANSPARENT);
			int prevStyleIdx = startLocationIndex > 0 ? startLocationIndex - 1 : 0;
			prevStyle.color = getStyle(prevStyleIdx, transparentWayStyle).color;
		}
		return true;
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

	private GradientGeometryWayProvider getGradientLocationProvider() {
		return (GradientGeometryWayProvider) getLocationProvider();
	}

	@ColorInt
	protected int getContrastLineColor(@ColorInt int lineColor) {
		return ColorUtilities.getContrastColor(getContext().getCtx(), lineColor, false);
	}

	protected Track3DStyle getTrack3DStyle() {
		return track3DStyle;
	}
}