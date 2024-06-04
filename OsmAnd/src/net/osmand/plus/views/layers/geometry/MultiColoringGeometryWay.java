package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;
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
import net.osmand.plus.utils.AndroidUtils;
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

public abstract class MultiColoringGeometryWay
		<C extends MultiColoringGeometryWayContext, D extends MultiColoringGeometryWayDrawer<C>> extends
		GeometryWay<C, D> {

	protected int customColor;
	protected float customWidth;
	protected float[] dashPattern;
	@NonNull
	protected ColoringType coloringType;
	protected String routeInfoAttribute;

	protected boolean coloringChanged;
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
		float exaggeration = track3DStyle == null ? 1f : track3DStyle.getAdditionalExaggeration();
		float elevationMeters = track3DStyle == null ? 1000f : track3DStyle.getElevationMeters();
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
				PlatformUtil.getLog(MultiColoringGeometryWay.class).error("Error reading color file ",
						e);
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

	protected static class GradientGeometryWayProvider implements GeometryWayProvider {

		private final RouteColorize routeColorize;
		private final List<RouteColorizationPoint> locations;
		private final List<Float> pointHeights;

		public GradientGeometryWayProvider(@Nullable RouteColorize routeColorize,
		                                   @NonNull List<RouteColorizationPoint> locations,
		                                   @Nullable List<Float> pointHeights) {
			this.routeColorize = routeColorize;
			this.locations = locations;
			this.pointHeights = pointHeights;
		}

		@Nullable
		public List<RouteColorizationPoint> simplify(int zoom) {
			return routeColorize != null ? routeColorize.simplify(zoom) : null;
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

		@Override
		public float getHeight(int index) {
			return pointHeights == null ? 0 : pointHeights.get(index);
		}
	}

	protected static class GradientPathGeometryZoom extends PathGeometryZoom {

		public GradientPathGeometryZoom(GeometryWayProvider locationProvider, RotatedTileBox tb, boolean simplify,
		                                @NonNull List<Integer> forceIncludedIndexes) {
			super(locationProvider, tb, simplify, forceIncludedIndexes);
		}

		@Override
		public void simplify(RotatedTileBox tb, GeometryWayProvider locationProvider, TByteArrayList simplifyPoints) {
			if (locationProvider instanceof GradientGeometryWayProvider) {
				GradientGeometryWayProvider provider = (GradientGeometryWayProvider) locationProvider;
				List<RouteColorizationPoint> simplified = provider.simplify(tb.getZoom());
				if (simplified != null) {
					for (RouteColorizationPoint location : simplified) {
						simplifyPoints.set(location.id, (byte) 1);
					}
				}
			}
		}
	}

	public static class GeometrySolidWayStyle<C extends MultiColoringGeometryWayContext> extends GeometryWayStyle<C> {

		private static final float LINE_WIDTH_THRESHOLD_DP = 8f;
		private static final float ARROW_DISTANCE_MULTIPLIER = 1.5f;
		private static final float SPECIAL_ARROW_DISTANCE_MULTIPLIER = 10f;

		public static final int OUTER_CIRCLE_COLOR = 0x33000000;

		private final int directionArrowColor;

		private final boolean hasPathLine;

		private final float lineWidthThresholdPix;
		private final float outerCircleRadius;
		private final float innerCircleRadius;

		GeometrySolidWayStyle(@NonNull C context, int lineColor, float lineWidth, int directionArrowColor,
		                      boolean hasPathLine) {
			super(context, lineColor, lineWidth);
			this.directionArrowColor = directionArrowColor;
			this.hasPathLine = hasPathLine;

			this.innerCircleRadius = AndroidUtils.dpToPxAuto(context.getCtx(), 7);
			this.outerCircleRadius = AndroidUtils.dpToPxAuto(context.getCtx(), 8);
			this.lineWidthThresholdPix = AndroidUtils.dpToPxAuto(context.getCtx(), LINE_WIDTH_THRESHOLD_DP);
		}

		@Override
		public Bitmap getPointBitmap() {
			return useSpecialArrow() ? getContext().getSpecialArrowBitmap() : getContext().getArrowBitmap();
		}

		@NonNull
		@Override
		public Integer getPointColor() {
			return directionArrowColor;
		}

		public boolean hasPathLine() {
			return hasPathLine;
		}

		public float getInnerCircleRadius() {
			return innerCircleRadius;
		}

		public float getOuterCircleRadius() {
			return outerCircleRadius;
		}

		@Override
		public double getPointStepPx(double zoomCoef) {
			return useSpecialArrow() ? getSpecialPointStepPx() : getRegularPointStepPx();
		}

		public double getSpecialPointStepPx() {
			Bitmap bitmap = getContext().getSpecialArrowBitmap();
			return bitmap.getHeight() * SPECIAL_ARROW_DISTANCE_MULTIPLIER;
		}

		public double getRegularPointStepPx() {
			Bitmap bitmap = getContext().getArrowBitmap();
			return bitmap.getHeight() + getWidth(0) * ARROW_DISTANCE_MULTIPLIER;
		}

		public boolean useSpecialArrow() {
			return getWidth(0) <= lineWidthThresholdPix;
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
			GeometrySolidWayStyle<?> o = (GeometrySolidWayStyle<?>) other;
			return Algorithms.objectEquals(directionArrowColor, o.directionArrowColor);
		}

		@Override
		public int getColorizationScheme() {
			return COLORIZATION_SOLID;
		}
	}

	public static class GeometryGradientWayStyle<C extends MultiColoringGeometryWayContext> extends GeometrySolidWayStyle<C> {

		public int currColor;
		public int nextColor;

		public GeometryGradientWayStyle(@NonNull C context, int color, float width) {
			super(context, color, width, Color.BLACK, true);
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
			if (!(other instanceof MultiColoringGeometryWay.GeometryGradientWayStyle)) {
				return false;
			}
			GeometryGradientWayStyle<?> o = (GeometryGradientWayStyle<?>) other;
			return currColor == o.currColor && nextColor == o.nextColor;
		}

		@Override
		public int getColorizationScheme() {
			return COLORIZATION_GRADIENT;
		}
	}

	protected Track3DStyle getTrack3DStyle() {
		return track3DStyle;
	}
}