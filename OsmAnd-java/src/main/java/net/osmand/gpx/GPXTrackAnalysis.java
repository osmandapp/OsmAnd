package net.osmand.gpx;

import net.osmand.gpx.GPXUtilities.TrkSegment;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.router.RouteColorize;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GPXTrackAnalysis {

	public String name;

	public float totalDistance = 0;
	public float totalDistanceWithoutGaps = 0;
	public int totalTracks = 0;
	public long startTime = Long.MAX_VALUE;
	public long endTime = Long.MIN_VALUE;
	public long timeSpan = 0;
	public long timeSpanWithoutGaps = 0;
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
	public GPXUtilities.WptPt locationStart;
	public GPXUtilities.WptPt locationEnd;

	public double left = 0;
	public double right = 0;
	public double top = 0;
	public double bottom = 0;

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

	public List<GPXUtilities.Elevation> elevationData;
	public List<GPXUtilities.Speed> speedData;

	public boolean hasElevationData;
	public boolean hasSpeedData;
	public boolean hasSpeedInTrack = false;

	public boolean isSpeedSpecified() {
		return avgSpeed > 0;
	}

	public boolean isHdopSpecified() {
		return minHdop > 0;
	}

	public boolean isColorizationTypeAvailable(RouteColorize.ColorizationType colorizationType) {
		if (colorizationType == RouteColorize.ColorizationType.SPEED) {
			return isSpeedSpecified();
		} else if (colorizationType == RouteColorize.ColorizationType.ELEVATION || colorizationType == RouteColorize.ColorizationType.SLOPE) {
			return isElevationSpecified();
		} else {
			return true;
		}
	}

	public static GPXTrackAnalysis segment(long filetimestamp, GPXUtilities.TrkSegment segment) {
		return new GPXTrackAnalysis().prepareInformation(filetimestamp, new SplitSegment(segment));
	}

	public GPXTrackAnalysis prepareInformation(long filestamp, SplitSegment... splitSegments) {
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

		elevationData = new ArrayList<>();
		speedData = new ArrayList<>();

		for (final SplitSegment s : splitSegments) {
			final int numberOfPoints = s.getNumberOfPoints();
			float segmentDistance = 0f;
			metricEnd += s.metricEnd;
			secondaryMetricEnd += s.secondaryMetricEnd;
			points += numberOfPoints;
			for (int j = 0; j < numberOfPoints; j++) {
				GPXUtilities.WptPt point = s.get(j);
				if (j == 0 && locationStart == null) {
					locationStart = point;
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

				double elevation = point.ele;
				GPXUtilities.Elevation elevation1 = new GPXUtilities.Elevation();
				if (!Double.isNaN(elevation)) {
					totalElevation += elevation;
					elevationPoints++;
					minElevation = Math.min(elevation, minElevation);
					maxElevation = Math.max(elevation, maxElevation);

					elevation1.elevation = (float) elevation;
				} else {
					elevation1.elevation = Float.NaN;
				}

				float speed = (float) point.speed;
				if (speed > 0) {
					hasSpeedInTrack = true;
				}

				double hdop = point.hdop;
				if (hdop > 0) {
					if (Double.isNaN(minHdop) || hdop < minHdop) {
						minHdop = hdop;
					}
					if (Double.isNaN(maxHdop) || hdop > maxHdop) {
						maxHdop = hdop;
					}
				}

				if (j > 0) {
					GPXUtilities.WptPt prev = s.get(j - 1);

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

				elevation1.timeDiff = ((float) timeDiffMillis) / 1000;
				elevation1.distance = (j > 0) ? calculations[0] : 0;
				elevationData.add(elevation1);
				if (!hasElevationData && !Float.isNaN(elevation1.elevation) && totalDistance > 0) {
					hasElevationData = true;
				}

				minSpeed = Math.min(speed, minSpeed);
				if (speed > 0) {
					totalSpeedSum += speed;
					maxSpeed = Math.max(speed, maxSpeed);
					speedCount++;
				}

				GPXUtilities.Speed speed1 = new GPXUtilities.Speed();
				speed1.speed = speed;
				speed1.timeDiff = ((float) timeDiffMillis) / 1000;
				speed1.distance = elevation1.distance;
				speedData.add(speed1);
				if (!hasSpeedData && speed1.speed > 0 && totalDistance > 0) {
					hasSpeedData = true;
				}
				if (s.segment.generalSegment) {
					distanceOfSingleSegment += calculations[0];
					if (point.firstPoint) {
						distanceOfSingleSegment = 0;
						timeMovingOfSingleSegment = 0;
						distanceMovingOfSingleSegment = 0;
						if (j > 0) {
							elevation1.firstPoint = true;
							speed1.firstPoint = true;
						}
					}
					if (point.lastPoint) {
						totalDistanceWithoutGaps += distanceOfSingleSegment;
						timeMovingWithoutGaps += timeMovingOfSingleSegment;
						totalDistanceMovingWithoutGaps += distanceMovingOfSingleSegment;
						if (j < numberOfPoints - 1) {
							elevation1.lastPoint = true;
							speed1.lastPoint = true;
						}
					}
				}
			}

			ElevationDiffsCalculator elevationDiffsCalc = new ElevationDiffsCalculator(0, numberOfPoints) {
				@Override
				public GPXUtilities.WptPt getPoint(int index) {
					return s.get(index);
				}
			};
			elevationDiffsCalc.calculateElevationDiffs();
			diffElevationUp += elevationDiffsCalc.diffElevationUp;
			diffElevationDown += elevationDiffsCalc.diffElevationDown;
		}
		if (totalDistance < 0) {
			hasElevationData = false;
			hasSpeedData = false;
		}
		if (!isTimeSpecified()) {
			startTime = filestamp;
			endTime = filestamp;
		}

		// OUTPUT:
		// 1. Total distance, Start time, End time
		// 2. Time span
		if (timeSpan == 0) {
			timeSpan = endTime - startTime;
		}

		// 3. Time moving, if any
		// 4. Elevation, eleUp, eleDown, if recorded
		if (elevationPoints > 0) {
			avgElevation = totalElevation / elevationPoints;
		}


		// 5. Max speed and Average speed, if any. Average speed is NOT overall (effective) speed, but only calculated for "moving" periods.
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
		return this;
	}

	public abstract static class ElevationDiffsCalculator {

		public static final double CALCULATED_GPX_WINDOW_LENGTH = 10d;

		private double windowLength;
		private final int startIndex;
		private final int numberOfPoints;

		private double diffElevationUp = 0;
		private double diffElevationDown = 0;

		public ElevationDiffsCalculator(int startIndex, int numberOfPoints) {
			this.startIndex = startIndex;
			this.numberOfPoints = numberOfPoints;
			GPXUtilities.WptPt lastPoint = getPoint(startIndex + numberOfPoints - 1);
			this.windowLength = lastPoint.time == 0 ? CALCULATED_GPX_WINDOW_LENGTH : Math.max(20d, lastPoint.distance / numberOfPoints * 4);
		}

		public ElevationDiffsCalculator(double windowLength, int startIndex, int numberOfPoints) {
			this(startIndex, numberOfPoints);
			this.windowLength = windowLength;
		}

		public abstract GPXUtilities.WptPt getPoint(int index);

		public double getDiffElevationUp() {
			return diffElevationUp;
		}

		public double getDiffElevationDown() {
			return diffElevationDown;
		}

		public void calculateElevationDiffs() {
			GPXUtilities.WptPt initialPoint = getPoint(startIndex);
			double eleSumm = initialPoint.ele;
			double prevEle = initialPoint.ele;
			int pointsCount = Double.isNaN(eleSumm) ? 0 : 1;
			double eleAvg = Double.NaN;
			double nextWindowPos = initialPoint.distance + windowLength;
			int pointIndex = startIndex + 1;
			while (pointIndex < numberOfPoints + startIndex) {
				GPXUtilities.WptPt point = getPoint(pointIndex);
				if (point.distance > nextWindowPos) {
					eleAvg = calcAvg(eleSumm, pointsCount, eleAvg);
					if (!Double.isNaN(point.ele)) {
						eleSumm = point.ele;
						prevEle = point.ele;
						pointsCount = 1;
					} else if (!Double.isNaN(prevEle)) {
						eleSumm = prevEle;
						pointsCount = 1;
					} else {
						eleSumm = Double.NaN;
						pointsCount = 0;
					}
					while (nextWindowPos < point.distance) {
						nextWindowPos += windowLength;
					}
				} else {
					if (!Double.isNaN(point.ele)) {
						eleSumm += point.ele;
						prevEle = point.ele;
						pointsCount++;
					} else if (!Double.isNaN(prevEle)) {
						eleSumm += prevEle;
						pointsCount++;
					}
				}
				pointIndex++;
			}
			if (pointsCount > 1) {
				calcAvg(eleSumm, pointsCount, eleAvg);
			}
			diffElevationUp = Math.round(diffElevationUp + 0.3f);
		}

		private double calcAvg(double eleSumm, int pointsCount, double eleAvg) {
			if (Double.isNaN(eleSumm) || pointsCount == 0) {
				return Double.NaN;
			}
			double avg = eleSumm / pointsCount;
			if (!Double.isNaN(eleAvg)) {
				double diff = avg - eleAvg;
				if (diff > 0) {
					diffElevationUp += diff;
				} else {
					diffElevationDown -= diff;
				}
			}
			return avg;
		}
	}

	static class SplitSegment {
		GPXUtilities.TrkSegment segment;
		double startCoeff = 0;
		int startPointInd;
		double endCoeff = 0;
		int endPointInd;
		double metricEnd;
		double secondaryMetricEnd;

		public SplitSegment(GPXUtilities.TrkSegment s) {
			startPointInd = 0;
			startCoeff = 0;
			endPointInd = s.points.size() - 2;
			endCoeff = 1;
			this.segment = s;
		}

		public SplitSegment(int startInd, int endInd, GPXUtilities.TrkSegment s) {
			startPointInd = startInd;
			startCoeff = 0;
			endPointInd = endInd - 2;
			endCoeff = 1;
			this.segment = s;
		}

		public SplitSegment(GPXUtilities.TrkSegment s, int pointInd, double cf) {
			this.segment = s;
			this.startPointInd = pointInd;
			this.startCoeff = cf;
		}


		public int getNumberOfPoints() {
			return endPointInd - startPointInd + 2;
		}

		public GPXUtilities.WptPt get(int j) {
			final int ind = j + startPointInd;
			if (j == 0) {
				if (startCoeff == 0) {
					return segment.points.get(ind);
				}
				return approx(segment.points.get(ind), segment.points.get(ind + 1), startCoeff);
			}
			if (j == getNumberOfPoints() - 1) {
				if (endCoeff == 1) {
					return segment.points.get(ind);
				}
				return approx(segment.points.get(ind - 1), segment.points.get(ind), endCoeff);
			}
			return segment.points.get(ind);
		}


		private GPXUtilities.WptPt approx(GPXUtilities.WptPt w1, GPXUtilities.WptPt w2, double cf) {
			long time = value(w1.time, w2.time, 0, cf);
			double speed = value(w1.speed, w2.speed, 0, cf);
			double ele = value(w1.ele, w2.ele, 0, cf);
			double hdop = value(w1.hdop, w2.hdop, 0, cf);
			double lat = value(w1.lat, w2.lat, -360, cf);
			double lon = value(w1.lon, w2.lon, -360, cf);
			return new GPXUtilities.WptPt(lat, lon, time, ele, speed, hdop);
		}

		private double value(double vl, double vl2, double none, double cf) {
			if (vl == none || Double.isNaN(vl)) {
				return vl2;
			} else if (vl2 == none || Double.isNaN(vl2)) {
				return vl;
			}
			return vl + cf * (vl2 - vl);
		}

		private long value(long vl, long vl2, long none, double cf) {
			if (vl == none) {
				return vl2;
			} else if (vl2 == none) {
				return vl;
			}
			return vl + ((long) (cf * (vl2 - vl)));
		}


		public double setLastPoint(int pointInd, double endCf) {
			endCoeff = endCf;
			endPointInd = pointInd;
			return endCoeff;
		}

	}

	abstract static class SplitMetric {

		public abstract double metric(GPXUtilities.WptPt p1, GPXUtilities.WptPt p2);

	}
	
	

	static SplitMetric getDistanceMetric() {
		return new SplitMetric() {

			private final float[] calculations = new float[1];

			@Override
			public double metric(WptPt p1, WptPt p2) {
				net.osmand.Location.distanceBetween(p1.lat, p1.lon, p2.lat, p2.lon, calculations);
				return calculations[0];
			}
		};
	}

	static GPXTrackAnalysis.SplitMetric getTimeSplit() {
		return new GPXTrackAnalysis.SplitMetric() {

			@Override
			public double metric(WptPt p1, WptPt p2) {
				if (p1.time != 0 && p2.time != 0) {
					return (int) Math.abs((p2.time - p1.time) / 1000l);
				}
				return 0;
			}
		};
	}

	static void splitSegment(GPXTrackAnalysis.SplitMetric metric, GPXTrackAnalysis.SplitMetric secondaryMetric,
									 double metricLimit, List<GPXTrackAnalysis.SplitSegment> splitSegments,
									 TrkSegment segment, boolean joinSegments) {
		double currentMetricEnd = metricLimit;
		double secondaryMetricEnd = 0;
		GPXTrackAnalysis.SplitSegment sp = new GPXTrackAnalysis.SplitSegment(segment, 0, 0);
		double total = 0;
		WptPt prev = null;
		for (int k = 0; k < segment.points.size(); k++) {
			WptPt point = segment.points.get(k);
			if (k > 0) {
				double currentSegment = 0;
				if (!(segment.generalSegment && !joinSegments && point.firstPoint)) {
					currentSegment = metric.metric(prev, point);
					secondaryMetricEnd += secondaryMetric.metric(prev, point);
				}
				while (total + currentSegment > currentMetricEnd) {
					double p = currentMetricEnd - total;
					double cf = (p / currentSegment);
					sp.setLastPoint(k - 1, cf);
					sp.metricEnd = currentMetricEnd;
					sp.secondaryMetricEnd = secondaryMetricEnd;
					splitSegments.add(sp);

					sp = new GPXTrackAnalysis.SplitSegment(segment, k - 1, cf);
					currentMetricEnd += metricLimit;
				}
				total += currentSegment;
			}
			prev = point;
		}
		if (segment.points.size() > 0
				&& !(sp.endPointInd == segment.points.size() - 1 && sp.startCoeff == 1)) {
			sp.metricEnd = total;
			sp.secondaryMetricEnd = secondaryMetricEnd;
			sp.setLastPoint(segment.points.size() - 2, 1);
			splitSegments.add(sp);
		}
	}
}
