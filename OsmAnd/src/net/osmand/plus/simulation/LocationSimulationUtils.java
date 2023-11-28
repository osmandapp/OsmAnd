package net.osmand.plus.simulation;

import static net.osmand.plus.SimulationProvider.SIMULATED_PROVIDER_GPX;

import androidx.annotation.NonNull;

import net.osmand.Location;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class LocationSimulationUtils {

	private static final float MOTORWAY_MAX_SPEED = 120.0f;
	private static final float TRUNK_MAX_SPEED = 90.0f;
	private static final float PRIMARY_MAX_SPEED = 60.0f;
	private static final float SECONDARY_MAX_SPEED = 50.0f;
	private static final float LIVING_STREET_MAX_SPEED = 15.0f;
	private static final float DEFAULT_MAX_SPEED = 40.0f;

	@NonNull
	protected static List<Object> useSimulationConstantSpeed(@NonNull SimulatedLocation current,
	                                                         @NonNull List<SimulatedLocation> directions,
	                                                         float speed, float meters,
	                                                         float intervalTime, float coeff) {
		List<Object> result = new ArrayList<>();
		if (current.distanceTo(directions.get(0)) > meters) {
			current = middleLocation(current, directions.get(0), meters);
		} else {
			current = new SimulatedLocation(directions.remove(0));
		}
		meters = speed * intervalTime * coeff;

		result.add(current);
		result.add(meters);

		return result;
	}

	@NonNull
	protected static List<Object> useDefaultSimulation(@NonNull SimulatedLocation current,
	                                                   @NonNull List<SimulatedLocation> directions,
	                                                   float meters, float intervalTime,
	                                                   float coeff, boolean realistic) {
		List<Object> result = new ArrayList<>();
		if (current.distanceTo(directions.get(0)) > meters) {
			current = middleLocation(current, directions.get(0), meters);
		} else {
			current = new SimulatedLocation(directions.remove(0));
			meters = metersToGoInFiveSteps(directions, current);
		}

		if (realistic) {
			float limit = getMetersLimitForPoint(current, intervalTime, coeff);
			if (meters > limit) {
				meters = limit;
			}
		}

		result.add(current);
		result.add(meters);

		return result;
	}

	@NonNull
	protected static SimulatedLocation middleLocation(@NonNull SimulatedLocation start, @NonNull SimulatedLocation end, float meters) {
		double lat1 = toRad(start.getLatitude());
		double lon1 = toRad(start.getLongitude());
		double R = 6371; // radius of earth in km
		double d = meters / 1000; // in km
		float bearing = (float) (toRad(start.bearingTo(end)));
		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R)
				+ Math.cos(lat1) * Math.sin(d / R) * Math.cos(bearing));
		double lon2 = lon1
				+ Math.atan2(Math.sin(bearing) * Math.sin(d / R) * Math.cos(lat1),
				Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));
		SimulatedLocation location = new SimulatedLocation(start);
		location.setLatitude(toDegree(lat2));
		location.setLongitude(toDegree(lon2));
		location.setBearing(bearing);
		location.setTrafficLight(false);
		return location;
	}

	protected static double toDegree(double radians) {
		return radians * 180 / Math.PI;
	}

	protected static double toRad(double degree) {
		return degree * Math.PI / 180;
	}

	@NonNull
	protected static List<SimulatedLocation> getSimulatedLocationsForGpx(@NonNull GPXFile gpxFile, int firstLocationOffset) {
		double distanceFromStart = 0;
		List<SimulatedLocation> locations = new ArrayList<>();
		List<WptPt> points = gpxFile.getAllSegmentsPoints();
		WptPt prevLocation = null;
		for (int i = 0; i < points.size(); i++) {
			WptPt location = points.get(i);
			if (prevLocation != null) {
				distanceFromStart += MapUtils.getDistance(prevLocation.getLatitude(),
						prevLocation.getLongitude(), location.getLatitude(), location.getLongitude());
			}
			if (distanceFromStart >= firstLocationOffset) {
				Location l = new Location(SIMULATED_PROVIDER_GPX, location.lat, location.lon);
				if (location.time > 0) {
					l.setTime(location.time);
				}
				if (location.speed > 0) {
					l.setSpeed((float) location.speed);
				} else {
					String sp = location.getExtensionsToRead().get("speed");
					if (!Algorithms.isEmpty(sp)) {
						l.setSpeed((float) Algorithms.parseDoubleSilently(sp, 0));
					}
				}
				if (!Double.isNaN(location.hdop)) {
					l.setAccuracy((float) location.hdop);
				}
				String br = location.getExtensionsToRead().get("bearing");
				if (!Algorithms.isEmpty(br)) {
					l.setBearing((float) Algorithms.parseDoubleSilently(br, 0));
				}
				if (!Double.isNaN(location.ele)) {
					l.setAltitude(location.ele);
				}
				locations.add(new SimulatedLocation(l, SIMULATED_PROVIDER_GPX));
			}
			prevLocation = location;
		}
		return locations;
	}

	protected static float metersToGoInFiveSteps(@NonNull List<SimulatedLocation> directions, @NonNull SimulatedLocation current) {
		return directions.isEmpty() ? 20.0f : Math.max(20.0f, current.distanceTo(directions.get(0)) / 2);
	}

	protected static float getMetersLimitForPoint(@NonNull SimulatedLocation point, float intervalTime, float coeff) {
		float maxSpeed = (float) (getMaxSpeedForRoadType(point.getHighwayType()) / 3.6);
		float speedLimit = point.getSpeedLimit();
		if (speedLimit > 0 && maxSpeed > speedLimit) {
			maxSpeed = speedLimit;
		}
		return maxSpeed * intervalTime / coeff;
	}

	protected static float getMaxSpeedForRoadType(@NonNull String roadType) {
		switch (roadType) {
			case "motorway":
				return MOTORWAY_MAX_SPEED;
			case "trunk":
				return TRUNK_MAX_SPEED;
			case "primary":
				return PRIMARY_MAX_SPEED;
			case "secondary":
				return SECONDARY_MAX_SPEED;
			case "living_street":
			case "service":
				return LIVING_STREET_MAX_SPEED;
			default:
				return DEFAULT_MAX_SPEED;
		}
	}
}
