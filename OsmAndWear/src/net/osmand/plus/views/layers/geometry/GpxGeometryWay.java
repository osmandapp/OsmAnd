package net.osmand.plus.views.layers.geometry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.router.RouteColorize.ColorizationType;
import net.osmand.router.RouteColorize.RouteColorizationPoint;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GpxGeometryWay extends MultiColoringGeometryWay<GpxGeometryWayContext, GpxGeometryWayDrawer> {

	public static final int VECTOR_LINES_RESERVED = 1000;

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
		for (int i = 0; i < points.size(); i++) {
			WptPt point = points.get(i);
			RouteColorizationPoint cp = new RouteColorizationPoint(i, point.lat, point.lon, 0);
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
		updateWay(new GradientGeometryWayProvider(null, colorizationPoints), createGradientStyles(colorizationPoints), tb);
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
		return style;
	}

	@NonNull
	@Override
	public GeometrySolidWayStyle<GpxGeometryWayContext> getSolidWayStyle(int lineColor) {
		GeometrySolidWayStyle<GpxGeometryWayContext> style = new GeometrySolidWayStyle<>(
				getContext(), lineColor, customWidth, getContrastLineColor(lineColor), true);
		style.dashPattern = dashPattern;
		return style;
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