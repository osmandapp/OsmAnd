package net.osmand.plus.plugins.externalsensors.devices.sensors.ble;

import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_CADENCE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_DISTANCE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_SPEED;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDistanceWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorSpeedWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLERunningSensor extends BLEAbstractSensor {

	private boolean running;

	private RunningCadenceData lastRunningCadenceData;
	private RunningSpeedData lastRunningSpeedData;
	private RunningDistanceData lastRunningDistanceData;
	private RunningStrideLengthData lastRunningStrideLengthData;

	public static class RunningCadenceData implements SensorData {

		private final long timestamp;
		private final int cadence;

		RunningCadenceData(long timestamp, int cadence) {
			this.timestamp = timestamp;
			this.cadence = cadence;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public int getCadence() {
			return cadence;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(
					new SensorDataField(R.string.external_device_characteristic_cadence, R.string.steps_per_minute_unit, cadence));
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
					new SensorWidgetDataField(SensorWidgetDataFieldType.BIKE_CADENCE, R.string.external_device_characteristic_cadence, R.string.steps_per_minute_unit, cadence));
		}

		@NonNull
		@Override
		public String toString() {
			return "CadenceData {" +
					"timestamp=" + timestamp +
					", cadence=" + cadence +
					'}';
		}
	}

	public static class RunningSpeedData implements SensorData {

		private final long timestamp;
		private final float speed;

		RunningSpeedData(long timestamp, float speed) {
			this.timestamp = timestamp;
			this.speed = speed;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public float getSpeed() {
			return speed;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(new SensorSpeedWidgetDataField(R.string.external_device_characteristic_speed, -1, speed));
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
					new SensorSpeedWidgetDataField(R.string.external_device_characteristic_speed, -1, speed));
		}

		@NonNull
		@Override
		public String toString() {
			return "CadenceData {" +
					"timestamp=" + timestamp +
					", speed=" + speed +
					'}';
		}
	}

	public static class RunningDistanceData implements SensorData {

		private final long timestamp;
		private final float totalDistance;

		RunningDistanceData(long timestamp, float totalDistance) {
			this.timestamp = timestamp;
			this.totalDistance = totalDistance;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public float getTotalDistance() {
			return totalDistance;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(new SensorDistanceWidgetDataField(R.string.external_device_characteristic_total_distance, -1, totalDistance));
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
					new SensorDistanceWidgetDataField(R.string.external_device_characteristic_total_distance, -1, totalDistance));
		}

		@NonNull
		@Override
		public String toString() {
			return "CadenceData {" +
					"timestamp=" + timestamp +
					", totalDistance=" + totalDistance +
					'}';
		}
	}

	public static class RunningStrideLengthData implements SensorData {

		private final long timestamp;
		private final float strideLength;

		RunningStrideLengthData(long timestamp, float strideLength) {
			this.timestamp = timestamp;
			this.strideLength = strideLength;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public float getStrideLength() {
			return strideLength;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(new SensorDistanceWidgetDataField(R.string.external_device_characteristic_stride_length, -1, strideLength));
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
					new SensorDistanceWidgetDataField(R.string.external_device_characteristic_stride_length, -1, strideLength));
		}

		@NonNull
		@Override
		public String toString() {
			return "CadenceData {" +
					"timestamp=" + timestamp +
					", strideLength=" + strideLength +
					'}';
		}
	}

	public BLERunningSensor(@NonNull BLEAbstractDevice device) {
		super(device, device.getDeviceId() + "_running");
	}

	public BLERunningSensor(@NonNull BLEAbstractDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Running Sensor";
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Arrays.asList(
				SensorWidgetDataFieldType.BIKE_SPEED,
				SensorWidgetDataFieldType.BIKE_CADENCE,
				SensorWidgetDataFieldType.BIKE_DISTANCE);
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Arrays.asList(lastRunningCadenceData, lastRunningSpeedData,
				lastRunningDistanceData, lastRunningStrideLengthData);
	}

	@NonNull
	@Override
	public UUID getRequestedCharacteristicUUID() {
		return GattAttributes.UUID_CHARACTERISTIC_RUNNING_SPEED_AND_CADENCE_MEASUREMENT;
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
			decodeRunningSpeedCharacteristic(gatt, characteristic);
		}
	}

	public boolean isRunning() {
		return running;
	}

	private void decodeRunningSpeedCharacteristic(@NonNull BluetoothGatt gatt,
	                                              @NonNull BluetoothGattCharacteristic characteristic) {
		int flags = characteristic.getValue()[0];

		boolean strideLengthPresent = (flags & 0x01) != 0;
		boolean totalDistancePreset = (flags & 0x02) != 0;
		running = (flags & 0x04) != 0;

		float speed = (float) characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1) / 3.6f * 256.0f;
		int cadence = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3);
		getDevice().fireSensorDataEvent(this, createRunningSpeedData(speed));
		getDevice().fireSensorDataEvent(this, createRunningCadenceData(cadence));

		float strideLength = -1;
		if (strideLengthPresent) {
			strideLength = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 4);
			getDevice().fireSensorDataEvent(this, createRunningStrideLengthData(strideLength));
		}

		float totalDistance = -1;
		if (totalDistancePreset) {
			totalDistance = (float) characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, strideLengthPresent ? 6 : 4) * 3.6f / 10.0f;
			getDevice().fireSensorDataEvent(this, createRunningDistanceData(totalDistance));
		}
	}

	@NonNull
	private SensorData createRunningSpeedData(float speed) {
		RunningSpeedData data = new RunningSpeedData(System.currentTimeMillis(), speed);
		lastRunningSpeedData = data;
		return data;
	}

	@NonNull
	private SensorData createRunningCadenceData(int cadence) {
		RunningCadenceData data = new RunningCadenceData(System.currentTimeMillis(), cadence);
		lastRunningCadenceData = data;
		return data;
	}

	@NonNull
	private SensorData createRunningDistanceData(float totalDistance) {
		RunningDistanceData data = new RunningDistanceData(System.currentTimeMillis(), totalDistance);
		lastRunningDistanceData = data;
		return data;
	}

	@NonNull
	private SensorData createRunningStrideLengthData(float strideLength) {
		RunningStrideLengthData data = new RunningStrideLengthData(System.currentTimeMillis(), strideLength);
		lastRunningStrideLengthData = data;
		return data;
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		switch (widgetDataFieldType) {
			case BIKE_SPEED:
				if (lastRunningSpeedData != null) {
					json.put(SENSOR_TAG_SPEED, lastRunningSpeedData.speed);
				}
				break;
			case BIKE_CADENCE:
				if (lastRunningCadenceData != null) {
					json.put(SENSOR_TAG_CADENCE, lastRunningCadenceData.cadence);
				}
				break;
			case BIKE_DISTANCE:
				if (lastRunningDistanceData != null) {
					json.put(SENSOR_TAG_DISTANCE, lastRunningDistanceData.totalDistance);
				}
				break;
			default:
				break;
		}
	}
}