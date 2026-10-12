package net.osmand.plus.plugins.externalsensors.devices.sensors.ble;

import static net.osmand.util.Algorithms.DECIMAL_FORMAT;
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

public class BLEBikeSensor extends BLEAbstractSensor {

	private int firstWheelRevolutions = -1;
	private int lastWheelRevolutions = -1;
	private int lastWheelEventTime = -1;
	private float wheelCadence = -1;
	private int lastCrankRevolutions = -1;
	private int lastCrankEventTime = -1;

	private float wheelSize; //m

	private BikeCadenceData lastBikeCadenceData;
	private BikeSpeedDistanceData lastBikeSpeedDistanceData;

	public static class BikeCadenceData implements SensorData {

		private final long timestamp;
		private final float gearRatio;
		private final int cadence;

		BikeCadenceData(long timestamp, float gearRatio, int cadence) {
			this.timestamp = timestamp;
			this.gearRatio = gearRatio;
			this.cadence = cadence;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public float getGearRatio() {
			return gearRatio;
		}

		public int getCadence() {
			return cadence;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(
					new SensorDataField(R.string.external_device_characteristic_cadence, R.string.revolutions_per_minute_unit, cadence));
		}

		@NonNull
		@Override
		public List<SensorDataField> getExtraDataFields() {
			return Arrays.asList(
					new SensorDataField(R.string.shared_string_time, -1, timestamp),
					new SensorDataField(R.string.external_device_characteristic_gear_ratio, -1, gearRatio));
		}

		@Nullable
		@Override
		public List<SensorWidgetDataField> getWidgetFields() {
			return Collections.singletonList(
					new SensorWidgetDataField(SensorWidgetDataFieldType.BIKE_CADENCE, R.string.external_device_characteristic_cadence, R.string.revolutions_per_minute_unit, cadence));
		}

		@NonNull
		@Override
		public String toString() {
			return "CadenceData {" +
					"timestamp=" + timestamp +
					", gearRatio=" + gearRatio +
					", cadence=" + cadence +
					'}';
		}
	}

	public static class BikeSpeedDistanceData implements SensorData {

		private final long timestamp;
		private final float speed;
		private final float distance;
		private final float totalDistance;

		BikeSpeedDistanceData(long timestamp, float speed, float distance, float totalDistance) {
			this.timestamp = timestamp;
			this.speed = speed;
			this.distance = distance;
			this.totalDistance = totalDistance;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public float getSpeed() {
			return speed;
		}

		public float getDistance() {
			return distance;
		}

		public float getTotalDistance() {
			return totalDistance;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Arrays.asList(
					new SensorSpeedWidgetDataField(R.string.external_device_characteristic_speed, -1, speed),
					new SensorDistanceWidgetDataField(R.string.external_device_characteristic_distance, -1, distance));
		}

		@NonNull
		@Override
		public List<SensorDataField> getExtraDataFields() {
			return Arrays.asList(
					new SensorDataField(R.string.shared_string_time, -1, timestamp),
					new SensorDistanceWidgetDataField(R.string.external_device_characteristic_total_distance, -1, totalDistance));
		}

		@Nullable
		@Override
		public List<SensorWidgetDataField> getWidgetFields() {
			return Arrays.asList(
					new SensorSpeedWidgetDataField(R.string.external_device_characteristic_speed, -1, speed),
					new SensorDistanceWidgetDataField(R.string.external_device_characteristic_distance, -1, distance));
		}

		@NonNull
		@Override
		public String toString() {
			return "SpeedDistanceData {" +
					"timestamp=" + timestamp +
					", speed=" + speed +
					", distance=" + distance +
					", totalDistance=" + totalDistance +
					'}';
		}
	}

	public BLEBikeSensor(@NonNull BLEAbstractDevice device) {
		super(device, device.getDeviceId() + "_bike");
	}

	public BLEBikeSensor(@NonNull BLEAbstractDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Bike Sensor";
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
		return Arrays.asList(lastBikeCadenceData, lastBikeSpeedDistanceData);
	}

	@NonNull
	@Override
	public UUID getRequestedCharacteristicUUID() {
		return GattAttributes.UUID_CHARACTERISTIC_CYCLING_SPEED_AND_CADENCE_MEASUREMENT;
	}

	public void setWheelSize(float wheelSize) {
		this.wheelSize = wheelSize;
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
			decodeSpeedCharacteristic(gatt, characteristic);
		}
	}

