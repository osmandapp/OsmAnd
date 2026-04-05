package net.osmand.plus.simulation;

import static net.osmand.plus.simulation.SimulationProvider.SIMULATED_PROVIDER;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.base.ProgressHelper;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.router.RouteSegmentResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LoadSimulatedLocationsTask extends AsyncTask<Void, Integer, List<SimulatedLocation>> {

	private final RouteCalculationResult route;
	private final LoadSimulatedLocationsListener listener;

	LoadSimulatedLocationsTask(@NonNull RouteCalculationResult route, @Nullable LoadSimulatedLocationsListener listener) {
		this.route = route;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.onLocationsStartedLoading();
		}
	}

	@Override
	protected List<SimulatedLocation> doInBackground(Void... voids) {
		return getSimulatedLocationsForRoute();
	}

	@NonNull
	private List<SimulatedLocation> getSimulatedLocationsForRoute() {
		List<SimulatedLocation> locations = new ArrayList<>();
		prepareImmutableLocations(locations);

		Map<LatLon, SimulatedLocation> locationMap = new HashMap<>(locations.size());
		for (SimulatedLocation loc : locations) {
			LatLon key = new LatLon(loc.getLatitude(), loc.getLongitude());
			locationMap.put(key, loc);
		}

		List<RouteSegmentResult> segments = route.getImmutableAllSegments();
		int segmentsSize = segments.size();
		
		for (int routeInd = 0; routeInd < segmentsSize; routeInd++) {
			if (isCancelled()) {
				break;
			} else {
				int progress = ProgressHelper.normalizeProgressPercent((routeInd * 100) / segmentsSize);
				publishProgress(progress);

				RouteSegmentResult segment = segments.get(routeInd);
				prepareRouteSegmentResult(locations, segment, routeInd, segmentsSize);
			}
		}
		return locations;
	}

	private void prepareRouteSegmentResult(@NonNull Map<LatLon, SimulatedLocation> locationMap, @NonNull RouteSegmentResult segmentResult, int routeInd, int segmentsSize) {
		boolean plus = segmentResult.getStartPointIndex() < segmentResult.getEndPointIndex();
		int startPointIndex = segmentResult.getStartPointIndex();
		int endPointIndex = segmentResult.getEndPointIndex();

		while (startPointIndex != endPointIndex || routeInd == segmentsSize - 1) {
			LatLon point = segmentResult.getPoint(startPointIndex);
			SimulatedLocation location = locationMap.get(point);
			if (location != null) {
				location.setHighwayType(segmentResult.getObject().getHighway());
				location.setSpeedLimit(segmentResult.getObject().getMaximumSpeed(true));
				if (segmentResult.getObject().hasTrafficLightAt(startPointIndex)) {
					location.setTrafficLight(true);
				}
			}
			if (startPointIndex == endPointIndex) {
				break;
			}
			startPointIndex += plus ? 1 : -1;
		}
	}

	private void prepareImmutableLocations(@NonNull List<SimulatedLocation> locations) {
		for (Location location : route.getImmutableAllLocations()) {
			locations.add(new SimulatedLocation(location, SIMULATED_PROVIDER));
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		Integer progress = values[0];
		if (listener != null) {
			listener.onLocationsLoadingProgress(progress);
		}
	}

	@Override
	protected void onCancelled() {
		onPostExecute(null);
	}

	@Override
	protected void onPostExecute(List<SimulatedLocation> simulatedLocations) {
		if (listener != null) {
			listener.onLocationsLoaded(simulatedLocations);
		}
	}

	public interface LoadSimulatedLocationsListener {
		void onLocationsStartedLoading();

		void onLocationsLoadingProgress(int progress);

		void onLocationsLoaded(@Nullable List<SimulatedLocation> locations);
	}
}
