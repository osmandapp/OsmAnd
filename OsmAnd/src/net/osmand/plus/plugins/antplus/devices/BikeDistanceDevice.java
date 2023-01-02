package net.osmand.plus.plugins.antplus.devices;

import static net.osmand.gpx.GPXUtilities.DECIMAL_FORMAT;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.antplus.antdevices.AntBikeDistanceDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeDistanceDevice.BikeDistanceData;
import net.osmand.plus.views.mapwidgets.WidgetType;

import org.json.JSONException;
import org.json.JSONObject;

public class BikeDistanceDevice extends CommonDevice<AntBikeDistanceDevice> {

	private static final String BIKE_DISTANCE_ENABLED_PREFERENCE_ID = "ant_bike_distance_enabled";
	private static final String BIKE_DISTANCE_ANT_NUMBER_PREFERENCE_ID = "ant_bike_distance_device_number";
	private static final String BIKE_DISTANCE_WRITE_GPX_PREFERENCE_ID = "ant_bike_distance_write_gpx";

	public BikeDistanceDevice(@NonNull IPreferenceFactory preferenceFactory) {
		super(preferenceFactory);
	}

	@Override
	@NonNull
	public AntBikeDistanceDevice createAntDevice() {
		int antDeviceNumber = getDeviceNumber();
		return antDeviceNumber != -1 ? new AntBikeDistanceDevice(antDeviceNumber) : new AntBikeDistanceDevice();
	}

	@NonNull
	@Override
	protected String getDeviceEnabledPrefId() {
		return BIKE_DISTANCE_ENABLED_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getDeviceNumberPrefId() {
		return BIKE_DISTANCE_ANT_NUMBER_PREFERENCE_ID;
	}

	@NonNull
	@Override
	protected String getWriteGpxPrefId() {
		return BIKE_DISTANCE_WRITE_GPX_PREFERENCE_ID;
	}

	@NonNull
	@Override
	public WidgetType getDeviceWidgetType() {
		return WidgetType.ANT_BICYCLE_DISTANCE;
	}

	@NonNull
	@Override
	public void writeDataToJson(@NonNull JSONObject json) throws JSONException {
		AntBikeDistanceDevice device = getAntDevice();
		BikeDistanceData data = device.getLastBikeDistanceData();
		double accumulatedDistance = data != null ? data.getAccumulatedDistance() : 0;
		if (accumulatedDistance > 0) {
			json.put("ant_bicycle_distance", DECIMAL_FORMAT.format(accumulatedDistance));
		}
	}
}
