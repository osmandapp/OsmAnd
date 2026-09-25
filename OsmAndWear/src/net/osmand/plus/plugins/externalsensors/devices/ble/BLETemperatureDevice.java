package net.osmand.plus.plugins.externalsensors.devices.ble;

import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLETemperatureSensor;

import java.util.UUID;

public class BLETemperatureDevice extends BLEAbstractDevice {

	public BLETemperatureDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull String deviceId) {
		super(bluetoothAdapter, deviceId);
		sensors.add(new BLETemperatureSensor(this));
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.BLE_TEMPERATURE;
	}

	@NonNull
	public static UUID getServiceUUID() {
		return GattAttributes.UUID_SERVICE_TEMPERATURE;
	}
}