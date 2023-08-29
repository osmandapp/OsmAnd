package net.osmand.plus.plugins.externalsensors;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_ANT_PLUS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_ANT_PLUS;
import static net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperties.NAME;
import static net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType.BIKE_CADENCE;
import static net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType.BIKE_DISTANCE;
import static net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType.BIKE_POWER;
import static net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType.BIKE_SPEED;
import static net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType.HEART_RATE;
import static net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType.TEMPERATURE;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.github.mikephil.charting.charts.LineChart;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.charts.GPXDataSetAxisType;
import net.osmand.plus.charts.GPXDataSetType;
import net.osmand.plus.charts.OrderedLineDataSet;
import net.osmand.plus.chooseplan.OsmAndFeature;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.AbstractSensor;
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperties;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorTextWidget;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.plugins.externalsensors.dialogs.ExternalDevicesListFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
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
import java.util.List;

public class ExternalSensorsPlugin extends OsmandPlugin {
	private static final Log LOG = PlatformUtil.getLog(ExternalSensorsPlugin.class);
	private static final int DEVICES_SEARCH_TIMEOUT = 10000;

	private final DevicesHelper devicesHelper;
	private ScanDevicesListener scanDevicesListener;
	private OsmandSettings settings;

	public final CommonPreference<String> SPEED_SENSOR_WRITE_TO_TRACK_DEVICE;
	public final CommonPreference<String> CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE;
	public final CommonPreference<String> POWER_SENSOR_WRITE_TO_TRACK_DEVICE;
	public final CommonPreference<String> HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE;
	public final CommonPreference<String> TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE;

