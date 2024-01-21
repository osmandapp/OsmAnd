package net.osmand.gpx;

import static net.osmand.gpx.GPXUtilities.POINT_ELEVATION;
import static net.osmand.gpx.GPXUtilities.POINT_SPEED;

import net.osmand.PlatformUtil;
import net.osmand.data.LatLon;
import net.osmand.gpx.GPXUtilities.RouteSegment;
import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.router.RouteColorize.ColorizationType;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GPXTrackAnalysis {

	public static final Log LOG = PlatformUtil.getLog(GPXTrackAnalysis.class);

	public String name;

	public float totalDistance = 0;
	public float totalDistanceWithoutGaps = 0;
	public int totalTracks = 0;
	public long startTime = Long.MAX_VALUE;
	public long endTime = Long.MIN_VALUE;
	public long timeSpan = 0;
	public long timeSpanWithoutGaps = 0;
	public long expectedRouteDuration = 0;
	//Next few lines for Issue 3222 heuristic testing only
	//public long timeMoving0 = 0;
	//public float totalDistanceMoving0 = 0;
	public long timeMoving = 0;
	public long timeMovingWithoutGaps = 0;
	public float totalDistanceMoving = 0;
	public float totalDistanceMovingWithoutGaps = 0;

	public double diffElevationUp = 0;
	public double diffElevationDown = 0;
	public double avgElevation = 0;
	public double minElevation = 99999;
	public double maxElevation = -100;

	public float minSpeed = Float.MAX_VALUE;
	public float maxSpeed = 0;
	public float avgSpeed;

	public double minHdop = Double.NaN;
	public double maxHdop = Double.NaN;

	public int points;
	public int wptPoints = 0;

	public Set<String> wptCategoryNames;

	public double metricEnd;
	public double secondaryMetricEnd;

	public LatLon latLonStart;
	public WptPt locationStart;
	public WptPt locationEnd;

	public double left = 0;
	public double right = 0;
	public double top = 0;
	public double bottom = 0;

	public List<PointAttributes> pointAttributes;
	public Set<String> availableAttributes;

	public boolean hasSpeedInTrack = false;

	public boolean isTimeSpecified() {
		return startTime != Long.MAX_VALUE && startTime != 0;
	}

	public boolean isTimeMoving() {
		return timeMoving != 0;
	}

	public boolean isElevationSpecified() {
		return maxElevation != -100;
	}

	public boolean hasSpeedInTrack() {
		return hasSpeedInTrack;
	}

	public boolean isBoundsCalculated() {
		return left != 0 && right != 0 && top != 0 && bottom != 0;
	}

	public boolean isSpeedSpecified() {
		return avgSpeed > 0;
	}

	public boolean isHdopSpecified() {
		return minHdop > 0;
	}

	public boolean isColorizationTypeAvailable(ColorizationType colorizationType) {
		if (colorizationType == ColorizationType.SPEED) {
			return isSpeedSpecified();
		} else if (colorizationType == ColorizationType.ELEVATION || colorizationType == ColorizationType.SLOPE) {
			return isElevationSpecified();
		} else {
			return true;
		}
	}

	public boolean hasSpeedData() {
		return hasData(POINT_SPEED);
	}

	public boolean hasElevationData() {
		return hasData(POINT_ELEVATION);
	}

	public boolean hasData(String tag) {
		return availableAttributes.contains(tag);
	}

	public void setHasData(String tag, boolean hasData) {
		if (hasData) {
			availableAttributes.add(tag);
		} else {
			availableAttributes.remove(tag);
		}
	}

	public static GPXTrackAnalysis prepareInformation(long fileTimeStamp, TrackPointsAnalyser pointsAnalyzer, TrkSegment segment) {
		return new GPXTrackAnalysis().prepareInformation(fileTimeStamp, pointsAnalyzer, new SplitSegment(segment));
	}

	public GPXTrackAnalysis prepareInformation(long fileTimeStamp, TrackPointsAnalyser pointsAnalyser, SplitSegment... splitSegments) {
		float[] calculations = new float[1];

		long startTimeOfSingleSegment = 0;
		long endTimeOfSingleSegment = 0;

		float distanceOfSingleSegment = 0;
		float distanceMovingOfSingleSegment = 0;
		long timeMovingOfSingleSegment = 0;

		float totalElevation = 0;
		int elevationPoints = 0;
		int speedCount = 0;
		long timeDiffMillis = 0;
		int timeDiff = 0;
		double totalSpeedSum = 0;
		points = 0;

		pointAttributes = new ArrayList<>();
		availableAttributes = new HashSet<>();

		for (final SplitSegment s : splitSegments) {
			final int numberOfPoints = s.getNumberOfPoints();
			float segmentDistance = 0f;
			metricEnd += s.metricEnd;
			secondaryMetricEnd += s.secondaryMetricEnd;
			points += numberOfPoints;
			expectedRouteDuration += getExpectedRouteSegmentDuration(s);

			for (int j = 0; j < numberOfPoints; j++) {
				WptPt point = s.get(j);
				if (j == 0 && locationStart == null) {
					locationStart = point;
					latLonStart = new LatLon(point.lat, point.lon);
				}
				if (j == numberOfPoints - 1) {
					locationEnd = point;
				}
				long time = point.time;
				if (time != 0) {
					if (s.metricEnd == 0) {
						if (s.segment.generalSegment) {
							if (point.firstPoint) {
								startTimeOfSingleSegment = time;
							} else if (point.lastPoint) {
								endTimeOfSingleSegment = time;
							}
							if (startTimeOfSingleSegment != 0 && endTimeOfSingleSegment != 0) {
								timeSpanWithoutGaps += endTimeOfSingleSegment - startTimeOfSingleSegment;
								startTimeOfSingleSegment = 0;
								endTimeOfSingleSegment = 0;
							}
						}
					}
					startTime = Math.min(startTime, time);
					endTime = Math.max(endTime, time);
				}
				updateBounds(point);

				float speed = (float) point.speed;
				if (speed > 0) {
					hasSpeedInTrack = true;
				}
				updateHdop(point);

				if (j > 0) {
					WptPt prev = s.get(j - 1);

					// Old complete summation approach for elevation gain/loss
					//if (!Double.isNaN(point.ele) && !Double.isNaN(prev.ele)) {
					//	double diff = point.ele - prev.ele;
					//	if (diff > 0) {
					//		diffElevationUp += diff;
					//	} else {
					//		diffElevationDown -= diff;
					//	}
					//}

					// totalDistance += MapUtils.getDistance(prev.lat, prev.lon, point.lat, point.lon);
					// using ellipsoidal 'distanceBetween' instead of spherical haversine (MapUtils.getDistance) is
					// a little more exact, also seems slightly faster:
					net.osmand.Location.distanceBetween(prev.lat, prev.lon, point.lat, point.lon, calculations);
					totalDistance += calculations[0];
					segmentDistance += calculations[0];
					point.distance = segmentDistance;

					// In case points are reversed and => time is decreasing
					timeDiffMillis = Math.max(0, point.time - prev.time);
					timeDiff = (int) ((timeDiffMillis) / 1000);

					//Last resort: Derive speed values from displacement if track does not originally contain speed
					if (!hasSpeedInTrack && speed == 0 && timeDiff > 0) {
						speed = calculations[0] / timeDiff;
					}

					// Motion detection:
					//   speed > 0  uses GPS chipset's motion detection
					//   calculations[0] > minDisplacment * time  is heuristic needed because tracks may be filtered at recording time, so points at rest may not be present in file at all
					boolean timeSpecified = point.time != 0 && prev.time != 0;
					if (speed > 0 && timeSpecified && calculations[0] > timeDiffMillis / 10000f) {
						timeMoving = timeMoving + timeDiffMillis;
						totalDistanceMoving += calculations[0];
						if (s.segment.generalSegment && !point.firstPoint) {
							timeMovingOfSingleSegment += timeDiffMillis;
							distanceMovingOfSingleSegment += calculations[0];
						}
					}

					//Next few lines for Issue 3222 heuristic testing only
					//	if (speed > 0 && point.time != 0 && prev.time != 0) {
					//		timeMoving0 = timeMoving0 + (point.time - prev.time);
					//		totalDistanceMoving0 += calculations[0];
					//	}
				}
				minSpeed = Math.min(speed, minSpeed);
				if (speed > 0) {
					totalSpeedSum += speed;
					maxSpeed = Math.max(speed, maxSpeed);
					speedCount++;
				}
				boolean isNaN = Double.isNaN(point.ele);
				float elevation = isNaN ? Float.NaN : (float) point.ele;
				if (!isNaN) {
					totalElevation += point.ele;
					elevationPoints++;
					minElevation = Math.min(point.ele, minElevation);
					maxElevation = Math.max(point.ele, maxElevation);
				}

				boolean firstPoint = false;
				boolean lastPoint = false;
				if (s.segment.generalSegment) {
					distanceOfSingleSegment += calculations[0];
					if (point.firstPoint) {
						firstPoint = j > 0;
						distanceOfSingleSegment = 0;
						timeMovingOfSingleSegment = 0;
						distanceMovingOfSingleSegment = 0;
					}
					if (point.lastPoint) {
						lastPoint = j < numberOfPoints - 1;
						totalDistanceWithoutGaps += distanceOfSingleSegment;
						timeMovingWithoutGaps += timeMovingOfSingleSegment;
						totalDistanceMovingWithoutGaps += distanceMovingOfSingleSegment;
					}
				}
				float distance = (j > 0) ? calculations[0] : 0;
				PointAttributes attribute = new PointAttributes(distance, timeDiff, firstPoint, lastPoint);
				attribute.speed = speed;
				attribute.elevation = elevation;
				addWptAttribute(point, attribute, pointsAnalyser);
			}
			processElevationDiff(s);
		}
		checkUnspecifiedValues(fileTimeStamp);
		processAverageValues(totalElevation, elevationPoints, totalSpeedSum, speedCount);

		return this;
	}

	private void addWptAttribute(WptPt point, PointAttributes attribute, TrackPointsAnalyser pointsAnalyser) {
		if (!hasSpeedData() && attribute.speed > 0 && totalDistance > 0) {
			setHasData(POINT_SPEED, true);
		}
		if (!hasElevationData() && !Float.isNaN(attribute.elevation) && totalDistance > 0) {
			setHasData(POINT_ELEVATION, true);
		}
		if (pointsAnalyser != null) {
			pointsAnalyser.onAnalysePoint(this, point, attribute);
		}
		pointAttributes.add(attribute);
	}

	private void updateBounds(WptPt point) {
		if (left == 0 && right == 0) {
			left = point.getLongitude();
			right = point.getLongitude();
			top = point.getLatitude();
			bottom = point.getLatitude();
		} else {
			left = Math.min(left, point.getLongitude());
			right = Math.max(right, point.getLongitude());
			top = Math.max(top, point.getLatitude());
			bottom = Math.min(bottom, point.getLatitude());
		}
	}

	private void updateHdop(WptPt point) {
		double hdop = point.hdop;
		if (hdop > 0) {
			if (Double.isNaN(minHdop) || hdop < minHdop) {
				minHdop = hdop;
			}
			if (Double.isNaN(maxHdop) || hdop > maxHdop) {
				maxHdop = hdop;
			}
		}
	}

	private void checkUnspecifiedValues(long fileTimeStamp) {
		if (totalDistance < 0) {
			availableAttributes.clear();
		}
		if (!isTimeSpecified()) {
			startTime = fileTimeStamp;
			endTime = fileTimeStamp;
		}
		if (timeSpan == 0) {
			timeSpan = endTime - startTime;
		}
	}

	public long getDurationInMs() {
		return timeSpan > 0 ? timeSpan : expectedRouteDuration;
	}

	public int getDurationInSeconds() {
		return (int) (getDurationInMs() / 1000f + 0.5f);
	}

	private long getExpectedRouteSegmentDuration(SplitSegment segment) {
		List<RouteSegment> routeSegments = segment.segment.routeSegments;
		if (routeSegments != null && !segment.segment.generalSegment) {
			long result = 0;
			for (RouteSegment routeSegment : routeSegments) {
				result += (long) (1000 * Algorithms.parseFloatSilently(routeSegment.segmentTime, 0.0f));
			}
			return result;
		}
		return 0;
	}

	private void processAverageValues(float totalElevation, int elevationPoints, double totalSpeedSum, int speedCount) {
		if (elevationPoints > 0) {
			avgElevation = totalElevation / elevationPoints;
		}
		//    Average speed, if any. Average speed is NOT overall (effective) speed, but only calculated for "moving" periods.
		//    Averaging speed values is less precise than totalDistanceMoving/timeMoving
		if (speedCount > 0) {
			if (timeMoving > 0) {
				avgSpeed = totalDistanceMoving / (float) timeMoving * 1000f;
			} else {
				avgSpeed = (float) totalSpeedSum / (float) speedCount;
			}
		} else {
			avgSpeed = -1;
		}
	}

	private void processElevationDiff(SplitSegment segment) {
		ElevationApproximator approximator = getElevationApproximator(segment);
		approximator.approximate();
		final double[] distances = approximator.getDistances();
		final double[] elevations = approximator.getElevations();
		if (distances != null && elevations != null) {
			ElevationDiffsCalculator elevationDiffsCalc = getElevationDiffsCalculator(distances, elevations);
			elevationDiffsCalc.calculateElevationDiffs();
			diffElevationUp += elevationDiffsCalc.getDiffElevationUp();
			diffElevationDown += elevationDiffsCalc.getDiffElevationDown();
		}
	}

	private ElevationApproximator getElevationApproximator(final SplitSegment segment) {
		return new ElevationApproximator() {
			@Override
			public double getPointLatitude(int index) {
				return segment.get(index).lat;
			}

			@Override
			public double getPointLongitude(int index) {
				return segment.get(index).lon;
			}

			@Override
			public double getPointElevation(int index) {
				return segment.get(index).ele;
			}

			@Override
			public int getPointsCount() {
				return segment.getNumberOfPoints();
			}
		};
	}

	private ElevationDiffsCalculator getElevationDiffsCalculator(final double[] distances, final double[] elevations) {
		return new ElevationDiffsCalculator() {
			@Override
			public double getPointDistance(int index) {
				return distances[index];
			}

			@Override
			public double getPointElevation(int index) {
				return elevations[index];
			}

			@Override
			public int getPointsCount() {
				return distances.length;
			}
		};
	}

	public interface TrackPointsAnalyser {
		void onAnalysePoint(GPXTrackAnalysis analysis, WptPt point, PointAttributes attribute);
	}
}
