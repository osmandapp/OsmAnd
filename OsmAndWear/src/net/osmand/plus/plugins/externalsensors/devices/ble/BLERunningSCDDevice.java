package net.osmand.plus.plugins.externalsensors.devices.ble;

import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLERunningSensor;

import java.util.UUID;

public class BLERunningSCDDevice extends BLEAbstractDevice {

	public BLERunningSCDDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull String deviceId) {
		super(bluetoothAdapter, deviceId);
		sensors.add(new BLERunningSensor(this));
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.BLE_RUNNING_SCDS;
	}

	@NonNull
	public static UUID getServiceUUID() {
		return GattAttributes.UUID_SERVICE_RUNNING_SPEED_AND_CADENCE;
	}
}