	private void decodeSpeedCharacteristic(@NonNull BluetoothGatt gatt,
	                                       @NonNull BluetoothGattCharacteristic characteristic) {
		int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		boolean wheelRevPresent = (flag & 0x01) == 0x01;
		boolean crankRevPreset = (flag & 0x02) == 0x02;
		int wheelRevolutions;
		int lastWheelEventTime;
		if (wheelRevPresent) {
			wheelRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1);
			lastWheelEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);
			float circumference = wheelSize;
			if (firstWheelRevolutions < 0) {
				firstWheelRevolutions = wheelRevolutions;
			}
			if (this.lastWheelEventTime == lastWheelEventTime) {
				float totalDistance = (float) wheelRevolutions * circumference;
				float distance = (float) (wheelRevolutions - firstWheelRevolutions) * circumference; //m
				float speed = 0;
				if (lastBikeSpeedDistanceData != null) {
					speed = lastBikeSpeedDistanceData.speed;
				}
				getDevice().fireSensorDataEvent(this, createBikeSpeedDistanceData(speed, distance, totalDistance));
			} else if (lastWheelRevolutions >= 0) {
				float timeDifference;
				if (lastWheelEventTime < this.lastWheelEventTime) {
					timeDifference = (65535 + lastWheelEventTime - this.lastWheelEventTime) / 1024.0f;
				} else {
					timeDifference = (lastWheelEventTime - this.lastWheelEventTime) / 1024.0f;
				}
				float distanceDifference = (wheelRevolutions - lastWheelRevolutions) * circumference;
				float totalDistance = (float) wheelRevolutions * circumference;
				float distance = (float) (wheelRevolutions - firstWheelRevolutions) * circumference;
				float speed = (distanceDifference / timeDifference);
				wheelCadence = (wheelRevolutions - lastWheelRevolutions) * 60.0f / timeDifference;
				getDevice().fireSensorDataEvent(this, createBikeSpeedDistanceData(speed, distance, totalDistance));
			}
			lastWheelRevolutions = wheelRevolutions;
			this.lastWheelEventTime = lastWheelEventTime;

		} else if (crankRevPreset) {
			int crankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
			int lastCrankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);
			if (lastCrankRevolutions >= 0) {
				float timeDifference;
				if (lastCrankEventTime < this.lastCrankEventTime) {
					timeDifference = (65535 + lastCrankEventTime - this.lastCrankEventTime) / 1024.0f;
				} else {
					timeDifference = (lastCrankEventTime - this.lastCrankEventTime) / 1024.0f;
				}
				float crankCadence = (crankRevolutions - lastCrankRevolutions) * 60.0f / timeDifference;
				if (crankCadence > 0) {
					float gearRatio = wheelCadence / crankCadence;
					getDevice().fireSensorDataEvent(this, createBikeCadenceData(gearRatio, Math.round(crankCadence)));
				}
			}
			lastCrankRevolutions = crankRevolutions;
			this.lastCrankEventTime = lastCrankEventTime;
		}
	}

	//speed m/s, distance m
	@NonNull
	private SensorData createBikeSpeedDistanceData(float speed, float distance, float totalDistance) {
		BikeSpeedDistanceData data = new BikeSpeedDistanceData(System.currentTimeMillis(), speed, distance, totalDistance);
		lastBikeSpeedDistanceData = data;
		return data;
	}

	@NonNull
	private SensorData createBikeCadenceData(float gearRatio, int crankCadence) {
		BikeCadenceData data = new BikeCadenceData(System.currentTimeMillis(), gearRatio, crankCadence);
		lastBikeCadenceData = data;
		return data;
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		switch (widgetDataFieldType) {
			case BIKE_SPEED:
				if (lastBikeSpeedDistanceData != null) {
					json.put(SENSOR_TAG_SPEED, DECIMAL_FORMAT.format(lastBikeSpeedDistanceData.speed));
				}
				break;
			case BIKE_CADENCE:
				BikeCadenceData cadenceData = lastBikeCadenceData;
				if (cadenceData != null) {
					json.put(SENSOR_TAG_CADENCE, cadenceData.cadence);
				}
				break;
			case BIKE_DISTANCE:
				if (lastBikeSpeedDistanceData != null) {
					json.put(SENSOR_TAG_DISTANCE, lastBikeSpeedDistanceData.distance);
				}
				break;
			default:
				break;
		}
	}
}