package net.osmand.plus.plugins.externalsensors.devices.sensors.ble;

import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_HEART_RATE;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEHeartRateDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLEHeartRateSensor extends BLEAbstractSensor {

	private BodyPart bodyPart;
	private HeartRateData lastHeartRateData;

	public enum BodyPart {
		OTHER(R.string.shared_string_other),
		CHEST(R.string.shared_string_chest),
		WRIST(R.string.shared_string_wrist),
		FINGER(R.string.shared_string_finger),
		HAND(R.string.shared_string_hand),
		EAR_LOBE(R.string.shared_string_ear_lobe),
		FOOT(R.string.shared_string_foot);

		@StringRes
		private final int stringRes;

		BodyPart(int stringRes) {
			this.stringRes = stringRes;
		}

		public int getStringRes() {
			return stringRes;
		}

		public String getString(@NonNull Context context) {
			return context.getString(stringRes);
		}
	}

	public static class HeartRateData implements SensorData {

		private final long timestamp;
		private final int heartRate;

		HeartRateData(long timestamp, int heartRate) {
			this.timestamp = timestamp;
			this.heartRate = heartRate;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public int getHeartRate() {
			return heartRate;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(
					new SensorDataField(R.string.map_widget_ant_heart_rate, R.string.beats_per_minute_short, heartRate));
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
					new SensorWidgetDataField(SensorWidgetDataFieldType.HEART_RATE, R.string.map_widget_ant_heart_rate, R.string.beats_per_minute_short, heartRate));
		}

		@NonNull
		@Override
		public String toString() {
			return "HeartRateData {" +
					"timestamp=" + timestamp +
					", heartRate=" + heartRate +
					'}';
		}
	}

	public BLEHeartRateSensor(@NonNull BLEHeartRateDevice device) {
		super(device, device.getDeviceId() + "_heart_rate");
	}

	public BLEHeartRateSensor(@NonNull BLEHeartRateDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.HEART_RATE);
	}

	@NonNull
	@Override
	public UUID getRequestedCharacteristicUUID() {
		return GattAttributes.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT;
	}

	@NonNull
	@Override
	public String getName() {
		return "Heart Rate";
	}

	@Override
	@Nullable
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastHeartRateData);
	}

	@Nullable
	public BodyPart getBodyPart() {
		return bodyPart;
	}

	@Nullable
	public HeartRateData getLastHeartRateData() {
		return lastHeartRateData;
	}

	@Override
	public void onCharacteristicRead(@NonNull BluetoothGatt gatt,
	                                 @NonNull BluetoothGattCharacteristic characteristic,
	                                 int status) {
		if (status == BluetoothGatt.GATT_SUCCESS) {
			if (GattAttributes.UUID_CHARACTERISTIC_HEART_RATE_BODY_PART.equals(characteristic.getUuid())) {
				decodeBodySensorPosition(gatt, characteristic);
			}
		}
	}

	@Override
	public void onCharacteristicChanged(@NonNull BluetoothGatt gatt,
	                                    @NonNull BluetoothGattCharacteristic characteristic) {
		UUID charaUUID = characteristic.getUuid();
		if (getRequestedCharacteristicUUID().equals(charaUUID)) {
			decodeHeartRateCharacteristic(gatt, characteristic);
		}
	}

	private void decodeBodySensorPosition(@NonNull BluetoothGatt gatt,
	                                      @NonNull BluetoothGattCharacteristic characteristic) {
		byte bodySensorPositionValue = characteristic.getValue()[0];
		this.bodyPart = bodySensorPositionValue >= BodyPart.values().length
				? BodyPart.OTHER
				: BodyPart.values()[bodySensorPositionValue];
	}

	private void decodeHeartRateCharacteristic(@NonNull BluetoothGatt gatt,
	                                           @NonNull BluetoothGattCharacteristic characteristic) {
		int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		int format;
		if ((flag & 0x01) != 0) {
			format = BluetoothGattCharacteristic.FORMAT_UINT16;
		} else {
			format = BluetoothGattCharacteristic.FORMAT_UINT8;
		}
		int heartRate = characteristic.getIntValue(format, 1);

		HeartRateData data = new HeartRateData(System.currentTimeMillis(), heartRate);
		this.lastHeartRateData = data;
		getDevice().fireSensorDataEvent(this, data);
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		HeartRateData data = lastHeartRateData;
		if (data != null) {
			json.put(SENSOR_TAG_HEART_RATE, data.heartRate);
		}
	}
}
