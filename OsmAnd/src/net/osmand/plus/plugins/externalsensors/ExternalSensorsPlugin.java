package net.osmand.plus.plugins.externalsensors;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_ANT_PLUS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_ANT_PLUS;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorTextWidget;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.plugins.externalsensors.dialogs.ExternalDevicesListFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.widgets.ctxmenu.ContextMenuAdapter;
import net.osmand.plus.widgets.ctxmenu.data.ContextMenuItem;

import org.apache.commons.logging.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class ExternalSensorsPlugin extends OsmandPlugin {
	private static final Log LOG = PlatformUtil.getLog(ExternalSensorsPlugin.class);
	private static final int DEVICES_SEARCH_TIMEOUT = 10000;

	private final DevicesHelper devicesHelper;
	private ScanDevicesListener scanDevicesListener;

	public ExternalSensorsPlugin(OsmandApplication app) {
		super(app);
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

	@NonNull
	public List<AbstractDevice<?>> getDevices() {
		return devicesHelper.getDevices();
	}

	@NonNull
	public List<AbstractDevice<?>> getPairedDevices() {
		return devicesHelper.getPairedDevices();
	}

	@NonNull
	public List<AbstractDevice<?>> getUnpairedDevices() {
		return devicesHelper.getUnpairedDevices();
	}

	@Nullable
	public AbstractDevice<?> getDevice(@NonNull String deviceId) {
		return devicesHelper.getDevice(deviceId);
	}

	@Override
	protected void attachAdditionalInfoToRecordedTrack(Location location, JSONObject json) {
		for (AbstractDevice<?> device : devicesHelper.getDevices()) {
			if (devicesHelper.isDeviceEnabled(device) && devicesHelper.shouldDeviceWriteGpx(device)
					&& device.isConnected()) {
				try {
					device.writeSensorDataToJson(json);
				} catch (JSONException e) {
					LOG.error(e);
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
		return true;
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
		super.disable(app);
		devicesHelper.disconnectDevices();
		devicesHelper.deinitBLE();
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos,
	                          @NonNull ApplicationMode appMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);
		for (AbstractDevice<?> device : getDevices()) {
			for (AbstractSensor sensor : device.getSensors()) {
				for (SensorWidgetDataFieldType fieldType : sensor.getSupportedWidgetDataFieldTypes()) {
					MapWidget widget = new SensorTextWidget(mapActivity, sensor, fieldType);
					widgetsInfos.add(creator.createWidgetInfo(widget));
				}
			}
		}
	}

	public CommonPreference<Boolean> registerBooleanPref(@NonNull String prefId, boolean defValue) {
		return registerBooleanPreference(prefId, defValue).makeGlobal().makeShared();
	}

	public CommonPreference<Integer> registerIntPref(@NonNull String prefId, int defValue) {
		return registerIntPreference(prefId, defValue).makeGlobal().makeShared();
	}

	public CommonPreference<String> registerStringPref(@NonNull String prefId, @Nullable String defValue) {
		return registerStringPreference(prefId, defValue).makeGlobal().makeShared();
	}

	@Override
	public void registerOptionsMenuItems(MapActivity mapActivity, ContextMenuAdapter helper) {
		if (isActive()) {
			helper.addItem(new ContextMenuItem(DRAWER_ANT_PLUS_ID)
					.setTitleId(R.string.external_sensors_plugin_name, mapActivity)
					.setIcon(R.drawable.ic_action_sensor)
					.setListener((uiAdapter, view, item, isChecked) -> {
						app.logEvent("externalSettingsOpen");
						ExternalDevicesListFragment.showInstance(mapActivity.getSupportFragmentManager());
						return true;
					}));
		}
	}

	public boolean isBlueToothEnabled() {
		return devicesHelper.isBLEEnabled();
	}

	public void searchAntDevices() {
		devicesHelper.scanAntDevices(true);
		new Handler(Looper.myLooper()).postDelayed(this::finishAntDevicesSearch, DEVICES_SEARCH_TIMEOUT);
	}

	private void finishAntDevicesSearch() {
		devicesHelper.scanAntDevices(false);
		if (scanDevicesListener != null) {
			scanDevicesListener.onScanFinished(devicesHelper.getUnpairedDevices());
		}
	}

	public void searchBLEDevices() {
		devicesHelper.scanBLEDevices(true);
		new Handler(Looper.myLooper()).postDelayed(this::finishBLEDevicesSearch, DEVICES_SEARCH_TIMEOUT);
	}

	private void finishBLEDevicesSearch() {
		devicesHelper.scanBLEDevices(false);
		if (scanDevicesListener != null) {
			scanDevicesListener.onScanFinished(devicesHelper.getUnpairedDevices());
		}
	}

	public void setScanDevicesListener(@Nullable ScanDevicesListener listener) {
		scanDevicesListener = listener;
	}

	public interface ScanDevicesListener {
		void onScanFinished(@NonNull List<AbstractDevice<?>> foundDevices);
	}

	private boolean isBLEDeviceConnected(@NonNull String address) {
		return devicesHelper.isBLEDeviceConnected(address);
	}

	public boolean isDevicePaired(@NonNull AbstractDevice<?> device) {
		return devicesHelper.isDevicePaired(device);
	}

	public void pairDevice(@NonNull AbstractDevice<?> device) {
		if (!isDevicePaired(device)) {
			devicesHelper.setDevicePaired(device, true);
		}
	}

	public void unpairDevice(@NonNull AbstractDevice<?> device) {
		if (isDevicePaired(device)) {
			devicesHelper.setDevicePaired(device, false);
		}
	}

	public void dropUnpairedDevices() {
		devicesHelper.dropUnpairedDevices();
	}

	public void connectDevice(@Nullable Activity activity, @NonNull AbstractDevice<?> device) {
		if (isDevicePaired(device)) {
			devicesHelper.setDeviceEnabled(device, true);
		}
		devicesHelper.connectDevice(activity, device);
	}

	public void disconnectDevice(@NonNull AbstractDevice<?> device) {
		if (isDevicePaired(device)) {
			devicesHelper.setDeviceEnabled(device, false);
		}
		devicesHelper.disconnectDevice(device);
	}

	@Nullable
	public String getDeviceName(@NonNull AbstractDevice<?> device) {
		String sensorName = devicesHelper.getDeviceName(device);
		return sensorName != null ? sensorName : device.getName();
	}

	public void changeDeviceName(@NonNull String deviceId, @NonNull String newName) {
		AbstractDevice<?> device = getDevice(deviceId);
		if (device != null) {
			devicesHelper.setDeviceName(device, newName);
		}
	}
}
