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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class GPXTrackAnalysis {

	public static final Log LOG = PlatformUtil.getLog(GPXTrackAnalysis.class);

	public static final int ANALYSIS_VERSION = 1;

	public String name;

	public float totalDistanceWithoutGaps = 0;
	public long timeSpanWithoutGaps = 0;
	public long expectedRouteDuration = 0;
	//Next few lines for Issue 3222 heuristic testing only
	//public long timeMoving0 = 0;
	//public float totalDistanceMoving0 = 0;
	public long timeMovingWithoutGaps = 0;
	public float totalDistanceMovingWithoutGaps = 0;

	private final Map<GpxParameter, Object> parameters = new HashMap<>();

	public double minHdop = Double.NaN;
	public double maxHdop = Double.NaN;

	public double metricEnd;
	public double secondaryMetricEnd;

	public WptPt locationStart;
	public WptPt locationEnd;

	public double left = 0;
	public double right = 0;
	public double top = 0;
	public double bottom = 0;

	public List<PointAttributes> pointAttributes;
	public Set<String> availableAttributes;

	public boolean hasSpeedInTrack = false;

	public Object getGpxParameter(GpxParameter gpxParameter) {
		Object value = gpxParameter.getDefaultValue();
		if (parameters.containsKey(gpxParameter)) {
			value = parameters.get(gpxParameter);
		}
		return value;
	}

	public void setGpxParameter(GpxParameter gpxParameter, Object value) {
		parameters.put(gpxParameter, value);
	}

	public void setStartTime(long startTime) {
		setGpxParameter(GpxParameter.START_TIME, startTime);
	}

	public long getStartTime() {
		Object startTime = getGpxParameter(GpxParameter.START_TIME);
		return (long) startTime;
	}

	public void setEndTime(long endTime) {
		setGpxParameter(GpxParameter.END_TIME, endTime);
	}

	public long getEndTime() {
		Object endTime = getGpxParameter(GpxParameter.END_TIME);
		return (long) endTime;
	}

	public void setTimeSpan(long timeSpan) {
		setGpxParameter(GpxParameter.TIME_SPAN, timeSpan);
	}

	public long getTimeSpan() {
		Object timeSpan = getGpxParameter(GpxParameter.TIME_SPAN);
		return (long) timeSpan;
	}

	public long getTimeMoving() {
		Object timeMoving = getGpxParameter(GpxParameter.TIME_MOVING);
		return (long) timeMoving;
	}

	public void setTimeMoving(long timeMoving) {
		setGpxParameter(GpxParameter.TIME_MOVING, timeMoving);
	}

	public void setMaxElevation(double maxElevation) {
		setGpxParameter(GpxParameter.MAX_ELEVATION, maxElevation);
	}

	public double getMaxElevation() {
		Object maxElevation = getGpxParameter(GpxParameter.MAX_ELEVATION);
		return (double) maxElevation;
	}

	public void setDiffElevationUp(double diffElevationUp) {
		setGpxParameter(GpxParameter.DIFF_ELEVATION_UP, diffElevationUp);
	}

	public double getDiffElevationUp() {
		Object diffElevationUp = getGpxParameter(GpxParameter.DIFF_ELEVATION_UP);
		return (double) diffElevationUp;
	}

	public void setDiffElevationDown(double diffElevationDown) {
		setGpxParameter(GpxParameter.DIFF_ELEVATION_DOWN, diffElevationDown);
	}

	public double getDiffElevationDown() {
		Object diffElevationDown = getGpxParameter(GpxParameter.DIFF_ELEVATION_DOWN);
		return (double) diffElevationDown;
	}

	public void setMinElevation(double minElevation) {
		setGpxParameter(GpxParameter.MIN_ELEVATION, minElevation);
	}

	public double getMinElevation() {
		Object minElevation = getGpxParameter(GpxParameter.MIN_ELEVATION);
		return (double) minElevation;
	}

	public void setAvgElevation(double avgElevation) {
		setGpxParameter(GpxParameter.AVG_ELEVATION, avgElevation);
	}

	public double getAvgElevation() {
		return (double) getGpxParameter(GpxParameter.AVG_ELEVATION);
	}


	public void setAvgSpeed(float avgSpeed) {
		setGpxParameter(GpxParameter.AVG_SPEED, (double) avgSpeed);
	}

	public float getAvgSpeed() {
		Object avgSpeed = getGpxParameter(GpxParameter.AVG_SPEED);
		return ((Double) avgSpeed).floatValue();
	}

	public void setMinSpeed(float minSpeed) {
		setGpxParameter(GpxParameter.MIN_SPEED, (double) minSpeed);
	}

	public float getMinSpeed() {
		Object minSpeed = getGpxParameter(GpxParameter.MIN_SPEED);
		return ((Double) minSpeed).floatValue();
	}

	public void setMaxSpeed(float maxSpeed) {
		setGpxParameter(GpxParameter.MAX_SPEED, (double) maxSpeed);
	}

	public float getMaxSpeed() {
		Object maxSpeed = getGpxParameter(GpxParameter.MAX_SPEED);
		return ((Double) maxSpeed).floatValue();
	}

	public void setMaxSensorHr(int maxSensorHr) {
		setGpxParameter(GpxParameter.MAX_SENSOR_HEART_RATE, maxSensorHr);
	}

	public int getMaxSensorHr() {
		Object maxSensorHr = getGpxParameter(GpxParameter.MAX_SENSOR_HEART_RATE);
		return (int) maxSensorHr;
	}

	public void setPoints(int points) {
		setGpxParameter(GpxParameter.POINTS, points);
	}

	public int getPoints() {
		Object points = getGpxParameter(GpxParameter.POINTS);
		return (int) points;
	}

	public void setWptPoints(int wptPoints) {
		setGpxParameter(GpxParameter.WPT_POINTS, wptPoints);
	}

	public int getWptPoints() {
		Object wptPoints = getGpxParameter(GpxParameter.WPT_POINTS);
		return (int) wptPoints;
	}

	public void setMaxSensorTemperature(int maxSensorTemperature) {
		setGpxParameter(GpxParameter.MAX_SENSOR_TEMPERATURE, maxSensorTemperature);
	}

	public int getMaxSensorPower() {
		Object maxSensorPower = getGpxParameter(GpxParameter.MAX_SENSOR_POWER);
		return (int) maxSensorPower;
	}

	public void setMaxSensorPower(int maxSensorPower) {
		setGpxParameter(GpxParameter.MAX_SENSOR_POWER, maxSensorPower);
	}

	public int getTotalTracks() {
		Object totalTracks = getGpxParameter(GpxParameter.TOTAL_TRACKS);
		return (int) totalTracks;
	}

	public void setTotalTracks(int totalTracks) {
		setGpxParameter(GpxParameter.TOTAL_TRACKS, totalTracks);
	}

	public int getMaxSensorTemperature() {
		Object maxSensorTemperature = getGpxParameter(GpxParameter.MAX_SENSOR_TEMPERATURE);
		return (int) maxSensorTemperature;
	}

	public void setMaxSensorSpeed(float maxSensorSpeed) {
		setGpxParameter(GpxParameter.MAX_SENSOR_SPEED, (double) maxSensorSpeed);
	}

	public float getMaxSensorSpeed() {
		Object maxSensorSpeed = getGpxParameter(GpxParameter.MAX_SENSOR_SPEED);
		return ((Double) maxSensorSpeed).floatValue();
	}

	public void setMaxSensorCadence(float maxSensorCadence) {
		setGpxParameter(GpxParameter.MAX_SENSOR_CADENCE, (double) maxSensorCadence);
	}

	public float getMaxSensorCadence() {
		Object maxSensorCadence = getGpxParameter(GpxParameter.MAX_SENSOR_CADENCE);
		return ((Double) maxSensorCadence).floatValue();
	}

	public void setAvgSensorSpeed(float avgSensorSpeed) {
		setGpxParameter(GpxParameter.AVG_SENSOR_SPEED, (double) avgSensorSpeed);
	}

	public float getAvgSensorSpeed() {
		Object avgSensorSpeed = getGpxParameter(GpxParameter.AVG_SENSOR_SPEED);
		return ((Double) avgSensorSpeed).floatValue();
	}

	public void setAvgSensorCadence(float avgSensorCadence) {
		setGpxParameter(GpxParameter.AVG_SENSOR_CADENCE, (double) avgSensorCadence);
	}

	public float getAvgSensorCadence() {
		Object avgSensorCadence = getGpxParameter(GpxParameter.AVG_SENSOR_CADENCE);
		return ((Double) avgSensorCadence).floatValue();
	}

	public void setAvgSensorHr(float avgSensorHr) {
		setGpxParameter(GpxParameter.AVG_SENSOR_HEART_RATE, (double) avgSensorHr);
	}

	public float getAvgSensorHr() {
		Object avgSensorHr = getGpxParameter(GpxParameter.AVG_SENSOR_HEART_RATE);
		return ((Double) avgSensorHr).floatValue();
	}

	public void setAvgSensorPower(float avgSensorPower) {
		setGpxParameter(GpxParameter.AVG_SENSOR_POWER, (double) avgSensorPower);
	}

	public float getAvgSensorPower() {
		Object avgSensorPower = getGpxParameter(GpxParameter.AVG_SENSOR_POWER);
		return ((Double) avgSensorPower).floatValue();
	}

	public void setAvgSensorTemperature(float avgSensorTemperature) {
		setGpxParameter(GpxParameter.AVG_SENSOR_TEMPERATURE, (double) avgSensorTemperature);
	}

	public float getAvgSensorTemperature() {
		Object avgSensorTemperature = getGpxParameter(GpxParameter.AVG_SENSOR_TEMPERATURE);
		return ((Double) avgSensorTemperature).floatValue();
	}

	public void setTotalDistanceMoving(float totalDistanceMoving) {
		setGpxParameter(GpxParameter.TOTAL_DISTANCE_MOVING, (double) totalDistanceMoving);
	}

	public float getTotalDistanceMoving() {
		Object totalDistanceMoving = getGpxParameter(GpxParameter.TOTAL_DISTANCE_MOVING);
		return ((Double) totalDistanceMoving).floatValue();
	}

	public void setTotalDistance(float totalDistance) {
		setGpxParameter(GpxParameter.TOTAL_DISTANCE, (double) totalDistance);
	}

	public float getTotalDistance() {
		Object totalDistance = getGpxParameter(GpxParameter.TOTAL_DISTANCE);
		return ((Double) totalDistance).floatValue();
	}

	public boolean isTimeSpecified() {
		return getStartTime() != Long.MAX_VALUE && getStartTime() != 0;
	}

	public boolean isTimeMoving() {
		return getTimeMoving() != 0;
	}

	public boolean isElevationSpecified() {
		return getMaxElevation() != -100;
	}

	public boolean hasSpeedInTrack() {
		return hasSpeedInTrack;
	}

	public boolean isBoundsCalculated() {
		return left != 0 && right != 0 && top != 0 && bottom != 0;
	}

	public boolean isSpeedSpecified() {
		return getAvgSpeed() > 0;
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

	public void setLatLonStart(double latitude, double longitude) {
		setGpxParameter(GpxParameter.START_LAT, latitude);
		setGpxParameter(GpxParameter.START_LON, longitude);
	}

	public LatLon getLatLonStart() {
		Object lat = getGpxParameter(GpxParameter.START_LAT);
		Object lon = getGpxParameter(GpxParameter.START_LON);

		if (lat != null && lon != null) {
			return new LatLon((double) lat, (double) lon);
		} else {
			return null;
		}
	}

	public Object getLatStart() {
		return getGpxParameter(GpxParameter.START_LAT);
	}

	public Object getLonStart() {
		return getGpxParameter(GpxParameter.START_LON);
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

	public void setWptCategoryNames(String wptCategoryNames) {
		setGpxParameter(GpxParameter.WPT_CATEGORY_NAMES, wptCategoryNames);
	}

	public void setWptCategoryNames(Set<String> wptCategoryNames) {
		setGpxParameter(GpxParameter.WPT_CATEGORY_NAMES, wptCategoryNames == null ? null : Algorithms.encodeCollection(wptCategoryNames));
	}

	public String getWptCategoryNames() {
		return (String) getGpxParameter(GpxParameter.WPT_CATEGORY_NAMES);
	}

	public Set<String> getWptCategoryNamesSet() {
		String wptCategoryNames = getWptCategoryNames();
		return wptCategoryNames == null ? null : Algorithms.decodeStringSet(wptCategoryNames);
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

		int sensorSpeedCount = 0;
		double totalSensorSpeedSum = 0;
		int sensorHrCount = 0;
		long totalSensorHrSum = 0;
		int sensorPowerCount = 0;
		long totalSensorPowerSum = 0;
		int sensorTemperatureCount = 0;
		long totalSensorTemperatureSum = 0;
		int sensorCadenceCount = 0;
		double totalSensorCadenceSum = 0;

		setPoints(0);

		pointAttributes = new ArrayList<>();
		availableAttributes = new HashSet<>();
		for (final SplitSegment s : splitSegments) {
			final int numberOfPoints = s.getNumberOfPoints();
			float segmentDistance = 0f;
			metricEnd += s.metricEnd;
			secondaryMetricEnd += s.secondaryMetricEnd;
			setPoints(getPoints() + numberOfPoints);
			expectedRouteDuration += getExpectedRouteSegmentDuration(s);
			for (int j = 0; j < numberOfPoints; j++) {
				WptPt point = s.get(j);
				if (j == 0 && locationStart == null) {
					locationStart = point;
					setLatLonStart(point.lat, point.lon);
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
					setStartTime(Math.min(getStartTime(), time));
					setEndTime(Math.max(getEndTime(), time));
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
					setTotalDistance(getTotalDistance() + calculations[0]);
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
						setTimeMoving(getTimeMoving() + timeDiffMillis);
						setTotalDistanceMoving(getTotalDistanceMoving() + calculations[0]);
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
				setMinSpeed(Math.min(speed, getMinSpeed()));
				if (speed > 0 && !Float.isInfinite(speed)) {
					totalSpeedSum += speed;
					setMaxSpeed(Math.max(speed, getMaxSpeed()));
					speedCount++;
				}
				boolean isNaN = Double.isNaN(point.ele);
				float elevation = isNaN ? Float.NaN : (float) point.ele;
				if (!isNaN) {
					totalElevation += point.ele;
					elevationPoints++;
					setMinElevation(Math.min(point.ele, getMinElevation()));
					setMaxElevation(Math.max(point.ele, getMaxElevation()));
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
				if (attribute.sensorSpeed > 0 && !Float.isInfinite(attribute.sensorSpeed)) {
					setMaxSensorSpeed(Math.max(attribute.sensorSpeed, getMaxSensorSpeed()));
					sensorSpeedCount++;
					totalSensorSpeedSum += attribute.sensorSpeed;
				}

				if (attribute.bikeCadence > 0) {
					setMaxSensorCadence(Math.max(attribute.bikeCadence, getMaxSensorCadence()));
					sensorCadenceCount++;
					totalSensorCadenceSum += attribute.bikeCadence;
				}

				if (attribute.heartRate > 0) {
					setMaxSensorHr(Math.max((int) attribute.heartRate, getMaxSensorHr()));
					sensorHrCount++;
					totalSensorHrSum += attribute.heartRate;
				}

				if (attribute.temperature > 0) {
					setMaxSensorTemperature(Math.max((int) attribute.temperature, getMaxSensorTemperature()));
					sensorTemperatureCount++;
					totalSensorTemperatureSum += attribute.temperature;
				}

				if (attribute.bikePower > 0) {
					setMaxSensorPower(Math.max((int) attribute.bikePower, getMaxSensorPower()));
					sensorPowerCount++;
					totalSensorPowerSum += attribute.bikePower;
				}
			}
			processElevationDiff(s);
		}
		checkUnspecifiedValues(fileTimeStamp);
		processAverageValues(totalElevation, elevationPoints, totalSpeedSum, speedCount);

		setAvgSensorSpeed(processAverageValue(totalSensorSpeedSum, sensorSpeedCount));
		setAvgSensorCadence(processAverageValue(totalSensorCadenceSum, sensorCadenceCount));
		setAvgSensorHr(processAverageValue(totalSensorHrSum, sensorHrCount));
		setAvgSensorPower(processAverageValue(totalSensorPowerSum, sensorPowerCount));
		setAvgSensorTemperature(processAverageValue(totalSensorTemperatureSum, sensorTemperatureCount));
		return this;
	}

	private void addWptAttribute(WptPt point, PointAttributes attribute, TrackPointsAnalyser pointsAnalyser) {
		if (!hasSpeedData() && attribute.speed > 0 && getTotalDistance() > 0) {
			setHasData(POINT_SPEED, true);
		}
		if (!hasElevationData() && !Float.isNaN(attribute.elevation) && getTotalDistance() > 0) {
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
		if (getTotalDistance() < 0) {
			availableAttributes.clear();
		}
		if (!isTimeSpecified()) {
			setStartTime(fileTimeStamp);
			setEndTime(fileTimeStamp);
		}
		if (getTimeSpan() == 0) {
			setTimeSpan(getEndTime() - getStartTime());
		}
	}

	public long getDurationInMs() {
		return getTimeSpan() > 0 ? getTimeSpan() : expectedRouteDuration;
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
			setAvgElevation(totalElevation / elevationPoints);
		}
		//    Average speed, if any. Average speed is NOT overall (effective) speed, but only calculated for "moving" periods.
		//    Averaging speed values is less precise than totalDistanceMoving/timeMoving
		if (speedCount > 0) {
			if (getTimeMoving() > 0) {
				setAvgSpeed(getTotalDistanceMoving() / (float) getTimeMoving() * 1000f);
			} else {
				setAvgSpeed((float) totalSpeedSum / (float) speedCount);
			}
		} else {
			setAvgSpeed(-1);
		}
	}

	private Float processAverageValue(Number totalSum, int valuesCount) {
		if (valuesCount > 0) {
			return (float) ((double) totalSum / valuesCount);
		} else {
			return -1f;
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
			setDiffElevationUp(getDiffElevationUp() + elevationDiffsCalc.getDiffElevationUp());
			setDiffElevationDown(getDiffElevationDown() + elevationDiffsCalc.getDiffElevationDown());
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
