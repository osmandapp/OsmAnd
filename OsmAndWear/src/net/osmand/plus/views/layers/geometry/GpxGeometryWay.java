package net.osmand.plus.views.layers.geometry;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.track.Gpx3DVisualizationType;
import net.osmand.plus.track.Track3DStyle;
import net.osmand.router.RouteSegmentResult;
import net.osmand.shared.gpx.primitives.WptPt;
import net.osmand.shared.routing.ColoringType;
import net.osmand.shared.routing.RouteColorize.RouteColorizationPoint;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GpxGeometryWay extends MultiColoringGeometryWay<GpxGeometryWayContext, GpxGeometryWayDrawer> {

	public static final int VECTOR_LINES_RESERVED = 1000;
	private final Log log = PlatformUtil.getLog(GpxGeometryWay.class);

	private List<WptPt> points;
	private List<RouteSegmentResult> routeSegments;

	private boolean drawDirectionArrows;

	public GpxGeometryWay(GpxGeometryWayContext context) {
		super(context, new GpxGeometryWayDrawer(context));
	}

	public void setTrackStyleParams(int trackColor,
	                                float trackWidth,
	                                @Nullable float[] dashPattern,
	                                boolean drawDirectionArrows,
	                                @Nullable Track3DStyle track3DStyle,
	                                @NonNull ColoringType coloringType,
	                                @Nullable String routeInfoAttribute,
	                                @Nullable String gradientPalette) {
		boolean track3DStyleChanged = !Algorithms.objectEquals(getTrack3DStyle(), track3DStyle);
		boolean gradientPaletteChanged = !Algorithms.stringsEqual(this.gradientPalette, gradientPalette);
		boolean coloringTypeChanged = gradientPaletteChanged || track3DStyleChanged
				|| this.coloringType != coloringType || coloringType == ColoringType.ATTRIBUTE
				&& !Algorithms.objectEquals(this.routeInfoAttribute, routeInfoAttribute);

		this.coloringChanged = this.customColor != trackColor || coloringTypeChanged;
		if (coloringTypeChanged) {
			resetSymbolProviders();
		}
		if (this.customWidth != trackWidth) {
			updateStylesWidth(trackWidth);
		}
		if (!Arrays.equals(this.dashPattern, dashPattern)) {
			updateStylesDashPattern(dashPattern);
		}
		if (this.drawDirectionArrows != drawDirectionArrows) {
			resetArrowsProvider();
		}
		updateTrack3DStyle(track3DStyle);
		updatePaints(trackWidth, coloringType);
		getDrawer().setColoringType(coloringType);
		getDrawer().setOutlineColoringType(getOutlineColoringType());
		getDrawer().setTrack3DStyle(track3DStyle);

		this.customColor = trackColor;
		this.customWidth = trackWidth;
		this.dashPattern = dashPattern;
		this.drawDirectionArrows = drawDirectionArrows;
		this.coloringType = coloringType;
		this.routeInfoAttribute = routeInfoAttribute;
		this.gradientPalette = gradientPalette;
	}

	public void updateSegment(RotatedTileBox tb, List<WptPt> points,
	                          List<RouteSegmentResult> routeSegments, boolean forceUpdate) {
		if (coloringChanged || forceUpdate || tb.getMapDensity() != getMapDensity()
				|| this.points != points || this.routeSegments != routeSegments) {
			this.points = points;
			this.routeSegments = routeSegments;

			if (points == null) {
				clearWay();
				return;
			}

			Track3DStyle track3DStyle = getTrack3DStyle();
			if (hasMapRenderer() && track3DStyle != null && track3DStyle.getVisualizationType().is3dType()) {
				updateGpx3dWay(tb, points, routeSegments, isHeightmapsActive());
			} else if (coloringType.isTrackSolid()) {
				if (hasMapRenderer()) {
					Map<Integer, GeometryWayStyle<?>> styleMap = new TreeMap<>();
					GeometrySolidWayStyle<?> style = getSolidWayStyle(customColor);
					styleMap.put(0, style);
					updateWay(new GeometryWayWptPtProvider(points), styleMap, tb);
				} else {
					updateWay(new GeometryWayWptPtProvider(points), tb);
				}
			} else if (coloringType.isGradient()) {
				if (hasMapRenderer()) {
					updateGpxGradientWay(tb, points, isHeightmapsActive());
				} else {
					updateWay(new GeometryWayWptPtProvider(points), tb);
				}
			} else if (coloringType.isRouteInfoAttribute()) {
				if (routeSegments != null) {
					updateSolidMultiColorRoute(tb, RouteProvider.locationsFromSharedWpts(points), routeSegments);
				} else {
					clearWay();
				}
			}
		}
	}

	protected void updateGpxGradientWay(@NonNull RotatedTileBox tb, @NonNull List<WptPt> points, boolean heightmapsActive) {
		List<RouteColorizationPoint> colorizationPoints = new ArrayList<>();
		List<Float> pointHeights = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			WptPt wptPt = points.get(i);
			pointHeights.add((float) getPointElevation(wptPt, heightmapsActive));
			RouteColorizationPoint point = new RouteColorizationPoint(i, wptPt.getLat(), wptPt.getLon(), 0);
			point.setPrimaryColor(getPointGradientColor(coloringType, wptPt));
			colorizationPoints.add(point);
		}
		updateWay(new GradientGeometryWayProvider(null, colorizationPoints, pointHeights), createGradientStyles(colorizationPoints), tb);
	}

	protected void updateGpx3dWay(@NonNull RotatedTileBox tileBox, @NonNull List<WptPt> points,
	                              @Nullable List<RouteSegmentResult> segments, boolean heightmapsActive) {
		ColoringType outlineColoringType = getOutlineColoringType();
		List<RouteColorizationPoint> colorization = new ArrayList<>(points.size());
		List<Integer> routeColors = coloringType.isRouteInfoAttribute()
				? getRouteInfoAttributesColors(RouteProvider.locationsFromSharedWpts(points), segments) : null;

		for (int i = 0; i < points.size(); i++) {
			WptPt wptPt = points.get(i);
			double value = getPointElevation(wptPt, heightmapsActive);
			RouteColorizationPoint point = new RouteColorizationPoint(i, wptPt.getLat(), wptPt.getLon(), value);
			point.setPrimaryColor(getPointColor(coloringType, wptPt, routeColors, i));
			if (outlineColoringType != null) {
				point.setSecondaryColor(getPointColor(outlineColoringType, wptPt, routeColors, i));
			}
			colorization.add(point);
		}
		updateWay(new Geometry3DWayProvider(colorization), createGradient3DStyles(colorization), tileBox);
	}

	private double getPointElevation(@NonNull WptPt point, boolean heightmapsActive) {
		Track3DStyle style = getTrack3DStyle();
		return style != null ? Gpx3DVisualizationType.getPointElevation(point, style, heightmapsActive) : 0;
	}

	@ColorInt
	private int getPointGradientColor(@NonNull ColoringType type, @NonNull WptPt point) {
		return type.isGradient() ? point.getColor(type.toColorizationType()) : point.getColor();
	}

	@ColorInt
	private int getPointColor(@NonNull ColoringType type, @NonNull WptPt point, @Nullable List<Integer> routeColors, int index) {
		if (type.isGradient()) {
			return getPointGradientColor(type, point);
		} else if (type.isRouteInfoAttribute()) {
			if (routeColors != null && routeColors.size() > index) {
				return routeColors.get(index);
			}
		}
		return type.isTrackSolid() ? customColor : point.getColor();
	}

	@Override
	protected GeometryWayStyle<?> getStyle(int index, GeometryWayStyle<?> defaultWayStyle) {
		return coloringType.isGradient() && styleMap.containsKey(index)
				? styleMap.get(index) : super.getStyle(index, defaultWayStyle);
	}

	@NonNull
	@Override
	public GeometryWayStyle<?> getDefaultWayStyle() {
		GeometrySolidWayStyle<GpxGeometryWayContext> style = new GeometrySolidWayStyle<>(
				getContext(), customColor, customWidth, getContrastLineColor(customColor), false);
		style.dashPattern = dashPattern;
		updateTrack3dStyle(style);
		return style;
	}

	@NonNull
	@Override
	public GeometrySolidWayStyle<GpxGeometryWayContext> getSolidWayStyle(int lineColor) {
		GeometrySolidWayStyle<GpxGeometryWayContext> style = new GeometrySolidWayStyle<>(
				getContext(), lineColor, customWidth, getContrastLineColor(lineColor), true);
		style.dashPattern = dashPattern;
		updateTrack3dStyle(style);
		return style;
	}

	private void updateTrack3dStyle(@NonNull GeometrySolidWayStyle<GpxGeometryWayContext> style) {
		Track3DStyle track3DStyle = getTrack3DStyle();
		if (track3DStyle != null) {
			style.trackVisualizationType = track3DStyle.getVisualizationType();
			style.trackWallColorType = track3DStyle.getWallColorType();
			style.trackLinePositionType = track3DStyle.getLinePositionType();
			style.additionalExaggeration = track3DStyle.getExaggeration();
			style.elevationMeters = track3DStyle.getElevation();
		}
	}

	@Override
	protected boolean shouldDrawArrows() {
		return drawDirectionArrows;
	}

	@Override
	public void clearWay() {
		if (points != null || routeSegments != null) {
			points = null;
			routeSegments = null;
			super.clearWay();
		}
	}
}