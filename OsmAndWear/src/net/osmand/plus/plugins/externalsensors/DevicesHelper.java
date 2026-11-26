package net.osmand.plus.plugins.externalsensors;

import static net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty.NAME;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dsi.ant.plugins.antplus.pcc.AntPlusHeartRatePcc;
import com.dsi.ant.plugins.antplus.pccbase.AntPluginPcc;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.ActivityResultListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.externalsensors.DevicesSettingsCollection.DevicePreferencesListener;
import net.osmand.plus.plugins.externalsensors.DevicesSettingsCollection.DeviceSettings;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice.DeviceListener;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntAbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntBikePowerDevice;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntBikeSpeedCadenceDevice;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntBikeSpeedDistanceDevice;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntHeartRateDevice;
import net.osmand.plus.plugins.externalsensors.devices.ant.AntTemperatureDevice;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEAbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEBPICPDevice;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEBikeSCDDevice;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLEHeartRateDevice;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLERunningSCDDevice;
import net.osmand.plus.plugins.externalsensors.devices.ble.BLETemperatureDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorData;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorDataField;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.BLEUtils;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;


public class DevicesHelper implements DeviceListener, DevicePreferencesListener {

	public static final int ENABLE_BLUETOOTH_REQUEST_CODE = 400;

	private static final Log LOG = PlatformUtil.getLog(DevicesHelper.class);

	private final static List<UUID> SUPPORTED_BLE_SERVICE_UUIDS = Arrays.asList(
			BLEBikeSCDDevice.getServiceUUID(),
			BLEHeartRateDevice.getServiceUUID(),
			BLERunningSCDDevice.getServiceUUID(),
			BLETemperatureDevice.getServiceUUID());

	private final OsmandApplication app;
	private final DevicesSettingsCollection devicesSettingsCollection;
	private final Map<String, AbstractDevice<?>> devices = new ConcurrentHashMap<>();
	private List<AntAbstractDevice<?>> antSearchableDevices = new ArrayList<>();

	private boolean antScanning;
	private boolean bleScanning;

	private Activity activity;
	private boolean installAntPluginAsked;
	private BluetoothAdapter bluetoothAdapter;
	private BluetoothLeScanner bleScanner;
	private final ExternalSensorsPlugin externalSensorsPlugin;

	DevicesHelper(@NonNull OsmandApplication app, @NonNull ExternalSensorsPlugin plugin) {
		this.app = app;
		this.devicesSettingsCollection = new DevicesSettingsCollection(plugin);
		externalSensorsPlugin = plugin;
	}

	void setActivity(@Nullable Activity activity) {
		if (this.activity != null) {
			dropUnpairedDevices();
			deinitBLE();
			devicesSettingsCollection.removeListener(this);
		}
		this.activity = activity;
		if (activity != null) {
			initBLE();
			initDevices();
			devicesSettingsCollection.addListener(this);
		}
	}

