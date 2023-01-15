package net.osmand.plus.plugins.antplus.devices;

import static net.osmand.gpx.GPXUtilities.DECIMAL_FORMAT;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.antplus.antdevices.AntBikeSpeedDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeSpeedDevice.BikeSpeedData;
import net.osmand.plus.views.mapwidgets.WidgetType;

import org.json.JSONException;
import org.json.JSONObject;

public class BikeSpeedDevice extends CommonDevice<AntBikeSpeedDevice> {

	private static final String BIKE_SPEED_ENABLED_PREFERENCE_ID = "ant_bike_speed_enabled";
	private static final String BIKE_SPEED_ANT_NUMBER_PREFERENCE_ID = "ant_bike_speed_device_number";
	private static final String BIKE_SPEED_WRITE_GPX_PREFERENCE_ID = "ant_bike_speed_write_gpx";

	public BikeSpeedDevice(@NonNull IPreferenceFactory preferenceFactory) {
		super(preferenceFactory);
	}

	@Override
	@NonNull
	public AntBikeSpeedDevice createAntDevice() {
		int antDeviceNumber = getDeviceNumber();
		return antDeviceNumber != -1 ? new AntBikeSpeedDevice(antDeviceNumber) : new AntBikeSpeedDevice();
	}

	@NonNull
	@Override
	protected String getDeviceEnabledPrefId() {
		return BIKE_SPEED_ENABLED_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getDeviceNumberPrefId() {
		return BIKE_SPEED_ANT_NUMBER_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getWriteGpxPrefId() {
		return BIKE_SPEED_WRITE_GPX_PREFERENCE_ID;
	}

	@NonNull
	@Override
	public WidgetType getDeviceWidgetType() {
		return WidgetType.ANT_BICYCLE_SPEED;
	}

	@NonNull
	@Override
	public void writeDataToJson(@NonNull JSONObject json) throws JSONException {
		AntBikeSpeedDevice device = getAntDevice();
		BikeSpeedData data = device.getLastBikeSpeedData();
		double calculatedSpeed = data != null ? data.getCalculatedSpeed() : 0;
		if (calculatedSpeed > 0) {
			json.put("ant_bicycle_speed", DECIMAL_FORMAT.format(calculatedSpeed));
		}
	}
}
