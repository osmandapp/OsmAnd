package net.osmand.plus.views.layers.geometry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.routing.ColoringType;
import net.osmand.plus.routing.RouteProvider;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.Algorithms;

import java.util.List;

public class GpxGeometryWay extends MultiColoringGeometryWay<GpxGeometryWayContext, GpxGeometryWayDrawer> {

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
	                                boolean drawDirectionArrows,
	                                @NonNull ColoringType routeColoringType,
	                                @Nullable String routeInfoAttribute) {
		this.coloringChanged = this.coloringType != routeColoringType
				|| routeColoringType == ColoringType.ATTRIBUTE
				&& !Algorithms.objectEquals(this.routeInfoAttribute, routeInfoAttribute);

		if (customWidth != trackWidth) {
			updateStylesWidth(trackWidth);
		}
		updatePaints(trackWidth, routeColoringType);
		getDrawer().setColoringType(routeColoringType);

		this.customColor = trackColor;
		this.customWidth = trackWidth;
		this.drawDirectionArrows = drawDirectionArrows;
		this.coloringType = routeColoringType;
		this.routeInfoAttribute = routeInfoAttribute;
	}

	public void updateSegment(RotatedTileBox tb, List<WptPt> points, List<RouteSegmentResult> routeSegments) {
		if (coloringChanged || tb.getMapDensity() != getMapDensity() || this.points != points
				|| this.routeSegments != routeSegments) {
			this.points = points;
			this.routeSegments = routeSegments;

			if (coloringType.isTrackSolid() || coloringType.isGradient()) {
				if (points != null) {
					updateWay(new GeometryWayWptPtProvider(points), tb);
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

	@NonNull
	@Override
	public GeometryWayStyle<?> getDefaultWayStyle() {
		return new GeometrySolidWayStyle<>(getContext(), customColor, customWidth,
				getContrastLineColor(customColor), false);
	}

	@NonNull
	@Override
	public GeometrySolidWayStyle<GpxGeometryWayContext> getSolidWayStyle(int lineColor) {
		return new GeometrySolidWayStyle<>(getContext(), lineColor, customWidth,
				getContrastLineColor(lineColor), true);
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