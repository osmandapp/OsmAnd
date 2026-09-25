package net.osmand.plus.plugins.externalsensors.devices.sensors.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorPressureUnit;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLECuffPressureSensor extends BLEAbstractSensor {

	private CuffPressureData lastCuffPressureData;

	public static class CuffPressureData implements SensorData {

		private final long timestamp;
		private final SensorPressureUnit unit;
		private final float cuffPressure;
		private final float pulseRate;

		public CuffPressureData(long timestamp, @NonNull SensorPressureUnit unit,
		                        float cuffPressure, float pulseRate) {
			this.timestamp = timestamp;
			this.unit = unit;
			this.cuffPressure = cuffPressure;
			this.pulseRate = pulseRate;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public SensorPressureUnit getUnit() {
			return unit;
		}

		public float getCuffPressure() {
			return cuffPressure;
		}

		public float getPulseRate() {
			return pulseRate;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Arrays.asList(
					new SensorDataField(R.string.external_device_characteristic_cuff_pressure, -1, cuffPressure),
					new SensorDataField(R.string.external_device_characteristic_pulse_rate, -1, pulseRate));
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
			return Arrays.asList(
					new SensorWidgetDataField(SensorWidgetDataFieldType.HEART_RATE, R.string.external_device_characteristic_cuff_pressure, -1, cuffPressure),
					new SensorWidgetDataField(SensorWidgetDataFieldType.HEART_RATE, R.string.external_device_characteristic_pulse_rate, -1, pulseRate));
		}

		@NonNull
		@Override
		public String toString() {
			return "BloodPressureData {" +
					"timestamp=" + timestamp +
					", unit=" + unit +
					", cuffPressure=" + cuffPressure +
					", pulseRate=" + pulseRate +
					'}';
		}
	}

	public BLECuffPressureSensor(@NonNull BLEAbstractDevice device) {
		super(device, device.getDeviceId() + "_cuff_pressure");
	}

	public BLECuffPressureSensor(@NonNull BLEAbstractDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Intermediate Cuff Pressure Sensor";
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.HEART_RATE);
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastCuffPressureData);
	}

	@NonNull
	@Override
	public UUID getRequestedCharacteristicUUID() {
		return GattAttributes.UUID_CHARACTERISTIC_ICP_MEASUREMENT;
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
			decodeBloodPressureCharacteristic(gatt, characteristic);
		}
	}

	private void decodeBloodPressureCharacteristic(@NonNull BluetoothGatt gatt,
	                                               @NonNull BluetoothGattCharacteristic characteristic) {
		int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

		int unit = flag & 0x01; //0 = mmHg Other = kPa
		boolean timestampPresent = (flag & 0x02) == 0x02;
		boolean pulseRatePresent = (flag & 0x04) == 0x04;

		float cuffPressure = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 1);

		long timestamp = System.currentTimeMillis();
		if (timestampPresent) {
			java.util.Calendar calendar = java.util.Calendar.getInstance();
			int year = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 7);
			int month = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 9);
			int dayOfMonth = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 10);
			int hourOfDay = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 11);
			int minute = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 12);
			int second = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 13);
			calendar.set(year, month, dayOfMonth, hourOfDay, minute, second);
			timestamp = calendar.getTimeInMillis();
		}

		float pulseRate = 0;
		if (pulseRatePresent) {
			pulseRate = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, timestampPresent ? 14 : 7);
		}

		SensorPressureUnit unitType = unit == 0 ? SensorPressureUnit.MMHG : SensorPressureUnit.KPA;
		getDevice().fireSensorDataEvent(this, new CuffPressureData(timestamp,
				unitType, cuffPressure, pulseRate));
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
	}
}
