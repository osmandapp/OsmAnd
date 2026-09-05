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
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLEBloodPressureSensor extends BLEAbstractSensor {

	private BloodPressureData lastBloodPressureData;

	public static class BloodPressureData implements SensorData {

		private final long timestamp;
		private final SensorPressureUnit unit;
		private final float systolic;
		private final float diastolic;
		private final float arterialPressure;
		private final float pulseRate;

		public BloodPressureData(long timestamp, @NonNull SensorPressureUnit unit,
		                         float systolic, float diastolic, float arterialPressure, float pulseRate) {
			this.timestamp = timestamp;
			this.unit = unit;
			this.systolic = systolic;
			this.diastolic = diastolic;
			this.arterialPressure = arterialPressure;
			this.pulseRate = pulseRate;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public SensorPressureUnit getUnit() {
			return unit;
		}

		public float getSystolic() {
			return systolic;
		}

		public float getDiastolic() {
			return diastolic;
		}

		public float getArterialPressure() {
			return arterialPressure;
		}

		public float getPulseRate() {
			return pulseRate;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Arrays.asList(
					new SensorDataField(R.string.external_device_characteristic_systolic, -1, systolic),
					new SensorDataField(R.string.external_device_characteristic_diastolic, -1, diastolic),
					new SensorDataField(R.string.external_device_characteristic_arterial_pressure, -1, arterialPressure),
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
					new SensorWidgetDataField(SensorWidgetDataFieldType.HEART_RATE, R.string.external_device_characteristic_systolic, -1, systolic),
					new SensorWidgetDataField(SensorWidgetDataFieldType.HEART_RATE, R.string.external_device_characteristic_diastolic, -1, diastolic),
					new SensorWidgetDataField(SensorWidgetDataFieldType.HEART_RATE, R.string.external_device_characteristic_arterial_pressure, -1, arterialPressure),
					new SensorWidgetDataField(SensorWidgetDataFieldType.HEART_RATE, R.string.external_device_characteristic_pulse_rate, -1, pulseRate));
		}

		@NonNull
		@Override
		public String toString() {
			return "BloodPressureData {" +
					"timestamp=" + timestamp +
					", unit=" + unit +
					", systolic=" + systolic +
					", diastolic=" + diastolic +
					", arterialPressure=" + arterialPressure +
					", pulseRate=" + pulseRate +
					'}';
		}
	}

	public BLEBloodPressureSensor(@NonNull BLEAbstractDevice device) {
		super(device, device.getDeviceId() + "_blood_pressure");
	}

	public BLEBloodPressureSensor(@NonNull BLEAbstractDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Blood Pressure Sensor";
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.HEART_RATE);
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastBloodPressureData);
	}

	@NonNull
	@Override
	public UUID getRequestedCharacteristicUUID() {
		return GattAttributes.UUID_CHARACTERISTIC_BP_MEASUREMENT;
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

		int unit = flag & 0x01; //0 = mmHg 1 = kPa
		boolean timestampPresent = (flag & 0x02) == 0x02;
		boolean pulseRatePresent = (flag & 0x04) == 0x04;

		// TODO (BLE): Looks like wrong reading according to GATT spec v8: "Present if bit 0 of Flags field is set to 0"
		float systolic = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 1);
		float diastolic = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 3);
		float arterialPressure = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 5);

		long timestamp = System.currentTimeMillis();
		if (timestampPresent) {
			Calendar calendar = Calendar.getInstance();
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

		getDevice().fireSensorDataEvent(this, new BloodPressureData(timestamp,
				SensorPressureUnit.getUnitById(unit), systolic, diastolic, arterialPressure, pulseRate));
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
	}
}