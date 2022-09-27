package net.osmand.plus.plugins.antplus.devices;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.antplus.antdevices.AntHeartRateDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntHeartRateDevice.HeartRateData;
import net.osmand.plus.views.mapwidgets.WidgetType;

import org.json.JSONException;
import org.json.JSONObject;

public class HeartRateDevice extends CommonDevice<AntHeartRateDevice> {

	private static final String HEART_RATE_ENABLED_PREFERENCE_ID = "ant_heart_rate_enabled";
	private static final String HEART_RATE_ANT_NUMBER_PREFERENCE_ID = "ant_heart_rate_device_number";
	private static final String HEART_RATE_WRITE_GPX_PREFERENCE_ID = "ant_heart_rate_write_gpx";

	public HeartRateDevice(@NonNull IPreferenceFactory preferenceFactory) {
		super(preferenceFactory);
	}

	@Override
	@NonNull
	public AntHeartRateDevice createAntDevice() {
		int antDeviceNumber = getDeviceNumber();
		return antDeviceNumber != -1 ? new AntHeartRateDevice(antDeviceNumber) : new AntHeartRateDevice();
	}

	@NonNull
	@Override
	protected String getDeviceEnabledPrefId() {
		return HEART_RATE_ENABLED_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getDeviceNumberPrefId() {
		return HEART_RATE_ANT_NUMBER_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getWriteGpxPrefId() {
		return HEART_RATE_WRITE_GPX_PREFERENCE_ID;
	}

	@NonNull
	@Override
	public WidgetType getDeviceWidgetType() {
		return WidgetType.ANT_HEART_RATE;
	}

	@NonNull
	@Override
	public void writeDataToJson(@NonNull JSONObject json) throws JSONException {
		AntHeartRateDevice device = getAntDevice();
		HeartRateData data = device.getLastHeartRateData();
		int computedHeartRate = data != null ? data.getComputedHeartRate() : 0;
		if (computedHeartRate > 0 && (System.currentTimeMillis() - data.getTimestamp()) <= TRACK_DATA_EXPIRATION_TIME_MIN) {
			json.put("hr", computedHeartRate);
		}
	}
}
