package net.osmand.router;

import net.osmand.ColorPalette;
import net.osmand.PlatformUtil;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.osm.edit.Node;
import net.osmand.osm.edit.OsmMapUtils;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;

@Deprecated
public class RouteColorize {

	public static double MAX_CORRECT_ELEVATION_DISTANCE = 100.0;// in meters
	public static int SLOPE_RANGE = 150;// 150 meters
	private static final Log LOG = PlatformUtil.getLog(RouteColorize.class);
	private static final float DEFAULT_BASE = 17.2f;
	private static final double MIN_DIFFERENCE_SLOPE = 0.05d;// 5%


	private double[] latitudes;
	private double[] longitudes;
	private double[] values;
	private double minValue;
	private double maxValue;
	private ColorPalette palette;
	private List<RouteColorizationPoint> dataList;
	private ColorizationType colorizationType;

	public enum ColorizationType {
		ELEVATION, SPEED, SLOPE, NONE
	}

	/**
	 * @param minValue can be NaN
	 * @param maxValue can be NaN
	 * @param palette  array {{value,color},...} - color in sRGB (decimal) format OR
	 *                 {{value,RED,GREEN,BLUE,ALPHA},...} - color in RGBA format
	 */
	public RouteColorize(double[] latitudes, double[] longitudes, double[] values, double minValue, double maxValue,
			ColorPalette palette) {
		this.latitudes = latitudes;
		this.longitudes = longitudes;
		this.values = values;
		this.minValue = minValue;
		this.maxValue = maxValue;
		this.palette = palette;
		if (Double.isNaN(minValue) || Double.isNaN(maxValue)) {
			calculateMinMaxValue();
		}
		if (palette == null || palette.getColors().size() < 2) {
			this.palette = new ColorPalette(ColorPalette.MIN_MAX_PALETTE, minValue, maxValue);
		} else {
			this.palette = palette;
		}
	}

	/**
	 * @param type ELEVATION, SPEED, SLOPE
	 */
	public RouteColorize(GPXFile gpxFile, ColorizationType type, ColorPalette palette) {
		this(gpxFile, null, type, palette, 0);
	}

	public RouteColorize(GPXFile gpxFile, GPXTrackAnalysis analysis, ColorizationType type,
			ColorPalette palette, float maxProfileSpeed) {
		if (!gpxFile.hasTrkPt()) {
			LOG.warn("GPX file is not consist of track points");
			return;
		}
		List<Double> latList = new ArrayList<>();
		List<Double> lonList = new ArrayList<>();
		List<Double> valList = new ArrayList<>();
		int wptIdx = 0;
		if (analysis == null) {
			long time = Algorithms.isEmpty(gpxFile.path) ? System.currentTimeMillis() : gpxFile.modifiedTime;
			analysis = gpxFile.getAnalysis(time);
		}
		for (Track t : gpxFile.tracks) {
			for (TrkSegment ts : t.segments) {
				if (ts.generalSegment || ts.points.size() < 2) {
					continue;
				}

				for (WptPt p : ts.points) {
					latList.add(p.lat);
					lonList.add(p.lon);
					if (type == ColorizationType.SPEED) {
						valList.add((double) analysis.pointAttributes.get(wptIdx).speed);
					} else {
						valList.add((double) analysis.pointAttributes.get(wptIdx).elevation);
					}
					wptIdx++;
				}
			}
		}

		colorizationType = type;
		latitudes = listToArray(latList);
		longitudes = listToArray(lonList);

		if (type == ColorizationType.SLOPE) {
			values = calculateSlopesByElevations(latitudes, longitudes, listToArray(valList), SLOPE_RANGE);
		} else {
			values = listToArray(valList);
		}
		calculateMinMaxValue(analysis, maxProfileSpeed);
		if (type == ColorizationType.SLOPE) {
			this.palette = isValidPalette(palette) ? palette : ColorPalette.SLOPE_PALETTE;
		} else {
			this.palette = new ColorPalette(isValidPalette(palette) ? palette : ColorPalette.MIN_MAX_PALETTE, minValue,
					maxValue);
		}
	}

