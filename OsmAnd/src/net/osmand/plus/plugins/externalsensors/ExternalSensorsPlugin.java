package net.osmand.plus.plugins.externalsensors;

import static net.osmand.aidlapi.OsmAndCustomizationConstants.DRAWER_ANT_PLUS_ID;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_ANT_PLUS;
import static net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty.NAME;
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

import com.github.mikephil.charting.charts.LineChart;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.shared.gpx.GpxTrackAnalysis;
import net.osmand.shared.gpx.GpxTrackAnalysis.TrackPointsAnalyser;
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
import net.osmand.plus.plugins.externalsensors.devices.sensors.DeviceChangeableProperty;
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
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
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
	private static final String ANY_DEVICE = "any_connected_device_write_sensor_data_to_track_key";

	private final OsmandSettings settings;
	private final DevicesHelper devicesHelper;

	public final CommonPreference<String> SPEED_SENSOR_WRITE_TO_TRACK_DEVICE_ID;
	public final CommonPreference<String> CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE_ID;
	public final CommonPreference<String> POWER_SENSOR_WRITE_TO_TRACK_DEVICE_ID;
	public final CommonPreference<String> HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE_ID;
	public final CommonPreference<String> TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE_ID;

	private ScanDevicesListener scanDevicesListener;

	public ExternalSensorsPlugin(@NonNull OsmandApplication app) {
		super(app);
		SPEED_SENSOR_WRITE_TO_TRACK_DEVICE_ID = registerStringPreference(ExternalSensorTrackDataType.BIKE_SPEED.getPreferenceId(), "").makeProfile().cache();
		CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE_ID = registerStringPreference(ExternalSensorTrackDataType.BIKE_CADENCE.getPreferenceId(), "").makeProfile().cache();
		POWER_SENSOR_WRITE_TO_TRACK_DEVICE_ID = registerStringPreference(ExternalSensorTrackDataType.BIKE_POWER.getPreferenceId(), "").makeProfile().cache();
		HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE_ID = registerStringPreference(ExternalSensorTrackDataType.HEART_RATE.getPreferenceId(), "").makeProfile().cache();
		TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE_ID = registerStringPreference(ExternalSensorTrackDataType.TEMPERATURE.getPreferenceId(), "").makeProfile().cache();

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
	public AbstractDevice<?> getPairedDeviceById(@NonNull String deviceId) {
		return devicesHelper.getPairedDeviceById(deviceId);
	}

	@NonNull
	public List<AbstractDevice<?>> getUnpairedDevices() {
		return devicesHelper.getUnpairedDevices();
	}

	@Nullable
	public AbstractDevice<?> getAnyDevice(@NonNull SensorWidgetDataFieldType fieldType) {
		for (AbstractDevice<?> device : getPairedDevices()) {
			for (AbstractSensor sensor : device.getSensors()) {
				List<SensorWidgetDataFieldType> supportedTypes = sensor.getSupportedWidgetDataFieldTypes();
				if (supportedTypes.contains(fieldType)) {
					return device;
				}
			}
		}
		return null;
	}

	@Nullable
	public AbstractDevice<?> getDevice(@NonNull String deviceId) {
		return devicesHelper.getAnyDevice(deviceId);
	}

	@Override
	protected void attachAdditionalInfoToRecordedTrack(@NonNull Location location, @NonNull JSONObject json) {
		for (ExternalSensorTrackDataType externalSensorTrackDataType : ExternalSensorTrackDataType.values()) {
			attachDeviceSensorInfoToRecordedTrack(externalSensorTrackDataType, json);
		}
	}

	private void attachDeviceSensorInfoToRecordedTrack(@NonNull ExternalSensorTrackDataType dataType, @NonNull JSONObject json) {
		CommonPreference<String> preference = getWriteToTrackDeviceIdPref(dataType);
		String deviceId = preference.getModeValue(settings.getApplicationMode());
		if (!Algorithms.isEmpty(deviceId)) {
			boolean anyConnected = ANY_DEVICE.equals(deviceId);
			AbstractDevice<?> deviceById = devicesHelper.getAnyDevice(deviceId);
			ArrayList<AbstractDevice<?>> devices = new ArrayList<>();
			if(anyConnected) {
				devices.addAll(devicesHelper.getDevices());
			} else if(deviceById != null) {
				devices.add(deviceById);
			}
			for (AbstractDevice<?> device : devices) {
				try {
					device.writeSensorDataToJson(json, dataType.getSensorType());
				} catch (JSONException e) {
					LOG.error(e);
				}
			}
		}
	}

	public boolean isAnyConnectedDeviceId(@NonNull String deviceId){
		return ANY_DEVICE.equals(deviceId);
	}

	public String getAnyConnectedDeviceId(){
		return ANY_DEVICE;
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
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		ApplicationMode appMode = settings.getApplicationMode();
		switch (widgetType) {
			case HEART_RATE:
				return new SensorTextWidget(mapActivity, appMode, HEART_RATE, customId, widgetsPanel);
			case BICYCLE_POWER:
				return new SensorTextWidget(mapActivity, appMode, BIKE_POWER, customId, widgetsPanel);
			case BICYCLE_CADENCE:
				return new SensorTextWidget(mapActivity, appMode, BIKE_CADENCE, customId, widgetsPanel);
			case BICYCLE_SPEED:
				return new SensorTextWidget(mapActivity, appMode, BIKE_SPEED, customId, widgetsPanel);
			case BICYCLE_DISTANCE:
				return new SensorTextWidget(mapActivity, appMode, BIKE_DISTANCE, customId, widgetsPanel);
			case TEMPERATURE:
				return new SensorTextWidget(mapActivity, appMode, TEMPERATURE, customId, widgetsPanel);
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
		String sensorName = devicesHelper.getFormattedDevicePropertyValue(device, NAME);
		return !Algorithms.isEmpty(sensorName) ? sensorName : device.getName();
	}

	public void changeDeviceName(@NonNull String deviceId, @NonNull String newName) {
		AbstractDevice<?> device = getDevice(deviceId);
		if (device != null) {
			devicesHelper.setDeviceProperty(device, DeviceChangeableProperty.NAME, newName);
		}
	}

	public CommonPreference<String> getWriteToTrackDeviceIdPref(@NonNull ExternalSensorTrackDataType dataType) {
		switch (dataType) {
			case BIKE_SPEED:
				return SPEED_SENSOR_WRITE_TO_TRACK_DEVICE_ID;
			case BIKE_POWER:
				return POWER_SENSOR_WRITE_TO_TRACK_DEVICE_ID;
			case BIKE_CADENCE:
				return CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE_ID;
			case HEART_RATE:
				return HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE_ID;
			case TEMPERATURE:
				return TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE_ID;
		}
		throw new IllegalArgumentException("Unknown sensor type");
	}

	void onDevicePaired(@NonNull AbstractDevice<?> device) {
		for (AbstractSensor sensor : device.getSensors()) {
			for (SensorWidgetDataFieldType widgetDataFieldType : sensor.getSupportedWidgetDataFieldTypes()) {
				ExternalSensorTrackDataType widgetType = ExternalSensorTrackDataType.Companion.getBySensorWidgetDataFieldType(widgetDataFieldType);
				if (widgetType != null) {
					CommonPreference<String> deviceIdPref = getWriteToTrackDeviceIdPref(widgetType);
					for (ApplicationMode appMode : ApplicationMode.allPossibleValues()) {
						String deviceId = deviceIdPref.getModeValue(appMode);
						if (Algorithms.isEmpty(deviceId)) {
							deviceIdPref.setModeValue(appMode, device.getDeviceId());
						}
					}
				}
			}
		}
	}

	@NonNull
	@Override
	protected TrackPointsAnalyser getTrackPointsAnalyser() {
		return SensorAttributesUtils::onAnalysePoint;
	}

	@Nullable
	@Override
	public OrderedLineDataSet getOrderedLineDataSet(@NonNull LineChart chart,
	                                                @NonNull GpxTrackAnalysis analysis,
	                                                @NonNull GPXDataSetType graphType,
	                                                @NonNull GPXDataSetAxisType axisType,
	                                                boolean calcWithoutGaps, boolean useRightAxis) {
		return SensorAttributesUtils.getOrderedLineDataSet(app, chart, analysis, graphType, axisType, calcWithoutGaps, useRightAxis);
	}

	@Override
	public void getAvailableGPXDataSetTypes(@NonNull GpxTrackAnalysis analysis, @NonNull List<GPXDataSetType[]> availableTypes) {
		SensorAttributesUtils.getAvailableGPXDataSetTypes(analysis, availableTypes);
	}

	public void setDeviceProperty(@NonNull AbstractDevice<?> device, @NonNull DeviceChangeableProperty property, @NonNull String value) {
		devicesHelper.setDeviceProperty(device, property, value);
	}

	public String getFormattedDevicePropertyValue(@NonNull AbstractDevice<?> device, @NonNull DeviceChangeableProperty property) {
		return devicesHelper.getFormattedDevicePropertyValue(device, property);
	}
}