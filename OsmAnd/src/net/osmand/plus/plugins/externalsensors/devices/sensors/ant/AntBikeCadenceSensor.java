package net.osmand.plus.plugins.externalsensors.devices.sensors.ant;

import static net.osmand.plus.plugins.externalsensors.SensorAttributesUtils.SENSOR_TAG_CADENCE;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusBikeCadencePcc;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntBikeSpeedCadenceDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.utils.OsmAndFormatter;

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

		private static class BikeCadenceDataField extends SensorWidgetDataField {

			public BikeCadenceDataField(int nameId, int unitNameId, @NonNull Number cadenceValue) {
				super(SensorWidgetDataFieldType.BIKE_CADENCE, nameId, unitNameId, cadenceValue);
			}

			@Nullable
			@Override
			public OsmAndFormatter.FormattedValue getFormattedValue(@NonNull OsmandApplication app) {
				float cadence = getNumberValue().floatValue();
				return cadence > 0
						? new OsmAndFormatter.FormattedValue(cadence, String.valueOf(cadence), "rpm")
						: null;
			}
		}

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
					new SensorDataField(R.string.map_widget_ant_bicycle_cadence, -1, calculatedCadence));
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
					new BikeCadenceDataField(R.string.map_widget_ant_bicycle_cadence, -1, calculatedCadence));
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
		getAntDevice().getPcc().subscribeCalculatedCadenceEvent((estTimestamp, eventFlags, calculatedCadence) -> {
			lastBikeCadenceData = new BikeCadenceData(estTimestamp, calculatedCadence.intValue());
			getDevice().fireSensorDataEvent(this, lastBikeCadenceData);
		});
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json) throws JSONException {
		BikeCadenceData data = lastBikeCadenceData;
		int calculatedCadence = data != null ? data.getCalculatedCadence() : 0;
		if (calculatedCadence > 0) {
			json.put(getGpxTagName(), calculatedCadence);
		}
	}

	@NonNull
	@Override
	protected String getGpxTagName() {
		return SENSOR_TAG_CADENCE;
	}

}
