package net.osmand.plus.plugins.externalsensors.devices.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanRecord;
import android.content.Context;
import android.util.SparseArray;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.plugins.externalsensors.DeviceType;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionState;
import net.osmand.plus.plugins.externalsensors.devices.ble.TpmsAdvertisementDecoder.Reading;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLETpmsSensor;
import net.osmand.util.Algorithms;

import java.util.UUID;

/**
 * Passive tire pressure sensor: it is never connected over GATT. "Connected" means OsmAnd listens
 * to its advertisements, see {@link net.osmand.plus.plugins.externalsensors.TpmsScanner}.
 */
public class BLETpmsDevice extends BLEAbstractDevice {

	private static final UUID SERVICE_UUID = UUID.fromString("0000fbb0-0000-1000-8000-00805f9b34fb");

	private final BLETpmsSensor tpmsSensor;

	public BLETpmsDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull String deviceId) {
		super(bluetoothAdapter, deviceId);
		sensors.clear();
		tpmsSensor = new BLETpmsSensor(this);
		sensors.add(tpmsSensor);
	}

	@Nullable
	public static BLETpmsDevice createDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull ScanRecord scanRecord,
	                                         @NonNull String address, @Nullable String name, int rssi) {
		if (decode(scanRecord) == null) {
			return null;
		}
		BLETpmsDevice device = new BLETpmsDevice(bluetoothAdapter, address);
		device.deviceName = !Algorithms.isEmpty(name) ? name : "TPMS " + address.substring(Math.max(0, address.length() - 5));
		device.rssi = rssi;
		return device;
	}

	@NonNull
	public static UUID getServiceUUID() {
		return SERVICE_UUID;
	}

	@NonNull
	@Override
	public DeviceType getDeviceType() {
		return DeviceType.BLE_TPMS;
	}

	public void onAdvertisement(@NonNull ScanRecord scanRecord, int rssi) {
		Reading reading = decode(scanRecord);
		if (reading != null) {
			this.rssi = rssi;
			if (reading.getBatteryPercent() != null) {
				batteryLevel = reading.getBatteryPercent();
			}
			tpmsSensor.onReading(reading);
		}
	}

	@Nullable
	private static Reading decode(@NonNull ScanRecord scanRecord) {
		SparseArray<byte[]> data = scanRecord.getManufacturerSpecificData();
		if (data != null) {
			for (int i = 0; i < data.size(); i++) {
				Reading reading = TpmsAdvertisementDecoder.decode(data.keyAt(i), data.valueAt(i));
				if (reading != null) {
					return reading;
				}
			}
		}
		return null;
	}

	@Override
	public boolean connect(@NonNull Context context, @Nullable Activity activity) {
		if (isDisconnected()) {
			setCurrentState(DeviceConnectionState.CONNECTED);
			for (DeviceListener listener : listeners) {
				listener.onDeviceConnect(this, DeviceConnectionResult.SUCCESS, null);
			}
		}
		return true;
	}

	@Override
	public boolean disconnect() {
		boolean wasConnected = !isDisconnected();
		setCurrentState(DeviceConnectionState.DISCONNECTED);
		return wasConnected;
	}
}
