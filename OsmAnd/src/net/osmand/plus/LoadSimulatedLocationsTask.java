package net.osmand.plus;

import static net.osmand.plus.OsmAndLocationSimulation.*;
import static net.osmand.plus.SimulationProvider.SIMULATED_PROVIDER;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.data.LatLon;
import net.osmand.plus.routing.RouteCalculationResult;
import net.osmand.router.RouteSegmentResult;

import java.util.ArrayList;
import java.util.List;

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
		List<SimulatedLocation> simulatedLocations = new ArrayList<>();
		for (Location l : route.getImmutableAllLocations()) {
			SimulatedLocation sm = new SimulatedLocation(l, SIMULATED_PROVIDER);
			simulatedLocations.add(sm);
		}
		List<RouteSegmentResult> segments = route.getImmutableAllSegments();

		for (int routeInd = 0; routeInd < segments.size(); routeInd++) {
			RouteSegmentResult s = segments.get(routeInd);
			boolean plus = s.getStartPointIndex() < s.getEndPointIndex();
			int i = s.getStartPointIndex();
			if (!isCancelled()) {
				int progress = ((routeInd + 1) * 100) / segments.size();
				publishProgress(progress);
			} else {
				break;
			}

			while (i != s.getEndPointIndex() || routeInd == segments.size() - 1) {
				LatLon point = s.getPoint(i);
				for (SimulatedLocation sd : simulatedLocations) {
					LatLon latLon = new LatLon(sd.getLatitude(), sd.getLongitude());
					if (latLon.equals(point)) {
						sd.setHighwayType(s.getObject().getHighway());
						sd.setSpeedLimit(s.getObject().getMaximumSpeed(true));
						if (s.getObject().hasTrafficLightAt(i)) {
							sd.setTrafficLight(true);
						}
					}
				}
				if (i == s.getEndPointIndex()) {
					break;
				}
				i += plus ? 1 : -1;
			}
		}
		return simulatedLocations;
	}

	@Override
	protected void onPostExecute(List<SimulatedLocation> simulatedLocations) {
		if (listener != null) {
			listener.onLocationsLoaded(simulatedLocations);
		}
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		Integer progress = values[0];

		if (listener != null) {
			listener.onLocationsLoading(progress);
		}
	}

	@Override
	protected void onCancelled() {
		if (listener != null) {
			listener.onLocationsLoaded(null);
		}
	}
}
