package net.osmand.plus.views.layers.geometry;

import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import net.osmand.GPXUtilities.WptPt;
import net.osmand.data.RotatedTileBox;

import java.util.List;

public class GpxGeometryWay extends GeometryWay<GpxGeometryWayContext, GeometryWayDrawer<GpxGeometryWayContext>> {

	private List<WptPt> points;

	private float trackWidth;
	private int trackColor;
	private int arrowColor;

	private static class GeometryWayWptPtProvider implements GeometryWayProvider {
		private List<WptPt> points;

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

	public void setTrackStyleParams(int arrowColor, int trackColor, float trackWidth) {
		this.arrowColor = arrowColor;
		this.trackColor = trackColor;
		this.trackWidth = trackWidth;
	}

	@NonNull
	@Override
	public GeometryWayStyle<?> getDefaultWayStyle() {
		return new GeometryArrowsStyle(getContext(), arrowColor, trackColor, trackWidth);
	}

	public void updatePoints(RotatedTileBox tb, List<WptPt> points) {
		if (tb.getMapDensity() != getMapDensity() || this.points != points) {
			this.points = points;
			if (points != null) {
				updateWay(new GeometryWayWptPtProvider(points), tb);
			} else {
				clearWay();
			}
		}
	}

	public void clearPoints() {
		if (points != null) {
			points = null;
			clearWay();
		}
	}

	public static class GeometryArrowsStyle extends GeometryWayStyle<GpxGeometryWayContext> {

		private static final double DIRECTION_ARROW_DISTANCE_MULTIPLIER = 10.0;

		private Bitmap arrowBitmap;

		protected int pointColor;
		protected int trackColor;
		protected float trackWidth;

		GeometryArrowsStyle(GpxGeometryWayContext context, int arrowColor, int trackColor, float trackWidth) {
			this(context, null, arrowColor, trackColor, trackWidth);
		}

		GeometryArrowsStyle(GpxGeometryWayContext context, Bitmap arrowBitmap, int arrowColor, int trackColor, float trackWidth) {
			super(context);
			this.arrowBitmap = arrowBitmap;
			this.pointColor = arrowColor;
			this.trackColor = trackColor;
			this.trackWidth = trackWidth;
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
			return false;
		}

		@Override
		public Bitmap getPointBitmap() {
			return arrowBitmap != null ? arrowBitmap : getContext().getArrowBitmap();
		}

		@Override
		public Integer getPointColor() {
			return pointColor;
		}

		public int getTrackColor() {
			return trackColor;
		}

		public float getTrackWidth() {
			return trackWidth;
		}

		@Override
		public double getPointStepPx(double zoomCoef) {
			return getPointBitmap().getHeight() + trackWidth * 1.5f;
		}
	}
}