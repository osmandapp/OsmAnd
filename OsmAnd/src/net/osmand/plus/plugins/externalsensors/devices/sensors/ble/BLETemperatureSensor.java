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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLETemperatureSensor extends BLEAbstractSensor {

	// NaN, NRes, reserved and infinity values are mantissas from 0x7FFFFE to 0x800002
	private static final int FLOAT_POSITIVE_INFINITY = 0x7FFFFE;
	private static final int FLOAT_NEGATIVE_INFINITY = -0x7FFFFE;

	private TemperatureData lastTemperatureData;

	public static class TemperatureData implements SensorData {

		private final long timestamp;
		private final BigDecimal temperature;

		TemperatureData(long timestamp, @NonNull BigDecimal temperature) {
			this.timestamp = timestamp;
			this.temperature = temperature;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public double getTemperature() {
			return temperature.doubleValue();
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
		BigDecimal temperature = parseIeee11073Float(characteristic.getValue(), 1);
		if (temperature == null) {
			return;
		}
		TemperatureData data = new TemperatureData(System.currentTimeMillis(), temperature);
		this.lastTemperatureData = data;
		getDevice().fireSensorDataEvent(this, data);
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		TemperatureData data = lastTemperatureData;
		if (data != null) {
			json.put(SENSOR_TAG_TEMPERATURE_A, data.getTemperature());
		}
	}

	// IEEE-11073 32-bit FLOAT keeps the decimal exponent, so the value is shown with the precision the sensor sent
	@Nullable
	public static BigDecimal parseIeee11073Float(@Nullable byte[] value, int offset) {
		if (value == null || value.length < offset + 4) {
			return null;
		}
		int mantissa = (value[offset] & 0xFF) | (value[offset + 1] & 0xFF) << 8 | value[offset + 2] << 16;
		int exponent = value[offset + 3];
		if (mantissa >= FLOAT_POSITIVE_INFINITY || mantissa <= FLOAT_NEGATIVE_INFINITY) {
			return null;
		}
		BigDecimal result = BigDecimal.valueOf(mantissa, -exponent);
		return result.scale() < 0 ? result.setScale(0) : result;
	}
}