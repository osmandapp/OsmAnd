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

		boolean inClimb = false;
		int climbStartIndex = 0;
		double currentClimbGain = 0;
		double peakElevation = 0;
		double distanceSincePeak = 0;
		double totalDistanceFromStart = 0;

		for (int i = 1; i < locations.size(); i++) {
			Location prevPoint = locations.get(i - 1);
			Location currPoint = locations.get(i);

			// Skip if no valid elevation data
			if (Double.isNaN(prevPoint.getAltitude()) || Double.isNaN(currPoint.getAltitude())) {
				if (inClimb && currentClimbGain >= minClimbElevation) {
					// Calculate grade for the climb
					double totalDistance = 0;
					for (int j = climbStartIndex + 1; j <= i - 1 && j < locations.size(); j++) {
						totalDistance += MapUtils.getDistance(
								locations.get(j-1).getLatitude(), locations.get(j-1).getLongitude(),
								locations.get(j).getLatitude(), locations.get(j).getLongitude());
					}
					float avgGrade = totalDistance > 0 ? (float) ((currentClimbGain / totalDistance) * 100) : 0;
					if (avgGrade >= minClimbGrade) {
						climbs.add(new ClimbSegment(climbStartIndex, i - 1, currentClimbGain, totalDistance, avgGrade));
					}
				}
				inClimb = false;
				continue;
			}

			double elevationGain = currPoint.getAltitude() - prevPoint.getAltitude();
			double segmentDistance = MapUtils.getDistance(prevPoint.getLatitude(), prevPoint.getLongitude(),
					currPoint.getLatitude(), currPoint.getLongitude());
			totalDistanceFromStart += segmentDistance;

			if (!inClimb) {
				if (elevationGain > 0) {
					inClimb = true;
					climbStartIndex = i - 1;
					currentClimbGain = elevationGain;
					peakElevation = currPoint.getAltitude();
					distanceSincePeak = 0;
				}
			} else {
				// Update peak elevation and distance since peak
				if (currPoint.getAltitude() > peakElevation) {
					peakElevation = currPoint.getAltitude();
					distanceSincePeak = 0;
				} else {
					distanceSincePeak += segmentDistance;
				}

				if (elevationGain >= 0) {
					currentClimbGain += elevationGain;
				} else {
					// End climb if we've descended significantly from peak (100m descent OR 200m without ascent)
					if (distanceSincePeak > FLAT_DISTANCE_THRESHOLD || (peakElevation - currPoint.getAltitude()) > PEAK_DESCENT_THRESHOLD) {
						if (currentClimbGain >= minClimbElevation) {
							// Calculate grade
							double totalDistance = 0;
							for (int j = climbStartIndex + 1; j <= i; j++) {
								if (j < locations.size()) {
									totalDistance += MapUtils.getDistance(
											locations.get(j-1).getLatitude(), locations.get(j-1).getLongitude(),
											locations.get(j).getLatitude(), locations.get(j).getLongitude());
								}
							}
							float avgGrade = totalDistance > 0 ? (float) ((currentClimbGain / totalDistance) * 100) : 0;
							if (avgGrade >= minClimbGrade) {
								climbs.add(new ClimbSegment(climbStartIndex, i - 1, currentClimbGain, totalDistance, avgGrade));
							}
						}
						inClimb = false;
					}
				}
			}
		}

		// Handle the last climb
		if (inClimb && currentClimbGain >= minClimbElevation) {
			// Calculate grade for the last climb
			double totalDistance = 0;
			for (int j = climbStartIndex + 1; j < locations.size(); j++) {
				totalDistance += MapUtils.getDistance(
						locations.get(j-1).getLatitude(), locations.get(j-1).getLongitude(),
						locations.get(j).getLatitude(), locations.get(j).getLongitude());
			}
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
