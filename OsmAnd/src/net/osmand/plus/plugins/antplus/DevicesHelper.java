package net.osmand.plus.plugins.antplus;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.preference.PreferenceManager;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.antplus.antdevices.AntCommonDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntCommonDevice.AntDeviceListener;
import net.osmand.plus.plugins.antplus.antdevices.AntDeviceConnectionResult;
import net.osmand.plus.plugins.antplus.devices.BikeCadenceDevice;
import net.osmand.plus.plugins.antplus.devices.BikeDistanceDevice;
import net.osmand.plus.plugins.antplus.devices.BikePowerDevice;
import net.osmand.plus.plugins.antplus.devices.BikeSpeedDevice;
import net.osmand.plus.plugins.antplus.devices.CommonDevice;
import net.osmand.plus.plugins.antplus.devices.CommonDevice.DeviceListener;
import net.osmand.plus.plugins.antplus.devices.DeviceType;
import net.osmand.plus.plugins.antplus.devices.HeartRateDevice;
import net.osmand.plus.plugins.antplus.models.BatteryData;
import net.osmand.plus.plugins.antplus.models.CadenceData;
import net.osmand.plus.plugins.antplus.models.HeartRateData;
import net.osmand.plus.plugins.antplus.models.PressureData;
import net.osmand.plus.plugins.antplus.models.RunningSpeedData;
import net.osmand.plus.plugins.antplus.models.TemperatureData;
import net.osmand.plus.plugins.antplus.models.WheelDeviceData;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DevicesHelper implements AntDeviceListener, DeviceListener {

	public static final int ENABLE_BLUETOOTH_REQUEST_CODE = 400;

	public final static String SCAN_MODE_PREFERENCE = "SCAN_MODE";
	public final static String MATCH_MODE_PREFERENCE = "MATCH_MODE";
	public final static String CALLBACK_TYPE_PREFERENCE = "CALLBACK_TYPE";
	public final static String MATCH_NUM_PREFERENCE = "MATCH_NUM";

	private static final Log LOG = PlatformUtil.getLog(DevicesHelper.class);

	private final OsmandApplication app;
	private final List<CommonDevice<?>> devices = new ArrayList<>();
	public HashMap<String, ScanResult> leDevices = new HashMap();
	private boolean bleIsScanning;

	private Activity activity;
	private boolean installPluginAsked;
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothLeScanner bluetoothLeScanner;
	public BluetoothLeService bluetoothLeService;
	private IntentFilter gattUpdateIntentFilter;
	public List<DeviceType> filterList = new ArrayList<>();
	private BleConnectionStateListener stateChangeListener;
	private BleDataListener bleDataListener;


	DevicesHelper(@NonNull OsmandApplication app, @NonNull AntPlusPlugin plugin) {
		this.app = app;

		devices.add(new HeartRateDevice(plugin));
		devices.add(new BikePowerDevice(plugin));
		devices.add(new BikeCadenceDevice(plugin));
		devices.add(new BikeSpeedDevice(plugin));
		devices.add(new BikeDistanceDevice(plugin));

		for (CommonDevice<?> device : devices) {
			device.addListener(this);
		}
		gattUpdateIntentFilter = createGatUpdateIntentFilter();
	}

	void setActivity(@Nullable Activity activity) {
		if (this.activity != null) {
			disconnectBLE();
		}
		this.activity = activity;
		if (activity != null) {
			initHelper();
		}
	}

	private void initHelper() {
		initBLE();
	}

	void connectAntDevices(@Nullable Activity activity) {
		for (CommonDevice<?> device : devices) {
			if (device.isEnabled()) {
				AntCommonDevice<?> antDevice = device.getAntDevice();
				antDevice.addListener(this);
				connectAntDevice(antDevice, activity);
			}
		}
	}

	void initBLE() {
		Intent gattServiceIntent = new Intent(activity, BluetoothLeService.class);
		activity.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
		BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			Toast.makeText(activity, R.string.bluetooth_not_supported, Toast.LENGTH_SHORT).show();
		} else {
			bluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();
		}
		activity.registerReceiver(gattUpdateReceiver, gattUpdateIntentFilter);
	}

	private IntentFilter createGatUpdateIntentFilter() {
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
		intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
		intentFilter.addAction(BluetoothLeService.BROADCAST_WHEEL_DATA);
		intentFilter.addAction(BluetoothLeService.BROADCAST_HEART_RATE_DATA);
		intentFilter.addAction(BluetoothLeService.BROADCAST_HEART_RATE_PLUS_DATA);
		intentFilter.addAction(BluetoothLeService.BROADCAST_CADENCE_DATA);
		intentFilter.addAction(BluetoothLeService.BROADCAST_TEMPERATURE_DATA);
		intentFilter.addAction(BluetoothLeService.BROADCAST_BATTERY_LEVEL_DATA);
		intentFilter.addAction(BluetoothLeService.BROADCAST_RUNNING_SPEED_DATA);
		return intentFilter;
	}

	public final ServiceConnection serviceConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder service) {
			bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
			android.util.Log.i("serviceConnected", String.valueOf(bluetoothLeService));
			if (!bluetoothLeService.initialize()) {
				bluetoothLeService = null;
				LOG.error("Unable to initialize Bluetooth");
			}
			bluetoothLeService.setConnectionStateListener(((address, newState) -> {
				if (stateChangeListener != null) {
					stateChangeListener.onStateChanged(address, newState);
				}
			}));
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			LOG.debug("serviceDisconnected. " + bluetoothLeService);
			bluetoothLeService.setConnectionStateListener(null);
			bluetoothLeService = null;
		}
	};


	void disconnectBLE() {
		try {
			activity.unregisterReceiver(gattUpdateReceiver);
		} catch (IllegalArgumentException exception) {

		}
		try{
			if (bluetoothAdapter != null) {
				bluetoothAdapter.cancelDiscovery();
				bluetoothAdapter = null;
			}
			if (bluetoothLeScanner != null) {
				bluetoothLeScanner.stopScan(leScanCallback);
				bluetoothLeScanner = null;
			}
		} catch(SecurityException error){
			LOG.debug("No permission on disable BLE");
		}
	}

	private final ScanCallback leScanCallback = new ScanCallback() {
		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			super.onScanResult(callbackType, result);
			BluetoothDevice device = result.getDevice();
			if (device.getName() != null) {
				addScanResult(result);
			}
		}

		@Override
		public void onBatchScanResults(List<ScanResult> results) {
			super.onBatchScanResults(results);
			for (ScanResult result : results) {
				addScanResult(result);
			}
		}

		private void addScanResult(ScanResult result) {
			if (isSupportedBleDevice(result.getScanRecord())) {
				leDevices.put(result.getDevice().getAddress(), result);
			}
		}

		@Override
		public void onScanFailed(int errorCode) {
			super.onScanFailed(errorCode);
		}
	};

	private boolean isSupportedBleDevice(ScanRecord scanRecord) {
		List<ParcelUuid> uuids = scanRecord.getServiceUuids();
		if (uuids != null) {
			for (ParcelUuid uuid : uuids) {
				if (GattAttributes.SUPPORTED_CHARACTERISTICS.contains(uuid.getUuid())) {
					return true;
				}
			}
		}
		return false;
	}


	void disconnectAntDevices() {
		for (CommonDevice<?> device : devices) {
			AntCommonDevice<?> antDevice = device.getAntDevice();
			antDevice.closeConnection();
			antDevice.removeListener(this);
		}
	}

	void updateAntDevices(@Nullable Activity activity) {
		for (CommonDevice<?> device : devices) {
			AntCommonDevice<?> antDevice = device.getAntDevice();
			if (device.isEnabled() && antDevice.isDisconnected()) {
				antDevice.addListener(this);
				connectAntDevice(antDevice, activity);
			} else if (!device.isEnabled() && antDevice.isConnected()) {
				antDevice.closeConnection();
				antDevice.removeListener(this);
			}
		}
	}

	@NonNull
	List<CommonDevice<?>> getDevices() {
		return new ArrayList<>(devices);
	}

	@Nullable
	<T extends CommonDevice<?>> T getDevice(@NonNull Class<T> clz) {
		for (CommonDevice<?> device : devices) {
			if (clz.isInstance(device)) {
				return (T) device;
			}
		}
		return null;
	}

	@Nullable
	<T extends AntCommonDevice<?>> T getAntDevice(@NonNull Class<T> clz) {
		for (CommonDevice<?> device : devices) {
			AntCommonDevice<?> antDevice = device.getAntDevice();
			if (clz.isInstance(antDevice)) {
				return (T) antDevice;
			}
		}
		return null;
	}

	@Nullable
	CommonDevice<?> getDeviceByAntDevice(@NonNull AntCommonDevice<?> antDevice) {
		for (CommonDevice<?> device : devices) {
			if (device.getAntDevice().equals(antDevice)) {
				return device;
			}
		}
		return null;
	}

	private void saveAntDeviceNumber(@NonNull AntCommonDevice<?> antDevice, int antDeviceNumber) {
		CommonDevice<?> device = getDeviceByAntDevice(antDevice);
		if (device != null) {
			device.setDeviceNumber(antDeviceNumber);
		}
	}

	public void connectBleDevice(String address) {
		if (bluetoothLeService == null) {
		} else if (!bluetoothLeService.isConnecting()) {
			bluetoothLeService.connect(address);
		}
	}

	public void disconnectBleDevice() {
		if (bluetoothLeService != null) {
			bluetoothLeService.disconnect();
		}
	}

	private void connectAntDevice(@NonNull AntCommonDevice<?> antDevice, @Nullable Activity activity) {
		if (antDevice.isDisconnected()) {
			if (antDevice.hasAntDeviceNumber()) {
				LOG.debug("ANT+ " + antDevice.getDeviceName() + " device connecting with device number " + antDevice.getAntDeviceNumber());
				antDevice.resetConnection(null, app);
			} else if (activity != null) {
				LOG.debug("ANT+ " + antDevice.getDeviceName() + " device connecting without device number");
				antDevice.resetConnection(activity, app);
			}
		}
	}

	void askPluginInstall() {
		if (activity == null || installPluginAsked) {
			return;
		}

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle(R.string.ant_missing_dependency);
		builder.setMessage(app.getString(R.string.ant_missing_dependency_descr, AntPlusHeartRatePcc.getMissingDependencyName()));
		builder.setCancelable(true);
		builder.setPositiveButton(R.string.ant_go_to_store, (dialog, which) -> {
			Uri uri = Uri.parse(Version.getUrlWithUtmRef(app, AntPluginPcc.getMissingDependencyPackageName()));
			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
			AndroidUtils.startActivityIfSafe(activity, intent);
		});
		builder.setNegativeButton(R.string.shared_string_cancel, (dialog, which) -> dialog.dismiss());
		builder.create().show();
		installPluginAsked = true;
	}

	@Override
	public void onAntDeviceConnect(@NonNull AntCommonDevice<?> antDevice, @NonNull AntDeviceConnectionResult result, int antDeviceNumber, @Nullable String error) {
		if (!Algorithms.isEmpty(error)) {
			LOG.error("ANT+ " + antDevice.getDeviceName() + " device connection error: " + error);
		}
		CommonDevice<?> device = getDeviceByAntDevice(antDevice);
		switch (result) {
			case SUCCESS:
				LOG.debug("ANT+ " + antDevice.getDeviceName() + " device connected. Device number = " + antDeviceNumber);
				saveAntDeviceNumber(antDevice, antDeviceNumber);
				if (device != null && !device.isEnabled()) {
					updateAntDevices(activity);
				}
				break;
			case DEPENDENCY_NOT_INSTALLED:
				LOG.debug("ANT+ plugin is not installed. Ask plugin install.");
				askPluginInstall();
				break;
			case SEARCH_TIMEOUT:
				if (device != null && !device.isEnabled()) {
					updateAntDevices(activity);
				} else {
					LOG.debug("ANT+ Reconnect " + antDevice.getDeviceName() + " after timeout");
					connectAntDevice(antDevice, activity);
				}
				break;
			default:
				break;
		}
	}

	@Override
	public void onAntDeviceDisconnect(@NonNull AntCommonDevice<?> antDevice) {
		LOG.debug("ANT+ " + antDevice.getDeviceName() + " (" + antDevice.getAntDeviceNumber() + ") disconnected");
	}

	@Override
	public void onDeviceConnected(@NonNull CommonDevice<?> device) {
		app.runInUIThread(() -> updateAntDevices(activity));
	}

	@Override
	public void onDeviceDisconnected(@NonNull CommonDevice<?> device) {
		app.runInUIThread(() -> updateAntDevices(activity));
	}

	@SuppressLint("MissingPermission")
	public void scanLeDevice(boolean enable) {
		if (!enable) {
			if (bluetoothLeScanner != null) {
				bluetoothLeScanner.stopScan(leScanCallback);
				bleIsScanning = false;
			}
		} else {
			if (!requestBlePermissions()) {
				Toast.makeText(activity, "Permissions not granted", Toast.LENGTH_SHORT).show();
				return;
			}
			if (!requestBle()) {
				Toast.makeText(activity, "Bluetooth isnt available", Toast.LENGTH_SHORT).show();
				return;
			}
			if (!requestLeService()) {
				return;
			}
			leDevices.clear();

			ArrayList<UUID> serviceUUIDs = new ArrayList<>();
			for (DeviceType type : filterList) {
				serviceUUIDs.add(UUID.fromString(type.getUUIDService()));
			}
			List<ScanFilter> filters = null;
			if (!serviceUUIDs.isEmpty()) {
				filters = new ArrayList<>();
				for (UUID serviceUUID : serviceUUIDs) {
					ScanFilter filter = new ScanFilter.Builder()
							.setServiceUuid(new ParcelUuid(serviceUUID))
							.build();
					filters.add(filter);
				}
			}

			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
			ScanSettings scanSettings = new ScanSettings.Builder()
					.setScanMode(Integer.parseInt(preferences.getString(SCAN_MODE_PREFERENCE, "1")))
					.setCallbackType(Integer.parseInt(preferences.getString(CALLBACK_TYPE_PREFERENCE, "1")))
					.setMatchMode(Integer.parseInt(preferences.getString(MATCH_MODE_PREFERENCE, "2")))
					.setNumOfMatches(Integer.parseInt(preferences.getString(MATCH_NUM_PREFERENCE, "1")))
					.setReportDelay(0L)
					.build();


			bluetoothLeScanner.startScan(filters, scanSettings, leScanCallback);
			bleIsScanning = true;
		}
	}

	public boolean requestLeService() {
		if (bluetoothLeService == null) {
			Intent gattServiceIntent = new Intent(activity, BluetoothLeService.class);
			activity.bindService(gattServiceIntent, serviceConnection, Context.BIND_AUTO_CREATE);
			Toast.makeText(activity, "Ble service unavailable", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}


	private boolean requestBlePermissions() {
		boolean hasNeededPermissions = true;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			if (!AndroidUtils.hasPermission(activity, Manifest.permission.BLUETOOTH_SCAN)) {
				hasNeededPermissions = false;
				ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_SCAN}, 4);
			}
			if (!AndroidUtils.hasPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)) {
				hasNeededPermissions = false;
				ActivityCompat.requestPermissions(
						activity,
						new String[]{Manifest.permission.BLUETOOTH_CONNECT},
						5
				);
			}
		} else {
			if (!AndroidUtils.hasPermission(activity, Manifest.permission.BLUETOOTH)) {
				hasNeededPermissions = false;
				ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH}, 2);
			}
			if (!AndroidUtils.hasPermission(activity, Manifest.permission.BLUETOOTH_ADMIN)) {
				hasNeededPermissions = false;
				ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, 3);
			}
		}
		return hasNeededPermissions;
	}

	public boolean isBleEnabled() {
		return bluetoothLeService != null
				&& activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
				&& bluetoothLeService.bluetoothAdapter.isEnabled();
	}

	public boolean requestBle() {
		boolean bluetoothEnabled = true;
		if (bluetoothLeService != null && activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			if (!bluetoothLeService.bluetoothAdapter.isEnabled()) {
				bluetoothEnabled = false;
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					try {
						Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						mapActivity.startActivityForResult(intent, ENABLE_BLUETOOTH_REQUEST_CODE);
						mapActivity.registerActivityResultListener(new ActivityResultListener(ENABLE_BLUETOOTH_REQUEST_CODE, (resultCode, resultData) -> {
							if (resultCode != Activity.RESULT_OK) {
								Toast.makeText(activity, "Bluetooth permission not granted", Toast.LENGTH_SHORT).show();
							}
						}));
					} catch (ActivityNotFoundException e) {
						Toast.makeText(mapActivity, R.string.no_activity_for_intent, Toast.LENGTH_LONG).show();
					}
				}
			}
		} else {
			bluetoothEnabled = false;
//			Toast.makeText(activity, "Bluetooth LE isnt supported on this device", Toast.LENGTH_SHORT).show();
		}
		return bluetoothEnabled;
	}

	private MapActivity getMapActivity() {
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		} else {
			return null;
		}
	}

	public boolean isBleConnected(String deviceAddress) {
		if (isBleEnabled()) {
			BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
			BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
			return bluetoothManager.getConnectionState(device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED;
		}
		return false;
	}

	private final BroadcastReceiver gattUpdateReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			final String action = intent.getAction();
			String address = intent.getStringExtra(BluetoothLeService.EXTRA_ADDRESS);
			if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
				ArrayList<BluetoothGattCharacteristic> characteristics = getCharacteristics();
				requestCharacteristic(characteristics);
			} else if (BluetoothLeService.BROADCAST_WHEEL_DATA.equals(action)) {
				float speed = intent.getFloatExtra(BluetoothLeService.EXTRA_SPEED, 0);
				float distance = intent.getFloatExtra(BluetoothLeService.EXTRA_DISTANCE, 0);
				float totalDistance = intent.getFloatExtra(BluetoothLeService.EXTRA_TOTAL_DISTANCE, 0);
				bleDataListener.onDataReceived(address, new WheelDeviceData(speed, distance, totalDistance));
			} else if (BluetoothLeService.BROADCAST_HEART_RATE_DATA.equals(action)) {
				int heartRateValue = intent.getIntExtra(BluetoothLeService.EXTRA_HEART_RATE, 0);
				bleDataListener.onDataReceived(address, new HeartRateData(heartRateValue));
			} else if (BluetoothLeService.BROADCAST_CADENCE_DATA.equals(action)) {
				float gearRatio = intent.getFloatExtra(BluetoothLeService.EXTRA_GEAR_RATIO, 0);
				float cadence = intent.getFloatExtra(BluetoothLeService.EXTRA_CADENCE, 0);
				bleDataListener.onDataReceived(address, new CadenceData(gearRatio, cadence));
			} else if (BluetoothLeService.BROADCAST_HEART_RATE_PLUS_DATA.equals(action)) {
				float systolic = intent.getFloatExtra(BluetoothLeService.EXTRA_SYSTOLIC, 0);
				float diastolic = intent.getFloatExtra(BluetoothLeService.EXTRA_DIASTOLIC, 0);
				float arterialPressure = intent.getFloatExtra(BluetoothLeService.EXTRA_ARTERIAL_PRESSURE, 0);
				float cuffPressure = intent.getFloatExtra(BluetoothLeService.EXTRA_CUFF_PRESSURE, 0);
				int unit = intent.getIntExtra(BluetoothLeService.EXTRA_UNIT, 0);
				String timestamp = intent.getStringExtra(BluetoothLeService.EXTRA_TIMESTAMP);
				float pulseRate = intent.getFloatExtra(BluetoothLeService.EXTRA_PULSE_RATE, 0);
				bleDataListener.onDataReceived(address, new PressureData(systolic, diastolic, arterialPressure, cuffPressure, unit, timestamp, pulseRate));
			} else if (BluetoothLeService.BROADCAST_TEMPERATURE_DATA.equals(action)) {
				float temperature = intent.getFloatExtra(BluetoothLeService.EXTRA_TEMPERATURE, 0);
				bleDataListener.onDataReceived(address, new TemperatureData(temperature));
			} else if (BluetoothLeService.BROADCAST_BATTERY_LEVEL_DATA.equals(action)) {
				int batteryLevel = intent.getIntExtra(BluetoothLeService.EXTRA_BATTERY_LEVEL, 0);
				bleDataListener.onDataReceived(address, new BatteryData(batteryLevel));
			} else if (BluetoothLeService.BROADCAST_RUNNING_SPEED_DATA.equals(action)) {
				float speed = intent.getFloatExtra(BluetoothLeService.EXTRA_RUNNING_SPEED, 0);
				int cadence = intent.getIntExtra(BluetoothLeService.EXTRA_RUNNING_CADENCE, 0);
				float totalDistance = intent.getFloatExtra(BluetoothLeService.EXTRA_RUNNING_TOTAL_DISTANCE, 0);
				float strideLength = intent.getFloatExtra(BluetoothLeService.EXTRA_RUNNING_STRIDE_LENGTH, 0);
				boolean isRunning = intent.getBooleanExtra(BluetoothLeService.EXTRA_RUNNING_IS_RUNNING, false);
				bleDataListener.onDataReceived(address, new RunningSpeedData(speed, cadence, totalDistance, strideLength));
			}
		}
	};

	private ArrayList<BluetoothGattCharacteristic> getCharacteristics() {
		ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<>();
		List<BluetoothGattService> services = bluetoothLeService.getSupportedGattServices();
		if (services != null) {
			for (BluetoothGattService gattService : bluetoothLeService.getSupportedGattServices()) {
				List<BluetoothGattCharacteristic> gattCharas = gattService.getCharacteristics();
				characteristics.addAll(gattCharas);
			}
		}
		return characteristics;
	}

	private BluetoothGattCharacteristic mNotifyCharacteristic;

	private void requestCharacteristic(List<BluetoothGattCharacteristic> characteristics) {
		for (BluetoothGattCharacteristic characteristic : characteristics) {
			if (GattAttributes.SUPPORTED_CHARACTERISTICS.contains(characteristic.getUuid())) {
				final int charaProp = characteristic.getProperties();
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
					if (mNotifyCharacteristic != null) {
						setCharacteristicNotification(mNotifyCharacteristic, false);
						mNotifyCharacteristic = null;
					}
					readCharacteristic(characteristic);
				}
				if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
					mNotifyCharacteristic = characteristic;
					setCharacteristicNotification(characteristic, true);
				}
			}
		}
	}

	@SuppressLint("MissingPermission")
	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (bluetoothLeService.bluetoothAdapter == null || bluetoothLeService.bluetoothGatt == null) {
			return;
		}
		bluetoothLeService.bluetoothGatt.readCharacteristic(characteristic);
	}

	@SuppressLint("MissingPermission")
	public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
	                                          boolean enabled) {
		if (bluetoothLeService.bluetoothAdapter == null || bluetoothLeService.bluetoothGatt == null) {
			return;
		}
		bluetoothLeService.bluetoothGatt.setCharacteristicNotification(characteristic, enabled);

		if (GattAttributes.SUPPORTED_CHARACTERISTICS.contains(characteristic.getUuid())) {
			BluetoothGattDescriptor descriptor = characteristic.getDescriptor(GattAttributes.UUID_CHARACTERISTIC_CLIENT_CONFIG);
			descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
			bluetoothLeService.bluetoothGatt.writeDescriptor(descriptor);
		}
	}

	public void setConnectionStateListener(BleConnectionStateListener listener) {
		stateChangeListener = listener;
	}

	public void setBleDataListenerListener(BleDataListener listener) {
		bleDataListener = listener;
	}


}