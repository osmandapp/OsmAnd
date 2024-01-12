package net.osmand.plus.simulation;


import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.simulation.LoadSimulatedLocationsTask.LoadSimulatedLocationsListener;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

public class OsmAndLocationSimulation {

	private final OsmandApplication app;

	private Thread simulationThread;

	private LoadSimulatedLocationsTask loadLocationsTask;
	private List<LoadSimulatedLocationsListener> loadLocationsListeners = new ArrayList<>();

	@Nullable
	private GPXFile gpxFile = null;
	private List<LocationSimulationListener> listeners = new ArrayList<>();

	public OsmAndLocationSimulation(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public boolean isRouteAnimating() {
		return simulationThread != null;
	}

	public boolean isLoadingRouteLocations() {
		return loadLocationsTask != null && loadLocationsTask.getStatus() == AsyncTask.Status.RUNNING;
	}

	public void addListener(@NonNull LoadSimulatedLocationsListener listener) {
		loadLocationsListeners = CollectionUtils.addToList(loadLocationsListeners, listener);
	}

	public void removeListener(@NonNull LoadSimulatedLocationsListener listener) {
		loadLocationsListeners = CollectionUtils.removeFromList(loadLocationsListeners, listener);
	}

	@Nullable
	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public void addSimulationListener(@NonNull LocationSimulationListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeSimulationListener(@NonNull LocationSimulationListener listener) {
		listeners.remove(listener);
	}

	private void notifyListeners(boolean simulating) {
		for (LocationSimulationListener listener : listeners) {
			listener.onSimulationStateChanged(simulating);
		}
	}

	public void startStopRouteAnimation(@Nullable FragmentActivity activity, boolean useGpx, @Nullable Runnable runnable) {
		if (!isRouteAnimating()) {
			if (useGpx) {
				if (activity == null) {
					stop();
					if (runnable != null) {
						runnable.run();
					}
				} else {
					SimulateRouteDialog.showInstance(activity, this, runnable);
				}
			} else {
				stopLoadLocationsTask();
				startLoadLocationsTask(runnable);
			}
		} else {
			stop();
			if (runnable != null) {
				runnable.run();
			}
		}
	}

	@NonNull
	private LoadSimulatedLocationsListener getLoadLocationsListener(@Nullable Runnable runnable) {
		return new LoadSimulatedLocationsListener() {
			@Override
			public void onLocationsStartedLoading() {
				for (LoadSimulatedLocationsListener listener : loadLocationsListeners) {
					listener.onLocationsStartedLoading();
				}
			}

			@Override
			public void onLocationsLoadingProgress(int progress) {
				for (LoadSimulatedLocationsListener listener : loadLocationsListeners) {
					listener.onLocationsLoadingProgress(progress);
				}
			}

			@Override
			public void onLocationsLoaded(@Nullable List<SimulatedLocation> currentRoute) {
				loadLocationsTask = null;
				notifyLocationsLoaded(currentRoute);

				if (Algorithms.isEmpty(currentRoute)) {
					app.showToastMessage(R.string.animate_routing_route_not_calculated);
				} else {
					startSimulationThread(app, new ArrayList<>(currentRoute), false, 1);
					if (runnable != null) {
						runnable.run();
					}
				}
			}
		};
	}

	private void notifyLocationsLoaded(@Nullable List<SimulatedLocation> locations) {
		for (LoadSimulatedLocationsListener listener : loadLocationsListeners) {
			listener.onLocationsLoaded(locations);
		}
	}

	private void startLoadLocationsTask(@Nullable Runnable runnable) {
		loadLocationsTask = new LoadSimulatedLocationsTask(app.getRoutingHelper().getRoute(), getLoadLocationsListener(runnable));
		loadLocationsTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	private void stopLoadLocationsTask() {
		if (loadLocationsTask != null && loadLocationsTask.getStatus() == AsyncTask.Status.RUNNING) {
			loadLocationsTask.cancel(false);
		}
		loadLocationsTask = null;
	}

	public void startStopRouteAnimation(@Nullable FragmentActivity activity) {
		startStopRouteAnimation(activity, false, null);
	}

	public void startStopGpxAnimation(@Nullable FragmentActivity activity) {
		startStopRouteAnimation(activity, true, null);
	}

	public void startSimulationThread(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
	                                  int firstLocationOffset, boolean useLocationTime, float coeff) {
		this.gpxFile = gpxFile;
		List<SimulatedLocation> locations = LocationSimulationUtils.getSimulatedLocationsForGpx(gpxFile, firstLocationOffset);
		startSimulationThread(app, locations, useLocationTime, coeff);
	}

	public void startSimulationThread(@NonNull OsmandApplication app, @NonNull List<SimulatedLocation> directions,
	                                  boolean useLocationTime, float coeff) {
		simulationThread = new LocationSimulationThread(app, directions, coeff, useLocationTime);
		notifyListeners(true);
		simulationThread.start();
	}

	public void stop() {
		gpxFile = null;
		simulationThread = null;
		stopLoadLocationsTask();
		notifyListeners(false);
	}

	public interface LocationSimulationListener {
		void onSimulationStateChanged(boolean simulating);
	}
}
