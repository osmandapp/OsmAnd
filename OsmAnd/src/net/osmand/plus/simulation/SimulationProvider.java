package net.osmand.plus.simulation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadPointDouble;
import net.osmand.plus.routing.RouteSegmentSearchResult;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SimulationProvider {
	public static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(SimulationProvider.class);

	public static final String SIMULATED_PROVIDER = "OsmAnd";
	public static final String SIMULATED_PROVIDER_GPX = "GPX";
	public static final String SIMULATED_PROVIDER_TUNNEL = "TUNNEL";
	private static final float MAX_SPEED_TUNNELS = 27.0f; // 27 m/s, 97.2 kmh, 60.4 mph

	private final Location startLocation;
	private final List<RouteSegmentResult> roads;

	private float minOfMaxSpeedInTunnel;

	private int currentRoad = -1;
	private int currentSegment;
	private QuadPointDouble currentPoint;

	public SimulationProvider(@NonNull Location location, @NonNull List<RouteSegmentResult> roads) {
		this.startLocation = new Location(location);
		this.roads = new ArrayList<>(roads);
		long time = System.currentTimeMillis();
		if (time - startLocation.getTime() > 5000 || time < startLocation.getTime()) {
			startLocation.setTime(time);
		}
		minOfMaxSpeedInTunnel = MAX_SPEED_TUNNELS;
		for (RouteSegmentResult r : roads) {
			float tunnelSpeed = r.getObject().getMaximumSpeed(r.isForwardDirection());
			if (tunnelSpeed > 0) {
				minOfMaxSpeedInTunnel = Math.min(minOfMaxSpeedInTunnel, tunnelSpeed);
			}
		}
		RouteSegmentSearchResult searchResult = RouteSegmentSearchResult.searchRouteSegment(
				startLocation.getLatitude(), startLocation.getLongitude(), -1, roads);
		if (searchResult != null) {
			currentRoad = searchResult.getRoadIndex();
			currentSegment = searchResult.getSegmentIndex();
			currentPoint = searchResult.getPoint();
		} else {
			currentRoad = -1;
		}
		LOG.info(String.format(Locale.US, "Start simulation road %d, segment %d - time %d, location %s",
				currentRoad, currentSegment, startLocation.getTime(), startLocation));
	}


	private double proceedMetersFromStartLocation(double meters, Location location) {
		// Location tried to be precise, but can be overshot for last segment
		// return how many meters overshot for the last point
		if (currentRoad == -1) {
			return -1;
		}
		for (int i = currentRoad; i < roads.size(); i++) {
			RouteSegmentResult road = roads.get(i);
			boolean firstRoad = i == currentRoad;
			boolean plus = road.getStartPointIndex() < road.getEndPointIndex();
			int increment = plus ? +1 : -1;
			int start = road.getStartPointIndex();
			if (firstRoad) {
				// first segment is [currentSegment - 1, currentSegment]
				if (plus) {
					start = currentSegment - increment;
				} else {
					start = currentSegment;
				}
			}
			for (int j = start; j != road.getEndPointIndex(); j += increment) {
				RouteDataObject obj = road.getObject();
				int st31x = obj.getPoint31XTile(j);
				int st31y = obj.getPoint31YTile(j);
				int end31x = obj.getPoint31XTile(j + increment);
				int end31y = obj.getPoint31YTile(j + increment);
				boolean last = i == roads.size() - 1 && j == road.getEndPointIndex() - increment;
				boolean first = firstRoad && j == start;
				if (first) {
					st31x = (int) currentPoint.x;
					st31y = (int) currentPoint.y;
				}
				double dd = MapUtils.measuredDist31(st31x, st31y, end31x, end31y);
				if (meters > dd && !last) {
					meters -= dd;
				} else if (dd > 0) {
					int prx = (int) (st31x + (end31x - st31x) * (meters / dd));
					int pry = (int) (st31y + (end31y - st31y) * (meters / dd));
					if (prx == 0 || pry == 0) {
						LOG.error(String.format(Locale.US, "proceedMeters zero x or y (%d,%d) (%s)", prx, pry, road));
						return -1;
					}
					location.setLongitude(MapUtils.get31LongitudeX(prx));
					location.setLatitude(MapUtils.get31LatitudeY(pry));
					return Math.max(meters - dd, 0);
				} else {
					LOG.error(String.format(Locale.US,
							"proceedMeters break at the end of the road (sx=%d, sy=%d) (%s)", st31x, st31y, road));
					break;
				}
			}
		}
		return -1;
	}

	/**
	 * @return null if it is not available of far from boundaries
	 */
	@Nullable
	public Location getSimulatedLocationForTunnel() {
		if (!isSimulatedDataAvailable()) {
			return null;
		}
		Location location = new Location(SIMULATED_PROVIDER_TUNNEL);
		float spd = Math.min(minOfMaxSpeedInTunnel, startLocation.getSpeed());

		location.setSpeed(spd);
		location.setAltitude(startLocation.getAltitude());
		location.setTime(System.currentTimeMillis());
		// here we can decrease speed - startLocation.getSpeed() or we can real speed BLE sensor
		double metersToPass = spd * ((System.currentTimeMillis() - startLocation.getTime()) / 1000.0);
		double metersSimLocationFromDesiredLocation = proceedMetersFromStartLocation(metersToPass, location);
		if (metersSimLocationFromDesiredLocation < 0) { // error simulation
			return null;
		}
		// limit 100m if we overpass tunnel
		if (metersSimLocationFromDesiredLocation >= 100) {
			return null;
		}
		return location;
	}

	public boolean isSimulatedDataAvailable() {
		return startLocation.getSpeed() > 0 && currentRoad >= 0;
	}

	public static boolean isNotSimulatedLocation(@Nullable Location location) {
		if (location != null) {
			return !(SIMULATED_PROVIDER.equals(location.getProvider())
					|| SIMULATED_PROVIDER_GPX.equals(location.getProvider())
					|| SIMULATED_PROVIDER_TUNNEL.equals(location.getProvider()));
		}
		return true;
	}

	public static boolean isTunnelLocationSimulated(@Nullable Location location) {
		return location != null && SIMULATED_PROVIDER_TUNNEL.equals(location.getProvider());
	}
}
