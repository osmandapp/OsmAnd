package net.osmand.plus.views.layers.geometry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.data.RotatedTileBox;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.plus.track.Track3DStyle;
import net.osmand.router.RouteColorize.ColorizationType;
import net.osmand.router.RouteColorize.RouteColorizationPoint;
import net.osmand.router.RouteSegmentResult;
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

	private static class GeometryWayWptPtProvider implements GeometryWayProvider {
		private final List<WptPt> points;

		public GeometryWayWptPtProvider(@NonNull List<WptPt> points) {
			this.points = points;
		}

		@Override
		public double getLatitude(int index) {
			return points.get(index).getLatitude();
		}

		@Override
		public double getLongitude(int index) {
			return points.get(index).getLongitude();
		}

		@Override
		public float getHeight(int index) {
			return (float) points.get(index).ele;
		}

		@Override
		public int getSize() {
			return points.size();
		}
	}

	public GpxGeometryWay(GpxGeometryWayContext context) {
		super(context, new GpxGeometryWayDrawer(context));
	}

	public void setTrackStyleParams(int trackColor,
	                                float trackWidth,
	                                @Nullable float[] dashPattern,
	                                boolean drawDirectionArrows,
	                                @Nullable Track3DStyle track3DStyle,
	                                @NonNull ColoringType coloringType,
	                                @Nullable String routeInfoAttribute) {
		boolean coloringTypeChanged = this.coloringType != coloringType
				|| coloringType == ColoringType.ATTRIBUTE
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
		if (!Algorithms.objectEquals(getTrack3DStyle(), track3DStyle)) {
			resetSymbolProviders();
		}
		updateTrack3DStyle(track3DStyle);
		updatePaints(trackWidth, coloringType);
		getDrawer().setColoringType(coloringType);

		this.customColor = trackColor;
		this.customWidth = trackWidth;
		this.dashPattern = dashPattern;
		this.drawDirectionArrows = drawDirectionArrows;
		this.coloringType = coloringType;
		this.routeInfoAttribute = routeInfoAttribute;
	}

	public void updateSegment(RotatedTileBox tb, List<WptPt> points, List<RouteSegmentResult> routeSegments) {
		if (coloringChanged || tb.getMapDensity() != getMapDensity() || this.points != points
				|| this.routeSegments != routeSegments) {
			this.points = points;
			this.routeSegments = routeSegments;

			if (coloringType.isTrackSolid()) {
				if (points != null) {
					if (hasMapRenderer()) {
						Map<Integer, GeometryWayStyle<?>> styleMap = new TreeMap<>();
						GeometrySolidWayStyle<?> style = getSolidWayStyle(customColor);
						styleMap.put(0, style);
						updateWay(new GeometryWayWptPtProvider(points), styleMap, tb);
					} else {
						updateWay(new GeometryWayWptPtProvider(points), tb);
					}
				} else {
					clearWay();
				}
			} else if (coloringType.isGradient()) {
				if (points != null) {
					if (hasMapRenderer()) {
						updateGpxGradientWay(tb, points);
					} else {
						updateWay(new GeometryWayWptPtProvider(points), tb);
					}
				} else {
					clearWay();
				}
			} else if (coloringType.isRouteInfoAttribute()) {
				if (points != null && routeSegments != null) {
					updateSolidMultiColorRoute(tb, RouteProvider.locationsFromWpts(points), routeSegments);
				} else {
					clearWay();
				}
			}
		}
	}

	protected void updateGpxGradientWay(RotatedTileBox tb, List<WptPt> points) {
		List<RouteColorizationPoint> colorizationPoints = new ArrayList<>();
		List<Float> pointHeights = new ArrayList<>();
		for (int i = 0; i < points.size(); i++) {
			WptPt point = points.get(i);
			RouteColorizationPoint cp = new RouteColorizationPoint(i, point.lat, point.lon, 0);
			pointHeights.add((float) point.ele);
			switch (coloringType) {
				case SPEED:
					cp.color = point.getColor(ColorizationType.SPEED);
					break;
				case ALTITUDE:
					cp.color = point.getColor(ColorizationType.ELEVATION);
					break;
				case SLOPE:
					cp.color = point.getColor(ColorizationType.SLOPE);
					break;
				case DEFAULT:
				case CUSTOM_COLOR:
				case TRACK_SOLID:
				case ATTRIBUTE:
					cp.color = point.getColor();
					break;
			}
			colorizationPoints.add(cp);
		}
		updateWay(new GradientGeometryWayProvider(null, colorizationPoints, pointHeights), createGradientStyles(colorizationPoints), tb);
	}

	@Override
	protected GeometryWayStyle<?> getStyle(int index, GeometryWayStyle<?> defaultWayStyle) {
		return coloringType.isGradient() && styleMap.containsKey(index)
				? styleMap.get(index)
				: super.getStyle(index, defaultWayStyle);
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

	private void updateTrack3dStyle(GeometrySolidWayStyle<GpxGeometryWayContext> style) {
		if (getTrack3DStyle() != null) {
			style.trackVisualizationType = getTrack3DStyle().getVisualizationType();
			style.trackWallColorType = getTrack3DStyle().getWallColorType();
			style.trackLinePositionType = getTrack3DStyle().getLinePositionType();
			style.additionalExaggeration = getTrack3DStyle().getAdditionalExaggeration();
			style.elevationMeters = getTrack3DStyle().getElevationMeters();
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