package net.osmand.data;

import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Fits points into a screen box of a tilted (3D) map: finds the map target and the zoom that put the screen bbox
 * of the points in the middle of the box, as large as the box allows. Tilt, rotation and the screen pixel of the
 * map target stay as they are.
 * <p>
 * The map is not moved for this. A zoom change scales the ground around the map target and a target change
 * shifts it, so with target T and scale s (ground length ratio to now) a point P is on the screen where the point
 * t0 + (P - T) / s is now, t0 being the target now.
 * <p>
 * On a tilted map the pixels per ground unit across a screen row fall linearly up the screen and reach zero at
 * the horizon. Two rows give this line, and the line gives
 * <ul>
 * <li>the horizon: a point that projects above it is behind the camera;</li>
 * <li>the box top: ground squeezed up the screen more than MAX_SQUEEZE times as much as at the box bottom
 * shows a route as a line at the horizon, so the box top is lowered to where that happens;</li>
 * <li>3D relief: a raised point is drawn away from the vanishing point of verticals, below the target on the
 * screen, by the ratio depth / (depth - height), and the depth of a ground point is inverse to the pixels per
 * ground unit of its row. The height of each point is measured on the screen now.</li>
 * </ul>
 */
public class TiltedMapFit {

	/** How many times more the ground at the box top may be squeezed up the screen than at the box bottom */
	private static final double MAX_SQUEEZE = 5;
	/** How many points of a long route are fitted */
	private static final int MAX_POINTS = 100;
	/** Fit steps at most, it takes a few */
	private static final int MAX_STEPS = 20;
	/** Part of the box size by which the fitted bbox may differ from it in size and position */
	private static final double PRECISION = 0.005;

	public interface Projection {

		/** Screen pixel {x, y} of a 31 location on the flat ground, behind the camera as well; null if unknown */
		double[] getPixel(double x31, double y31);

		/** Screen pixel {x, y} of a 31 location as the 3D relief raises it; null if unknown */
		double[] getElevatedPixel(double x31, double y31);

		/** 31 location {x, y} on the flat ground under a screen pixel; null over the horizon */
		double[] getLocation(double x, double y);
	}

	private final Projection projection;
	private final double[] target0;
	private final List<double[]> points = new ArrayList<>();
	/** Height of each point as depthLeft = 1 - raise * pixelsPerUnit, see measureRaises(); 0 if unknown */
	private final double[] raises;
	private final double targetPixelX;
	private final double verticalsRow;
	private final double boxLeft;
	private final double boxTop;
	private final double boxRight;
	private final double boxBottom;
	private final double bottomPixelsPerUnit;
	private final double pixelsPerUnitSlope;
	private final double horizon;

	private double[] target;
	private double scale;
	private double minX, minY, maxX, maxY;

	/**
	 * @param targetX31      map target now
	 * @param elevationAngle tilt of the map, 90 is top down
	 * @param points         points to fit, at least one
	 * @param boxLeft        screen box to fit the points into, margins already taken off; its top is lowered on a
	 *                       low tilt
	 */
	public TiltedMapFit(Projection projection, double targetX31, double targetY31, double elevationAngle,
	                    List<LatLon> points, double boxLeft, double boxTop, double boxRight, double boxBottom) {
		this.projection = projection;
		this.target0 = new double[] {targetX31, targetY31};
		this.boxLeft = boxLeft;
		this.boxRight = boxRight;
		this.boxBottom = boxBottom;
		addPoints(points);

		bottomPixelsPerUnit = getPixelsPerUnitNow(boxBottom);
		// The second row: the box top, or lower if the top is over the horizon
		double row = boxTop;
		double rowPixelsPerUnit = getPixelsPerUnitNow(row);
		while (rowPixelsPerUnit <= 0 && boxBottom - row > 1) {
			row = (row + boxBottom) / 2;
			rowPixelsPerUnit = getPixelsPerUnitNow(row);
		}
		pixelsPerUnitSlope = rowPixelsPerUnit > 0 && row < boxBottom
				? Math.max(0, (bottomPixelsPerUnit - rowPixelsPerUnit) / (boxBottom - row)) : 0;
		if (pixelsPerUnitSlope > 0) {
			horizon = boxBottom - bottomPixelsPerUnit / pixelsPerUnitSlope;
			// Squeeze up the screen grows as the square of the squeeze across it
			double squeezed = boxBottom - bottomPixelsPerUnit * (1 - 1 / Math.sqrt(MAX_SQUEEZE)) / pixelsPerUnitSlope;
			this.boxTop = Math.max(boxTop, squeezed);
		} else {
			horizon = Double.NEGATIVE_INFINITY;
			this.boxTop = boxTop;
		}
		// The target is the center of the view: the horizon is focus * tan(elevation) above it and the vanishing
		// point of verticals focus / tan(elevation) below it
		double[] targetPixel = projection.getPixel(targetX31, targetY31);
		double tan = Math.tan(Math.toRadians(elevationAngle));
		targetPixelX = targetPixel != null ? targetPixel[0] : (boxLeft + boxRight) / 2;
		verticalsRow = targetPixel != null && pixelsPerUnitSlope > 0
				? targetPixel[1] + (targetPixel[1] - horizon) / (tan * tan) : Double.POSITIVE_INFINITY;
		raises = measureRaises();
	}

