package net.osmand.plus.plugins.externalsensors.devices.sensors.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;

import org.apache.commons.logging.Log;

import java.util.List;
import java.util.UUID;

public abstract class BLEAbstractSensor extends AbstractSensor {

	private static final Log LOG = PlatformUtil.getLog(BLEAbstractSensor.class);

	private BluetoothGattCharacteristic notifyCharacteristic;

	public BLEAbstractSensor(@NonNull BLEAbstractDevice device, @NonNull String sensorId) {
		super(device, sensorId);
	}

	@NonNull
	protected BLEAbstractDevice getBLEDevice() {
		return (BLEAbstractDevice) device;
	}

	@Nullable
	protected BluetoothGatt getBluetoothGatt() {
		return getBLEDevice().getBluetoothGatt();
	}

	@Nullable
	protected BluetoothAdapter getBluetoothAdapter() {
		return getBLEDevice().getBluetoothAdapter();
	}

	@Nullable
	private List<BluetoothGattService> getSupportedGattServices() {
		BluetoothGatt bluetoothGatt = getBLEDevice().getBluetoothGatt();
		return bluetoothGatt == null ? null : getBLEDevice().getBluetoothGatt().getServices();
	}

	@NonNull
	public abstract UUID getRequestedCharacteristicUUID();

	public void requestCharacteristic(@NonNull List<BluetoothGattCharacteristic> characteristics) {
		for (BluetoothGattCharacteristic characteristic : characteristics) {
			if (getRequestedCharacteristicUUID().equals(characteristic.getUuid())) {
				BLEAbstractDevice bleDevice = getBLEDevice();
				final int characteristicProp = characteristic.getProperties();
				if ((characteristicProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
					if (notifyCharacteristic != null) {
						bleDevice.setCharacteristicNotification(notifyCharacteristic, false);
						notifyCharacteristic = null;
					}
					bleDevice.readCharacteristic(characteristic);
				}
				if ((characteristicProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
					notifyCharacteristic = characteristic;
					bleDevice.setCharacteristicNotification(characteristic, true);
				}
				break;
			}
		}
	}

	public abstract void onCharacteristicRead(
			@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, int status);

	public abstract void onCharacteristicChanged(
			@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic);
}
