package net.osmand.plus.plugins.externalsensors.devices.ble;

import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEHeartRateSensor;

import java.util.UUID;

public class BLEHeartRateDevice extends BLEAbstractDevice {

	public BLEHeartRateDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull String deviceId) {
		super(bluetoothAdapter, deviceId);
		sensors.add(new BLEHeartRateSensor(this));
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.BLE_HEART_RATE;
	}

	@NonNull
	public static UUID getServiceUUID() {
		return GattAttributes.UUID_SERVICE_HEART_RATE;
	}
}