	public ExternalSensorsPlugin(@NonNull OsmandApplication app) {
		super(app);
		SPEED_SENSOR_WRITE_TO_TRACK_DEVICE = registerStringPreference("speed_sensor_write_to_track_device", "").makeProfile().cache();
		CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE = registerStringPreference("cadence_sensor_write_to_track_device", "").makeProfile().cache();
		POWER_SENSOR_WRITE_TO_TRACK_DEVICE = registerStringPreference("power_sensor_write_to_track_device", "").makeProfile().cache();
		HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE = registerStringPreference("heart_rate_sensor_write_to_track_device", "").makeProfile().cache();
		TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE = registerStringPreference("temperature_sensor_write_to_track_device", "").makeProfile().cache();
		devicesHelper = new DevicesHelper(app, this);
		settings = app.getSettings();
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
	public CharSequence getDescription(boolean linksEnabled) {
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

	private boolean isDeviceForWidgetFieldType(@NonNull AbstractDevice<?> device, @NonNull SensorWidgetDataFieldType fieldType) {
		for (AbstractSensor sensor : device.getSensors()) {
			for (SensorWidgetDataFieldType type :
					sensor.getSupportedWidgetDataFieldTypes()) {
				if (type == fieldType) {
					return true;
				}
			}
		}
		return false;
	}

	@NonNull
	public List<AbstractDevice<?>> getPairedDevicesByWidgetType(@NonNull SensorWidgetDataFieldType fieldType) {
		List<AbstractDevice<?>> devices = getPairedDevices();
		ArrayList<AbstractDevice<?>> filteredDevices = new ArrayList<>();
		for (AbstractDevice<?> device : devices) {
			if (isDeviceForWidgetFieldType(device, fieldType)) {
				filteredDevices.add(device);
			}
		}

		return filteredDevices;
	}


	@Nullable
	public AbstractDevice<?> getPairedDeviceById(String deviceId) {
		return devicesHelper.getPairedDeviceById(deviceId);
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
	protected void attachAdditionalInfoToRecordedTrack(@NonNull Location location, @NonNull JSONObject json) {
		for (WriteToGpxWidgetType writeToGpxWidgetType : WriteToGpxWidgetType.values()) {
			attachDeviceSensorInfoToRecordedTrack(writeToGpxWidgetType, json);
		}
	}

	private void attachDeviceSensorInfoToRecordedTrack(WriteToGpxWidgetType writeToGpxWidgetType, JSONObject json) {
		ApplicationMode selectedAppMode = settings.getApplicationMode();
		CommonPreference<String> preference = getPrefSettingsForWidgetType(writeToGpxWidgetType);
		String speedDeviceId = preference.getModeValue(selectedAppMode);
		if (!Algorithms.isEmpty(speedDeviceId)) {
			AbstractDevice<?> device = devicesHelper.getDevice(speedDeviceId);
			if (device != null) {
				try {
					device.writeSensorDataToJson(json, writeToGpxWidgetType.getSensorType());
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

		MapWidget heartRateWidget = new SensorTextWidget(mapActivity, appMode, HEART_RATE);
		widgetsInfos.add(creator.createWidgetInfo(heartRateWidget));

		MapWidget bikePowerWidget = new SensorTextWidget(mapActivity, appMode, BIKE_POWER);
		widgetsInfos.add(creator.createWidgetInfo(bikePowerWidget));

		MapWidget bikeCadenceWidget = new SensorTextWidget(mapActivity, appMode, BIKE_SPEED);
		widgetsInfos.add(creator.createWidgetInfo(bikeCadenceWidget));

		MapWidget bikeSpeedWidget = new SensorTextWidget(mapActivity, appMode, BIKE_CADENCE);
		widgetsInfos.add(creator.createWidgetInfo(bikeSpeedWidget));

		MapWidget bikeDistanceWidget = new SensorTextWidget(mapActivity, appMode, BIKE_DISTANCE);
		widgetsInfos.add(creator.createWidgetInfo(bikeDistanceWidget));

		MapWidget temperatureWidget = new SensorTextWidget(mapActivity, appMode, TEMPERATURE);
		widgetsInfos.add(creator.createWidgetInfo(temperatureWidget));
	}

	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId) {
		ApplicationMode appMode = settings.getApplicationMode();
		switch (widgetType) {
			case HEART_RATE:
				return new SensorTextWidget(mapActivity, appMode, HEART_RATE, customId);
			case BICYCLE_POWER:
				return new SensorTextWidget(mapActivity, appMode, BIKE_POWER, customId);
			case BICYCLE_CADENCE:
				return new SensorTextWidget(mapActivity, appMode, BIKE_CADENCE, customId);
			case BICYCLE_SPEED:
				return new SensorTextWidget(mapActivity, appMode, BIKE_SPEED, customId);
			case BICYCLE_DISTANCE:
				return new SensorTextWidget(mapActivity, appMode, BIKE_DISTANCE, customId);
			case TEMPERATURE:
				return new SensorTextWidget(mapActivity, appMode, TEMPERATURE, customId);
		}
		return null;
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

	@NonNull
	public String getDeviceName(@NonNull AbstractDevice<?> device) {
		String sensorName = devicesHelper.getDeviceProperty(device, NAME);
		return !Algorithms.isEmpty(sensorName) ? sensorName : device.getName();
	}

	public void changeDeviceName(@NonNull String deviceId, @NonNull String newName) {
		AbstractDevice<?> device = getDevice(deviceId);
		if (device != null) {
			devicesHelper.setDeviceProperty(device, DeviceChangeableProperties.NAME, newName);
		}
	}

	public CommonPreference<String> getPrefSettingsForWidgetType(@NonNull WriteToGpxWidgetType widgetType) {
		switch (widgetType) {
			case BIKE_SPEED:
				return SPEED_SENSOR_WRITE_TO_TRACK_DEVICE;
			case BIKE_POWER:
				return POWER_SENSOR_WRITE_TO_TRACK_DEVICE;
			case BIKE_CADENCE:
				return CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE;
			case HEART_RATE:
				return HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE;
			case TEMPERATURE:
				return TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE;
		}
		throw new IllegalArgumentException("Unknown widget type");
	}

	@Override
	protected void onAnalysePoint(@NonNull GPXTrackAnalysis analysis, @NonNull WptPt point,
	                              float distance, int timeDiff, boolean firstPoint, boolean lastPoint) {
		SensorAttributesUtils.onAnalysePoint(analysis, point, distance, timeDiff, firstPoint, lastPoint);
	}

	@Nullable
	@Override
	public OrderedLineDataSet getOrderedLineDataSet(@NonNull LineChart chart,
	                                                @NonNull GPXTrackAnalysis analysis,
	                                                @NonNull GPXDataSetType graphType,
	                                                @NonNull GPXDataSetAxisType axisType,
	                                                boolean calcWithoutGaps, boolean useRightAxis) {
		return SensorAttributesUtils.getOrderedLineDataSet(app, chart, analysis, graphType, axisType, calcWithoutGaps, useRightAxis);
	}

	@Override
	public void getAvailableGPXDataSetTypes(@NonNull GPXTrackAnalysis analysis, @NonNull List<GPXDataSetType[]> availableTypes) {
		SensorAttributesUtils.getAvailableGPXDataSetTypes(analysis, availableTypes);
	}

	public void setDeviceProperty(@NonNull AbstractDevice<?> device, @NonNull DeviceChangeableProperties property, @NonNull String value) {
		devicesHelper.setDeviceProperty(device, property, value);
	}

	public String getDeviceProperty(@NonNull AbstractDevice<?> device, @NonNull DeviceChangeableProperties property) {
		return devicesHelper.getDeviceProperty(device, property);
	}

	@StringRes
	public int getPropertyMetric(@NonNull DeviceChangeableProperties property, boolean shortForm) {
		return devicesHelper.getPropertyMetric(property, shortForm);
	}
}