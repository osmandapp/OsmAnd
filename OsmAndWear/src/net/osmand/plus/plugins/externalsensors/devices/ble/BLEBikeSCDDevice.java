package net.osmand.plus.plugins.externalsensors.devices.ble;

import android.bluetooth.BluetoothAdapter;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEBikeSensor;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BLEBikeSCDDevice extends BLEAbstractDevice {
	private final BLEBikeSensor bikeSensor;
	private static final Log LOG = PlatformUtil.getLog(BLEBikeSCDDevice.class);

	public BLEBikeSCDDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull String deviceId) {
		super(bluetoothAdapter, deviceId);
		bikeSensor = new BLEBikeSensor(this);
		sensors.add(bikeSensor);
	}

	@NonNull
	@Override
	public List<DeviceChangeableProperty> getChangeableProperties() {
		return Collections.singletonList(DeviceChangeableProperty.WHEEL_CIRCUMFERENCE);
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
	public void setChangeableProperty(DeviceChangeableProperty property, String value) {
		if (property == DeviceChangeableProperty.WHEEL_CIRCUMFERENCE) {
			// FIXME copy paste
			try {
				setWheelCircumference(Float.parseFloat(value));
			} catch(RuntimeException e) {
				LOG.error(e);
			}
		} else {
			super.setChangeableProperty(property, value);
		}
	}
}