package net.osmand.plus.plugins.externalsensors.devices.ble;

import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperties;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEBikeSensor;
import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLEBikeSCDDevice extends BLEAbstractDevice {
	private final BLEBikeSensor bikeSensor;

	public BLEBikeSCDDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull String deviceId) {
		super(bluetoothAdapter, deviceId);
		bikeSensor = new BLEBikeSensor(this);
		sensors.add(bikeSensor);
	}

	@NonNull
	@Override
	public List<DeviceChangeableProperties> getChangeableProperties() {
		return Collections.singletonList(DeviceChangeableProperties.WHEEL_CIRCUMFERENCE);
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.BLE_BICYCLE_SCD;
	}

	@NonNull
	public static UUID getServiceUUID() {
		return GattAttributes.UUID_SERVICE_CYCLING_SPEED_AND_CADENCE;
	}

	public void setWheelCircumference(float wheelCircumference) {
		bikeSensor.setWheelSize(wheelCircumference);
	}

	@Override
	public void setChangeableProperty(DeviceChangeableProperties property, String value) {
		if (property == DeviceChangeableProperties.WHEEL_CIRCUMFERENCE && Algorithms.isFloat(value, true)) {
			setWheelCircumference(Float.parseFloat(value));
		} else {
			super.setChangeableProperty(property, value);
		}
	}
}