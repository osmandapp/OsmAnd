package net.osmand.plus.plugins.externalsensors.devices.sensors;

import static net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin.ANY_CONNECTED_DEVICE_WRITE_SENSOR_DATA_TO_TRACK_KEY;

import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.DeviceStateListener;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.DeviceConnectionResult;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.utils.OsmAndFormatter.FormattedValue;
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget;
import net.osmand.util.Algorithms;

import java.util.List;

public class SensorTextWidget extends SimpleWidget implements DeviceStateListener {

	private final ExternalSensorsPlugin plugin;
	private final SensorWidgetDataFieldType fieldType;
	private final CommonPreference<String> deviceIdPref;

	private AbstractSensor sensor;
	private String externalDeviceId;
	private AbstractDevice<?> currentDevice;

	private Number cachedNumber;


	public SensorTextWidget(@NonNull MapActivity mapActivity, @NonNull ApplicationMode appMode,
	                        @NonNull SensorWidgetDataFieldType fieldType, @Nullable String customId,
	                        @Nullable WidgetsPanel widgetsPanel) {
		super(mapActivity, fieldType.getWidgetType(), customId, widgetsPanel);
		this.fieldType = fieldType;
		deviceIdPref = registerSensorDevicePref(customId);
		externalDeviceId = getDeviceId(appMode);
		plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
		applyDeviceId();
		updateInfo(null);
		setIcons(fieldType.getWidgetType());
	}

	private void applyDeviceId() {
		if (externalDeviceId == null || ANY_CONNECTED_DEVICE_WRITE_SENSOR_DATA_TO_TRACK_KEY.equals(externalDeviceId)) {
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
	public AbstractSensor getWidgetSensor() {
		return sensor;
	}

	@Override
	protected void updateSimpleWidgetInfo(@Nullable DrawSettings drawSettings) {
		if (sensor != null) {
			List<SensorData> dataList = sensor.getLastSensorDataList();
			if (!sensor.getDevice().isConnected() || Algorithms.isEmpty(dataList)) {
				setText(NO_VALUE, null);
				return;
			}
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
	}

	@Override
	public boolean isMetricSystemDepended() {
		return true;
	}

	private AbstractDevice.DeviceListener deviceListener = new AbstractDevice.DeviceListener() {
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
		return settings.registerStringPreference(prefId, null)
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
		registerSensorDevicePref(customId).setModeValue(appMode, deviceIdPref.getModeValue(appMode));
	}

	public SensorWidgetDataFieldType getFieldType() {
		return fieldType;
	}

	@Override
	public void attachView(@NonNull ViewGroup container, @NonNull WidgetsPanel widgetsPanel, @NonNull List<MapWidget> followingWidgets) {
		super.attachView(container, widgetsPanel, followingWidgets);
		plugin.addDeviceStateListener(this);
	}

	@Override
	public void detachView(@NonNull WidgetsPanel widgetsPanel) {
		super.detachView(widgetsPanel);
		plugin.removeDeviceStateListener(this);
	}

	@Override
	public void onDeviceConnected(@NonNull String deviceId) {
		if (ANY_CONNECTED_DEVICE_WRITE_SENSOR_DATA_TO_TRACK_KEY.equals(externalDeviceId) &&
				(currentDevice == null || (!currentDevice.isConnected() && !currentDevice.isConnecting()))) {
			AbstractDevice<?> device = plugin.getDevice(deviceId);
			if (device != null) {
				AbstractSensor sensor = getSensor(device);
				if (sensor != null) {
					if (currentDevice != null) {
						currentDevice.removeListener(deviceListener);
					}
					currentDevice = device;
					setSensor(sensor);
				}
			}
		}
	}

	@Override
	public void onDeviceDisconnected(@NonNull String deviceId) {

	}
}