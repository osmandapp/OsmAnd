package net.osmand.plus.simulation;

import androidx.annotation.NonNull;

import net.osmand.Location;

public class SimulatedLocation extends Location {

	private boolean trafficLight;
	private String highwayType;
	private float speedLimit;

	public SimulatedLocation(@NonNull SimulatedLocation location) {
		super(location);
		trafficLight = location.isTrafficLight();
		highwayType = location.getHighwayType();
		speedLimit = location.getSpeedLimit();
	}

	public SimulatedLocation(@NonNull Location location, @NonNull String provider) {
		super(location);
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
