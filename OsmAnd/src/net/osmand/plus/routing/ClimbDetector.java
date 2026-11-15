package net.osmand.plus.routing;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects climb segments in a route for visualization purposes.
 * A climb is defined as a segment where the elevation increases significantly.
 */
public class ClimbDetector {

	private static final int DEFAULT_MIN_CLIMB_ELEVATION = 1; // meters
	private static final float DEFAULT_MIN_CLIMB_GRADE = 0.2f; // percentage
	private static final double PEAK_DESCENT_THRESHOLD = 100; // meters
	private static final double FLAT_DISTANCE_THRESHOLD = 200; // meters

	private int minClimbElevation = DEFAULT_MIN_CLIMB_ELEVATION;
	private float minClimbGrade = DEFAULT_MIN_CLIMB_GRADE;

	/**
	 * Represents a climb segment with start and end indices in the locations list
	 */
	public static class ClimbSegment {
		public final int startIndex;
		public final int endIndex;
		public final double elevationGain;
		public final double distance;
		public final float averageGrade;

		public ClimbSegment(int startIndex, int endIndex, double elevationGain, double distance, float averageGrade) {
			this.startIndex = startIndex;
			this.endIndex = endIndex;
			this.elevationGain = elevationGain;
			this.distance = distance;
			this.averageGrade = averageGrade;
		}
	}

	/**
	 * Detect climbs in the given list of locations
	 * @param locations List of locations with elevation data
	 * @return List of detected climb segments
	 */
	@NonNull
	public List<ClimbSegment> detectClimbs(@NonNull List<Location> locations) {
		List<ClimbSegment> climbs = new ArrayList<>();
		if (locations.size() < 2) {
			return climbs;
		}

		// Pre-calculate cumulative distances for efficiency (avoid O(n^2) complexity)
		List<Double> cumulativeDistances = new ArrayList<>();
		cumulativeDistances.add(0.0);
		for (int i = 1; i < locations.size(); i++) {
			Location prev = locations.get(i - 1);
			Location curr = locations.get(i);
			double dist = MapUtils.getDistance(prev.getLatitude(), prev.getLongitude(),
					curr.getLatitude(), curr.getLongitude());
			cumulativeDistances.add(cumulativeDistances.get(i - 1) + dist);
		}

		// Use same algorithm as ClimbsCountWidget for consistency
		// Allow small descents and plateaus within a climb
		// A climb ends only after significant descent (10m) or when net elevation becomes negative
		int climbStartIndex = 0;
		boolean inClimb = false;
		double currentClimbGain = 0;  // Total elevation gain (only ascending parts)
		double netElevationSinceStart = 0;  // Net elevation from climb start

		for (int i = 1; i < locations.size(); i++) {
			Location prevPoint = locations.get(i - 1);
			Location currPoint = locations.get(i);

			// Skip if no valid elevation data
			if (Double.isNaN(prevPoint.getAltitude()) || Double.isNaN(currPoint.getAltitude())) {
				// End current climb if we were in one and it meets criteria
				if (inClimb && currentClimbGain >= minClimbElevation) {
					// Use pre-calculated cumulative distances for efficiency
					double totalDistance = cumulativeDistances.get(i - 1) - cumulativeDistances.get(climbStartIndex);
					float avgGrade = totalDistance > 0 ? (float) ((currentClimbGain / totalDistance) * 100) : 0;
					if (avgGrade >= minClimbGrade) {
						climbs.add(new ClimbSegment(climbStartIndex, i - 1, currentClimbGain, totalDistance, avgGrade));
					}
				}
				inClimb = false;
				currentClimbGain = 0;
				netElevationSinceStart = 0;
				continue;
			}

			double elevationGain = currPoint.getAltitude() - prevPoint.getAltitude();
			double segmentDistance = cumulativeDistances.get(i) - cumulativeDistances.get(i - 1);

			if (!inClimb) {
				// Not in a climb, check if we should start one (only on positive elevation gain)
				if (elevationGain > 0) {
					inClimb = true;
					climbStartIndex = i - 1;
					currentClimbGain = elevationGain;
					netElevationSinceStart = elevationGain;
				}
			} else {
				// We're in a climb - track net elevation
				netElevationSinceStart += elevationGain;

				if (elevationGain > 0) {
					// Still climbing - add to total gain
					currentClimbGain += elevationGain;
				} else if (elevationGain < 0) {
					// Descending - check if we should end the climb
					// End climb if: net elevation < 0 OR single descent > 10m
					// This allows small descents (<10m) within a climb
					if (netElevationSinceStart < 0 || (prevPoint.getAltitude() - currPoint.getAltitude()) > 10) {
						// End of climb - check if it meets criteria
						if (currentClimbGain >= minClimbElevation) {
							// ALWAYS use cumulative distances for accurate distance
							double totalDistance = cumulativeDistances.get(i - 1) - cumulativeDistances.get(climbStartIndex);
							float avgGrade = totalDistance > 0 ? (float) ((currentClimbGain / totalDistance) * 100) : 0;
							if (avgGrade >= minClimbGrade) {
								climbs.add(new ClimbSegment(climbStartIndex, i - 1, currentClimbGain, totalDistance, avgGrade));
							}
						}
						// Reset
						inClimb = false;
						currentClimbGain = 0;
						netElevationSinceStart = 0;
					}
				}
				// Flat segments: continue climb, no action needed
			}
		}

		// Handle the last climb if we're still in one
		if (inClimb && currentClimbGain >= minClimbElevation) {
			// ALWAYS use cumulative distances for accurate distance
			double totalDistance = cumulativeDistances.get(locations.size() - 1) - cumulativeDistances.get(climbStartIndex);
			float avgGrade = totalDistance > 0 ? (float) ((currentClimbGain / totalDistance) * 100) : 0;
			if (avgGrade >= minClimbGrade) {
				climbs.add(new ClimbSegment(climbStartIndex, locations.size() - 1, currentClimbGain, totalDistance, avgGrade));
			}
		}

		return climbs;
	}

	/**
	 * Set minimum elevation gain for a climb to be considered valid
	 * @param minElevation Minimum elevation in meters
	 */
	public void setMinClimbElevation(int minElevation) {
		this.minClimbElevation = minElevation;
	}

	/**
	 * Set minimum grade for a climb to be considered valid
	 * @param minGrade Minimum grade in percentage
	 */
	public void setMinClimbGrade(float minGrade) {
		this.minClimbGrade = minGrade;
	}
}
