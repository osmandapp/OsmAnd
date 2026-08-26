package net.osmand.plus.plugins.externalsensors.devices.ble;

import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEBikePowerSensor;

import org.apache.commons.logging.Log;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLEBikePowerDevice extends BLEAbstractDevice {
	private final BLEBikePowerSensor bikeSensor;
	private static final Log LOG = PlatformUtil.getLog(BLEBikeSCDDevice.class);

	public BLEBikePowerDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull String deviceId) {
		super(bluetoothAdapter, deviceId);
		bikeSensor = new BLEBikePowerSensor(this);
		sensors.add(bikeSensor);
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.BLE_BICYCLE_POWER;
	}

	@NonNull
	public static UUID getServiceUUID() {
		return GattAttributes.UUID_SERVICE_CYCLING_POWER;
	}

}