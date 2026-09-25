package net.osmand.plus.plugins.externalsensors.devices.sensors.ant;

import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_CADENCE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntBikeSpeedCadenceDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.BikeCadenceDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;

public class AntBikeCadenceSensor extends AntAbstractSensor<AntPlusBikeCadencePcc> {

	private BikeCadenceData lastBikeCadenceData;

	public static class BikeCadenceData implements SensorData {

		// The estimated timestamp of when this event was triggered.
		// Useful for correlating multiple events and determining when data was sent for more accurate data records.
		private final long timestamp;

		// The cadence calculated from the raw values in the sensor broadcast. Units: rpm.
		private final int calculatedCadence;

		BikeCadenceData(long timestamp, int calculatedCadence) {
			this.timestamp = timestamp;
			this.calculatedCadence = calculatedCadence;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public int getCalculatedCadence() {
			return calculatedCadence;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(
					new BikeCadenceDataField(R.string.map_widget_ant_bicycle_cadence, R.string.revolutions_per_minute_unit, calculatedCadence));
		}

		@NonNull
		@Override
		public List<SensorDataField> getExtraDataFields() {
			return Collections.singletonList(
					new SensorDataField(R.string.shared_string_time, -1, timestamp));
		}

		@Nullable
		@Override
		public List<SensorWidgetDataField> getWidgetFields() {
			return Collections.singletonList(
					new BikeCadenceDataField(R.string.map_widget_ant_bicycle_cadence, R.string.revolutions_per_minute_unit, calculatedCadence));
		}

		@NonNull
		@Override
		public String toString() {
			return "BikeCadenceData {" +
					"timestamp=" + timestamp +
					", calculatedCadence=" + calculatedCadence +
					'}';
		}
	}

	public AntBikeCadenceSensor(@NonNull AntBikeSpeedCadenceDevice device) {
		super(device, device.getDeviceId() + "_cadence");
	}

	public AntBikeCadenceSensor(@NonNull AntBikeSpeedCadenceDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Bicycle Cadence";
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.BIKE_CADENCE);
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastBikeCadenceData);
	}

	@Override
	public void subscribeToEvents() {
		AntPlusBikeCadencePcc pcc = getAntDevice().getPcc();
		if (pcc != null) {
			pcc.subscribeCalculatedCadenceEvent(null);
			pcc.subscribeCalculatedCadenceEvent((estTimestamp, eventFlags, calculatedCadence) -> {
				lastBikeCadenceData = new BikeCadenceData(estTimestamp, calculatedCadence.intValue());
				getDevice().fireSensorDataEvent(this, lastBikeCadenceData);
			});
		}
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		BikeCadenceData data = lastBikeCadenceData;
		int calculatedCadence = data != null ? data.getCalculatedCadence() : 0;
		if (calculatedCadence > 0) {
			json.put(SENSOR_TAG_CADENCE, calculatedCadence);
		}
	}
}