	/** The box top, lowered on a low tilt */
	public double getBoxTop() {
		return boxTop;
	}

	/** Map target found, 31 {x, y} */
	public double[] getTarget() {
		return target.clone();
	}

	/** Ground length ratio of the found zoom to the zoom now: above 1 is zooming out */
	public double getScale() {
		return scale;
	}

	/**
	 * Finds the target and the scale.
	 *
	 * @return false if the points cannot be fitted, e.g. the box is over the horizon
	 */
	public boolean fit() {
		double[] boxGround = getBoxGround();
		if (boxGround == null) {
			return false;
		}
		double[] center = getPointsCenter();
		scale = 1;
		target = new double[] {center[0] - (boxGround[0] - target0[0]), center[1] - (boxGround[1] - target0[1])};
		return run(boxGround, true);
	}

	/**
	 * Centers the points again at another scale, e.g. the fitted one rounded to a zoom the map allows.
	 *
	 * @return false if the points do not fit at this scale
	 */
	public boolean center(double newScale) {
		double[] boxGround = getBoxGround();
		if (boxGround == null || target == null) {
			return false;
		}
		zoom(newScale, boxGround);
		return run(boxGround, false);
	}

	private boolean run(double[] boxGround, boolean fitScale) {
		double boxX = (boxLeft + boxRight) / 2;
		double boxY = (boxTop + boxBottom) / 2;
		double boxWidth = boxRight - boxLeft;
		double boxHeight = boxBottom - boxTop;
		for (int step = 0; step <= MAX_STEPS; step++) {
			if (!measure()) {
				if (!fitScale) {
					return false;
				}
				// A point is behind the camera: zoom out
				zoom(scale * 2, boxGround);
				continue;
			}
			double x = (minX + maxX) / 2;
			double y = (minY + maxY) / 2;
			double ratio = Math.max((maxX - minX) / boxWidth, (maxY - minY) / boxHeight);
			boolean centered = Math.abs(x - boxX) < boxWidth * PRECISION && Math.abs(y - boxY) < boxHeight * PRECISION;
			// A single point has no size: it keeps the zoom
			boolean sized = !fitScale || ratio == 0 || Math.abs(ratio - 1) < PRECISION;
			if (centered && sized) {
				return true;
			}
			if (step == MAX_STEPS) {
				return ratio < 1 + PRECISION;
			}
			// The ground under the bbox center moves to the box center
			double[] ground = projection.getLocation(x, y);
			if (ground == null) {
				return false;
			}
			target[0] += scale * (ground[0] - boxGround[0]);
			target[1] += scale * (ground[1] - boxGround[1]);
			if (fitScale && ratio > 0) {
				zoom(scale * ratio, boxGround);
			}
		}
		return false;
	}

	/** Changes the scale keeping the ground under the box center */
	private void zoom(double newScale, double[] boxGround) {
		double offsetX = boxGround[0] - target0[0];
		double offsetY = boxGround[1] - target0[1];
		target[0] += (scale - newScale) * offsetX;
		target[1] += (scale - newScale) * offsetY;
		scale = newScale;
	}

