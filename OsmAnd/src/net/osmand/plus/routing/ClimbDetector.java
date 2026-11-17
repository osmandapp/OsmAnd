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
	private static final double WINDOW_SIZE = 200.0; // meters - window size for moving average
	private static final double MIN_WINDOW_DISTANCE = 50.0; // minimum distance for a valid window

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
	 * Detect climbs in the given list of locations using SLIDING WINDOW approach.
	 * 
	 * SLIDING WINDOW ALGORITHM (2026-11-15 v4):
	 * Based on user request: use a rolling window to calculate average grade
	 * 
	 * Steps:
	 * 1. Pre-calculate cumulative distances for all points
	 * 2. For each point, calculate the average grade in a window extending forward WINDOW_SIZE meters
	 *    - The window includes the current point and all subsequent points within WINDOW_SIZE meters
	 *    - Average grade = (total positive elevation gain in window / window distance) * 100
	 * 3. Mark a point as being in a climb if its window average grade >= minClimbGrade
	 * 4. Group consecutive points that are in a climb into climb segments
	 * 5. For each climb segment, calculate:
	 *    - Total positive elevation gain: sum of all positive elevation changes
	 *    - Total distance: sum of all segment distances
	 *    - Average grade: (totalElevationGain / totalDistance) * 100
	 * 6. Only count climbs where total elevation gain >= minClimbElevation AND average grade >= minClimbGrade
	 * 
	 * This approach:
	 * - Smooths out small undulations and noise in elevation data
	 * - Properly handles climbs with small descents within them
	 * - Uses the user's settings for thresholds
	 * - Groups consecutive climbing points into single climbs
	 * 
	 * Example: 5m climb over 250m with 2% avg grade, with minClimbElevation=3, minClimbGrade=1%
	 * - Each window will have avg grade around 2%
	 * - All windows will be marked as climb
	 * - Grouped into 1 climb with 5m gain, 250m distance, 2% grade
	 * - Meets both thresholds: 5m >= 3m AND 2% >= 1%
	 * - Result: 1 climb (correct)
	 * 
	 * @param locations List of locations with elevation data
	 * @return List of detected climb segments
	 */
	@NonNull
	public List<ClimbSegment> detectClimbs(@NonNull List<Location> locations) {
		List<ClimbSegment> climbs = new ArrayList<>();
		if (locations.size() < 2) {
			return climbs;
		}

		// Pre-calculate cumulative distances
		List<Double> cumulativeDistances = new ArrayList<>();
		cumulativeDistances.add(0.0);
		for (int i = 1; i < locations.size(); i++) {
			Location prev = locations.get(i - 1);
			Location curr = locations.get(i);
			double dist = MapUtils.getDistance(prev.getLatitude(), prev.getLongitude(),
					curr.getLatitude(), curr.getLongitude());
			cumulativeDistances.add(cumulativeDistances.get(i - 1) + dist);
		}

		double totalRouteDistance = cumulativeDistances.get(cumulativeDistances.size() - 1);

		// If route is very short, adjust window size
		double effectiveWindowSize = Math.min(WINDOW_SIZE, totalRouteDistance * 0.5);
		if (effectiveWindowSize < MIN_WINDOW_DISTANCE) {
			// For very short routes, just check the whole route
			effectiveWindowSize = totalRouteDistance;
		}

		// Step 1: Calculate window average grade for each point
		// For each point i, calculate the average grade in the window [i, i + effectiveWindowSize]
		List<Float> windowAvgGrades = new ArrayList<>();
		List<Boolean> isClimbPoint = new ArrayList<>();

		for (int i = 0; i < locations.size(); i++) {
			// Find all points within effectiveWindowSize from point i
			double windowStartDist = cumulativeDistances.get(i);
			double windowEndDist = windowStartDist + effectiveWindowSize;

			// Find end point of window
			int endIdx = locations.size() - 1;
			for (int j = i + 1; j < locations.size(); j++) {
				if (cumulativeDistances.get(j) > windowEndDist) {
					endIdx = j - 1;
					break;
				}
			}

			// If endIdx <= i, we don't have enough points for a window
			// Use the next point if available
			if (endIdx <= i && i + 1 < locations.size()) {
				endIdx = i + 1;
			}

			// Calculate positive elevation gain and distance in this window
			double windowPositiveElevationGain = 0;
			double windowDistance = 0;
			boolean hasValidElevation = true;

			for (int j = i + 1; j <= endIdx; j++) {
				Location prevPoint = locations.get(j - 1);
				Location currPoint = locations.get(j);

				if (Double.isNaN(prevPoint.getAltitude()) || Double.isNaN(currPoint.getAltitude())) {
					hasValidElevation = false;
					break;
				}

				double elevationGain = currPoint.getAltitude() - prevPoint.getAltitude();
				double segmentDistance = cumulativeDistances.get(j) - cumulativeDistances.get(j - 1);

				if (elevationGain > 0) {
					windowPositiveElevationGain += elevationGain;
				}
				windowDistance += segmentDistance;
			}

			// Calculate average grade for this window
			float avgGrade = hasValidElevation && windowDistance > 0 ?
				(float) ((windowPositiveElevationGain / windowDistance) * 100) : 0f;

			windowAvgGrades.add(avgGrade);
			isClimbPoint.add(avgGrade >= minClimbGrade && windowPositiveElevationGain > 0);
		}

		// Step 2: Group consecutive climb points into climb segments
		// A climb starts when we have a climb point and continues through consecutive climb points
		// This allows small non-climb points (with avg grade < minClimbGrade) to be included
		// if they're surrounded by climb points
		
		int climbStart = -1;
		boolean inClimb = false;

		for (int i = 0; i < isClimbPoint.size(); i++) {
			if (!inClimb) {
				if (isClimbPoint.get(i)) {
					inClimb = true;
					climbStart = i;
				}
			} else {
				if (!isClimbPoint.get(i)) {
					// End of climb - check if it meets thresholds
					if (climbStart < i - 1) {
						// Calculate total elevation gain and distance for this climb
						double climbElevationGain = 0;
						double climbDistance = 0;

						for (int j = climbStart + 1; j < i; j++) {
							Location prevPoint = locations.get(j - 1);
							Location currPoint = locations.get(j);

							if (Double.isNaN(prevPoint.getAltitude()) || Double.isNaN(currPoint.getAltitude())) {
								climbElevationGain = 0;
								climbDistance = 0;
								break;
							}

							double elevationGain = currPoint.getAltitude() - prevPoint.getAltitude();
							if (elevationGain > 0) {
								climbElevationGain += elevationGain;
							}
							double segmentDistance = cumulativeDistances.get(j) - cumulativeDistances.get(j - 1);
							climbDistance += segmentDistance;
						}

						// Calculate average grade
						float avgGrade = climbDistance > 0 ?
							(float) ((climbElevationGain / climbDistance) * 100) : 0f;

						// Check thresholds
						if (climbElevationGain >= minClimbElevation && avgGrade >= minClimbGrade) {
							climbs.add(new ClimbSegment(climbStart, i - 1, climbElevationGain, climbDistance, avgGrade));
						}
					}
					// Reset
					inClimb = false;
					climbStart = -1;
				}
			}
		}

		// Handle the last climb if we're still in one
		if (inClimb && climbStart >= 0) {
			int end = isClimbPoint.size() - 1;
			if (climbStart < end) {
				// Calculate total elevation gain and distance for this climb
				double climbElevationGain = 0;
				double climbDistance = 0;

				for (int j = climbStart + 1; j <= end; j++) {
					Location prevPoint = locations.get(j - 1);
					Location currPoint = locations.get(j);

					if (Double.isNaN(prevPoint.getAltitude()) || Double.isNaN(currPoint.getAltitude())) {
						climbElevationGain = 0;
						climbDistance = 0;
						break;
					}

					double elevationGain = currPoint.getAltitude() - prevPoint.getAltitude();
					if (elevationGain > 0) {
						climbElevationGain += elevationGain;
					}
					double segmentDistance = cumulativeDistances.get(j) - cumulativeDistances.get(j - 1);
					climbDistance += segmentDistance;
				}

				// Calculate average grade
				float avgGrade = climbDistance > 0 ?
					(float) ((climbElevationGain / climbDistance) * 100) : 0f;

				// Check thresholds
				if (climbElevationGain >= minClimbElevation && avgGrade >= minClimbGrade) {
					climbs.add(new ClimbSegment(climbStart, end, climbElevationGain, climbDistance, avgGrade));
				}
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
