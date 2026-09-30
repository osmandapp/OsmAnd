package net.osmand.plus.plugins.externalsensors.devices.sensors.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLETpmsDevice;
import net.osmand.plus.plugins.externalsensors.devices.ble.TpmsAdvertisementDecoder.Reading;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataField;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLETpmsSensor extends BLEAbstractSensor {

	// A parked wheel sleeps and may stay silent for many minutes
	private static final long DATA_UPDATE_TIME_PERIOD = 15 * 60 * 1000;

	private TpmsData lastTpmsData;

	public static class TpmsData implements SensorData {

		private final long timestamp;
		private final Reading reading;

		TpmsData(long timestamp, @NonNull Reading reading) {
			this.timestamp = timestamp;
			this.reading = reading;
		}

		@NonNull
		public Reading getReading() {
			return reading;
		}

		@NonNull
		@Override
		public List<SensorDataField> getDataFields() {
			List<SensorDataField> fields = new ArrayList<>();
			fields.add(new SensorDataField(R.string.tire_pressure, R.string.kpa_unit, Math.round(reading.getPressureKpa())));
			if (reading.getTemperatureC() != null) {
				fields.add(new SensorDataField(R.string.external_device_characteristic_temperature, R.string.degree_celsius, reading.getTemperatureC()));
			}
			return fields;
		}

		@NonNull
		@Override
		public List<SensorDataField> getExtraDataFields() {
			return Collections.singletonList(new SensorDataField(R.string.shared_string_time, -1, timestamp));
		}

		@Nullable
		@Override
		public List<SensorWidgetDataField> getWidgetFields() {
			return Collections.singletonList(new SensorWidgetDataField(SensorWidgetDataFieldType.TIRE_PRESSURE,
					R.string.tire_pressure, R.string.kpa_unit, Math.round(reading.getPressureKpa())));
		}

		@NonNull
		@Override
		public String toString() {
			return "TpmsData {timestamp=" + timestamp + ", reading=" + reading + '}';
		}
	}

	public BLETpmsSensor(@NonNull BLETpmsDevice device) {
		super(device, device.getDeviceId() + "_tpms");
	}

	public void onReading(@NonNull Reading reading) {
		TpmsData data = new TpmsData(System.currentTimeMillis(), reading);
		lastTpmsData = data;
		lastTimeDifferentValue = data.timestamp;
		getDevice().fireSensorDataEvent(this, data);
	}

	@Override
	protected long getDataUpdateTimePeriod() {
		return DATA_UPDATE_TIME_PERIOD;
	}

	@NonNull
	@Override
	public List<SensorWidgetDataFieldType> getSupportedWidgetDataFieldTypes() {
		return Collections.singletonList(SensorWidgetDataFieldType.TIRE_PRESSURE);
	}

	@NonNull
	@Override
	public UUID getRequestedCharacteristicUUID() {
		// not used: TPMS data comes from advertisements, there is no GATT connection
		return BLETpmsDevice.getServiceUUID();
	}

	@NonNull
	@Override
	public String getName() {
		return "Tire pressure";
	}

	@Nullable
	@Override
	public List<SensorData> getLastSensorDataList() {
		return Collections.singletonList(lastTpmsData);
	}

	@Override
	public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status) {
	}

	@Override
	public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic) {
	}

	@Override
	public void writeSensorDataToJson(@NonNull JSONObject json, @NonNull SensorWidgetDataFieldType widgetDataFieldType) {
	}
}
