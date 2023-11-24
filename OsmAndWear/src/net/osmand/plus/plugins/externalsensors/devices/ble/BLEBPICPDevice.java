package net.osmand.plus.plugins.externalsensors.devices.ble;

import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEBloodPressureSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLECuffPressureSensor;

import java.util.UUID;

public class BLEBPICPDevice extends BLEAbstractDevice {

	public BLEBPICPDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull String deviceId) {
		super(bluetoothAdapter, deviceId);
		sensors.add(new BLEBloodPressureSensor(this));
		sensors.add(new BLECuffPressureSensor(this));
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.BLE_BLOOD_PRESSURE;
	}

	@NonNull
	public static UUID getServiceUUID() {
		return GattAttributes.UUID_SERVICE_BLOOD_PRESSURE;
	}
}