	private boolean isValidPalette(ColorPalette palette) {
		return palette != null && palette.getColors().size() >= 2;
	}

	/**
	 * Calculate slopes from elevations needs for right colorizing
	 *
	 * @param slopeRange - in what range calculate the derivative, usually we used
	 *                   150 meters
	 * @return slopes array, in the begin and the end present NaN values!
	 */
	public double[] calculateSlopesByElevations(double[] latitudes, double[] longitudes, double[] elevations,
			double slopeRange) {
		correctElevations(latitudes, longitudes, elevations);
		double[] newElevations = elevations;
		for (int i = 2; i < elevations.length - 2; i++) {
			newElevations[i] = elevations[i - 2] + elevations[i - 1] + elevations[i] + elevations[i + 1]
					+ elevations[i + 2];
			newElevations[i] /= 5;
		}
		elevations = newElevations;

		double[] slopes = new double[elevations.length];
		if (latitudes.length != longitudes.length || latitudes.length != elevations.length) {
			LOG.warn("Sizes of arrays latitudes, longitudes and values are not match");
			return slopes;
		}

		double[] distances = new double[elevations.length];
		double totalDistance = 0.0d;
		distances[0] = totalDistance;
		for (int i = 0; i < elevations.length - 1; i++) {
			totalDistance += MapUtils.getDistance(latitudes[i], longitudes[i], latitudes[i + 1], longitudes[i + 1]);
			distances[i + 1] = totalDistance;
		}

		for (int i = 0; i < elevations.length; i++) {
			if (distances[i] < slopeRange / 2 || distances[i] > totalDistance - slopeRange / 2) {
				slopes[i] = Double.NaN;
			} else {
				double[] arg = findDerivativeArguments(distances, elevations, i, slopeRange);
				slopes[i] = (arg[1] - arg[0]) / (arg[3] - arg[2]);
			}
		}
		return slopes;
	}

	private void correctElevations(double[] latitudes, double[] longitudes, double[] elevations) {
		for (int i = 0; i < elevations.length; i++) {
			if (Double.isNaN(elevations[i])) {
				double leftDist = MAX_CORRECT_ELEVATION_DISTANCE;
				double rightDist = MAX_CORRECT_ELEVATION_DISTANCE;
				double leftElevation = Double.NaN;
				double rightElevation = Double.NaN;
				for (int left = i - 1; left > 0 && leftDist <= MAX_CORRECT_ELEVATION_DISTANCE; left--) {
					if (!Double.isNaN(elevations[left])) {
						double dist = MapUtils.getDistance(latitudes[left], longitudes[left], latitudes[i],
								longitudes[i]);
						if (dist < leftDist) {
							leftDist = dist;
							leftElevation = elevations[left];
						} else {
							break;
						}
					}
				}
				for (int right = i + 1; right < elevations.length
						&& rightDist <= MAX_CORRECT_ELEVATION_DISTANCE; right++) {
					if (!Double.isNaN(elevations[right])) {
						double dist = MapUtils.getDistance(latitudes[right], longitudes[right], latitudes[i],
								longitudes[i]);
						if (dist < rightDist) {
							rightElevation = elevations[right];
							rightDist = dist;
						} else {
							break;
						}
					}
				}
				if (!Double.isNaN(leftElevation) && !Double.isNaN(rightElevation)) {
					elevations[i] = (leftElevation + rightElevation) / 2;
				} else if (Double.isNaN(leftElevation) && !Double.isNaN(rightElevation)) {
					elevations[i] = rightElevation;
				} else if (!Double.isNaN(leftElevation) && Double.isNaN(rightElevation)) {
					elevations[i] = leftElevation;
				} else {
					for (int right = i + 1; right < elevations.length; right++) {
						if (!Double.isNaN(elevations[right])) {
							elevations[i] = elevations[right];
							break;
						}
					}
				}
			}
		}
	}

