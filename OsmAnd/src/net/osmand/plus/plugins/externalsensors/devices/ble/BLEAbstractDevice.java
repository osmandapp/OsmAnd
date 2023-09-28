package net.osmand.plus.plugins.externalsensors.devices.ble;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;

import net.osmand.PlatformUtil;
import net.osmand.plus.plugins.externalsensors.GattAttributes;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionState;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEAbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.ble.BLEBatterySensor;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

public abstract class BLEAbstractDevice extends AbstractDevice<BLEAbstractSensor> {

	private static final Log LOG = PlatformUtil.getLog(BLEAbstractDevice.class);

	protected BluetoothAdapter bluetoothAdapter;
	protected BluetoothDevice device;
	protected BluetoothGatt bluetoothGatt;

	private final Handler callbackHandler = new Handler();
	private final Handler mainHandler = new Handler(Looper.getMainLooper());
	private final Queue<Runnable> commandQueue = new ConcurrentLinkedQueue<>();
	private boolean commandQueueBusy;

	public BLEAbstractDevice(@NonNull BluetoothAdapter bluetoothAdapter, @NonNull String deviceId) {
		super(deviceId);
		this.bluetoothAdapter = bluetoothAdapter;
		sensors.add(new BLEBatterySensor(this));
	}

	@Nullable
	public static BLEAbstractDevice createDeviceByUUID(@NonNull BluetoothAdapter bluetoothAdapter,
	                                                   @NonNull UUID uuid, @NonNull String address,
	                                                   @NonNull String name, int rssi) {
		BLEAbstractDevice device = null;
		if (BLEHeartRateDevice.getServiceUUID().equals(uuid)) {
			device = new BLEHeartRateDevice(bluetoothAdapter, address);
		} else if (BLETemperatureDevice.getServiceUUID().equals(uuid)) {
			device = new BLETemperatureDevice(bluetoothAdapter, address);
		} else if (BLEBikeSCDDevice.getServiceUUID().equals(uuid)) {
			device = new BLEBikeSCDDevice(bluetoothAdapter, address);
		} else if (BLERunningSCDDevice.getServiceUUID().equals(uuid)) {
			device = new BLERunningSCDDevice(bluetoothAdapter, address);
		} else if (BLEBPICPDevice.getServiceUUID().equals(uuid)) {
			device = new BLEBPICPDevice(bluetoothAdapter, address);
		}
		if (device != null) {
			device.deviceName = name;
			device.rssi = rssi;
		}
		return device;
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

					if (bondState == BOND_NONE || bondState == BOND_BONDED) {
						LOG.debug("Discovering services");
						boolean result = gatt.discoverServices();

						if (!result) {
							LOG.error("DiscoverServices failed to start");
						}
					} else if (bondState == BOND_BONDING) {
						LOG.debug("Waiting for bonding to complete");
					}
					setCurrentState(DeviceConnectionState.CONNECTED);
					fireDeviceConnectedEvent();
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					fireDeviceDisconnectedEvent();
					gatt.close();
					bluetoothGatt = null;
					setCurrentState(DeviceConnectionState.DISCONNECTED);
				}
			} else {
				fireDeviceDisconnectedEvent();
				gatt.close();
				bluetoothGatt = null;
				setCurrentState(DeviceConnectionState.DISCONNECTED);
			}
		}

		@SuppressLint("MissingPermission")
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == 129) {
				LOG.error("Service discovery failed");
				gatt.disconnect();
				setCurrentState(DeviceConnectionState.DISCONNECTED);
				return;
			}
			if (status == BluetoothGatt.GATT_SUCCESS) {
				List<BluetoothGattService> services = gatt.getServices();
				LOG.debug(String.format(Locale.US, "discovered %d services for '%s'", services.size(), gatt.getDevice().getName()));

				List<BluetoothGattCharacteristic> characteristics = getCharacteristics();
				for (BLEAbstractSensor sensor : sensors) {
					sensor.requestCharacteristic(characteristics);
				}
				enqueueCommand(() -> {
					if (!gatt.readRemoteRssi()) {
						completedCommand();
					}
				});
			} else {
				LOG.debug("onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
		                                 BluetoothGattCharacteristic characteristic,
		                                 int status) {
			if (status != GATT_SUCCESS) {
				LOG.error("ERROR: Read failed for characteristic: " + characteristic.getUuid() + ", status " + status);
				completedCommand();
				return;
			}
			callbackHandler.post(() -> {
				for (BLEAbstractSensor sensor : sensors) {
					sensor.onCharacteristicRead(gatt, characteristic, status);
				}
			});
			completedCommand();
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
		                                    BluetoothGattCharacteristic characteristic) {
			callbackHandler.post(() -> {
				for (BLEAbstractSensor sensor : sensors) {
					sensor.onCharacteristicChanged(gatt, characteristic);
				}
			});
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			super.onReadRemoteRssi(gatt, rssi, status);
			BLEAbstractDevice.this.rssi = rssi;
			LOG.debug("'" + getName() + "' <Rssi>: " + rssi);
			completedCommand();
		}

		@Override
		public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
			super.onDescriptorWrite(gatt, descriptor, status);
			completedCommand();
		}
	};

	@SuppressLint("MissingPermission")
	@Override
	public boolean connect(@NonNull Context context, @Nullable Activity activity) {
		if (!AndroidUtils.hasBLEPermission(activity)) {
			LOG.error("Try to connect " + deviceName + " while no ble permission");
			return false;
		}
		if (isDisconnected()) {
			if (bluetoothAdapter == null) {
				LOG.debug("BluetoothAdapter not initialized");
				return false;
			}

			if (bluetoothGatt != null) {
				if (bluetoothGatt.connect()) {
					setCurrentState(DeviceConnectionState.CONNECTING);
					for (DeviceListener listener : listeners) {
						listener.onDeviceConnecting(this);
					}
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

			bluetoothGatt = device.connectGatt(context, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
			LOG.debug("Trying to create new connection");
			setCurrentState(DeviceConnectionState.CONNECTING);
			for (DeviceListener listener : listeners) {
				listener.onDeviceConnecting(this);
			}
		}
		return true;
	}

	@SuppressLint("MissingPermission")
	@Override
	public boolean disconnect() {
		setCurrentState(DeviceConnectionState.DISCONNECTED);
		if (bluetoothAdapter == null || bluetoothGatt == null) {
			LOG.debug("BluetoothAdapter not initialized");
			return false;
		}
		bluetoothGatt.disconnect();
		device = null;
		return true;
	}

	@Override
	public void fireSensorDataEvent(@NonNull AbstractSensor sensor, @NonNull SensorData data) {
		if (data instanceof BLEBatterySensor.BatteryData) {
			batteryLevel = ((BLEBatterySensor.BatteryData) data).getBatteryLevel();
		}
		super.fireSensorDataEvent(sensor, data);
	}

	@SuppressLint("MissingPermission")
	public void readCharacteristic(@NonNull BluetoothGattCharacteristic characteristic) {
		if (bluetoothAdapter == null || bluetoothGatt == null) {
			return;
		}
		enqueueCommand(() -> {
			if (!bluetoothGatt.readCharacteristic(characteristic)) {
				LOG.error("Device readCharacteristic failed " + getName());
				completedCommand();
			}
		});
	}

	@SuppressLint("MissingPermission")
	public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
	                                          boolean enabled) {
		if (bluetoothAdapter == null || bluetoothGatt == null) {
			return;
		}
		boolean res = bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
		if (!res) {
			LOG.error("Device setCharacteristicNotification failed " + getName());
		}

		BluetoothGattDescriptor descriptor =
				characteristic.getDescriptor(GattAttributes.UUID_CHARACTERISTIC_CLIENT_CONFIG);
		descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
		enqueueCommand(() -> {
			if (!bluetoothGatt.writeDescriptor(descriptor)) {
				LOG.error("Device writeDescriptor failed " + getName());
				completedCommand();
			}
		});
	}

	private boolean enqueueCommand(@NonNull Runnable command) {
		final boolean result = commandQueue.add(command);
		if (result) {
			nextCommand();
		} else {
			LOG.error("Could not enqueue BLE command");
		}
		return result;
	}

	private void nextCommand() {
		// If there is still a command being executed then bail out
		if (commandQueueBusy) {
			return;
		}

		// Check if we still have a valid gatt object
		if (bluetoothGatt == null) {
			LOG.error("ERROR: GATT is 'null' for peripheral '" + getDeviceId() + "', clearing command queue");
			commandQueue.clear();
			commandQueueBusy = false;
			return;
		}

		// Execute the next command in the queue
		if (commandQueue.size() > 0) {
			final Runnable bluetoothCommand = commandQueue.peek();
			if (bluetoothCommand == null) {
				return;
			}
			commandQueueBusy = true;

			mainHandler.post((Runnable) () -> {
				try {
					bluetoothCommand.run();
				} catch (Exception ex) {
					LOG.error("ERROR: Command exception for device '" + getName() + "'", ex);
				}
			});
		}
	}

	private void completedCommand() {
		commandQueueBusy = false;
		commandQueue.poll();
		nextCommand();
	}
}
