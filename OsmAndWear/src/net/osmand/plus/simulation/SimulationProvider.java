package net.osmand.plus.simulation;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.binary.RouteDataObject;
import net.osmand.data.QuadPoint;
import net.osmand.data.QuadPointDouble;
import net.osmand.plus.routing.RouteSegmentSearchResult;
import net.osmand.router.RouteSegmentResult;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class SimulationProvider {

	public static final String SIMULATED_PROVIDER = "OsmAnd";
	public static final String SIMULATED_PROVIDER_GPX = "GPX";

	private final Location startLocation;
	private final List<RouteSegmentResult> roads;

	private int currentRoad;
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

	private float proceedMeters(float meters, Location location) {
		for (int i = currentRoad; i < roads.size(); i++) {
			RouteSegmentResult road = roads.get(i);
			boolean firstRoad = i == currentRoad;
			boolean plus = road.getStartPointIndex() < road.getEndPointIndex();
			for (int j = firstRoad ? currentSegment : road.getStartPointIndex() + 1; j <= road.getEndPointIndex(); ) {
				RouteDataObject obj = road.getObject();
				int st31x = obj.getPoint31XTile(j - 1);
				int st31y = obj.getPoint31YTile(j - 1);
				int end31x = obj.getPoint31XTile(j);
				int end31y = obj.getPoint31YTile(j);
				boolean last = i == roads.size() - 1 && j == road.getEndPointIndex();
				boolean first = firstRoad && j == currentSegment;
				if (first) {
					st31x = (int) currentPoint.x;
					st31y = (int) currentPoint.y;
				}
				double dd = MapUtils.measuredDist31(st31x, st31y, end31x, end31y);
				if (meters > dd && !last) {
					meters -= dd;
				} else {
					int prx = (int) (st31x + (end31x - st31x) * (meters / dd));
					int pry = (int) (st31y + (end31y - st31y) * (meters / dd));
					location.setLongitude(MapUtils.get31LongitudeX(prx));
					location.setLatitude(MapUtils.get31LatitudeY(pry));
					return (float) Math.max(meters - dd, 0);
				}
				j += plus ? 1 : -1;
			}
		}
		return -1;
	}

	/**
	 * @return null if it is not available of far from boundaries
	 */
	@Nullable
	public Location getSimulatedLocation() {
		if (!isSimulatedDataAvailable()) {
			return null;
		}

		Location location = new Location(SIMULATED_PROVIDER);
		location.setSpeed(startLocation.getSpeed());
		location.setAltitude(startLocation.getAltitude());
		location.setTime(System.currentTimeMillis());
		float meters = startLocation.getSpeed() * ((System.currentTimeMillis() - startLocation.getTime()) / 1000);
		float proc = proceedMeters(meters, location);
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
			return !SIMULATED_PROVIDER.equals(location.getProvider());
		}
		return true;
	}
}
