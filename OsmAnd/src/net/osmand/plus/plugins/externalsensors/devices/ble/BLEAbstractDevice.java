package net.osmand.plus.plugins.externalsensors.devices.ble;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.PlatformUtil;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionState;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEAbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEBatterySensor;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public abstract class BLEAbstractDevice extends AbstractDevice<BLEAbstractSensor> {

	private static final Log LOG = PlatformUtil.getLog(BLEAbstractDevice.class);

	protected BluetoothAdapter bluetoothAdapter;
	protected BluetoothDevice device;
	protected BluetoothGatt bluetoothGatt;

	public BLEAbstractDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull String deviceId) {
		super(deviceId);
		this.bluetoothAdapter = bluetoothAdapter;
		sensors.add(new BLEBatterySensor(this));
	}

	@Nullable
	public static BLEAbstractDevice createDeviceByUUID(@NonNull BluetoothAdapter bluetoothAdapter,
	                                                   @NonNull UUID uuid, @NonNull String address) {
		if (BLEHeartRateDevice.getServiceUUID().equals(uuid)) {
			return new BLEHeartRateDevice(bluetoothAdapter, address);
		} else if (BLETemperatureDevice.getServiceUUID().equals(uuid)) {
			return new BLETemperatureDevice(bluetoothAdapter, address);
		} else if (BLEBikeSCDDevice.getServiceUUID().equals(uuid)) {
			return new BLEBikeSCDDevice(bluetoothAdapter, address);
		} else if (BLERunningSCDDevice.getServiceUUID().equals(uuid)) {
			return new BLERunningSCDDevice(bluetoothAdapter, address);
		} else if (BLEBPICPDevice.getServiceUUID().equals(uuid)) {
			return new BLEBPICPDevice(bluetoothAdapter, address);
		}
		return null;
	}

	@NonNull
	@Override
	public String getName() {
		String name = device != null ? device.getName() : null;
		if (name == null) {
			name = getClass().getSimpleName();
		}
		return name;
	}

	@Nullable
	public BluetoothAdapter getBluetoothAdapter() {
		return bluetoothAdapter;
	}

	@Nullable
	public BluetoothDevice getDevice() {
		return device;
	}

	@Nullable
	public BluetoothGatt getBluetoothGatt() {
		return bluetoothGatt;
	}

	private void fireDeviceConnectedEvent() {
		for (DeviceListener listener : listeners) {
			listener.onDeviceConnect(this, DeviceConnectionResult.SUCCESS, null);
		}
	}

	private void fireDeviceDisconnectedEvent() {
		for (DeviceListener listener : listeners) {
			listener.onDeviceDisconnect(this);
		}
	}

	@Nullable
	private List<BluetoothGattService> getSupportedGattServices() {
		return bluetoothGatt == null ? null : bluetoothGatt.getServices();
	}

	@NonNull
	private List<BluetoothGattCharacteristic> getCharacteristics() {
		List<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
		List<BluetoothGattService> services = getSupportedGattServices();
		if (services != null) {
			for (BluetoothGattService gattService : services) {
				List<BluetoothGattCharacteristic> gattCharas = gattService.getCharacteristics();
				characteristics.addAll(gattCharas);
			}
		}
		return characteristics;
	}

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@SuppressLint("MissingPermission")
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			LOG.debug("status: " + status);
			LOG.debug("newState: " + newState);
			if (status == GATT_SUCCESS) {
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					int bondState = device.getBondState();
					fireDeviceConnectedEvent();

					if (bondState == BOND_NONE || bondState == BOND_BONDED) {
						LOG.debug("Discovering services");
						boolean result = gatt.discoverServices();

						if (!result) {
							LOG.error("DiscoverServices failed to start");
						}
					} else if (bondState == BOND_BONDING) {
						LOG.debug("Waiting for bonding to complete");
					}
					state = DeviceConnectionState.CONNECTED;
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					fireDeviceDisconnectedEvent();
					gatt.close();
					bluetoothGatt = null;
					state = DeviceConnectionState.DISCONNECTED;
				}
			} else {
				fireDeviceDisconnectedEvent();
				gatt.close();
				bluetoothGatt = null;
				state = DeviceConnectionState.DISCONNECTED;
			}
		}

		@SuppressLint("MissingPermission")
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == 129) {
				LOG.error("Service discovery failed");
				gatt.disconnect();
				return;
			}
			if (status == BluetoothGatt.GATT_SUCCESS) {
				List<BluetoothGattService> services = gatt.getServices();
				LOG.debug(String.format(Locale.US, "discovered %d services for '%s'", services.size(), gatt.getDevice().getName()));

				List<BluetoothGattCharacteristic> characteristics = getCharacteristics();
				for (BLEAbstractSensor sensor : sensors) {
					sensor.requestCharacteristic(characteristics);
				}
				gatt.readRemoteRssi();
			} else {
				LOG.debug("onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
		                                 BluetoothGattCharacteristic characteristic,
		                                 int status) {
			for (BLEAbstractSensor sensor : sensors) {
				sensor.onCharacteristicRead(gatt, characteristic, status);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
		                                    BluetoothGattCharacteristic characteristic) {
			for (BLEAbstractSensor sensor : sensors) {
				sensor.onCharacteristicChanged(gatt, characteristic);
			}
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			super.onReadRemoteRssi(gatt, rssi, status);
			BLEAbstractDevice.this.rssi = rssi;
			LOG.debug("'" + getName() + "' <Rssi>: " + rssi);
		}
	};

	@SuppressLint("MissingPermission")
	@Override
	public boolean connect(@NonNull Context context, @Nullable Activity activity) {
		if (isDisconnected()) {
			if (bluetoothAdapter == null) {
				LOG.debug("BluetoothAdapter not initialized");
				return false;
			}

			if (bluetoothGatt != null) {
				if (bluetoothGatt.connect()) {
					state = DeviceConnectionState.CONNECTING;
					return true;
				} else {
					return false;
				}
			}

			device = bluetoothAdapter.getRemoteDevice(deviceId);

			if (device == null) {
				LOG.debug("Device not found");
				return false;
			}

			bluetoothGatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE);
			LOG.debug("Trying to create new connection");
			state = DeviceConnectionState.CONNECTING;
		}
		return true;
	}

	@SuppressLint("MissingPermission")
	@Override
	public void disconnect() {
		state = DeviceConnectionState.DISCONNECTED;
		if (bluetoothAdapter == null || bluetoothGatt == null) {
			LOG.debug("BluetoothAdapter not initialized");
			return;
		}
		bluetoothGatt.disconnect();
		device = null;
	}

	@Override
	public void fireSensorDataEvent(@NonNull AbstractSensor sensor, @NonNull SensorData data) {
		if (data instanceof BLEBatterySensor.BatteryData) {
			batteryLevel = ((BLEBatterySensor.BatteryData) data).getBatteryLevel();
		}
		super.fireSensorDataEvent(sensor, data);
	}
}
