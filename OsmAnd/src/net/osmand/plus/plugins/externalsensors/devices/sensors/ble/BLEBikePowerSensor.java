package net.osmand.plus.plugins.externalsensors.devices.sensors.ble;

import static net.osmand.util.Algorithms.DECIMAL_FORMAT;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_BIKE_POWER;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_CADENCE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_DISTANCE;
import static net.osmand.shared.gpx.PointAttributes.SENSOR_TAG_SPEED;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDistanceWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorSpeedWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.utils.FormattedValue;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLEBikePowerSensor extends BLEAbstractSensor {

	private float wheelCadence = -1;
	private int lastCrankRevolutions = -1;
	private int lastCrankEventTime = -1;


	private BikeCadenceData lastBikeCadenceData;
	private BikePowerData lastBikePowerData;



	public static class BikeCadenceData implements SensorData {

		private final long timestamp;
		private final int cadence;

		BikeCadenceData(long timestamp, int cadence) {
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
					new SensorDataField(R.string.external_device_characteristic_cadence, R.string.revolutions_per_minute_unit, cadence));
		}

		@NonNull
		@Override
		public List<SensorDataField> getExtraDataFields() {
			return Arrays.asList(
					new SensorDataField(R.string.shared_string_time, -1, timestamp));
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
					", cadence=" + cadence +
					'}';
		}
	}

	@NonNull
	private SensorData createBikeCadenceData(int crankCadence) {
		BikeCadenceData data = new BikeCadenceData(System.currentTimeMillis(), crankCadence);
		lastBikeCadenceData = data;
		return data;
	}

	public static class BikePowerData implements SensorData {

		// The estimated timestamp of when this event was triggered.
		private final long timestamp;

		// The average power calculated from sensor data. Units: W.
		private final double calculatedPower;

		private static class BikePowerDataField extends SensorWidgetDataField {

			public BikePowerDataField(int nameId, int unitNameId, @NonNull Number powerValue) {
				super(SensorWidgetDataFieldType.BIKE_POWER, nameId, unitNameId, powerValue);
			}

			@Nullable
			@Override
			public FormattedValue getFormattedValue(@NonNull OsmandApplication app) {
				float power = getNumberValue().floatValue();
				return power > 0
						? new FormattedValue(power, String.valueOf(power), "W")
						: null;
			}
		}

		BikePowerData(long timestamp,
		              double calculatedPower) {
			this.timestamp = timestamp;
			this.calculatedPower = calculatedPower;
		}

		public long getTimestamp() {
			return timestamp;
		}

		public double getCalculatedPower() {
			return calculatedPower;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			return Collections.singletonList(new BikePowerDataField(R.string.map_widget_ant_bicycle_power, -1, calculatedPower));
		}

		@NonNull
		@Override
		public List<SensorDataField> getExtraDataFields() {
			return Collections.singletonList(new SensorDataField(R.string.shared_string_time, -1, timestamp));
		}

		@Nullable
		@Override
		public List<SensorWidgetDataField> getWidgetFields() {
			return Collections.singletonList(new BikePowerDataField(R.string.map_widget_ant_bicycle_power, -1, calculatedPower));
		}

		@NonNull
		@Override
		public String toString() {
			return "BikePowerData {" +
					"timestamp=" + timestamp +
					", calculatedPower=" + calculatedPower +
					'}';
		}
	}


	public BLEBikePowerSensor(@NonNull BLEAbstractDevice device) {
		super(device, device.getDeviceId() + "_bike_pwr");
	}

	public BLEBikePowerSensor(@NonNull BLEAbstractDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	@Override
	public String getName() {
		return "Bike Power Sensor";
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Arrays.asList(
				SensorWidgetDataFieldType.BIKE_CADENCE,
				SensorWidgetDataFieldType.BIKE_POWER);
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Arrays.asList(lastBikeCadenceData, lastBikePowerData);
	}

	@NonNull
	@Override
	public UUID getRequestedCharacteristicUUID() {
		return GattAttributes.UUID_CHARACTERISTIC_CYCLING_POWER_MEASUREMENT;
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
			decodeBikePowerCharacteristic(gatt, characteristic);
		}
	}


	private void decodeBikePowerCharacteristic(@NonNull BluetoothGatt gatt,
		@NonNull BluetoothGattCharacteristic characteristic) {
			
			int offset = 0;
			int flags = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
			offset += 2;

			//decode flags
			boolean pedalPowerBalancePresent   = (flags & (1 << 0)) != 0;
			boolean pedalPowerBalanceReference = (flags & (1 << 1)) != 0;
			boolean AccumulatedTorquePresent   = (flags & (1 << 2)) != 0;
			boolean AccumulatedTorqueSource    = (flags & (1 << 3)) != 0;
			boolean WheelRevolutionDataPresent = (flags & (1 << 4)) != 0;
			boolean CrankRevolutionDataPresent = (flags & (1 << 5)) != 0;
			//note: the other flags aren't implemented yet


			//Parse power
			int power = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT16, offset);
			getDevice().fireSensorDataEvent(this, createBikePowerData(Math.round(power)));
			offset += 2;
			if (pedalPowerBalancePresent){
				offset += 1;
			}
			if (AccumulatedTorquePresent){
				offset += 2;
			}
			if (WheelRevolutionDataPresent) {
				offset += 6;
			}
			if (CrankRevolutionDataPresent) {
				int crankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
				offset += 2;
				int lastCrankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, offset);
				if (lastCrankRevolutions >= 0) {
					float timeDifference;
					if (lastCrankEventTime < this.lastCrankEventTime) {
						timeDifference = (65536 + lastCrankEventTime - this.lastCrankEventTime) / 1024.0f;
					} else {
						timeDifference = (lastCrankEventTime - this.lastCrankEventTime) / 1024.0f;
					}
					float crankCadence = (crankRevolutions - lastCrankRevolutions) * 60.0f / timeDifference;
					if (crankCadence > 0) {
						getDevice().fireSensorDataEvent(this, createBikeCadenceData(Math.round(crankCadence)));
					}
				}
				if(crankRevolutions > lastCrankRevolutions) {
					lastTimeDifferentValue = System.currentTimeMillis();
				}
				lastCrankRevolutions = crankRevolutions;
				this.lastCrankEventTime = lastCrankEventTime;
			}

		}

	@NonNull
	private SensorData createBikePowerData(double calculatedPower) {
		BikePowerData data = new BikePowerData(System.currentTimeMillis(), calculatedPower);
		lastBikePowerData = data;
		return data;
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) throws JSONException {
		switch (widgetDataFieldType) {
			case BIKE_CADENCE:
				BikeCadenceData cadenceData = lastBikeCadenceData;
				if (cadenceData != null) {
					json.put(SENSOR_TAG_CADENCE, cadenceData.cadence);
				}
				break;
			case BIKE_POWER:
				BikePowerData powerData = lastBikePowerData;
				if (powerData != null) {
					json.put(SENSOR_TAG_BIKE_POWER, powerData.getCalculatedPower());
				}
				break;
			default:
				break;
		}
	}

	@Override
	protected long getDataUpdateTimePeriod() {
		return 2000;
	}
}