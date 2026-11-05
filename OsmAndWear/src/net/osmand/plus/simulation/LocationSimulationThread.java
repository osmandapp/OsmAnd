package net.osmand.plus.simulation;

import static net.osmand.plus.simulation.SimulationProvider.SIMULATED_PROVIDER;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.enums.SimulationMode;

import java.util.List;
import java.util.Random;

import kotlin.Triple;

class LocationSimulationThread extends Thread {

	private static final float PRECISION_1_M = 0.00001f;
	private static final float LOCATION_TIMEOUT = 1.5f;
	private static final float DEVIATION_M = 6;

	private final OsmandApplication app;
	private final OsmAndLocationProvider provider;
	private final OsmAndLocationSimulation locationSimulation;

	private final List<SimulatedLocation> directions;
	private final SimulationMode mode;
	private final float speed;
	private final float coeff;
	private final boolean realistic;
	private final boolean locationTime;

	public LocationSimulationThread(@NonNull OsmandApplication app,
	                                @NonNull List<SimulatedLocation> directions,
	                                float coeff, boolean locationTime) {
		super();
		this.app = app;
		this.provider = app.getLocationProvider();
		this.locationSimulation = provider.getLocationSimulation();
		this.directions = directions;
		this.coeff = coeff;
		this.locationTime = locationTime;

		OsmandSettings settings = app.getSettings();
		speed = settings.simulateNavigationSpeed;
		mode = SimulationMode.getMode(settings.simulateNavigationMode);
		realistic = mode == SimulationMode.REALISTIC;
	}

	@Override
	public void run() {
		SimulatedLocation current = directions.isEmpty() ? null : new SimulatedLocation(directions.remove(0));
		boolean useLocationTime = locationTime && current != null && current.getTime() != 0;
		SimulatedLocation prev = current;
		long prevTime = current == null ? 0 : current.getTime();
		float meters = LocationSimulationUtils.metersToGoInFiveSteps(directions, current);
		if (current != null) {
			current.setProvider(SIMULATED_PROVIDER);
		}
		Triple<Boolean, Boolean, Boolean> triple = getSimulationParams(directions, useLocationTime);

		int stopDelayCount = 0;
		while (!directions.isEmpty() && locationSimulation.isRouteAnimating()) {
			long timeout = (long) (LOCATION_TIMEOUT * 1000);
			float intervalTime = LOCATION_TIMEOUT;
			if (stopDelayCount == 0) {
				if (useLocationTime) {
					current = directions.remove(0);
					meters = current.distanceTo(prev);
					if (!directions.isEmpty()) {
						long currentTime = current.getTime();
						long nextTime = directions.get(0).getTime();
						if (currentTime != 0 && nextTime != 0) {
							timeout = Math.abs(nextTime - currentTime);
							intervalTime = Math.abs((currentTime - prevTime) / 1000f);
							prevTime = currentTime;
						}
					}
				} else {
					Pair<SimulatedLocation, Float> pair = LocationSimulationUtils.createSimulatedLocation(
							current, directions, mode, meters, intervalTime, coeff, speed, realistic);
					current = pair.first;
					meters = pair.second;
				}
				setupLocation(triple, current, prev, meters, intervalTime);
			}
			current.setTime(System.currentTimeMillis());
			Location toSet = current;
			if (realistic) {
				addNoise(toSet);
			}
			if (realistic && current.isTrafficLight() && stopDelayCount == 0) {
				stopDelayCount = 5;
				current.setSpeed(0);
				current.removeBearing();
			} else if (stopDelayCount > 0) {
				stopDelayCount--;
			}
			app.runInUIThread(() -> provider.setLocationFromSimulation(toSet));
			try {
				long time = (long) (timeout / coeff);
				Thread.sleep(time);
			} catch (InterruptedException e) {
				// do nothing
			}
			prev = current;
		}
		locationSimulation.stop();
	}

	@NonNull
	private Triple<Boolean, Boolean, Boolean> getSimulationParams(@NonNull List<SimulatedLocation> directions, boolean useLocationTime) {
		boolean bearingSimulation = true;
		boolean accuracySimulation = true;
		boolean speedSimulation = true;
		if (useLocationTime) {
			for (SimulatedLocation location : directions) {
				if (location.hasBearing()) {
					bearingSimulation = false;
				}
				if (location.hasAccuracy()) {
					accuracySimulation = false;
				}
				if (location.hasSpeed()) {
					speedSimulation = false;
				}
			}
		}
		return new Triple<>(bearingSimulation, accuracySimulation, speedSimulation);
	}

	private void setupLocation(@NonNull Triple<Boolean, Boolean, Boolean> triple,
	                           @NonNull Location current, @Nullable Location previous,
	                           float meters, float intervalTime) {
		float speed = meters / intervalTime * coeff;
		if (intervalTime != 0 && triple.getThird()) {
			current.setSpeed(speed);
		}
		if ((!current.hasAccuracy() || Double.isNaN(current.getAccuracy())
				|| (realistic && speed < 10)) && triple.getSecond()) {
			current.setAccuracy(5);
		}
		if (previous != null && triple.getFirst()
				&& previous.distanceTo(current) > 3 && (!realistic || speed >= 1)) {
			current.setBearing(previous.bearingTo(current));
		}
	}

	private void addNoise(@NonNull Location location) {
		Random random = new Random();
		float d = (random.nextInt((int) (DEVIATION_M + 1)) - DEVIATION_M / 2) * PRECISION_1_M;
		location.setLatitude(location.getLatitude() + d);
		d = (random.nextInt((int) (DEVIATION_M + 1)) - DEVIATION_M / 2) * PRECISION_1_M;
		location.setLongitude(location.getLongitude() + d);
	}
}
