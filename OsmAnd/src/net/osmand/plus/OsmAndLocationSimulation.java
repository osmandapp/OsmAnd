package net.osmand.plus;


import static net.osmand.plus.SimulationProvider.SIMULATED_PROVIDER;
import static net.osmand.plus.SimulationProvider.SIMULATED_PROVIDER_GPX;

import android.app.Activity;
import android.os.AsyncTask;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import com.google.android.material.slider.Slider;

import net.osmand.Location;
import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.LoadSimulatedLocationsTask.LoadSimulatedLocationsListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.SimulationMode;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OsmAndLocationSimulation {

	public static final float PRECISION_1_M = 0.00001f;
	public static final float LOCATION_TIMEOUT = 1.5f;
	public static final int DEVIATION_M = 6;
	private final float MOTORWAY_MAX_SPEED = 120.0f;
	private final float TRUNK_MAX_SPEED = 90.0f;
	private final float PRIMARY_MAX_SPEED = 60.0f;
	private final float SECONDARY_MAX_SPEED = 50.0f;
	private final float LIVING_SPTREET_MAX_SPEED = 15.0f;
	private final float DEFAULT_MAX_SPEED = 40.0f;

	private final OsmandApplication app;
	private final OsmAndLocationProvider provider;

	private Thread routeAnimation;

	private LoadSimulatedLocationsTask loadLocationsTask;
	private List<LoadSimulatedLocationsListener> loadLocationsListeners = new ArrayList<>();

	@Nullable
	private GPXFile gpxFile = null;
	private List<LocationSimulationListener> listeners = new ArrayList<>();

	public OsmAndLocationSimulation(@NonNull OsmandApplication app, @NonNull OsmAndLocationProvider provider) {
		this.app = app;
		this.provider = provider;
	}

	public boolean isRouteAnimating() {
		return routeAnimation != null;
	}

	public boolean isLoadingRouteLocations() {
		return loadLocationsTask != null && loadLocationsTask.getStatus() == AsyncTask.Status.RUNNING;
	}

	public void addListener(@NonNull LoadSimulatedLocationsListener listener) {
		loadLocationsListeners = Algorithms.addToList(loadLocationsListeners, listener);
	}

	public void removeListener(@NonNull LoadSimulatedLocationsListener listener) {
		loadLocationsListeners = Algorithms.removeFromList(loadLocationsListeners, listener);
	}

	@Nullable
	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public void addSimulationListener(LocationSimulationListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	public void removeSimulationListener(LocationSimulationListener listener) {
		listeners.remove(listener);
	}

	private void notifyListeners(boolean simulating) {
		for (LocationSimulationListener listener : listeners) {
			listener.onSimulationStateChanged(simulating);
		}
	}

//	public void startStopRouteAnimationRoute(final MapActivity ma) {
//		if (!isRouteAnimating()) {
//			List<Location> currentRoute = app.getRoutingHelper().getCurrentRoute();
//			if (currentRoute.isEmpty()) {
//				Toast.makeText(app, R.string.animate_routing_route_not_calculated, Toast.LENGTH_LONG).show();
//			} else {
//				startAnimationThread(app.getRoutingHelper(), ma, new ArrayList<Location>(currentRoute), false, 1);
//			}
//		} else {
//			stop();
//		}
//	}

	public void startStopRouteAnimation(@Nullable Activity activity, boolean useGpx, Runnable runnable) {
		if (!isRouteAnimating()) {
			if (useGpx) {
				if (activity == null) {
					stop();
					if (runnable != null) {
						runnable.run();
					}
					return;
				}
				boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
				int themeRes = nightMode ? R.style.OsmandDarkTheme : R.style.OsmandLightTheme;
				ApplicationMode appMode = app.getSettings().getApplicationMode();
				int selectedModeColor = appMode.getProfileColor(nightMode);
				AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(activity, themeRes));
				builder.setTitle(R.string.animate_route);

				View view = activity.getLayoutInflater().inflate(R.layout.animate_route, null);
				((TextView) view.findViewById(R.id.MinSpeedup)).setText("1");
				((TextView) view.findViewById(R.id.MaxSpeedup)).setText("4");
				Slider speedup = view.findViewById(R.id.Speedup);
				speedup.setValueTo(3);
				UiUtilities.setupSlider(speedup, nightMode, selectedModeColor, true);
				builder.setView(view);
				builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
					boolean nightMode1 = app.getDaynightHelper().isNightMode(activity instanceof MapActivity);
					GpxUiHelper.selectGPXFile(activity, false, false, result -> {
						startAnimationThread(app, result[0], 0, true, speedup.getValue() + 1);
						if (runnable != null) {
							runnable.run();
						}
						return true;
					}, nightMode1);
				});
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.show();
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
					startAnimationThread(app, new ArrayList<>(currentRoute), false, 1);
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

	public void startStopRouteAnimation(@Nullable Activity activity) {
		startStopRouteAnimation(activity, false, null);
	}

	public void startStopGpxAnimation(@Nullable Activity activity) {
		startStopRouteAnimation(activity, true, null);
	}

	public void startAnimationThread(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
	                                 int firstLocationOffset, boolean locTime, float coeff) {
		this.gpxFile = gpxFile;
		List<SimulatedLocation> locations = getSimulatedLocationsForGpx(app, gpxFile, firstLocationOffset);
		startAnimationThread(app, locations, locTime, coeff);
	}

	public void startAnimationThread(@NonNull OsmandApplication app, @NonNull List<SimulatedLocation> directions,
	                                 boolean locTime, float coeff) {
		final float time = LOCATION_TIMEOUT;
		float simSpeed = app.getSettings().simulateNavigationSpeed;
		SimulationMode simulationMode = SimulationMode.getMode(app.getSettings().simulateNavigationMode);
		boolean realistic = simulationMode == SimulationMode.REALISTIC;
		routeAnimation = new Thread() {
			@Override
			public void run() {
				SimulatedLocation current = directions.isEmpty() ? null : new SimulatedLocation(directions.remove(0));
				boolean useLocationTime = locTime && current.getTime() != 0;
				SimulatedLocation prev = current;
				long prevTime = current == null ? 0 : current.getTime();
				float meters = metersToGoInFiveSteps(directions, current);
				if (current != null) {
					current.setProvider(SIMULATED_PROVIDER);
				}
				int stopDelayCount = 0;
				boolean bearingSimulation = true;
				boolean accuracySimulation = true;
				boolean speedSimulation = true;
				if (useLocationTime) {
					for (SimulatedLocation l : directions) {
						if (l.hasBearing()) {
							bearingSimulation = false;
						}
						if (l.hasAccuracy()) {
							accuracySimulation = false;
						}
						if (l.hasSpeed()) {
							speedSimulation = false;
						}
					}
				}

				while (!directions.isEmpty() && routeAnimation != null) {
					long timeout = (long) (time * 1000);
					float intervalTime = time;
					if (stopDelayCount == 0) {
						if (useLocationTime) {
							current = directions.remove(0);
							meters = current.distanceTo(prev);
							if (!directions.isEmpty()) {
								timeout = Math.abs((directions.get(0).getTime() - current.getTime()));
								intervalTime = Math.abs((current.getTime() - prevTime) / 1000f);
								prevTime = current.getTime();
							}
						} else {
							List<Object> result;
							if (simulationMode == SimulationMode.CONSTANT) {
								result = useSimulationConstantSpeed(simSpeed, current, directions, meters, intervalTime, coeff);
							} else {
								result = useDefaultSimulation(current, directions, meters, intervalTime, coeff, realistic);
							}
							current = (SimulatedLocation) result.get(0);
							meters = (float) result.get(1);
						}
						float speed = meters / intervalTime * coeff;
						if (intervalTime != 0) {
							current.setSpeed(speed);
						}
						if ((!current.hasAccuracy() || Double.isNaN(current.getAccuracy()) ||
								(realistic && speed < 10)) && accuracySimulation) {
							current.setAccuracy(5);
						}
						if (prev != null && bearingSimulation && prev.distanceTo(current) > 3
								&& (!realistic || speed >= 1)) {
							current.setBearing(prev.bearingTo(current));
						}
					}
					current.setTime(System.currentTimeMillis());
					Location toset = current;
					if (realistic) {
						addNoise(toset);
					}
					if (realistic && current.isTrafficLight() && stopDelayCount == 0) {
						stopDelayCount = 5;
						current.setSpeed(0);
						current.removeBearing();
					} else if (stopDelayCount > 0) {
						stopDelayCount--;
					}
					app.runInUIThread(() -> provider.setLocationFromSimulation(toset));
					try {
						Thread.sleep((long) (timeout / coeff));
					} catch (InterruptedException e) {
						// do nothing
					}
					prev = current;
				}
				OsmAndLocationSimulation.this.stop();
			}

			private void addNoise(Location location) {
				Random r = new Random();
				float d = (r.nextInt(DEVIATION_M + 1) - DEVIATION_M / 2) * PRECISION_1_M;
				location.setLatitude(location.getLatitude() + d);
				d = (r.nextInt(DEVIATION_M + 1) - DEVIATION_M / 2) * PRECISION_1_M;
				location.setLongitude(location.getLongitude() + d);
			}

		};
		notifyListeners(true);
		routeAnimation.start();
	}

	private List<Object> useSimulationConstantSpeed(float speed, SimulatedLocation current, List<SimulatedLocation> directions,
	                                                float meters, float intervalTime, float coeff) {
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

	private List<Object> useDefaultSimulation(SimulatedLocation current, List<SimulatedLocation> directions, float meters, float intervalTime, float coeff, boolean isRealistic) {
		List<Object> result = new ArrayList<>();
		if (current.distanceTo(directions.get(0)) > meters) {
			current = middleLocation(current, directions.get(0), meters);
		} else {
			current = new SimulatedLocation(directions.remove(0));
			meters = metersToGoInFiveSteps(directions, current);
		}

		if (isRealistic) {
			float limit = getMetersLimitForPoint(current, intervalTime, coeff);
			if (meters > limit) {
				meters = limit;
			}
		}

		result.add(current);
		result.add(meters);

		return result;
	}

	private float metersToGoInFiveSteps(List<SimulatedLocation> directions, SimulatedLocation current) {
		return directions.isEmpty() ? 20.0f : Math.max(20.0f, current.distanceTo(directions.get(0)) / 2);
	}

	private float getMetersLimitForPoint(SimulatedLocation point, float intervalTime, float coeff) {
		float maxSpeed = (float) (getMaxSpeedForRoadType(point.getHighwayType()) / 3.6);
		float speedLimit = point.getSpeedLimit();
		if (speedLimit > 0 && maxSpeed > speedLimit) {
			maxSpeed = speedLimit;
		}
		return maxSpeed * intervalTime / coeff;
	}

	private float getMaxSpeedForRoadType(String roadType) {
		if ("motorway".equals(roadType)) {
			return MOTORWAY_MAX_SPEED;
		} else if ("trunk".equals(roadType)) {
			return TRUNK_MAX_SPEED;
		} else if ("primary".equals(roadType)) {
			return PRIMARY_MAX_SPEED;
		} else if ("secondary".equals(roadType)) {
			return SECONDARY_MAX_SPEED;
		} else if ("living_street".equals(roadType) || "service".equals(roadType)) {
			return LIVING_SPTREET_MAX_SPEED;
		} else {
			return DEFAULT_MAX_SPEED;
		}
	}

	public void stop() {
		gpxFile = null;
		routeAnimation = null;
		stopLoadLocationsTask();
		notifyListeners(false);
	}

	public static SimulatedLocation middleLocation(SimulatedLocation start, SimulatedLocation end, float meters) {
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
		SimulatedLocation nl = new SimulatedLocation(start);
		nl.setLatitude(toDegree(lat2));
		nl.setLongitude(toDegree(lon2));
		nl.setBearing(bearing);
		nl.setTrafficLight(false);
		return nl;
	}

	private static double toDegree(double radians) {
		return radians * 180 / Math.PI;
	}

	private static double toRad(double degree) {
		return degree * Math.PI / 180;
	}

	private static List<SimulatedLocation> getSimulatedLocationsForGpx(@NonNull OsmandApplication app,
	                                                                   @NonNull GPXFile gpxFile,
	                                                                   int firstLocationOffset) {
		double distanceFromStart = 0;
		List<SimulatedLocation> simulatedLocations = new ArrayList<>();
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
				simulatedLocations.add(new SimulatedLocation(l, SIMULATED_PROVIDER_GPX));
			}
			prevLocation = location;
		}
		return simulatedLocations;
	}

	public static class SimulatedLocation extends Location {

		private boolean trafficLight;
		private String highwayType;
		private float speedLimit;

		public SimulatedLocation(SimulatedLocation location) {
			super(location);
			trafficLight = location.isTrafficLight();
			highwayType = location.getHighwayType();
			speedLimit = location.getSpeedLimit();
		}

		public SimulatedLocation(Location l, String provider) {
			super(l);
			setProvider(provider);
		}

		public boolean isTrafficLight() {
			return trafficLight;
		}

		public void setTrafficLight(boolean trafficLight) {
			this.trafficLight = trafficLight;
		}

		public String getHighwayType() {
			return this.highwayType;
		}

		public void setHighwayType(String highwayType) {
			this.highwayType = highwayType;
		}

		public float getSpeedLimit() {
			return this.speedLimit;
		}

		public void setSpeedLimit(float speedLimit) {
			this.speedLimit = speedLimit;
		}
	}

	public interface LocationSimulationListener {
		void onSimulationStateChanged(boolean simulating);
	}
}
