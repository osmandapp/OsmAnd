package net.osmand.plus;


import static net.osmand.plus.OsmAndLocationProvider.SIMULATED_PROVIDER;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;

import com.google.android.material.slider.Slider;

import net.osmand.Location;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.routing.GPXRouteParams.GPXRouteParamsBuilder;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.enums.SimulationMode;
import net.osmand.plus.utils.UiUtilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class OsmAndLocationSimulation {

	public static final float PRECISION_1_M = 0.00001f;
	public static final int DEVIATION_M = 6;
	private final float MOTORWAY_MAX_SPEED = 120.0f;
	private final float TRUNK_MAX_SPEED = 90.0f;
	private final float PRIMARY_MAX_SPEED = 60.0f;
	private final float SECONDARY_MAX_SPEED = 50.0f;
	private final float LIVING_SPTREET_MAX_SPEED = 15.0f;
	private final float DEFAULT_MAX_SPEED = 40.0f;
	private Thread routeAnimation;
	private final OsmAndLocationProvider provider;
	private final OsmandApplication app;

	public OsmAndLocationSimulation(OsmandApplication app, OsmAndLocationProvider provider) {
		this.app = app;
		this.provider = provider;
	}

	public boolean isRouteAnimating() {
		return routeAnimation != null;
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
				((TextView) view.findViewById(R.id.MinSpeedup)).setText("1"); //$NON-NLS-1$
				((TextView) view.findViewById(R.id.MaxSpeedup)).setText("4"); //$NON-NLS-1$
				Slider speedup = view.findViewById(R.id.Speedup);
				speedup.setValueTo(3);
				UiUtilities.setupSlider(speedup, nightMode, selectedModeColor, true);
				builder.setView(view);
				builder.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
					boolean nightMode1 = activity instanceof MapActivity ? app.getDaynightHelper().isNightModeForMapControls() : !app.getSettings().isLightContent();
					GpxUiHelper.selectGPXFile(activity, false, false, result -> {
						GPXRouteParamsBuilder gpxParamsBuilder = new GPXRouteParamsBuilder(result[0], app.getSettings());
						startAnimationThread(app, gpxParamsBuilder.getSimulatedLocations(app), true, speedup.getValue() + 1);
						if (runnable != null) {
							runnable.run();
						}
						return true;
					}, nightMode1);
				});
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.show();
			} else {
				List<SimulatedLocation> currentRoute = app.getRoutingHelper().getRoute().getImmutableSimulatedLocations();
				if (currentRoute.isEmpty()) {
					Toast.makeText(app, R.string.animate_routing_route_not_calculated,
							Toast.LENGTH_LONG).show();
				} else {
					startAnimationThread(app, new ArrayList<>(currentRoute), false, 1);
					if (runnable != null) {
						runnable.run();
					}
				}
			}
		} else {
			stop();
			if (runnable != null) {
				runnable.run();
			}
		}
	}

	public void startStopRouteAnimation(@Nullable Activity activity) {
		startStopRouteAnimation(activity, false, null);
	}

	public void startStopGpxAnimation(@Nullable Activity activity) {
		startStopRouteAnimation(activity, true, null);
	}

	private void startAnimationThread(OsmandApplication app, List<SimulatedLocation> directions,
	                                  boolean locTime, float coeff) {
		final float time = 1.5f;
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
						if (!current.hasAccuracy() || Double.isNaN(current.getAccuracy()) || (realistic && speed < 10)) {
							current.setAccuracy(5);
						}
						if (prev != null && prev.distanceTo(current) > 3 || (realistic && speed >= 3)) {
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
		routeAnimation = null;
	}

	public static SimulatedLocation middleLocation(SimulatedLocation start, SimulatedLocation end, float meters) {
		double lat1 = toRad(start.getLatitude());
		double lon1 = toRad(start.getLongitude());
		double R = 6371; // radius of earth in km
		double d = meters / 1000; // in km
		float brng = (float) (toRad(start.bearingTo(end)));
		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R)
				+ Math.cos(lat1) * Math.sin(d / R) * Math.cos(brng));
		double lon2 = lon1
				+ Math.atan2(Math.sin(brng) * Math.sin(d / R) * Math.cos(lat1),
				Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));
		SimulatedLocation nl = new SimulatedLocation(start);
		nl.setLatitude(toDegree(lat2));
		nl.setLongitude(toDegree(lon2));
		nl.setBearing(brng);
		nl.setTrafficLight(false);
		return nl;
	}

	private static double toDegree(double radians) {
		return radians * 180 / Math.PI;
	}

	private static double toRad(double degree) {
		return degree * Math.PI / 180;
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

		public SimulatedLocation(Location l) {
			super(l);
			setProvider(SIMULATED_PROVIDER);
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
}
