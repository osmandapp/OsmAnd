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
	private static final double WINDOW_SIZE = 200.0; // meters - rolling window size for segment-based detection
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
	 * Detect climbs in the given list of locations using ROLLING WINDOW approach.
	 * 
	 * ROLLING WINDOW ALGORITHM (2026-11-15 v3):
	 * Based on user suggestion: use a rolling window of 200m to calculate average grade
	 * 
	 * Steps:
	 * 1. Create fixed-size windows of WINDOW_SIZE (200m) along the route
	 * 2. For each window, calculate:
	 *    - Total positive elevation gain
	 *    - Total distance (WINDOW_SIZE)
	 *    - Average grade = (positiveElevationGain / WINDOW_SIZE) * 100
	 * 3. A window is a "climb window" if its average grade >= minClimbGrade
	 * 4. Group consecutive climb windows into a single climb
	 * 5. For each climb, calculate:
	 *    - Total elevation gain: sum of all positive elevation changes in all windows
	 *    - Total distance: sum of all window distances
	 *    - Average grade: (totalElevationGain / totalDistance) * 100
	 * 6. Only count climbs where total elevation gain >= minClimbElevation AND average grade >= minClimbGrade
	 * 
	 * This approach provides more stable climb detection by averaging over fixed distances,
	 * reducing the impact of small undulations and noise in the elevation data.
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

		// Create windows of WINDOW_SIZE (200m)
		// Each window starts at a location and spans WINDOW_SIZE meters
		List<Boolean> isClimbWindow = new ArrayList<>();
		List<Double> windowElevationGains = new ArrayList<>();
		List<Double> windowStartDistances = new ArrayList<>();
		List<Double> windowEndDistances = new ArrayList<>();
		List<Integer> windowStartIndices = new ArrayList<>();
		List<Integer> windowEndIndices = new ArrayList<>();

		// Create windows
		double totalDistance = cumulativeDistances.get(cumulativeDistances.size() - 1);
		int numWindows = (int) Math.ceil(totalDistance / WINDOW_SIZE);
		
		for (int w = 0; w < numWindows; w++) {
			double windowStartDist = w * WINDOW_SIZE;
			double windowEndDist = Math.min((w + 1) * WINDOW_SIZE, totalDistance);
			
			// Find the indices for this window
			int startIdx = 0;
			int endIdx = locations.size() - 1;
			
			// Find first location >= windowStartDist
			for (int i = 0; i < cumulativeDistances.size(); i++) {
				if (cumulativeDistances.get(i) >= windowStartDist) {
					startIdx = i;
					break;
				}
			}
			
			// Find last location <= windowEndDist
			for (int i = cumulativeDistances.size() - 1; i >= 0; i--) {
				if (cumulativeDistances.get(i) <= windowEndDist) {
					endIdx = i;
					break;
				}
			}
			
			// If we have valid indices, calculate the elevation gain for this window
			if (startIdx < endIdx && startIdx >= 0 && endIdx < locations.size()) {
				double positiveElevationGain = 0;
				
				// Calculate elevation gain within this window
				for (int i = startIdx + 1; i <= endIdx; i++) {
					Location prevPoint = locations.get(i - 1);
					Location currPoint = locations.get(i);
					
					if (Double.isNaN(prevPoint.getAltitude()) || Double.isNaN(currPoint.getAltitude())) {
						continue;
					}
					
					double elevationGain = currPoint.getAltitude() - prevPoint.getAltitude();
					if (elevationGain > 0) {
						positiveElevationGain += elevationGain;
					}
				}
				
				double windowActualDistance = cumulativeDistances.get(endIdx) - cumulativeDistances.get(startIdx);
				
				// Calculate average grade for this window
				float avgGrade = windowActualDistance > 0 ? 
					(float) ((positiveElevationGain / windowActualDistance) * 100) : 0f;
				
				// Check if this is a climb window
				boolean climbWindow = avgGrade >= minClimbGrade && positiveElevationGain > 0;
				
				isClimbWindow.add(climbWindow);
				windowElevationGains.add(positiveElevationGain);
				windowStartDistances.add(windowStartDist);
				windowEndDistances.add(windowEndDist);
				windowStartIndices.add(startIdx);
				windowEndIndices.add(endIdx);
			} else {
				// Empty window, add default values
				isClimbWindow.add(false);
				windowElevationGains.add(0.0);
				windowStartDistances.add(windowStartDist);
				windowEndDistances.add(windowEndDist);
				windowStartIndices.add(startIdx);
				windowEndIndices.add(endIdx);
			}
		}

		// Now group consecutive climb windows into climbs
		int climbStartWindow = -1;
		boolean inClimb = false;
		double currentClimbElevationGain = 0;
		double currentClimbDistance = 0;
		int currentClimbStartIndex = -1;
		int currentClimbEndIndex = -1;

		for (int w = 0; w < isClimbWindow.size(); w++) {
			if (!inClimb) {
				if (isClimbWindow.get(w)) {
					inClimb = true;
					climbStartWindow = w;
					currentClimbElevationGain = windowElevationGains.get(w);
					currentClimbDistance = windowEndDistances.get(w) - windowStartDistances.get(w);
					currentClimbStartIndex = windowStartIndices.get(w);
					currentClimbEndIndex = windowEndIndices.get(w);
				}
			} else {
				if (isClimbWindow.get(w)) {
					// Continue the climb
					currentClimbElevationGain += windowElevationGains.get(w);
					currentClimbDistance += windowEndDistances.get(w) - windowStartDistances.get(w);
					currentClimbEndIndex = windowEndIndices.get(w);
				} else {
					// End of climb
					if (currentClimbElevationGain >= minClimbElevation) {
						float avgGrade = currentClimbDistance > 0 ? 
							(float) ((currentClimbElevationGain / currentClimbDistance) * 100) : 0f;
						if (avgGrade >= minClimbGrade) {
							climbs.add(new ClimbSegment(currentClimbStartIndex, currentClimbEndIndex, 
									currentClimbElevationGain, currentClimbDistance, avgGrade));
						}
					}
					// Reset
					inClimb = false;
					currentClimbElevationGain = 0;
					currentClimbDistance = 0;
				}
			}
		}

		// Handle the last climb if we're still in one
		if (inClimb && currentClimbElevationGain >= minClimbElevation) {
			float avgGrade = currentClimbDistance > 0 ? 
				(float) ((currentClimbElevationGain / currentClimbDistance) * 100) : 0f;
			if (avgGrade >= minClimbGrade) {
				climbs.add(new ClimbSegment(currentClimbStartIndex, currentClimbEndIndex, 
						currentClimbElevationGain, currentClimbDistance, avgGrade));
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
