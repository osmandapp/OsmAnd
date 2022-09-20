package net.osmand.plus.plugins.antplus.devices;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.antplus.antdevices.AntBikeSpeedDistanceDevice;

public class BikeSpeedAndDistanceDevice extends CommonDevice<AntBikeSpeedDistanceDevice> {

	private static final String BIKE_SPEED_AND_DISTANCE_ENABLED_PREFERENCE_ID = "ant_bike_speed_and_distance_enabled";
	private static final String BIKE_SPEED_AND_DISTANCE_ANT_NUMBER_PREFERENCE_ID = "ant_bike_speed_and_distance_device_number";
	private static final String BIKE_SPEED_AND_DISTANCE_WRITE_GPX_PREFERENCE_ID = "ant_bike_speed_and_distance_write_gpx";

	public BikeSpeedAndDistanceDevice(@NonNull IPreferenceFactory preferenceFactory) {
		super(preferenceFactory);
	}

	@Override
	@NonNull
	public AntBikeSpeedDistanceDevice createAntDevice() {
		int antDeviceNumber = getDeviceNumber();
		return antDeviceNumber != -1 ? new AntBikeSpeedDistanceDevice(antDeviceNumber) : new AntBikeSpeedDistanceDevice();
	}

	@NonNull
	@Override
	protected String getDeviceEnabledPrefId() {
		return BIKE_SPEED_AND_DISTANCE_ENABLED_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getDeviceNumberPrefId() {
		return BIKE_SPEED_AND_DISTANCE_ANT_NUMBER_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getWriteGpxPrefId() {
		return BIKE_SPEED_AND_DISTANCE_WRITE_GPX_PREFERENCE_ID;
	}
}
