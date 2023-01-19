package net.osmand.plus.plugins.antplus.devices;

import static net.osmand.gpx.GPXUtilities.DECIMAL_FORMAT;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.antplus.antdevices.AntBikePowerDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikePowerDevice.BikePowerData;
import net.osmand.plus.views.mapwidgets.WidgetType;

import org.json.JSONException;
import org.json.JSONObject;

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

	@NonNull
	@Override
	public WidgetType getDeviceWidgetType() {
		return WidgetType.ANT_BICYCLE_POWER;
	}

	@NonNull
	@Override
	public void writeDataToJson(@NonNull JSONObject json) throws JSONException {
		AntBikePowerDevice device = getAntDevice();
		BikePowerData data = device.getLastBikePowerData();
		double calculatedPower = data != null ? data.getCalculatedPower() : null;
		if (calculatedPower > 0) {
			json.put("ant_bicycle_power",  DECIMAL_FORMAT.format(calculatedPower));
		}
	}
}

