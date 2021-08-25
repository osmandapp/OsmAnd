package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.AndroidUtils;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.RotatedTileBox;
import net.osmand.plus.ColorUtilities;
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
		return new GeometryArrowsStyle(getContext(), customColor, customWidth,
				getTrackContrastColor(customColor), false);
	}

	@NonNull
	@Override
	public GeometrySolidWayStyle<GpxGeometryWayContext> getSolidWayStyle(int lineColor) {
		return new GeometryArrowsStyle(getContext(), lineColor, customWidth,
				getTrackContrastColor(lineColor), true);
	}

	@ColorInt
	private int getTrackContrastColor(@ColorInt int trackColor) {
		return ColorUtilities.getContrastColor(getContext().getCtx(), trackColor, false);
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

	public static class GeometryArrowsStyle extends GeometrySolidWayStyle<GpxGeometryWayContext> {

		private static final float TRACK_WIDTH_THRESHOLD_DP = 8f;
		private static final float ARROW_DISTANCE_MULTIPLIER = 1.5f;
		private static final float SPECIAL_ARROW_DISTANCE_MULTIPLIER = 10f;

		public static final int OUTER_CIRCLE_COLOR = 0x33000000;

		private final boolean hasPathLine;

		private final float trackWidthThresholdPix;
		private final float outerCircleRadius;
		private final float innerCircleRadius;

		GeometryArrowsStyle(GpxGeometryWayContext context, int trackColor, float trackWidth,
		                    int directionArrowColor, boolean hasPathLine) {
			super(context, trackColor, trackWidth, directionArrowColor);

			this.hasPathLine = hasPathLine;

			this.innerCircleRadius = AndroidUtils.dpToPx(context.getCtx(), 7);
			this.outerCircleRadius = AndroidUtils.dpToPx(context.getCtx(), 8);
			this.trackWidthThresholdPix = AndroidUtils.dpToPx(context.getCtx(), TRACK_WIDTH_THRESHOLD_DP);
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}
			if (!super.equals(other)) {
				return false;
			}
			return other instanceof GeometryArrowsStyle;
		}

		@Override
		public boolean hasPathLine() {
			return hasPathLine;
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

		public int getTrackColor() {
			return color;
		}

		public float getTrackWidth() {
			return width;
		}

		public float getOuterCircleRadius() {
			return outerCircleRadius;
		}

		public float getInnerCircleRadius() {
			return innerCircleRadius;
		}

		public boolean useSpecialArrow() {
			return getTrackWidth() <= trackWidthThresholdPix;
		}

		@Override
		public double getPointStepPx(double zoomCoef) {
			return useSpecialArrow() ?
					getPointBitmap().getHeight() * SPECIAL_ARROW_DISTANCE_MULTIPLIER :
					getPointBitmap().getHeight() + getTrackWidth() * ARROW_DISTANCE_MULTIPLIER;
		}
	}
}