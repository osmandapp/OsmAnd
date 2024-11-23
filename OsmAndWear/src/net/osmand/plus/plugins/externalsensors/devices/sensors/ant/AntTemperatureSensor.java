package net.osmand.plus.plugins.externalsensors.devices.sensors.ant;

import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_TEMPERATURE_A;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusEnvironmentPcc;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntTemperatureDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

public class AntTemperatureSensor extends AntAbstractSensor<AntPlusEnvironmentPcc> {

	private TemperatureData lastTemperatureData;

	public static class TemperatureData implements SensorData {

		private final boolean sensorConnected;

		// The estimated timestamp of when this event was triggered.
		private final long timestamp;

		//Degrees Celsius
		private final double temperature;
		private final long eventCount;

		// Sensor reported time counter value of last distance or speed computation (up to 1/1024s accuracy). Units: s
		private final BigDecimal lowLast24Hours;
		private final BigDecimal highLast24Hours;

		TemperatureData(boolean sensorConnected, long timestamp,
		                double temperature, long eventCount, BigDecimal lowLast24Hours, BigDecimal highLast24Hours) {
			this.sensorConnected = sensorConnected;
			this.timestamp = timestamp;
			this.temperature = temperature;
			this.eventCount = eventCount;
			this.lowLast24Hours = lowLast24Hours;
			this.highLast24Hours = highLast24Hours;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public double getTemperatureRate() {
			return temperature;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(new SensorDataField(R.string.external_device_characteristic_temperature, R.string.degree_celsius,
					temperature, temperature + ""));
		}

		@NonNull
		@Override
		public List<SensorDataField> getExtraDataFields() {
			return Collections.singletonList(new SensorDataField(R.string.shared_string_time, -1, timestamp));
		}

		@Nullable
		@Override
		public List<SensorWidgetDataField> getWidgetFields() {
			return Collections.singletonList(new SensorWidgetDataField(
					SensorWidgetDataFieldType.TEMPERATURE, R.string.external_device_characteristic_temperature, R.string.degree_celsius, temperature));
		}

		@NonNull
		@Override
		public String toString() {
			return "TemperatureData {" +
					"timestamp=" + timestamp +
					", temperature=" + temperature +
					", lowLast24Hours=" + lowLast24Hours +
					", highLast24Hours=" + highLast24Hours +
					'}';
		}
	}

	public AntTemperatureSensor(@NonNull AntTemperatureDevice device) {
		super(device, device.getDeviceId() + "_temperature");
	}

	public AntTemperatureSensor(@NonNull AntTemperatureDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Temperature";
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.TEMPERATURE);
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastTemperatureData);
	}

	@Override
	public void subscribeToEvents() {
		AntPlusEnvironmentPcc pcc = getAntDevice().getPcc();
		if (pcc != null) {
			pcc.subscribeTemperatureDataEvent(null);
			pcc.subscribeTemperatureDataEvent((estTimestamp, eventFlags, temperature, eventCount, lowLast24Hours, highLast24Hours) -> {
				lastTemperatureData = new TemperatureData(getDevice().isConnected(), estTimestamp, temperature.doubleValue(), eventCount, lowLast24Hours, highLast24Hours);
				getDevice().fireSensorDataEvent(AntTemperatureSensor.this, lastTemperatureData);
			});
		}
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		TemperatureData data = lastTemperatureData;
		double computedTemperature = data != null ? data.getTemperatureRate() : 0;
		if (computedTemperature > 0) {
			json.put(SENSOR_TAG_TEMPERATURE_A, computedTemperature);
		}
	}
}
