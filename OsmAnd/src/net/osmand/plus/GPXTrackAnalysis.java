package net.osmand.plus;

public class GPXTrackAnalysis {

	public float totalDistance = 0;
	public int totalTracks = 0;
	public long startTime = Long.MAX_VALUE;
	public long endTime = Long.MIN_VALUE;
	public long timeSpan = 0;
	public long timeMoving = 0;
	public float totalDistanceMoving = 0;

	public double diffElevationUp = 0;
	public double diffElevationDown = 0;
	public double avgElevation = 0;
	public double minElevation = 99999;
	public double maxElevation = -100;

	public float maxSpeed = 0;
	public float avgSpeed;

	public int points;
	public int wptPoints = 0;

	public double metricEnd;
	public double secondaryMetricEnd;
	public WptPt locationStart;
	public WptPt locationEnd;

	public boolean isTimeSpecified() {
		return startTime != Long.MAX_VALUE && startTime != 0;
	}

	public boolean isTimeMoving() {
		return timeMoving != 0;
	}

	public boolean isElevationSpecified() {
		return maxElevation != -100;
	}

	public boolean isSpeedSpecified() {
		return avgSpeed > 0;
	}


	public static GPXTrackAnalysis segment(long filetimestamp, TrkSegment segment) {
		return new GPXTrackAnalysis().prepareInformation(filetimestamp, new SplitSegment(segment));
	}

	public GPXTrackAnalysis prepareInformation(long filestamp, SplitSegment... splitSegments) {
		float[] calculations = new float[1];

		float totalElevation = 0;
		int elevationPoints = 0;
		int speedCount = 0;
		double totalSpeedSum = 0;
		points = 0;

		double channelThresMin = 5;            // Minimum oscillation amplitude considered as noise for Up/Down analysis
		double channelThres = channelThresMin; // Actual oscillation amplitude considered as noise, try depedency on current hdop
		double channelBase;
		double channelTop;
		double channelBottom;
		boolean climb = false;

		for (SplitSegment s : splitSegments) {
			final int numberOfPoints = s.getNumberOfPoints();

			channelBase = 99999;
			channelTop = channelBase;
			channelBottom = channelBase;
			channelThres = channelThresMin;

			metricEnd += s.metricEnd;
			secondaryMetricEnd += s.secondaryMetricEnd;
			points += numberOfPoints;
			for (int j = 0; j < numberOfPoints; j++) {
				WptPt point = s.get(j);
				if(j == 0 && locationStart == null) {
					locationStart = point;
				}
				if(j == numberOfPoints - 1) {
					locationEnd = point;
				}
				long time = point.time;
				if (time != 0) {
					startTime = Math.min(startTime, time);
					endTime = Math.max(endTime, time);
				}

				double elevation = point.ele;
				if (!Double.isNaN(elevation)) {
					totalElevation += elevation;
					elevationPoints++;
					minElevation = Math.min(elevation, minElevation);
					maxElevation = Math.max(elevation, maxElevation);
				}

				float speed = (float) point.speed;
				if (speed > 0) {
					totalSpeedSum += speed;
					maxSpeed = Math.max(speed, maxSpeed);
					speedCount++;
				}

				// Trend channel approach for elevation gain/loss, Hardy 2015-09-22
				// Self-adjusting turnarund threshold added for testing 2015-09-25: Current rule is now: "All up/down trends of amplitude <X are ignored to smooth the noise, where X is the maximum observed DOP value of any point which contributed to the current trend (but at least 5 m as the minimum noise threshold)".
				if (!Double.isNaN(point.ele)) {
					// Init channel
					if (channelBase == 99999) {
						channelBase = point.ele;
						channelTop = channelBase;
						channelBottom = channelBase;
						channelThres = channelThresMin;
					}
					// Channel maintenance
					if (point.ele > channelTop) {
						channelTop = point.ele;
						if (!Double.isNaN(point.hdop)) {
							channelThres = Math.max(channelThres, 2.0*point.hdop);  //Try empirical 2*hdop, may better serve very flat tracks, or high dop tracks
						}
					} else if (point.ele < channelBottom) {
						channelBottom = point.ele;
						if (!Double.isNaN(point.hdop)) {
							channelThres = Math.max(channelThres, 2.0*point.hdop);
						}
					}
					// Turnaround (breakout) detection
					if ((point.ele <= (channelTop - channelThres)) && (climb == true)) {
						if ((channelTop - channelBase) >= channelThres) {
							diffElevationUp += channelTop - channelBase;
						}
						channelBase = channelTop;
						channelBottom = point.ele;
						climb = false;
						channelThres = channelThresMin;
					} else if ((point.ele >= (channelBottom + channelThres)) && (climb == false)) {
						if ((channelBase - channelBottom) >= channelThres) {
							diffElevationDown += channelBase - channelBottom;
						}
						channelBase = channelBottom;
						channelTop = point.ele;
						climb = true;
						channelThres = channelThresMin;
					}
					// End detection without breakout
					if (j == (numberOfPoints -1)) {
						if ((channelTop - channelBase) >= channelThres) {
							diffElevationUp += channelTop - channelBase;
						}
						if ((channelBase - channelBottom) >= channelThres) {
							diffElevationDown += channelBase - channelBottom;
						}
					}
				}

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

					// Averaging speed values is less exact than totalDistance/timeMoving
					if (speed > 0 && point.time != 0 && prev.time != 0) {
						timeMoving = timeMoving + (point.time - prev.time);
						totalDistanceMoving += calculations[0];
					}
				}
			}
		}
		if(!isTimeSpecified()){
			startTime = filestamp;
			endTime = filestamp;
		}

		// OUTPUT:
		// 1. Total distance, Start time, End time
		// 2. Time span
		timeSpan = endTime - startTime;

		// 3. Time moving, if any
		// 4. Elevation, eleUp, eleDown, if recorded
		if (elevationPoints > 0) {
			avgElevation =  totalElevation / elevationPoints;
		}



		// 5. Max speed and Average speed, if any. Average speed is NOT overall (effective) speed, but only calculated for "moving" periods.
		if(speedCount > 0) {
			if(timeMoving > 0){
				avgSpeed = (float)totalDistanceMoving / (float)timeMoving * 1000f;
			} else {
				avgSpeed = (float)totalSpeedSum / (float)speedCount;
			}
		} else {
			avgSpeed = -1;
		}
		return this;
	}

}
