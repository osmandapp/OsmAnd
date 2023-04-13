package net.osmand.plus.plugins.antplus;

import static android.bluetooth.BluetoothDevice.BOND_BONDED;
import static android.bluetooth.BluetoothDevice.BOND_BONDING;
import static android.bluetooth.BluetoothDevice.BOND_NONE;
import static android.bluetooth.BluetoothGatt.GATT_SUCCESS;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTED;
import static android.bluetooth.BluetoothProfile.STATE_CONNECTING;
import static android.bluetooth.BluetoothProfile.STATE_DISCONNECTED;

import android.annotation.SuppressLint;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Calendar;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import net.osmand.plus.R;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class BluetoothLeService extends Service {
	private final static String TAG = BluetoothLeService.class.getSimpleName();

	public static final String BROADCAST_WHEEL_DATA = "BROADCAST_WHEEL_DATA";
	public static final String BROADCAST_CADENCE_DATA = "BROADCAST_CADENCE_DATA";
	public static final String BROADCAST_HEART_RATE_DATA = "BROADCAST_HEART_RATE_DATA";
	public static final String BROADCAST_TEMPERATURE_DATA = "BROADCAST_TEMPERATURE_DATA";
	public static final String BROADCAST_RUNNING_SPEED_DATA = "BROADCAST_RUNNING_SPEED_DATA";
	public static final String BROADCAST_BATTERY_LEVEL_DATA = "BROADCAST_BATTERY_LEVEL_DATA";
	public static final String BROADCAST_HEART_RATE_PLUS_DATA = "BROADCAST_HEART_RATE_PLUS_DATA";

	public static final String EXTRA_ADDRESS = "EXTRA_ADDRESS";
	public static final String EXTRA_GEAR_RATIO = "EXTRA_GEAR_RATIO";
	public static final String EXTRA_CADENCE = "EXTRA_CADENCE";
	public static final String EXTRA_SPEED = "EXTRA_SPEED";
	public static final String EXTRA_DISTANCE = "EXTRA_DISTANCE";
	public static final String EXTRA_TOTAL_DISTANCE = "EXTRA_TOTAL_DISTANCE";
	public static final String EXTRA_HEART_RATE = "EXTRA_HEART_RATE";
	public static final String EXTRA_TEMPERATURE = "EXTRA_TEMPERATURE";
	public static final String EXTRA_RUNNING_SPEED = "EXTRA_RUNNING_SPEED";
	public static final String EXTRA_RUNNING_CADENCE = "EXTRA_RUNNING_CADENCE";
	public static final String EXTRA_RUNNING_STRIDE_LENGTH = "EXTRA_RUNNING_STRIDE_LENGTH";
	public static final String EXTRA_RUNNING_TOTAL_DISTANCE = "EXTRA_RUNNING_TOTAL_DISTANCE";
	public static final String EXTRA_RUNNING_IS_RUNNING = "EXTRA_RUNNING_IS_RUNNING";
	public static final String EXTRA_BATTERY_LEVEL = "EXTRA_TEMPERATURE";
	public static final String EXTRA_HEART_RATE_BODY_PART = "EXTRA_HEART_RATE_BODY_PART";
	public static final String EXTRA_SYSTOLIC = "EXTRA_SYSTOLIC";
	public static final String EXTRA_DIASTOLIC = "EXTRA_DIASTOLIC";
	public static final String EXTRA_ARTERIAL_PRESSURE = "EXTRA_ARTERIAL_PRESSURE";
	public static final String EXTRA_CUFF_PRESSURE = "EXTRA_CUFF_PRESSURE";
	public static final String EXTRA_TIMESTAMP = "EXTRA_TIMESTAMP";
	public static final String EXTRA_PULSE_RATE = "EXTRA_PULSE_RATE";
	public static final String EXTRA_UNIT = "EXTRA_UNIT";

	public final static String ACTION_GATT_CONNECTED = "ACTION_GATT_CONNECTED";
	public final static String ACTION_GATT_DISCONNECTED = "ACTION_GATT_DISCONNECTED";
	public final static String ACTION_GATT_SERVICES_DISCOVERED = "ACTION_GATT_SERVICES_DISCOVERED";
	public final static String ACTION_DATA_AVAILABLE = "ACTION_DATA_AVAILABLE";
	public final static String BLUETOOTH_CHARACTERISTIC_KEY = "BLUETOOTH_CHARACTERISTIC_KEY";
	public final static String WHEEL_SIZE_PREFERENCE = "WHEEL_SIZE_PREFERENCE";

	private int firstWheelRevolutions = -1;
	private int lastWheelRevolutions = -1;
	private int lastWheelEventTime = -1;
	private float wheelCadence = -1;
	private int lastCrankRevolutions = -1;
	private int lastCrankEventTime = -1;

	private BluetoothManager bluetoothManager;
	public BluetoothAdapter bluetoothAdapter;
	public String bluetoothDeviceAddress;
	public BluetoothGatt bluetoothGatt;
	private final Binder binder = new LocalBinder();
	private int connectionState = STATE_DISCONNECTED;
	public BluetoothDevice device = null;

	private BleConnectionStateListener stateChangeListener;

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return binder;
	}

	public boolean initialize() {
		if (bluetoothManager == null) {
			bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
			if (bluetoothManager == null) {
				Log.e(TAG, "Unable to initialize BluetoothManager");
				return false;
			}
		}

		bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			Log.e(TAG, "Unable to obtain a BluetoothAdapter");
			return false;
		}

		return true;
	}

	@SuppressLint("MissingPermission")
	public boolean connect(String address) {
		if (bluetoothAdapter == null || address == null) {
			Log.i(TAG, "BluetoothAdapter not initialized or wrong address");
			return false;
		}

		if (address.equals(bluetoothDeviceAddress) && bluetoothGatt != null) {
			if (bluetoothGatt.connect()) {
				setConnectionState(STATE_CONNECTING);
				return true;
			} else {
				return false;
			}
		}

		device = bluetoothAdapter.getRemoteDevice(address);

		if (device == null) {
			Log.i(TAG, "Device not found");
			return false;
		}

		bluetoothGatt = device.connectGatt(this, true, gattCallback, BluetoothDevice.TRANSPORT_LE);
		Log.d(TAG, "Trying to create new connection");
		bluetoothDeviceAddress = address;
		setConnectionState(STATE_CONNECTING);

		return true;
	}

	private void setConnectionState(int newState) {
		connectionState = newState;
		if (stateChangeListener != null) {
			stateChangeListener.onStateChanged(bluetoothDeviceAddress, newState);
		}
	}

	public boolean isConnecting() {
		return connectionState == STATE_CONNECTING;
	}

	@SuppressLint("MissingPermission")
	public void disconnect() {
		setConnectionState(STATE_DISCONNECTED);
		if (bluetoothAdapter == null || bluetoothGatt == null) {
			Log.w(TAG, "BluetoothAdapter not initialized");
			return;
		}
		bluetoothGatt.disconnect();
		device = null;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		device = null;
		return super.onUnbind(intent);
	}

	private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
		@SuppressLint("MissingPermission")
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			Log.d(TAG, "status: " + status);
			Log.d(TAG, "newState: " + newState);
			if (status == GATT_SUCCESS) {
				if (newState == BluetoothProfile.STATE_CONNECTED) {
					int bondState = device.getBondState();
					broadcastUpdate(ACTION_GATT_CONNECTED);

					if (bondState == BOND_NONE || bondState == BOND_BONDED) {
						Log.d(TAG, "Discovering services");
						boolean result = gatt.discoverServices();

						if (!result) {
							Log.e(TAG, "DiscoverServices failed to start");
						}
					} else if (bondState == BOND_BONDING) {
						Log.d(TAG, "Waiting for bonding to complete");
					}
					setConnectionState(STATE_CONNECTED);
				} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
					broadcastUpdate(ACTION_GATT_DISCONNECTED);
					gatt.close();
					bluetoothGatt = null;
					setConnectionState(STATE_DISCONNECTED);
				}

			} else {
				broadcastUpdate(ACTION_GATT_DISCONNECTED);
				gatt.close();
				bluetoothGatt = null;
				setConnectionState(STATE_DISCONNECTED);
			}
		}

		@SuppressLint("MissingPermission")
		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status == 129) {
				Log.e(TAG, "Service discovery failed");
				gatt.disconnect();
				return;
			}
			if (status == BluetoothGatt.GATT_SUCCESS) {
				List<BluetoothGattService> services = gatt.getServices();
				Log.d(TAG, String.format(Locale.US, "discovered %d services for '%s'", services.size(), gatt.getDevice().getName()));
				broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
			} else {
				Log.w(TAG, "onServicesDiscovered received: " + status);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt,
		                                 BluetoothGattCharacteristic characteristic,
		                                 int status) {
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (GattAttributes.UUID_CHARACTERISTIC_HEART_RATE_BODY_PART.equals(characteristic.getUuid())) {
					decodeBodySensorPosition(gatt, characteristic);
				} else if (GattAttributes.UUID_CHARACTERISTIC_BATTERY_LEVEL.equals(characteristic.getUuid())) {
					decodeBatteryLevel(gatt, characteristic);
				}
				broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
			}
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt,
		                                    BluetoothGattCharacteristic characteristic) {
			UUID charaUUID = characteristic.getUuid();
			if (GattAttributes.UUID_CHARACTERISTIC_CYCLING_SPEED_AND_CADENCE_MEASUREMENT.equals(charaUUID)) {
				decodeSpeedCharacteristic(gatt, characteristic);
			} else if (GattAttributes.UUID_CHARACTERISTIC_HEART_RATE_MEASUREMENT.equals(charaUUID)) {
				decodeHeartRateCharacteristic(gatt, characteristic);
			} else if (GattAttributes.UUID_CHARACTERISTIC_BP_MEASUREMENT.equals(charaUUID) || GattAttributes.UUID_CHARACTERISTIC_ICP_MEASUREMENT.equals(charaUUID)) {
				decodeBPMICPCharacteristic(gatt, characteristic);
			} else if (GattAttributes.UUID_CHAR_TEMPERATURE_UUID.equals(charaUUID)) {
				decodeTemperatureCharacteristic(gatt, characteristic);
			} else if (GattAttributes.UUID_CHARACTERISTIC_RUNNING_SPEED_AND_CADENCE_MEASUREMENT.equals(charaUUID)) {
				decodeRunningSpeedCharacteristic(gatt, characteristic);

			}
		}

		@Override
		public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
			super.onReadRemoteRssi(gatt, rssi, status);
		}
	};

	private void decodeRunningSpeedCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		int flags = characteristic.getValue()[0];

		boolean strideLengthPresent = (flags & 0x01) != 0;
		boolean totalDistancePreset = (flags & 0x02) != 0;
		boolean running = (flags & 0x04) != 0;

		float speed = (float) characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1) / 3.6f * 256.0f;
		int cadence = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 3);

		float strideLength = -1;
		if (strideLengthPresent) {
			strideLength = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 4);
		}

		float totalDistance = -1;
		if (totalDistancePreset) {
			totalDistance = (float) characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, strideLengthPresent ? 6 : 4) * 3.6f / 10.0f;
		}

		Intent broadcast = new Intent(BROADCAST_RUNNING_SPEED_DATA);
		broadcast.putExtra(EXTRA_RUNNING_SPEED, speed);
		broadcast.putExtra(EXTRA_RUNNING_CADENCE, cadence);
		broadcast.putExtra(EXTRA_RUNNING_TOTAL_DISTANCE, totalDistance);
		broadcast.putExtra(EXTRA_RUNNING_STRIDE_LENGTH, strideLength);
		broadcast.putExtra(EXTRA_RUNNING_IS_RUNNING, running);
		broadcast.putExtra(EXTRA_ADDRESS, gatt.getDevice().getAddress());
		sendBroadcast(broadcast);
	}

	private void decodeBatteryLevel(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		int batteryValue = characteristic.getValue()[0];

		Intent broadcast = new Intent(BROADCAST_BATTERY_LEVEL_DATA);
		broadcast.putExtra(EXTRA_BATTERY_LEVEL, batteryValue);
		broadcast.putExtra(EXTRA_ADDRESS, gatt.getDevice().getAddress());
		sendBroadcast(broadcast);
	}

	private void decodeTemperatureCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		double temperature = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_FLOAT, 1);

		Intent broadcast = new Intent(BROADCAST_TEMPERATURE_DATA);
		broadcast.putExtra(EXTRA_TEMPERATURE, temperature);
		broadcast.putExtra(EXTRA_ADDRESS, gatt.getDevice().getAddress());
		sendBroadcast(broadcast);
	}

	private void decodeBPMICPCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);
		Intent broadcast = new Intent(BROADCAST_HEART_RATE_PLUS_DATA);

		int unit = flag & 0x01;
		boolean timestampPresent = (flag & 0x02) == 0x02;
		boolean pulseRatePresent = (flag & 0x04) == 0x04;

		if (GattAttributes.UUID_CHARACTERISTIC_BP_MEASUREMENT.equals(characteristic.getUuid())) {
			float systolic = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 1);
			float diastolic = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 3);
			float arterialPressure = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 5);
			broadcast.putExtra(EXTRA_SYSTOLIC, systolic);
			broadcast.putExtra(EXTRA_DIASTOLIC, diastolic);
			broadcast.putExtra(EXTRA_ARTERIAL_PRESSURE, arterialPressure);
			broadcast.putExtra(EXTRA_UNIT, unit);


		} else if (GattAttributes.UUID_CHARACTERISTIC_ICP_MEASUREMENT.equals(characteristic.getUuid())) {
			float cuffPressure = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, 1);
			broadcast.putExtra(EXTRA_CUFF_PRESSURE, cuffPressure);
			broadcast.putExtra(EXTRA_UNIT, unit);
		}

		if (timestampPresent) {
			Calendar calendar = Calendar.getInstance();
			int year = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 7);
			int month = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 9);
			int dayOfMonth = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 10);
			int hourOfDay = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 11);
			int minute = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 12);
			int second = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 13);
			calendar.set(year, month, dayOfMonth, hourOfDay, minute, second);
			broadcast.putExtra(EXTRA_TIMESTAMP, calendar.toString());
		}

		if (pulseRatePresent) {
			float pulseRate = characteristic.getFloatValue(BluetoothGattCharacteristic.FORMAT_SFLOAT, timestampPresent ? 14 : 7);
			broadcast.putExtra(EXTRA_PULSE_RATE, pulseRate);
		}
		sendBroadcast(broadcast);
	}

	private void decodeHeartRateCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

		int format = -1;
		if ((flag & 0x01) != 0) {
			format = BluetoothGattCharacteristic.FORMAT_UINT16;
		} else {
			format = BluetoothGattCharacteristic.FORMAT_UINT8;
		}
		int heartRate = characteristic.getIntValue(format, 1);

		Intent broadcast = new Intent(BROADCAST_HEART_RATE_DATA);
		broadcast.putExtra(EXTRA_HEART_RATE, heartRate);
		sendBroadcast(broadcast);
	}

	private void decodeBodySensorPosition(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		byte bodySensorPositionValue = characteristic.getValue()[0];
		String location;
		String[] locations = getApplicationContext().getResources().getStringArray(R.array.heart_rate_body_parts);
		if (bodySensorPositionValue > locations.length) {
			location = locations[0];
		} else {
			location = locations[bodySensorPositionValue];
		}

		Intent broadcast = new Intent(BROADCAST_HEART_RATE_DATA);
		broadcast.putExtra(EXTRA_HEART_RATE_BODY_PART, location);
		sendBroadcast(broadcast);
	}

	private void decodeSpeedCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
		int flag = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0);

		boolean wheelRevPresent = (flag & 0x01) == 0x01;
		boolean crankRevPreset = (flag & 0x02) == 0x02;
		int wheelRevolutions = 0;
		int lastWheelEventTime = 0;

		if (wheelRevPresent) {
			wheelRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 1);

			lastWheelEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 5);

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			int circumference = Integer.parseInt(preferences.getString(WHEEL_SIZE_PREFERENCE, "2086"));

			if (firstWheelRevolutions < 0)
				firstWheelRevolutions = wheelRevolutions;

			if (this.lastWheelEventTime == lastWheelEventTime) {
				float totalDistance = (float) wheelRevolutions * (float) circumference / 1000.0f;
				float distance = (float) (wheelRevolutions - firstWheelRevolutions) * (float) circumference / 1000.0f;
				float speed = 0;

				Intent broadcast = new Intent(BROADCAST_WHEEL_DATA);
				broadcast.putExtra(EXTRA_ADDRESS, gatt.getDevice().getAddress());
				broadcast.putExtra(EXTRA_SPEED, speed);
				broadcast.putExtra(EXTRA_DISTANCE, distance);
				broadcast.putExtra(EXTRA_TOTAL_DISTANCE, totalDistance);
				sendBroadcast(broadcast);
			} else if (lastWheelRevolutions >= 0) {
				float timeDifference;
				if (lastWheelEventTime < this.lastWheelEventTime)
					timeDifference = (65535 + lastWheelEventTime - this.lastWheelEventTime) / 1024.0f;
				else
					timeDifference = (lastWheelEventTime - this.lastWheelEventTime) / 1024.0f;
				float distanceDifference = (wheelRevolutions - lastWheelRevolutions) * circumference / 1000.0f;
				float totalDistance = (float) wheelRevolutions * (float) circumference / 1000.0f;
				float distance = (float) (wheelRevolutions - firstWheelRevolutions) * (float) circumference / 1000.0f;
				float speed = (distanceDifference / timeDifference) * 3.6f;
				wheelCadence = (wheelRevolutions - lastWheelRevolutions) * 60.0f / timeDifference;

				Intent broadcast = new Intent(BROADCAST_WHEEL_DATA);
				broadcast.putExtra(EXTRA_ADDRESS, gatt.getDevice().getAddress());
				broadcast.putExtra(EXTRA_SPEED, speed);
				broadcast.putExtra(EXTRA_DISTANCE, distance);
				broadcast.putExtra(EXTRA_TOTAL_DISTANCE, totalDistance);
				sendBroadcast(broadcast);
			}

			lastWheelRevolutions = wheelRevolutions;
			this.lastWheelEventTime = lastWheelEventTime;

		} else if (crankRevPreset) {
			int crankRevolutions = 0;
			int lastCrankEventTime = 0;

			crankRevolutions = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 1);
			lastCrankEventTime = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);

			if (this.lastCrankEventTime == lastCrankEventTime) {
				Intent broadcast = new Intent(BROADCAST_WHEEL_DATA);
				broadcast.putExtra(EXTRA_ADDRESS, gatt.getDevice().getAddress());
				broadcast.putExtra(EXTRA_GEAR_RATIO, 0);
				broadcast.putExtra(EXTRA_CADENCE, 0);
				sendBroadcast(broadcast);
			} else if (lastCrankRevolutions >= 0) {
				float timeDifference;
				if (lastCrankEventTime < this.lastCrankEventTime)
					timeDifference = (65535 + lastCrankEventTime - this.lastCrankEventTime) / 1024.0f;
				else
					timeDifference = (lastCrankEventTime - this.lastCrankEventTime) / 1024.0f;
				float crankCadence = (crankRevolutions - lastCrankRevolutions) * 60.0f / timeDifference;

				if (crankCadence > 0) {
					float gearRatio = wheelCadence / crankCadence;

					Intent broadcast = new Intent(BROADCAST_CADENCE_DATA);
					broadcast.putExtra(EXTRA_GEAR_RATIO, gearRatio);
					broadcast.putExtra(EXTRA_CADENCE, crankCadence);
					broadcast.putExtra(EXTRA_ADDRESS, gatt.getDevice().getAddress());
					sendBroadcast(broadcast);
				}
			}
			lastCrankRevolutions = crankRevolutions;
			this.lastCrankEventTime = lastCrankEventTime;
		}
	}

	private void broadcastUpdate(String action) {
		Intent intent = new Intent(action);
		sendBroadcast(intent);
	}

	private void broadcastUpdate(String action, BluetoothGattCharacteristic characteristic) {
	}

	public class LocalBinder extends Binder {
		public BluetoothLeService getService() {
			return BluetoothLeService.this;
		}
	}

	public List<BluetoothGattService> getSupportedGattServices() {
		if (bluetoothGatt == null) return null;

		return bluetoothGatt.getServices();
	}

	public void setConnectionStateListener(BleConnectionStateListener listener) {
		stateChangeListener = listener;
	}
}
