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
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLEBatterySensor extends BLEAbstractSensor {

	private BatteryData lastBatteryData;

	public static class BatteryData implements SensorData {

		private final long timestamp;
		private final int batteryLevel;

		BatteryData(long timestamp, int batteryLevel) {
			this.timestamp = timestamp;
			this.batteryLevel = batteryLevel;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public int getBatteryLevel() {
			return batteryLevel;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(new SensorDataField(R.string.map_widget_battery, -1, batteryLevel));
		}

		@NonNull
		@Override
		public List<SensorDataField> getExtraDataFields() {
			return Collections.singletonList(new SensorDataField(R.string.shared_string_time, -1, timestamp));
		}

		@Nullable
		@Override
		public List<SensorWidgetDataField> getWidgetFields() {
			return Collections.singletonList(
					new SensorWidgetDataField(SensorWidgetDataFieldType.BATTERY, R.string.map_widget_battery, -1, batteryLevel));
		}

		@NonNull
		@Override
		public String toString() {
			return "BatteryData {" +
					"timestamp=" + timestamp +
					", batteryLevel=" + batteryLevel +
					'}';
		}
	}

	public BLEBatterySensor(@NonNull BLEAbstractDevice device) {
		super(device, device.getDeviceId() + "_battery_level");
	}

	public BLEBatterySensor(@NonNull BLEAbstractDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.BATTERY);
	}

	@NonNull
	@Override
	public UUID getRequestedCharacteristicUUID() {
		return GattAttributes.UUID_CHARACTERISTIC_BATTERY_LEVEL;
	}

	@NonNull
	@Override
	public String getName() {
		return "Battery Level";
	}

	@Override
	@Nullable
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastBatteryData);
	}

	@Nullable
	public BatteryData getLastBatteryData() {
		return lastBatteryData;
	}

	@Override
	public void onCharacteristicRead(@NonNull BluetoothGatt gatt,
	                                 @NonNull BluetoothGattCharacteristic characteristic,
	                                 int status) {
		if (status == BluetoothGatt.GATT_SUCCESS) {
			if (getRequestedCharacteristicUUID().equals(characteristic.getUuid())) {
				decodeBatteryCharacteristic(gatt, characteristic);
			}
		}
	}

	@Override
	public void onCharacteristicChanged(@NonNull BluetoothGatt gatt,
	                                    @NonNull BluetoothGattCharacteristic characteristic) {
		UUID charaUUID = characteristic.getUuid();
		if (getRequestedCharacteristicUUID().equals(charaUUID)) {
			decodeBatteryCharacteristic(gatt, characteristic);
		}
	}

	private void decodeBatteryCharacteristic(@NonNull BluetoothGatt gatt,
	                                         @NonNull BluetoothGattCharacteristic characteristic) {
		int batteryLevel = characteristic.getValue()[0];
		BatteryData data = new BatteryData(System.currentTimeMillis(), batteryLevel);
		this.lastBatteryData = data;
		getBLEDevice().fireSensorDataEvent(this, data);
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
	}
}