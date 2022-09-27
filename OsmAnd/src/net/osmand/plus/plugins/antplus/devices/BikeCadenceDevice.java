package net.osmand.plus.plugins.antplus.devices;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.antplus.antdevices.AntBikeCadenceDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeCadenceDevice.BikeCadenceData;
import net.osmand.plus.views.mapwidgets.WidgetType;

import org.json.JSONException;
import org.json.JSONObject;

public class BikeCadenceDevice extends CommonDevice<AntBikeCadenceDevice> {

	private static final String BIKE_CADENCE_ENABLED_PREFERENCE_ID = "ant_bike_cadence_enabled";
	private static final String BIKE_CADENCE_ANT_NUMBER_PREFERENCE_ID = "ant_bike_cadence_device_number";
	private static final String BIKE_CADENCE_WRITE_GPX_PREFERENCE_ID = "ant_bike_cadence_write_gpx";

	public BikeCadenceDevice(@NonNull IPreferenceFactory preferenceFactory) {
		super(preferenceFactory);
	}

	@Override
	@NonNull
	public AntBikeCadenceDevice createAntDevice() {
		int antDeviceNumber = getDeviceNumber();
		return antDeviceNumber != -1 ? new AntBikeCadenceDevice(antDeviceNumber) : new AntBikeCadenceDevice();
	}

	@NonNull
	@Override
	protected String getDeviceEnabledPrefId() {
		return BIKE_CADENCE_ENABLED_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getDeviceNumberPrefId() {
		return BIKE_CADENCE_ANT_NUMBER_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getWriteGpxPrefId() {
		return BIKE_CADENCE_WRITE_GPX_PREFERENCE_ID;
	}

	@NonNull
	@Override
	public WidgetType getDeviceWidgetType() {
		return WidgetType.ANT_BICYCLE_CADENCE;
	}

	@NonNull
	@Override
	public void writeDataToJson(@NonNull JSONObject json) throws JSONException {
		AntBikeCadenceDevice device = getAntDevice();
		BikeCadenceData data = device.getLastBikeCadenceData();
		int calculatedCadence = data != null ? data.getCalculatedCadence() : 0;
		if (calculatedCadence > 0 && (System.currentTimeMillis() - data.getTimestamp()) <= TRACK_DATA_EXPIRATION_TIME_MIN) {
			json.put("cad", calculatedCadence);
		}
	}
}