	/** Screen bbox of the points at the target and the scale; false if a point is behind the camera */
	private boolean measure() {
		minX = minY = Double.POSITIVE_INFINITY;
		maxX = maxY = Double.NEGATIVE_INFINITY;
		for (int i = 0; i < points.size(); i++) {
			double[] p = points.get(i);
			double[] pixel = projection.getPixel(target0[0] + (p[0] - target[0]) / scale,
					target0[1] + (p[1] - target[1]) / scale);
			if (pixel == null || pixel[1] <= horizon) {
				return false;
			}
			double x = pixel[0];
			double y = pixel[1];
			if (raises[i] != 0) {
				// The height is scale times smaller in the ground units now
				double depthLeft = 1 - raises[i] * getPixelsPerUnit(y) / scale;
				if (depthLeft <= 0) {
					return false;
				}
				x = targetPixelX + (x - targetPixelX) / depthLeft;
				y = verticalsRow + (y - verticalsRow) / depthLeft;
			}
			minX = Math.min(minX, x);
			maxX = Math.max(maxX, x);
			minY = Math.min(minY, y);
			maxY = Math.max(maxY, y);
		}
		return true;
	}

	/**
	 * Raise of the points on the screen now: a raised point is (depth / (depth - height)) times farther from
	 * the vanishing point of verticals than its ground point, and depth is inverse to pixels per unit
	 */
	private double[] measureRaises() {
		double[] result = new double[points.size()];
		if (Double.isInfinite(verticalsRow)) {
			return result;
		}
		for (int i = 0; i < points.size(); i++) {
			double[] p = points.get(i);
			double[] pixel = projection.getPixel(p[0], p[1]);
			double[] elevated = projection.getElevatedPixel(p[0], p[1]);
			if (pixel != null && elevated != null && pixel[1] > horizon && elevated[1] != verticalsRow) {
				double depthLeft = (pixel[1] - verticalsRow) / (elevated[1] - verticalsRow);
				result[i] = (1 - depthLeft) / getPixelsPerUnit(pixel[1]);
			}
		}
		return result;
	}

	private double getPixelsPerUnit(double row) {
		return bottomPixelsPerUnit - (boxBottom - row) * pixelsPerUnitSlope;
	}

	/** Pixels per ground unit across a row of the screen now, 0 over the horizon */
	private double getPixelsPerUnitNow(double row) {
		double[] left = projection.getLocation(boxLeft, row);
		double[] right = projection.getLocation(boxRight, row);
		return left != null && right != null
				? (boxRight - boxLeft) / Math.hypot(right[0] - left[0], right[1] - left[1]) : 0;
	}

	private double[] getBoxGround() {
		if (points.isEmpty() || bottomPixelsPerUnit <= 0 || boxTop >= boxBottom) {
			return null;
		}
		return projection.getLocation((boxLeft + boxRight) / 2, (boxTop + boxBottom) / 2);
	}

	private double[] getPointsCenter() {
		double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY;
		double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
		for (double[] p : points) {
			minX = Math.min(minX, p[0]);
			maxX = Math.max(maxX, p[0]);
			minY = Math.min(minY, p[1]);
			maxY = Math.max(maxY, p[1]);
		}
		return new double[] {(minX + maxX) / 2, (minY + maxY) / 2};
	}

	/** Every n-th point of a long route, and its extreme points */
	private void addPoints(List<LatLon> latLons) {
		if (latLons.isEmpty()) {
			return;
		}
		int step = (latLons.size() + MAX_POINTS - 1) / MAX_POINTS;
		LatLon west = latLons.get(0);
		LatLon east = west;
		LatLon north = west;
		LatLon south = west;
		for (int i = 0; i < latLons.size(); i++) {
			LatLon p = latLons.get(i);
			west = p.getLongitude() < west.getLongitude() ? p : west;
			east = p.getLongitude() > east.getLongitude() ? p : east;
			north = p.getLatitude() > north.getLatitude() ? p : north;
			south = p.getLatitude() < south.getLatitude() ? p : south;
			if (i % step == 0 || i == latLons.size() - 1) {
				addPoint(p);
			}
		}
		addPoint(west);
		addPoint(east);
		addPoint(north);
		addPoint(south);
	}

	private void addPoint(LatLon p) {
		points.add(new double[] {MapUtils.get31TileNumberX(p.getLongitude()), MapUtils.get31TileNumberY(p.getLatitude())});
	}
}
