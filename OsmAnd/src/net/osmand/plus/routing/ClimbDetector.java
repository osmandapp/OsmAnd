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
	private static final float DESCENT_TOLERANCE = 3.0f; // percentage - allow descents up to this slope within a climb
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
	 * Detect climbs in the given list of locations using HYBRID approach.
	 * 
	 * HYBRID ALGORITHM (2026-11-15 v2):
	 * Based on user feedback: "kijk naar de route. detecteer waar er hoogteverschil wordt gemaakt
	 * en bepaal op basis van de instellingen wanneer dat zo is."
	 * 
	 * This approach:
	 * 1. Identifies all segments with positive elevation gain (climbing segments)
	 * 2. Groups consecutive climbing segments into a single climb
	 * 3. Allows small descents (slope > -minClimbGrade) and flat segments within a climb
	 * 4. Ends a climb when a significant descent is encountered (slope <= -minClimbGrade)
	 * 5. For each climb, calculates:
	 *    - Total elevation gain: sum of all positive elevation changes
	 *    - Total distance: sum of all segment distances (including descents and flats within the climb)
	 *    - Average grade: (totalElevationGain / totalDistance) * 100
	 * 6. Only counts climbs where total elevation gain >= minClimbElevation AND average grade >= minClimbGrade
	 * 
	 * This allows detecting a 5m climb with small undulations as a single climb,
	 * while still properly separating distinct climbs.
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

		// Pre-calculate segment slopes and distances
		List<Float> segmentSlopes = new ArrayList<>();
		List<Double> segmentDistances = new ArrayList<>();
		List<Double> segmentElevationGains = new ArrayList<>();
		
		for (int i = 1; i < locations.size(); i++) {
			Location prevPoint = locations.get(i - 1);
			Location currPoint = locations.get(i);
			
			// Skip if no valid elevation data
			if (Double.isNaN(prevPoint.getAltitude()) || Double.isNaN(currPoint.getAltitude())) {
				segmentSlopes.add(0f);
				segmentDistances.add(0.0);
				segmentElevationGains.add(0.0);
				continue;
			}
			
			double elevationGain = currPoint.getAltitude() - prevPoint.getAltitude();
			double distance = MapUtils.getDistance(
					prevPoint.getLatitude(), prevPoint.getLongitude(),
					currPoint.getLatitude(), currPoint.getLongitude());
			
			// Calculate slope as percentage: (elevationGain / distance) * 100
			float slope = distance > 0 ? (float) ((elevationGain / distance) * 100) : 0f;
			
			segmentSlopes.add(slope);
			segmentDistances.add(distance);
			segmentElevationGains.add(elevationGain);
		}

		// Now detect climbs based on grouping segments
		// A climb starts with a positive slope segment and continues through:
		// - Positive slope segments
			// - Flat segments (slope close to 0)
			// - Small negative slope segments (downhill but not too steep)
		// A climb ends when we hit a significant downhill (slope <= -minClimbGrade)
		
		int climbStartSegment = -1;
		boolean inClimb = false;
		double currentClimbElevationGain = 0;
		double currentClimbDistance = 0;

		for (int i = 0; i < segmentSlopes.size(); i++) {
			float slope = segmentSlopes.get(i);
			double elevationGain = segmentElevationGains.get(i);
			double distance = segmentDistances.get(i);

			if (!inClimb) {
				// Not in a climb, check if we should start one
				// Start a climb when we have a positive slope that meets the grade threshold
				if (slope >= minClimbGrade && elevationGain > 0) {
					inClimb = true;
					climbStartSegment = i;
					currentClimbElevationGain = elevationGain;
					currentClimbDistance = distance;
				}
			} else {
				// We're in a climb
				// Check if we should continue or end the climb
				// Continue if: slope > -DESCENT_TOLERANCE (allows descents up to DESCENT_TOLERANCE)
				// End if: slope <= -DESCENT_TOLERANCE (significant downhill)
				
				if (slope > -DESCENT_TOLERANCE) {
					// Continue the climb (includes positive, flat, and small negative slopes)
					if (elevationGain > 0) {
						currentClimbElevationGain += elevationGain;
					}
					currentClimbDistance += distance;
				} else {
					// Significant downhill - end the climb
					if (currentClimbElevationGain >= minClimbElevation) {
						float avgGrade = currentClimbDistance > 0 ? 
							(float) ((currentClimbElevationGain / currentClimbDistance) * 100) : 0f;
						if (avgGrade >= minClimbGrade) {
							// Climb goes from location climbStartSegment to location i
							climbs.add(new ClimbSegment(climbStartSegment, i, currentClimbElevationGain, currentClimbDistance, avgGrade));
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
				// Last segment is locations.size() - 1
				climbs.add(new ClimbSegment(climbStartSegment, locations.size() - 1, currentClimbElevationGain, currentClimbDistance, avgGrade));
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
