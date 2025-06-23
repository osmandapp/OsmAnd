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

	private final Location startLocation;
	private final List<RouteSegmentResult> roads;

	private int currentRoad = -1;
	private int currentSegment;
	private QuadPointDouble currentPoint;

	public SimulationProvider(@NonNull Location location, @NonNull List<RouteSegmentResult> roads) {
		this.startLocation = new Location(location);
		this.roads = new ArrayList<>(roads);
	}

	public void startSimulation() {
		long time = System.currentTimeMillis();
		if (time - startLocation.getTime() > 5000 || time < startLocation.getTime()) {
			startLocation.setTime(time);
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
	}

	private double proceedMeters(double meters, Location location) {
		if (currentRoad == -1) {
			return -1;
		}

		for (int i = currentRoad; i < roads.size(); i++) {
			RouteSegmentResult road = roads.get(i);
			boolean firstRoad = (i == currentRoad);
			int increment = road.getStartPointIndex() < road.getEndPointIndex() ? 1 : -1;

			int startIndex = firstRoad ? currentSegment : road.getStartPointIndex() + increment;
			int endIndex = road.getEndPointIndex();

			RouteDataObject obj = road.getObject();
			int lastIdx = startIndex - increment;

			for (int j = startIndex; increment > 0 ? j <= endIndex : j >= endIndex; j += increment) {
				int st31x = obj.getPoint31XTile(lastIdx);
				int st31y = obj.getPoint31YTile(lastIdx);
				int end31x = obj.getPoint31XTile(j);
				int end31y = obj.getPoint31YTile(j);
				lastIdx = j;

				boolean isLast = (i == roads.size() - 1) && (j == endIndex);
				boolean isFirst = firstRoad && (j == currentSegment);

				if (isFirst) {
					st31x = (int) currentPoint.x;
					st31y = (int) currentPoint.y;
				}

				double dist = MapUtils.measuredDist31(st31x, st31y, end31x, end31y);
				if (meters > dist && !isLast) {
					meters -= dist;
				} else if (dist > 0) {
					double ratio = meters / dist;
					int prx = st31x + (int) ((end31x - st31x) * ratio);
					int pry = st31y + (int) ((end31y - st31y) * ratio);

					if (prx == 0 || pry == 0) {
						LOG.error(String.format(Locale.US, "proceedMeters zero x or y (%d,%d) (%s)", prx, pry, road));
						return -1;
					}

					location.setLongitude(MapUtils.get31LongitudeX(prx));
					location.setLatitude(MapUtils.get31LatitudeY(pry));
					return Math.max(meters - dist, 0);
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
		location.setSpeed(startLocation.getSpeed());
		location.setAltitude(startLocation.getAltitude());
		location.setTime(System.currentTimeMillis());
		double meters = startLocation.getSpeed() * ((System.currentTimeMillis() - startLocation.getTime()) / 1000.0);
		double proc = proceedMeters(meters, location);
		if (proc < 0 || proc >= 100) {
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
