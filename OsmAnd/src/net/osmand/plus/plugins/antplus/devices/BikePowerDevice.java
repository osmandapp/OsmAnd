package net.osmand.plus.plugins.antplus.devices;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.antplus.antdevices.AntBikePowerDevice;

public class BikePowerDevice extends CommonDevice<AntBikePowerDevice> {

	private static final String BIKE_POWER_ENABLED_PREFERENCE_ID = "ant_bike_power_enabled";
	private static final String BIKE_POWER_ANT_NUMBER_PREFERENCE_ID = "ant_bike_power_device_number";
	private static final String BIKE_POWER_WRITE_GPX_PREFERENCE_ID = "ant_bike_power_write_gpx";

	public BikePowerDevice(@NonNull IPreferenceFactory preferenceFactory) {
		super(preferenceFactory);
	}

	@Override
	@NonNull
	public AntBikePowerDevice createAntDevice() {
		int antDeviceNumber = getDeviceNumber();
		return antDeviceNumber != -1 ? new AntBikePowerDevice(antDeviceNumber) : new AntBikePowerDevice();
	}

	@NonNull
	@Override
	protected String getDeviceEnabledPrefId() {
		return BIKE_POWER_ENABLED_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getDeviceNumberPrefId() {
		return BIKE_POWER_ANT_NUMBER_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getWriteGpxPrefId() {
		return BIKE_POWER_WRITE_GPX_PREFERENCE_ID;
	}
}

