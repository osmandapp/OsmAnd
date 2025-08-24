package net.osmand.plus.plugins.externalsensors.devices.sensors;

import android.view.View;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.plus.settings.enums.ExternalDeviceShowMode;
import net.osmand.plus.utils.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.plus.views.mapwidgets.widgetstates.ExternalSensorWidgetState;
import net.osmand.util.Algorithms;

import java.util.List;

public class SensorTextWidget extends SimpleWidget {

	private final ExternalSensorsPlugin plugin;
	private final SensorWidgetDataFieldType fieldType;
	private final CommonPreference<String> deviceIdPref;
	private final ExternalSensorWidgetState widgetState;

	private AbstractSensor sensor;
	private String externalDeviceId;

	private Number cachedNumber;
	private boolean forceUpdate;

	public SensorTextWidget(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode,
	                        @NonNull SensorWidgetDataFieldType fieldType, @Nullable String customId,
	                        @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, fieldType.getWidgetType(), customId, widgetsPanel);
		this.fieldType = fieldType;
		plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
		deviceIdPref = registerSensorDevicePref(customId);
		externalDeviceId = getDeviceId(appMode);
		this.widgetState = new ExternalSensorWidgetState(app, customId, fieldType);
		applyDeviceId();
		updateInfo(null);
		setImageDrawable(getIconId());
		updateWidgetName();
	}

	@Override
	public void setImageDrawable(int res) {
		if (isDeviceConnected()) {
			super.setImageDrawable(res);
		} else {
			if (shouldShowIcon()) {
				setImageDrawable(app.getUIUtilities().getIcon(res, nightMode));
				imageView.setVisibility(View.VISIBLE);
			} else {
				imageView.setVisibility(View.GONE);
			}
		}
	}

	private boolean isDeviceConnected() {
		AbstractDevice<?> currentDevice = null;
		if (sensor != null) {
			currentDevice = this.sensor.getDevice();
		}
		return currentDevice != null && currentDevice.isConnected();
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

	@Override
	protected View.OnClickListener getOnClickListener() {
		return v -> {
			forceUpdate = true;
			widgetState.changeToNextState();
			updateInfo(null);
			setImageDrawable(getIconId());
			updateWidgetName();
			if (this.sensor != null && this.sensor.device.isDisconnected()) {
				plugin.connectDevice(mapActivity, this.sensor.device);
			}
		};
	}

	@Nullable
	protected String getAdditionalWidgetName() {
		if (widgetState != null) {
			return getPreference().get() == ExternalDeviceShowMode.SENSOR_DATA ? null : getString(R.string.battery);
		}
		return null;
	}

	@NonNull
	public OsmandPreference<ExternalDeviceShowMode> getPreference() {
		return widgetState.getShowModePreference();
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
		if (isDeviceConnected() && (sensor != null && !Algorithms.isEmpty(sensor.getLastSensorDataList()))) {
			if (isShowSensorData()) {
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
				AbstractDevice<?> device = sensor.getDevice();
				if(device.hasBatteryLevel()) {
					setText(String.valueOf(device.getBatteryLevel()), "%");
				} else {
					setText(app.getString(R.string.n_a), null);
				}
			}
		} else {
			setText(NO_VALUE, null);
		}
		AbstractDevice<?> currentDevice = null;
		if (sensor != null) {
			currentDevice = this.sensor.getDevice();
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
		setImageDrawable(getIconId());
		forceUpdate = false;
	}

	@Override
	public boolean isUpdateNeeded() {
		return forceUpdate || super.isUpdateNeeded();
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
		widgetState.copyPrefs(appMode, customId);
	}

	@Override
	public void copySettingsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId) {
		super.copySettingsFromMode(sourceAppMode, appMode, customId);
		registerSensorDevicePref(customId).setModeValue(appMode, deviceIdPref.getModeValue(sourceAppMode));
		widgetState.copyPrefsFromMode(sourceAppMode, appMode, customId);
	}

	public SensorWidgetDataFieldType getFieldType() {
		return fieldType;
	}

	private boolean isShowSensorData() {
		return getPreference().get() == ExternalDeviceShowMode.SENSOR_DATA;
	}

	@DrawableRes
	@Override
	public int getIconId(boolean nightMode) {
		AbstractDevice<?> currentDevice = null;
		if (sensor != null) {
			currentDevice = this.sensor.getDevice();
		}
		boolean isConnected = sensor != null && currentDevice.isConnected();
		if (isConnected) {
			return isShowSensorData() ? nightMode ? fieldType.nightIconId : fieldType.dayIconId : nightMode ? fieldType.nightBatteryIconId : fieldType.dayBatteryIconId;
		} else {
			return isShowSensorData() ? fieldType.disconnectedIconId : fieldType.disconnectedBatteryIconId;
		}
	}

	@DrawableRes
	@Override
	public int getMapIconId(boolean nightMode) {
		return isShowSensorData() ? nightMode ? fieldType.nightIconId : fieldType.dayIconId : nightMode ? fieldType.nightBatteryIconId : fieldType.dayBatteryIconId;
	}
}