	void initBLE() {
		BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
		bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			app.showShortToastMessage(R.string.bluetooth_not_supported);
		} else {
			bleScanner = bluetoothAdapter.getBluetoothLeScanner();
		}
	}

	void deinitBLE() {
		try {
			if (bluetoothAdapter != null) {
				if (bleScanner != null && bluetoothAdapter.isEnabled()) {
					try {
						bleScanner.stopScan(bleScanCallback);
					} catch (Throwable error) {
						LOG.error("Stop ble scan error. " + error);
					}
				}
				bluetoothAdapter.cancelDiscovery();
				bluetoothAdapter = null;
			}
			bleScanner = null;
		} catch (SecurityException error) {
			LOG.debug("No permission on disable BLE");
		}
	}

	private void initDevices() {
		for (String deviceId : devicesSettingsCollection.getDeviceIds()) {
			DeviceSettings deviceSettings = devicesSettingsCollection.getDeviceSettings(deviceId);
			if (deviceSettings != null && !devices.containsKey(deviceId)) {
				AbstractDevice<?> device = createDevice(deviceSettings.getDeviceType(), deviceId);
				if (device != null) {
					devices.put(deviceId, device);
					updateDeviceProperties(device);
				}
			}
		}
		updateDevices(activity);
	}

	private void updateDeviceProperties(@NonNull AbstractDevice<?> device) {
		String deviceId = device.getDeviceId();
		DevicesSettingsCollection.DeviceSettings deviceSettings = devicesSettingsCollection.getDeviceSettings(deviceId);
		if (deviceSettings != null) {
			for (DeviceChangeableProperty property : deviceSettings.getParams().keySet()) {
				device.setChangeableProperty(property, deviceSettings.getParams().get(property));
			}
		}
	}

	@Nullable
	private AbstractDevice<?> createDevice(@NonNull DeviceType deviceType, @NonNull String deviceId) {
		switch (deviceType) {
			case ANT_HEART_RATE:
				return new AntHeartRateDevice(deviceId);
			case ANT_TEMPERATURE:
				return new AntTemperatureDevice(deviceId);
			case ANT_BICYCLE_POWER:
				return new AntBikePowerDevice(deviceId);
			case ANT_BICYCLE_SC:
				return new AntBikeSpeedCadenceDevice(deviceId);
			case ANT_BICYCLE_SD:
				return new AntBikeSpeedDistanceDevice(deviceId);
			case BLE_TEMPERATURE:
				return bluetoothAdapter != null ? new BLETemperatureDevice(bluetoothAdapter, deviceId) : null;
			case BLE_HEART_RATE:
				return bluetoothAdapter != null ? new BLEHeartRateDevice(bluetoothAdapter, deviceId) : null;
			case BLE_BLOOD_PRESSURE:
				return bluetoothAdapter != null ? new BLEBPICPDevice(bluetoothAdapter, deviceId) : null;
			case BLE_BICYCLE_SCD:
				return bluetoothAdapter != null ? new BLEBikeSCDDevice(bluetoothAdapter, deviceId) : null;
			case BLE_RUNNING_SCDS:
				return bluetoothAdapter != null ? new BLERunningSCDDevice(bluetoothAdapter, deviceId) : null;
			default:
				return null;
		}
	}

	public boolean isAntScanning() {
		return antScanning;
	}

	public boolean isBleScanning() {
		return bleScanning;
	}

	private final ScanCallback bleScanCallback = new ScanCallback() {
		@SuppressLint("MissingPermission")
		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			super.onScanResult(callbackType, result);
			BluetoothDevice device = result.getDevice();
			ScanRecord record = result.getScanRecord();
			if (AndroidUtils.hasBLEPermission(activity)) {
				LOG.debug("BLE scan result " + device.getAddress() + "; name " + device.getName() + "; " + record.getServiceUuids());
				if (device.getName() != null) {
					addScanResult(result);
				}
			} else {
				LOG.error("Try to add ble device while no permission");
			}
		}

		@Override
		public void onBatchScanResults(List<ScanResult> results) {
			super.onBatchScanResults(results);
			for (ScanResult result : results) {
				addScanResult(result);
			}
		}

		@SuppressLint("MissingPermission")
		private void addScanResult(ScanResult result) {
			if (!AndroidUtils.hasBLEPermission(activity)) {
				LOG.error("Try to addScanResult while no permission");
				return;
			}
			ScanRecord scanRecord = result.getScanRecord();
			if (scanRecord != null && isSupportedBleDevice(scanRecord)) {
				String address = result.getDevice().getAddress();
				DevicesSettingsCollection.DeviceSettings settings = devicesSettingsCollection.getDeviceSettings(address);
				String deviceName;
				deviceName = settings == null ? result.getDevice().getName() : settings.getParams().get(NAME);
				List<ParcelUuid> uuids = scanRecord.getServiceUuids();
				for (ParcelUuid uuid : uuids) {
					BLEAbstractDevice device = BLEAbstractDevice.createDeviceByUUID(
							bluetoothAdapter, uuid.getUuid(), address, deviceName, result.getRssi());
					if (device != null) {
						if (!devices.containsKey(device.getDeviceId())) {
							devices.put(device.getDeviceId(), device);
						}
						break;
					}
				}
			}
		}

		@Override
		public void onScanFailed(int errorCode) {
			super.onScanFailed(errorCode);
			if (errorCode == SCAN_FAILED_ALREADY_STARTED) {
				scanBLEDevices(false);
				scanBLEDevices(true);
			}
			LOG.error("BLE scan failed. Error " + errorCode);
		}
	};

	private boolean isSupportedBleDevice(@NonNull ScanRecord scanRecord) {
		List<ParcelUuid> uuids = scanRecord.getServiceUuids();
		if (uuids != null) {
			for (ParcelUuid uuid : uuids) {
				if (SUPPORTED_BLE_SERVICE_UUIDS.contains(uuid.getUuid())) {
					return true;
				}
			}
		}
		return false;
	}

	void connectDevice(@Nullable Activity activity, @NonNull AbstractDevice<?> device) {
		device.addListener(this);
		device.connect(app, activity);
	}

	void disconnectDevice(@NonNull AbstractDevice<?> device) {
		disconnectDevice(device, true);
	}

	void disconnectDevice(@NonNull AbstractDevice<?> device, boolean notify) {
		device.removeListener(this);
		if (device.disconnect() && notify) {
			onDeviceDisconnect(device);
		}
	}

	void connectDevices(@Nullable Activity activity) {
		for (AbstractDevice<?> device : getDevices()) {
			if (isDeviceEnabled(device)) {
				connectDevice(activity, device);
			}
		}
	}

	void disconnectDevices() {
		for (AbstractDevice<?> device : getDevices()) {
			disconnectDevice(device);
		}
	}

	void updateDevice(@Nullable Activity activity, @NonNull AbstractDevice<?> device) {
		if (isDeviceEnabled(device) && device.isDisconnected()) {
			connectDevice(activity, device);
		} else if (!isDeviceEnabled(device) && device.isConnected()) {
			disconnectDevice(device);
		}
	}

	void updateDevices(@Nullable Activity activity) {
		for (AbstractDevice<?> device : getDevices()) {
			updateDevice(activity, device);
		}
	}

	void dropUnpairedDevice(@NonNull AbstractDevice<?> device) {
		disconnectDevice(device);
		devices.remove(device.getDeviceId());
	}

	void dropUnpairedDevices() {
		for (AbstractDevice<?> device : getUnpairedDevices()) {
			dropUnpairedDevice(device);
		}
	}

	boolean isAntDevice(@NonNull AbstractDevice<?> device) {
		return device instanceof AntAbstractDevice<?>;
	}

	boolean isBLEDevice(@NonNull AbstractDevice<?> device) {
		return device instanceof BLEAbstractDevice;
	}

	@NonNull
	List<AbstractDevice<?>> getDevices() {
		return new ArrayList<>(devices.values());
	}

	@NonNull
	List<AbstractDevice<?>> getPairedDevices() {
		List<AbstractDevice<?>> res = new ArrayList<>();
		for (AbstractDevice<?> device : getDevices()) {
			if (isDevicePaired(device)) {
				res.add(device);
			}
		}
		return res;
	}

	@Nullable
	AbstractDevice<?> getPairedDeviceById(@NonNull String deviceId) {
		for (AbstractDevice<?> device : getDevices()) {
			if (isDevicePaired(device) && deviceId.equals(device.getDeviceId())) {
				return device;
			}
		}
		return null;
	}

	@NonNull
	List<AbstractDevice<?>> getUnpairedDevices() {
		List<AbstractDevice<?>> res = new ArrayList<>();
		for (AbstractDevice<?> device : getDevices()) {
			if (!isDevicePaired(device)) {
				res.add(device);
			}
		}
		return res;
	}

	@Nullable
	AbstractDevice<?> getAnyDevice(@NonNull String deviceId) {
		for (AbstractDevice<?> device : getDevices()) {
			if (Algorithms.stringsEqual(device.getDeviceId(), deviceId)) {
				return device;
			}
		}
		return null;
	}

	void askAntPluginInstall() {
		if (activity == null || installAntPluginAsked) {
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
		installAntPluginAsked = true;
	}

	@Override
	public void onDeviceConnecting(@NonNull AbstractDevice<?> device) {
	}

	@Override
	public void onDeviceConnect(@NonNull AbstractDevice<?> device, @NonNull DeviceConnectionResult result, @Nullable String error) {
		if (!Algorithms.isEmpty(error)) {
			LOG.error(device + " sensor connection error: " + error);
		}
		switch (result) {
			case SUCCESS:
				LOG.debug(device + " sensor connected");
				if (antScanning && isAntDevice(device)) {
					devices.put(device.getDeviceId(), device);
				} else if (bleScanning && isBLEDevice(device)) {
					// skip
				} else {
					if (!isDeviceEnabled(device)) {
						updateDevice(activity, device);
					} else {
						app.showShortToastMessage(R.string.device_connected, getFormattedDevicePropertyValue(device, NAME));
					}
				}
				break;
			case DEPENDENCY_NOT_INSTALLED:
				if (isAntDevice(device)) {
					LOG.debug("ANT+ External Sensors plugin is not installed. Ask plugin install");
					askAntPluginInstall();
				}
				break;
			case SEARCH_TIMEOUT:
				if (!antScanning && isAntDevice(device) || !bleScanning && isBLEDevice(device)) {
					if (!isDeviceEnabled(device)) {
						updateDevice(activity, device);
					} else {
						LOG.debug("Reconnect " + device + " after timeout");
						connectDevice(activity, device);
					}
				}
				break;
			default:
				break;
		}
	}

	@Override
	public void onDeviceDisconnect(@NonNull AbstractDevice<?> device) {
		LOG.debug(device + " disconnected");
		app.showShortToastMessage(R.string.device_disconnected, getFormattedDevicePropertyValue(device, NAME));
	}

	@Override
	public void onSensorData(@NonNull AbstractSensor sensor, @NonNull SensorData data) {
		for (SensorDataField dataField : data.getDataFields()) {
			FormattedValue fmtValue = dataField.getFormattedValue(app);
			if (fmtValue != null) {
				LOG.debug("onSensorData '" + sensor.getDevice().getName() + "' <" + sensor.getName() + ">: "
						+ fmtValue.value + (!Algorithms.isEmpty(fmtValue.unit) ? " " + fmtValue.unit : ""));
			}
		}
	}

	public boolean isDevicePaired(@NonNull AbstractDevice<?> device) {
		DevicesSettingsCollection.DeviceSettings settings = devicesSettingsCollection.getDeviceSettings(device.getDeviceId());
		return settings != null;
	}

	public void setDevicePaired(@NonNull AbstractDevice<?> device, boolean paired) {
		String deviceId = device.getDeviceId();
		DeviceSettings settings = devicesSettingsCollection.getDeviceSettings(deviceId);
		if (!paired) {
			devicesSettingsCollection.setDeviceSettings(deviceId, null);
			dropUnpairedDevice(device);
		} else {
			if (settings == null) {
				if (!Algorithms.isEmpty(deviceId)) {
					settings = DevicesSettingsCollection.createDeviceSettings(deviceId, device, true);
					devicesSettingsCollection.setDeviceSettings(deviceId, settings);
					updateDeviceProperties(device);
					externalSensorsPlugin.onDevicePaired(device);
				}
			}
			//connectDevice(activity, device);
		}
		app.runInUIThread(() -> {
			MapActivity mapActivity = getMapActivity();
			if (mapActivity != null) {
				mapActivity.updateApplicationModeSettings();
			}
		});
	}

	public boolean isDeviceEnabled(@NonNull AbstractDevice<?> device) {
		DeviceSettings settings = devicesSettingsCollection.getDeviceSettings(device.getDeviceId());
		return settings != null && settings.getDeviceEnabled();
	}

	public void setDeviceEnabled(@NonNull AbstractDevice<?> device, boolean enabled) {
		String deviceId = device.getDeviceId();
		DeviceSettings settings = devicesSettingsCollection.getDeviceSettings(deviceId);
		if (settings == null) {
			if (!Algorithms.isEmpty(deviceId)) {
				settings = DevicesSettingsCollection.createDeviceSettings(deviceId, device, enabled);
				devicesSettingsCollection.setDeviceSettings(deviceId, settings);
			}
		} else {
			settings.setDeviceEnabled(enabled);
			devicesSettingsCollection.setDeviceSettings(deviceId, settings);
		}
	}

	@NonNull
	public String getFormattedDevicePropertyValue(@NonNull AbstractDevice<?> device, @NonNull DeviceChangeableProperty property) {
		DeviceSettings settings = devicesSettingsCollection.getDeviceSettings(device.getDeviceId());
		String value = settings != null ? settings.getParams().get(property) : null;
		return property.getFormattedValue(app, value);
	}

	public void setDeviceProperty(@NonNull AbstractDevice<?> device, @NonNull DeviceChangeableProperty property, @NonNull String value) {
		String normalizedValue = property.normalizeValue(app, value);
		String deviceId = device.getDeviceId();
		DeviceSettings settings = devicesSettingsCollection.getDeviceSettings(deviceId);
		if (settings == null) {
			if (!Algorithms.isEmpty(deviceId)) {
				settings = DevicesSettingsCollection.createDeviceSettings(deviceId, device, false);
			}
		}
		if (settings != null) {
			settings.setDeviceProperty(property, normalizedValue);
			device.setChangeableProperty(property, value);
			devicesSettingsCollection.setDeviceSettings(deviceId, settings);
		}
	}

	@Override
	public void onDeviceEnabled(@NonNull String deviceId) {
		app.runInUIThread(() -> updateDevices(activity));
	}

	@Override
	public void onDeviceDisabled(@NonNull String deviceId) {
		app.runInUIThread(() -> updateDevices(activity));
	}

	public void scanAntDevices(boolean enable) {
		if (enable) {
			antSearchableDevices = Arrays.asList(
					AntTemperatureDevice.createSearchableDevice(),
					AntHeartRateDevice.createSearchableDevice(),
					AntBikeSpeedCadenceDevice.createSearchableDevice(),
					AntBikeSpeedDistanceDevice.createSearchableDevice(app),
					AntBikePowerDevice.createSearchableDevice());

			for (AntAbstractDevice<?> device : antSearchableDevices) {
				connectDevice(activity, device);
			}
			antScanning = true;
		} else {
			for (AntAbstractDevice<?> device : antSearchableDevices) {
				disconnectDevice(device, false);
			}
			antSearchableDevices = new ArrayList<>();
			antScanning = false;
		}
	}

	@SuppressLint("MissingPermission")
	public void scanBLEDevices(boolean enable) {
		if (!enable) {
			if (bleScanner != null) {
				if (AndroidUtils.hasBLEPermission(activity)
						&& bleScanning
						&& AndroidUtils.isBluetoothEnabled(activity)) {
					bleScanner.stopScan(bleScanCallback);
				}
				bleScanning = false;
			}
		} else {
			if (!AndroidUtils.requestBLEPermissions(activity)) {
				app.showShortToastMessage("Permissions not granted");
				return;
			}
			if (!requestBLE()) {
				app.showShortToastMessage("Bluetooth isnt available");
				return;
			}

			if (bleScanner == null) {
				initBLE();
			}

			if (bleScanner == null) {
				app.showShortToastMessage("Can't initialize ble");
				return;
			}

			ArrayList<ScanFilter> filters = new ArrayList<>();
			for (UUID serviceUUID : SUPPORTED_BLE_SERVICE_UUIDS) {
				ScanFilter filter = new ScanFilter.Builder()
						.setServiceUuid(new ParcelUuid(serviceUUID))
						.build();
				filters.add(filter);
			}
			ScanSettings scanSettings = new ScanSettings.Builder()
					.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
					.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
					.setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
					.setNumOfMatches(ScanSettings.MATCH_NUM_ONE_ADVERTISEMENT)
					.setReportDelay(0L)
					.build();

			bleScanner.startScan(filters, scanSettings, bleScanCallback);
			bleScanning = true;
		}
	}

	public boolean isBLEEnabled() {
		return activity != null && BLEUtils.INSTANCE.isBLEEnabled(activity);
	}

	public boolean requestBLE() {
		boolean bluetoothEnabled = true;
		if (activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
				bluetoothEnabled = false;
				MapActivity mapActivity = getMapActivity();
				if (mapActivity != null) {
					try {
						Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
						mapActivity.startActivityForResult(intent, ENABLE_BLUETOOTH_REQUEST_CODE);
						mapActivity.registerActivityResultListener(new ActivityResultListener(ENABLE_BLUETOOTH_REQUEST_CODE, (resultCode, resultData) -> {
							if (resultCode != Activity.RESULT_OK) {
								app.showShortToastMessage(R.string.no_bt_permission);
							}
						}));
					} catch (ActivityNotFoundException e) {
						app.showToastMessage(R.string.no_activity_for_intent);
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

	@SuppressLint("MissingPermission")
	public boolean isBLEDeviceConnected(@NonNull String address) {
		if (isBLEEnabled() && AndroidUtils.hasBLEPermission(activity)) {
			BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
			BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
			return bluetoothManager.getConnectionState(device, BluetoothProfile.GATT) == BluetoothProfile.STATE_CONNECTED;
		}
		return false;
	}
}