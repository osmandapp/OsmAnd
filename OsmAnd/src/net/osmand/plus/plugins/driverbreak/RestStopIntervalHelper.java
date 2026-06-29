/*
 * OsmAnd, OsmAnd BV <info@osmand.net>, 2010–present
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package net.osmand.plus.plugins.driverbreak;

import androidx.annotation.NonNull;

import net.osmand.router.RouteSegmentResult;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * Computes along-route distances for rest stops (main stage, optional alternative stage, daily cap).
 */
final class RestStopIntervalHelper {

	private static final double MIN_INTERVAL_M = 1.0;
	/** Fallback speed when a route segment has no timing data (~80 km/h). */
	private static final double DEFAULT_SPEED_MPS = 22.0;

	private RestStopIntervalHelper() {
	}

	@NonNull
	static List<Double> hikingOrCyclingDistancesM(double totalDistanceM, double mainDistKm,
			double altDistKm, boolean altEnabled, double maxDailyKm) {
		double mainIntervalM = mainDistKm * 1000.0;
		Double altIntervalM = altEnabled && altDistKm > 0.0 ? altDistKm * 1000.0 : null;
		List<Double> merged = mergeIntervalTargets(totalDistanceM, mainIntervalM, altIntervalM);
		return applyDailyCap(merged, maxDailyKm * 1000.0);
	}

	/**
	 * Places driving breaks by walking route segment travel times.
	 *
	 * @param breakIntervalSec      continuous driving time before a rest (seconds)
	 * @param maxDailyDrivingSec    max driving time per day (seconds); 0 disables daily cap
	 */
	@NonNull
	static List<Double> drivingBreakDistancesM(@NonNull List<RouteSegmentResult> segments,
			double breakIntervalSec, double maxDailyDrivingSec) {
		if (breakIntervalSec <= 0.0 || segments.isEmpty()) {
			return new ArrayList<>();
		}
		List<Double> targets = new ArrayList<>();
		double routeDistM = 0.0;
		double timeSinceBreakSec = 0.0;
		double timeInDaySec = 0.0;

		for (RouteSegmentResult segment : segments) {
			double segDistM = segment.getDistance();
			if (segDistM <= 0.0) {
				continue;
			}
			double segTimeSec = segmentTimeSec(segment);
			double segStartDistM = routeDistM;
			double consumedSec = 0.0;

			while (consumedSec + 0.001 < segTimeSec) {
				double remainingSegSec = segTimeSec - consumedSec;
				double timeUntilBreakSec = breakIntervalSec - timeSinceBreakSec;
				if (timeUntilBreakSec > remainingSegSec + 0.001) {
					timeSinceBreakSec += remainingSegSec;
					timeInDaySec += remainingSegSec;
					consumedSec = segTimeSec;
					continue;
				}
				double elapsedInSegSec = consumedSec + timeUntilBreakSec;
				double fraction = elapsedInSegSec / segTimeSec;
				double breakDistM = segStartDistM + fraction * segDistM;
				if (maxDailyDrivingSec > 0.0 && timeInDaySec + timeUntilBreakSec > maxDailyDrivingSec + 0.001) {
					timeInDaySec = 0.0;
				}
				targets.add(breakDistM);
				timeSinceBreakSec = 0.0;
				timeInDaySec += timeUntilBreakSec;
				consumedSec = elapsedInSegSec;
			}
			routeDistM += segDistM;
		}
		return targets;
	}

	@NonNull
	static List<Double> mergeIntervalTargets(double totalDistanceM, double mainIntervalM,
			Double altIntervalM) {
		TreeSet<Double> distances = new TreeSet<>();
		addIntervalTargets(distances, totalDistanceM, mainIntervalM);
		if (altIntervalM != null) {
			addIntervalTargets(distances, totalDistanceM, altIntervalM);
		}
		return new ArrayList<>(distances);
	}

	@NonNull
	static List<Double> applyDailyCap(@NonNull List<Double> sortedTargetsM, double maxDailyM) {
		if (maxDailyM <= MIN_INTERVAL_M || sortedTargetsM.isEmpty()) {
			return sortedTargetsM;
		}
		List<Double> capped = new ArrayList<>();
		for (double distanceM : sortedTargetsM) {
			double dayStartM = Math.floor(distanceM / maxDailyM) * maxDailyM;
			if (distanceM - dayStartM <= maxDailyM + 0.01) {
				capped.add(distanceM);
			}
		}
		return capped;
	}

	@NonNull
	static List<RouteSegmentSample> samplesForTest(double distanceM, float timeSec) {
		List<RouteSegmentSample> samples = new ArrayList<>();
		samples.add(new RouteSegmentSample(distanceM, timeSec));
		return samples;
	}

	@NonNull
	static List<Double> drivingBreakDistancesFromSamples(@NonNull List<RouteSegmentSample> samples,
			double breakIntervalSec, double maxDailyDrivingSec) {
		List<RouteSegmentResult> segments = new ArrayList<>(samples.size());
		for (RouteSegmentSample sample : samples) {
			segments.add(sample.asSegment());
		}
		return drivingBreakDistancesM(segments, breakIntervalSec, maxDailyDrivingSec);
	}

	private static double segmentTimeSec(@NonNull RouteSegmentResult segment) {
		float segmentTime = segment.getSegmentTime();
		if (segmentTime > 0.0f) {
			return segmentTime;
		}
		float routingTime = segment.getRoutingTime();
		if (routingTime > 0.0f) {
			return routingTime;
		}
		double distanceM = segment.getDistance();
		if (distanceM > 0.0) {
			return distanceM / DEFAULT_SPEED_MPS;
		}
		return 0.0;
	}

	private static void addIntervalTargets(@NonNull TreeSet<Double> distances, double totalDistanceM,
			double intervalM) {
		if (intervalM <= MIN_INTERVAL_M || totalDistanceM <= intervalM) {
			return;
		}
		for (double d = intervalM; d < totalDistanceM; d += intervalM) {
			distances.add(d);
		}
	}

	static final class RouteSegmentSample {
		final double distanceM;
		final float timeSec;

		RouteSegmentSample(double distanceM, float timeSec) {
			this.distanceM = distanceM;
			this.timeSec = timeSec;
		}

		@NonNull
		RouteSegmentResult asSegment() {
			RouteSegmentResult segment = new RouteSegmentResult(null, 0, 0);
			segment.setDistance((float) distanceM);
			segment.setSegmentTime(timeSec);
			return segment;
		}
	}
}