	public List<RouteColorizationPoint> getResult() {
		List<RouteColorizationPoint> result = new ArrayList<>();
		for (int i = 0; i < latitudes.length; i++) {
			result.add(new RouteColorizationPoint(i, latitudes[i], longitudes[i], values[i]));
		}
		setColorsToPoints(result);
		return result;
	}

	public List<RouteColorizationPoint> getSimplifiedResult(int simplificationZoom) {
		List<RouteColorizationPoint> simplifiedResult = simplify(simplificationZoom);
		setColorsToPoints(simplifiedResult);
		return simplifiedResult;
	}

	private void setColorsToPoints(List<RouteColorizationPoint> points) {
		for (RouteColorizationPoint point : points) {
			point.primaryColor = palette.getColorByValue(point.val);
		}
	}

	public void setPalette(ColorPalette palette) {
		if (palette != null) {
			this.palette = palette;
		}
	}

	public List<RouteColorizationPoint> simplify(int simplificationZoom) {
		if (dataList == null) {
			dataList = new ArrayList<>();
			for (int i = 0; i < latitudes.length; i++) {
				dataList.add(new RouteColorizationPoint(i, latitudes[i], longitudes[i], values[i]));
			}
		}
		List<Node> nodes = new ArrayList<>();
		List<Node> result = new ArrayList<>();
		for (RouteColorizationPoint data : dataList) {
			nodes.add(new net.osmand.osm.edit.Node(data.lat, data.lon, data.id));
		}

		double epsilon = Math.pow(2.0, DEFAULT_BASE - simplificationZoom);
		result.add(nodes.get(0));
		OsmMapUtils.simplifyDouglasPeucker(nodes, 0, nodes.size() - 1, result, epsilon);

		List<RouteColorizationPoint> simplified = new ArrayList<>();
		for (int i = 1; i < result.size(); i++) {
			int prevId = (int) result.get(i - 1).getId();
			int currentId = (int) result.get(i).getId();
			List<RouteColorizationPoint> sublist = dataList.subList(prevId, currentId);
			simplified.addAll(getExtremums(sublist));
		}
		Node lastSurvivedPoint = result.get(result.size() - 1);
		simplified.add(dataList.get((int) lastSurvivedPoint.getId()));
		return simplified;
	}

	private List<RouteColorizationPoint> getExtremums(List<RouteColorizationPoint> subDataList) {
		if (subDataList.size() <= 2) {
			return subDataList;
		}

		List<RouteColorizationPoint> result = new ArrayList<>();
		double min;
		double max;
		min = max = subDataList.get(0).val;
		for (RouteColorizationPoint pt : subDataList) {
			if (min > pt.val) {
				min = pt.val;
			}
			if (max < pt.val) {
				max = pt.val;
			}
		}

		double diff = max - min;

		result.add(subDataList.get(0));
		for (int i = 1; i < subDataList.size() - 1; i++) {
			double prev = subDataList.get(i - 1).val;
			double current = subDataList.get(i).val;
			double next = subDataList.get(i + 1).val;
			RouteColorizationPoint currentData = subDataList.get(i);

			if ((current > prev && current > next) || (current < prev && current < next)
					|| (current < prev && current == next) || (current == prev && current < next)
					|| (current > prev && current == next) || (current == prev && current > next)) {
				RouteColorizationPoint prevInResult;
				if (result.size() > 0) {
					prevInResult = result.get(0);
					if (prevInResult.val / diff > MIN_DIFFERENCE_SLOPE) {
						result.add(currentData);
					}
				} else
					result.add(currentData);
			}
		}
		result.add(subDataList.get(subDataList.size() - 1));
		return result;
	}


