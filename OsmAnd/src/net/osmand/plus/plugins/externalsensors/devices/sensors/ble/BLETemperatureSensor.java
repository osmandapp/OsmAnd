package net.osmand.plus.plugins.externalsensors.devices.sensors.ble;

import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_TEMPERATURE_A;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLETemperatureSensor extends BLEAbstractSensor {

	private TemperatureData lastTemperatureData;

	public static class TemperatureData implements SensorData {

		private final long timestamp;
		private final double temperature;

		TemperatureData(long timestamp, double temperature) {
			this.timestamp = timestamp;
			this.temperature = temperature;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public double getTemperature() {
			return temperature;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(
					new SensorDataField(R.string.external_device_characteristic_temperature, R.string.degree_celsius, temperature));
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
					new SensorWidgetDataField(SensorWidgetDataFieldType.TEMPERATURE,
							R.string.external_device_characteristic_temperature, -1, temperature));
		}

		@NonNull
		@Override
		public String toString() {
			return "BatteryData {" +
					"timestamp=" + timestamp +
					", temperature=" + temperature +
					'}';
		}
	}

	public BLETemperatureSensor(@NonNull BLEAbstractDevice device) {
		super(device, device.getDeviceId() + "_temperature");
	}

	public BLETemperatureSensor(@NonNull BLEAbstractDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.TEMPERATURE);
	}

	@NonNull
	@Override
	public UUID getRequestedCharacteristicUUID() {
		return GattAttributes.UUID_CHAR_TEMPERATURE_UUID;
	}

	@NonNull
	@Override
	public String getName() {
		return "Temperature";
	}

	@Override
	@Nullable
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastTemperatureData);
	}

	@Nullable
	public TemperatureData getLastBatteryData() {
		return lastTemperatureData;
	}

	@Override
	public void onCharacteristicRead(@NonNull BluetoothGatt gatt,
	                                 @NonNull BluetoothGattCharacteristic characteristic,
	                                 int status) {
	}

	@Override
	public void onCharacteristicChanged(@NonNull BluetoothGatt gatt,
	                                    @NonNull BluetoothGattCharacteristic characteristic) {
		UUID charaUUID = characteristic.getUuid();
		if (getRequestedCharacteristicUUID().equals(charaUUID)) {
			decodeTemperatureCharacteristic(gatt, characteristic);
		}
	}

	private void decodeTemperatureCharacteristic(@NonNull BluetoothGatt gatt,
	                                             @NonNull BluetoothGattCharacteristic characteristic) {
		double temperature = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);
		TemperatureData data = new TemperatureData(System.currentTimeMillis(), temperature);
		this.lastTemperatureData = data;
		getDevice().fireSensorDataEvent(this, data);
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		TemperatureData data = lastTemperatureData;
		if (data != null) {
			json.put(SENSOR_TAG_TEMPERATURE_A, data.temperature);
		}
	}
}