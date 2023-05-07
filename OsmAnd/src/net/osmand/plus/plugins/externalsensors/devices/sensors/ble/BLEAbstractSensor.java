package net.osmand.plus.plugins.externalsensors.devices.sensors.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
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

	@SuppressLint("MissingPermission")
	private void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
	                                          boolean enabled) {
		BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
		BluetoothGatt bluetoothGatt = getBluetoothGatt();
		if (bluetoothAdapter == null || bluetoothGatt == null) {
			return;
		}
		bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		if (GattAttributes.SUPPORTED_CHARACTERISTICS.contains(characteristic.getUuid())) {
			BluetoothGattDescriptor descriptor = characteristic.getDescriptor(GattAttributes.UUID_CHARACTERISTIC_CLIENT_CONFIG);
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			bluetoothGatt.writeDescriptor(descriptor);
		}
	}

	@SuppressLint("MissingPermission")
	private void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
		BluetoothGatt bluetoothGatt = getBluetoothGatt();
		if (bluetoothAdapter == null || bluetoothGatt == null) {
			return;
		}
		bluetoothGatt.readCharacteristic(characteristic);
	}

	@NonNull
	public abstract UUID getRequestedCharacteristicUUID();

	public void requestCharacteristic(@NonNull List<BluetoothGattCharacteristic> characteristics) {
		for (BluetoothGattCharacteristic characteristic : characteristics) {
			if (getRequestedCharacteristicUUID().equals(characteristic.getUuid())) {
				final int characteristicProp = characteristic.getProperties();
				if ((characteristicProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
					if (notifyCharacteristic != null) {
						setCharacteristicNotification(notifyCharacteristic, false);
						notifyCharacteristic = null;
					}
					readCharacteristic(characteristic);
				}
				if ((characteristicProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
					notifyCharacteristic = characteristic;
					setCharacteristicNotification(characteristic, true);
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
