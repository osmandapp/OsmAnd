package net.osmand.plus.plugins.antplus;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_ANT_PLUS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_ANT_PLUS;
import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_CADENCE;
import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_DISTANCE;
import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_POWER;
import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_BICYCLE_SPEED;
import static net.osmand.plus.views.mapwidgets.WidgetType.ANT_HEART_RATE;

import android.app.Activity;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.api.SettingsAPI;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeCadenceDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeDistanceDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikePowerDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntBikeSpeedDevice;
import net.osmand.plus.plugins.antplus.antdevices.AntHeartRateDevice;
import net.osmand.plus.plugins.antplus.devices.CommonDevice;
import net.osmand.plus.plugins.antplus.devices.CommonDevice.IPreferenceFactory;
import net.osmand.plus.plugins.antplus.devices.DeviceType;
import net.osmand.plus.plugins.antplus.dialogs.AntPlusSensorsListFragment;
import net.osmand.plus.plugins.antplus.models.BatteryData;
import net.osmand.plus.plugins.antplus.models.BleDeviceData;
import net.osmand.plus.plugins.antplus.widgets.BikeCadenceTextWidget;
import net.osmand.plus.plugins.antplus.widgets.BikeDistanceTextWidget;
import net.osmand.plus.plugins.antplus.widgets.BikePowerTextWidget;
import net.osmand.plus.plugins.antplus.widgets.BikeSpeedTextWidget;
import net.osmand.plus.plugins.antplus.widgets.HeartRateTextWidget;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AntPlusPlugin extends OsmandPlugin implements IPreferenceFactory, BleConnectionStateListener, BleDataListener {
	private static final Log log = PlatformUtil.getLog(AntPlusPlugin.class);
	private static final String PREFERENCES_NAME = "net.osmand.plugins.ble";
	private static final String PAIRED_DEVICES_KEY = "paired_devices";

	private final DevicesHelper devicesHelper;
	private ScanForDevicesListener scanForDevicesListener;
	private ArrayList<ExternalDevice> pairedBleDevices = new ArrayList<>();
	private ArrayList<BleConnectionStateListener> stateChangeListeners = new ArrayList<>();
	private ArrayList<BleDataListener> bleDeviceDataListeners = new ArrayList<>();
	private Gson gson;

	public AntPlusPlugin(OsmandApplication app) {
		super(app);
		gson = new GsonBuilder().create();
		SettingsAPI settingsAPI = app.getSettings().getSettingsAPI();
		Object blePrefs = settingsAPI.getPreferenceObject(PREFERENCES_NAME);
		String customPluginsJson = settingsAPI.getString(blePrefs, PAIRED_DEVICES_KEY, "");
		if (!Algorithms.isEmpty(customPluginsJson)) {
			List<ExternalDevice> pairedDevices = gson.fromJson(customPluginsJson, new TypeToken<List<ExternalDevice>>() {
			}.getType());
			pairedBleDevices.addAll(pairedDevices);
		}
		devicesHelper = new DevicesHelper(app, this);
	}

	@Override
	public String getId() {
		return PLUGIN_ANT_PLUS;
	}

	@Override
	public String getName() {
		return app.getString(R.string.external_sensors_plugin_name);
	}

	@Override
	public boolean isPaid() {
		return true;
	}

	@Override
	public boolean isLocked() {
		return !Version.isPaidVersion(app);
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.external_sensors_plugin_description);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_external_sensor;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.osmand_development);
	}

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.ANT_PLUS_SETTINGS;
	}

	@Nullable
	@Override
	public OsmAndFeature getOsmAndFeature() {
		return OsmAndFeature.EXTERNAL_SENSORS_SUPPORT;
	}

	@Nullable
	<T extends CommonDevice<?>> T getDevice(@NonNull Class<T> clz) {
		return devicesHelper.getDevice(clz);
	}

	@NonNull
	List<CommonDevice<?>> getDevices() {
		return devicesHelper.getDevices();
	}

	@Override
	protected void attachAdditionalInfoToRecordedTrack(Location location, JSONObject json) {
		for (CommonDevice<?> device : devicesHelper.getDevices()) {
			if (device.isEnabled() && device.shouldWriteGpx() && device.getAntDevice().isConnected()) {
				try {
					device.writeDataToJson(json);
				} catch (JSONException e) {
					log.error(e);
				}
			}
		}
	}

	@Override
	public void mapActivityCreate(@NonNull MapActivity activity) {
		devicesHelper.setActivity(activity);
	}

	@Override
	public void mapActivityDestroy(@NonNull MapActivity activity) {
		devicesHelper.setActivity(null);
	}

	@Override
	public boolean init(@NonNull OsmandApplication app, Activity activity) {
		devicesHelper.setActivity(activity);
		devicesHelper.setConnectionStateListener(this);
		devicesHelper.setBleDataListenerListener(this);
		return true;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		super.disable(app);
		devicesHelper.disconnectAntDevices();
		devicesHelper.disconnectBLE();
		devicesHelper.setConnectionStateListener(null);
		devicesHelper.setBleDataListenerListener(null);
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos, @NonNull ApplicationMode appMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);

		MapWidget heartRateWidget = createMapWidgetForParams(mapActivity, ANT_HEART_RATE);
		widgetsInfos.add(creator.createWidgetInfo(heartRateWidget));

		MapWidget bikePowerWidget = createMapWidgetForParams(mapActivity, ANT_BICYCLE_POWER);
		widgetsInfos.add(creator.createWidgetInfo(bikePowerWidget));

		MapWidget bikeCadenceWidget = createMapWidgetForParams(mapActivity, ANT_BICYCLE_CADENCE);
		widgetsInfos.add(creator.createWidgetInfo(bikeCadenceWidget));

		MapWidget bikeSpeedWidget = createMapWidgetForParams(mapActivity, ANT_BICYCLE_SPEED);
		widgetsInfos.add(creator.createWidgetInfo(bikeSpeedWidget));

		MapWidget bikeDistanceWidget = createMapWidgetForParams(mapActivity, ANT_BICYCLE_DISTANCE);
		widgetsInfos.add(creator.createWidgetInfo(bikeDistanceWidget));
	}

	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType) {
		switch (widgetType) {
			case ANT_HEART_RATE:
				AntHeartRateDevice heartRateDevice = devicesHelper.getAntDevice(AntHeartRateDevice.class);
				return heartRateDevice != null ? new HeartRateTextWidget(mapActivity, heartRateDevice) : null;
			case ANT_BICYCLE_POWER:
				AntBikePowerDevice powerDevice = devicesHelper.getAntDevice(AntBikePowerDevice.class);
				return powerDevice != null ? new BikePowerTextWidget(mapActivity, powerDevice) : null;
			case ANT_BICYCLE_CADENCE:
				AntBikeCadenceDevice cadenceDevice = devicesHelper.getAntDevice(AntBikeCadenceDevice.class);
				return cadenceDevice != null ? new BikeCadenceTextWidget(mapActivity, cadenceDevice) : null;
			case ANT_BICYCLE_SPEED:
				AntBikeSpeedDevice speedDevice = devicesHelper.getAntDevice(AntBikeSpeedDevice.class);
				return speedDevice != null ? new BikeSpeedTextWidget(mapActivity, speedDevice) : null;
			case ANT_BICYCLE_DISTANCE:
				AntBikeDistanceDevice distanceDevice = devicesHelper.getAntDevice(AntBikeDistanceDevice.class);
				return distanceDevice != null ? new BikeDistanceTextWidget(mapActivity, distanceDevice) : null;
		}
		return null;
	}

	@Override
	public CommonPreference<Boolean> registerBooleanPref(@NonNull String prefId, boolean defValue) {
		return registerBooleanPreference(prefId, defValue).makeGlobal().makeShared();
	}

	@Override
	public CommonPreference<Integer> registerIntPref(@NonNull String prefId, int defValue) {
		return registerIntPreference(prefId, defValue).makeGlobal().makeShared();
	}

	@Override
	public void registerOptionsMenuItems(MapActivity mapActivity, ContextMenuAdapter helper) {
		if (isActive()) {
			helper.addItem(new ContextMenuItem(DRAWER_ANT_PLUS_ID)
					.setTitleId(R.string.external_sensors_plugin_name, mapActivity)
					.setIcon(R.drawable.ic_action_sensor)
					.setListener((uiAdapter, view, item, isChecked) -> {
						app.logEvent("externalSettingsOpen");
						AntPlusSensorsListFragment.showInstance(mapActivity.getSupportFragmentManager());
						return true;
					}));
		}
	}

	public boolean isBlueToothEnabled() {
		return devicesHelper.isBleEnabled();
	}

	public void searchForDevices() {
		devicesHelper.scanLeDevice(true);
		new Handler(Looper.myLooper()).postDelayed(this::finishScanBle, 10000);
	}

	private void finishScanBle() {
		devicesHelper.scanLeDevice(false);
		if (scanForDevicesListener != null) {
			scanForDevicesListener.onScanFinished(new HashMap<>(devicesHelper.leDevices));
		}
	}

	public ArrayList<ExternalDevice> getLastFoundDevices() {
		ArrayList<ExternalDevice> foundDevices = new ArrayList<>();
		for (ScanResult result : devicesHelper.leDevices.values()) {
			ScanRecord scanRecord = result.getScanRecord();
			List<ParcelUuid> uuids = scanRecord.getServiceUuids();
			DeviceType foundDeviceType = null;
			for (ParcelUuid uuid : uuids) {
				DeviceType deviceType = DeviceType.getDeviceTypeByUuid(uuid.getUuid());
				if (deviceType != null) {
					foundDeviceType = deviceType;
					break;
				}
			}
			foundDevices.add(new ExternalDevice(result.getDevice().getName(),
					isDevicePaired(result.getDevice().getAddress()),
					isBleDeviceConnected(result.getDevice().getAddress()),
					result.getDevice().getAddress(),
					ExternalDevice.DeviceConnectionType.BLE,
					foundDeviceType,
					result.getRssi()));
		}
		return foundDevices;
	}

	public void setScanForDevicesListener(ScanForDevicesListener listener) {
		scanForDevicesListener = listener;
	}

	@Override
	public void onStateChanged(@Nullable String address, int newState) {
		new Handler(Looper.getMainLooper()).post(()->{
			if (newState == BluetoothProfile.STATE_CONNECTED) {
				Toast.makeText(app, R.string.external_device_connected, Toast.LENGTH_SHORT).show();
			} else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				Toast.makeText(app, R.string.external_device_disconnected, Toast.LENGTH_SHORT).show();
			}
		});
		for (BleConnectionStateListener listener : stateChangeListeners) {
			listener.onStateChanged(address, newState);
		}
	}

	public void addBleDeviceConnectionStateListener(BleConnectionStateListener listener) {
		stateChangeListeners.add(listener);
	}

	public void removeBleDeviceConnectionStateListener(BleConnectionStateListener listener) {
		stateChangeListeners.remove(listener);
	}

	public void addBleDeviceDataListener(BleDataListener listener) {
		bleDeviceDataListeners.add(listener);
	}

	public void removeBleDeviceDataListener(BleDataListener listener) {
		bleDeviceDataListeners.remove(listener);
	}

	@Override
	public void onDataReceived(@Nullable String address, @NonNull BleDeviceData data) {
		if (data instanceof BatteryData) {
			for (ExternalDevice device :
					pairedBleDevices) {
				if (device.getAddress().equals(address)) {
					device.setBatteryLevel(((BatteryData) data).getBatteryLevel());
					break;
				}
			}
		}
		for (BleDataListener listener : bleDeviceDataListeners) {
			listener.onDataReceived(address, data);
		}
	}

	public interface ScanForDevicesListener {
		void onScanFinished(HashMap<String, ScanResult> foundDevices);
	}

	public boolean isDeviceConnected(ExternalDevice device) {
		if (device.getConnectionType() == ExternalDevice.DeviceConnectionType.BLE) {
			return isBleDeviceConnected(device.getAddress());
		} else {
			return false;
		}
	}

	private boolean isBleDeviceConnected(String deviceAddress) {
		return devicesHelper.isBleConnected(deviceAddress);
	}

	public boolean isDevicePaired(ExternalDevice device) {
		return isDevicePaired(device.getAddress());
	}

	public boolean isDevicePaired(String deviceAddress) {
		ArrayList<ExternalDevice> tmpDevicesList = new ArrayList<>(pairedBleDevices);
		for (ExternalDevice pairedDevice : tmpDevicesList) {
			if (pairedDevice.getAddress().equals(deviceAddress)) {
				return true;
			}
		}
		return false;
	}

	public void unpairDevice(String address) {
		for (ExternalDevice device : pairedBleDevices) {
			if (device.getAddress().equals(address)) {
				pairedBleDevices.remove(device);
				savePairedDevices();
				break;
			}
		}
	}

	public void pairDevice(ExternalDevice device) {
		if (!isDevicePaired(device)) {
			pairedBleDevices.add(device);
			savePairedDevices();
		}
	}

	private void savePairedDevices() {
		SettingsAPI settingsAPI = app.getSettings().getSettingsAPI();
		Object blePrefs = settingsAPI.getPreferenceObject(PREFERENCES_NAME);
		String devicesJson = gson.toJson(pairedBleDevices);
		settingsAPI.edit(blePrefs).putString(PAIRED_DEVICES_KEY, devicesJson).commit();
	}

	public void connectDevice(ExternalDevice device) {
		if (device.getConnectionType() == ExternalDevice.DeviceConnectionType.BLE) {
			devicesHelper.connectBleDevice(device.getAddress());
		} else {
			//todo implement ant+ connection
//			devicesHelper.connectAntDevice();
		}
	}

	public void disconnectDevice(ExternalDevice device) {
		if (device.getConnectionType() == ExternalDevice.DeviceConnectionType.BLE) {
			devicesHelper.disconnectBleDevice();
		} else {
			//todo implement ant+ disconnection
//			devicesHelper.connectAntDevice();
		}
	}

	public ArrayList<ExternalDevice> getPairedDevices() {
		ArrayList<ExternalDevice> devicesList = new ArrayList<>();
		devicesList.addAll(pairedBleDevices);
		return devicesList;
	}

	private ExternalDevice getPairedDevice(@NonNull String deviceAddress){
		for (ExternalDevice device: pairedBleDevices) {
			if(device.getAddress().equals(deviceAddress)){
				return device;
			}
		}
		return null;
	}

	public void changeDeviceName(@NonNull String deviceAddress, @NonNull String newName){
		ExternalDevice device = getPairedDevice(deviceAddress);
		if(device != null){
			device.setName(newName);
			savePairedDevices();
		}
	}

}