	/**
	 * @return double[minElevation, maxElevation, minDist, maxDist]
	 */
	private double[] findDerivativeArguments(double[] distances, double[] elevations, int index, double slopeRange) {
		double[] result = new double[4];
		double minDist = distances[index] - slopeRange / 2;
		double maxDist = distances[index] + slopeRange / 2;
		result[0] = Double.NaN;
		result[1] = Double.NaN;
		result[2] = minDist;
		result[3] = maxDist;
		int closestMaxIndex = -1;
		int closestMinIndex = -1;
		for (int i = index; i < distances.length; i++) {
			if (distances[i] == maxDist) {
				result[1] = elevations[i];
				break;
			}
			if (distances[i] > maxDist) {
				closestMaxIndex = i;
				break;
			}
		}
		for (int i = index; i >= 0; i--) {
			if (distances[i] == minDist) {
				result[0] = elevations[i];
				break;
			}
			if (distances[i] < minDist) {
				closestMinIndex = i;
				break;
			}
		}
		if (closestMaxIndex > 0) {
			double diff = distances[closestMaxIndex] - distances[closestMaxIndex - 1];
			double coef = (maxDist - distances[closestMaxIndex - 1]) / diff;
			if (coef > 1 || coef < 0) {
				LOG.warn("Coefficient fo max must be 0..1 , coef=" + coef);
			}
			result[1] = (1 - coef) * elevations[closestMaxIndex - 1] + coef * elevations[closestMaxIndex];
		}
		if (closestMinIndex >= 0) {
			double diff = distances[closestMinIndex + 1] - distances[closestMinIndex];
			double coef = (minDist - distances[closestMinIndex]) / diff;
			if (coef > 1 || coef < 0) {
				LOG.warn("Coefficient for min must be 0..1 , coef=" + coef);
			}
			result[0] = (1 - coef) * elevations[closestMinIndex] + coef * elevations[closestMinIndex + 1];
		}
		if (Double.isNaN(result[0]) || Double.isNaN(result[1])) {
			LOG.warn("Elevations wasn't calculated");
		}
		return result;
	}

	public static double getMinValue(ColorizationType type, GPXTrackAnalysis analysis) {
		switch (type) {
		case SPEED:
			return 0.0;
		case ELEVATION:
			return analysis.getMinElevation();
		case SLOPE:
			return ColorPalette.SLOPE_MIN_VALUE;
		default:
			return -1;
		}
	}

	public static double getMaxValue(ColorizationType type, GPXTrackAnalysis analysis, double minValue,
			double maxProfileSpeed) {
		switch (type) {
		case SPEED:
			return Math.max(analysis.getMaxSpeed(), maxProfileSpeed);
		case ELEVATION:
			return Math.max(analysis.getMaxElevation(), minValue + 50);
		case SLOPE:
			return ColorPalette.SLOPE_MAX_VALUE;
		default:
			return -1;
		}
	}


	private void calculateMinMaxValue() {
		if (values.length == 0)
			return;
		minValue = maxValue = Double.NaN;
		for (double value : values) {
			if ((Double.isNaN(maxValue) || Double.isNaN(minValue)) && !Double.isNaN(value))
				maxValue = minValue = value;
			if (minValue > value)
				minValue = value;
			if (maxValue < value)
				maxValue = value;
		}
	}

	private void calculateMinMaxValue(GPXTrackAnalysis analysis, float maxProfileSpeed) {
		calculateMinMaxValue();
		// set strict limitations for maxValue
		maxValue = getMaxValue(colorizationType, analysis, minValue, maxProfileSpeed);
	}

	private double[] listToArray(List<Double> doubleList) {
		double[] result = new double[doubleList.size()];
		for (int i = 0; i < doubleList.size(); i++) {
			result[i] = doubleList.get(i);
		}
		return result;
	}

	public static ColorPalette getDefaultPalette(ColorizationType colorizationType) {
		if (colorizationType == ColorizationType.SLOPE) {
			return ColorPalette.SLOPE_PALETTE;
		} else {
			return ColorPalette.MIN_MAX_PALETTE;
		}
	}

	public static class RouteColorizationPoint {

		public final int id;
		public final double lat;
		public final double lon;
		public final double val;

		public int primaryColor;
		public int secondaryColor;

		public RouteColorizationPoint(int id, double lat, double lon, double val) {
			this.id = id;
			this.lat = lat;
			this.lon = lon;
			this.val = val;
		}
	}
}
