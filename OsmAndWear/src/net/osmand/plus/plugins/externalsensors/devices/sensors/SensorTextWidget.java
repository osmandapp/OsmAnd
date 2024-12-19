package net.osmand.plus.plugins.externalsensors.devices.sensors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.util.Algorithms;

import java.util.List;

public class SensorTextWidget extends SimpleWidget {

	private final ExternalSensorsPlugin plugin;
	private final SensorWidgetDataFieldType fieldType;
	private final CommonPreference<String> deviceIdPref;

	private AbstractSensor sensor;
	private String externalDeviceId;

	private Number cachedNumber;


	public SensorTextWidget(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode,
	                        @NonNull SensorWidgetDataFieldType fieldType, @Nullable String customId,
	                        @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, fieldType.getWidgetType(), customId, widgetsPanel);
		this.fieldType = fieldType;
		plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
		deviceIdPref = registerSensorDevicePref(customId);
		externalDeviceId = getDeviceId(appMode);
		applyDeviceId();
		updateInfo(null);
		setIcons(fieldType.getWidgetType());
	}

	private void applyDeviceId() {
		AbstractDevice<?> currentDevice = null;
		if (externalDeviceId == null || plugin.isAnyConnectedDeviceId(externalDeviceId)) {
			List<AbstractDevice<?>> deviceList = plugin.getPairedDevicesByWidgetType(fieldType);
			if (!Algorithms.isEmpty(deviceList)) {
				currentDevice = deviceList.get(0);
			}
		} else {
			currentDevice = plugin.getPairedDeviceById(externalDeviceId);
		}
		setSensor(getSensor(currentDevice));
	}

	public SensorTextWidget(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode,
	                        @NonNull SensorWidgetDataFieldType fieldType) {
		this(mapActivity, appMode, fieldType, null, null);
	}

	@Nullable
	private AbstractSensor getSensor(@Nullable AbstractDevice<?> device) {
		if (device != null) {
			for (AbstractSensor sensor :
					device.getSensors()) {
				if (sensor.getSupportedWidgetDataFieldTypes().contains(fieldType)) {
					return sensor;
				}
			}
		}
		return null;
	}

	public void setSensor(@Nullable AbstractSensor sensor) {
		if (this.sensor != null) {
			this.sensor.device.removeListener(deviceListener);
		}
		this.sensor = sensor;
		if (sensor != null) {
			sensor.device.addListener(deviceListener);
		}
	}

	@Nullable
	public AbstractDevice<?> getWidgetDevice() {
		return sensor == null ? null : sensor.device;
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		AbstractDevice<?> currentDevice = null;
		if (sensor != null) {
			currentDevice = this.sensor.getDevice();
		}
		if (sensor != null && currentDevice.isConnected() && !Algorithms.isEmpty(sensor.getLastSensorDataList())) {
			List<SensorData> dataList = sensor.getLastSensorDataList();
			SensorWidgetDataField field = null;
			for (SensorData data : dataList) {
				if (data != null) {
					field = data.getWidgetField(fieldType);
					if (field != null) {
						break;
					}
				}
			}
			if (field != null) {
				if (isUpdateNeeded() || !Algorithms.objectEquals(cachedNumber, field.getNumberValue())) {
					cachedNumber = field.getNumberValue();
					FormattedValue formattedValue = field.getFormattedValue(app);
					if (formattedValue != null) {
						setText(formattedValue.value, formattedValue.unit);
					} else {
						setText(NO_VALUE, null);
					}
				}
			} else {
				setText(NO_VALUE, null);
			}
		} else {
			setText(NO_VALUE, null);
		}
		if (plugin.isAnyConnectedDeviceId(externalDeviceId) &&
				(currentDevice == null || (!currentDevice.isConnected() && !currentDevice.isConnecting()))) {
			AbstractDevice<?> device = plugin.getAnyDevice(getFieldType());
			if (device != null) {
				AbstractSensor newSensor = getSensor(device);
				if (newSensor != null) {
					if (currentDevice != null) {
						currentDevice.removeListener(deviceListener);
					}
					setSensor(newSensor);
				}
			}
		}
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}

	private final AbstractDevice.DeviceListener deviceListener = new AbstractDevice.DeviceListener() {
		@Override
		public void onDeviceConnect(@NonNull AbstractDevice<?> device, @NonNull DeviceConnectionResult result, @Nullable String error) {
			app.runInUIThread(() -> updateInfo(null));
		}

		@Override
		public void onDeviceDisconnect(@NonNull AbstractDevice<?> device) {
		}

		@Override
		public void onSensorData(@NonNull AbstractSensor sensor, @NonNull SensorData data) {
			app.runInUIThread(() -> updateInfo(null));
		}

		@Override
		public void onDeviceConnecting(@NonNull AbstractDevice<?> device) {
		}
	};

	@NonNull
	private CommonPreference<String> registerSensorDevicePref(@Nullable String customId) {
		String prefId = Algorithms.isEmpty(customId) ? fieldType.name() : fieldType.name() + customId;
		return settings.registerStringPreference(prefId, plugin.getAnyConnectedDeviceId())
				.makeProfile()
				.cache();
	}

	@Nullable
	public String getDeviceId(@NonNull ApplicationMode appMode) {
		return deviceIdPref.getModeValue(appMode);
	}

	public void setDeviceId(@NonNull String deviceId) {
		saveDeviceId(deviceId);
		applyDeviceId();
	}

	private void saveDeviceId(@NonNull String deviceId) {
		ApplicationMode appMode = app.getSettings().getApplicationMode();
		deviceIdPref.setModeValue(appMode, deviceId);
		externalDeviceId = deviceId;
	}

	@Override
	public void copySettings(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copySettingsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copySettingsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copySettingsFromMode(sourceAppMode, appMode, customId);
		registerSensorDevicePref(customId).setModeValue(appMode, deviceIdPref.getModeValue(sourceAppMode));
	}

	public SensorWidgetDataFieldType getFieldType() {
		return fieldType;
	}
}