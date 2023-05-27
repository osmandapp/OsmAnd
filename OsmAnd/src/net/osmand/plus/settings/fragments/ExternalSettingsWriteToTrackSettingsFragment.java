package net.osmand.plus.settings.fragments;

import android.os.Bundle;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.ExternalSensorsPlugin;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.plugins.externalsensors.dialogs.SelectExternalDeviceFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

public class ExternalSettingsWriteToTrackSettingsFragment extends BaseSettingsFragment implements SelectExternalDeviceFragment.SelectDeviceListener {

	public static final String TAG = ExternalSettingsWriteToTrackSettingsFragment.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(ExternalSettingsWriteToTrackSettingsFragment.class);

	protected ExternalSensorsPlugin plugin;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
	}

	@Override
	protected void setupPreferences() {
		setupSpeedSensor();
		setupCadenceSensor();
		setupBikePowerSensor();
		setupHeartRateSensor();
		setupTemperatureSensor();
	}

	private void setupTemperatureSensor() {
		setupSensorSettings(settings.TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE, R.drawable.ic_action_thermometer);
	}

	private void setupHeartRateSensor() {
		setupSensorSettings(settings.HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE, R.drawable.ic_action_sensor_heart_rate_outlined);
	}

	private void setupBikePowerSensor() {
		setupBikePowerSensor(settings.POWER_SENSOR_WRITE_TO_TRACK_DEVICE, R.drawable.ic_action_sensor_bicycle_power_outlined);
	}

	private void setupBikePowerSensor(CommonPreference<String> settings, int ic_action_sensor_bicycle_power_outlined) {
		setupSensorSettings(settings, ic_action_sensor_bicycle_power_outlined);
	}

	private void setupCadenceSensor() {
		setupSensorSettings(settings.CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE, R.drawable.ic_action_sensor_cadence_outlined);
	}

	private void setupSpeedSensor() {
		setupSensorSettings(settings.SPEED_SENSOR_WRITE_TO_TRACK_DEVICE, R.drawable.ic_action_speed_outlined);
	}

	private void setupSensorSettings(@NonNull CommonPreference<String> prefSettings, @DrawableRes int iconId) {
		Preference pref = findPreference(prefSettings.getId());
		if (pref != null) {
			String deviceId = prefSettings.getModeValue(getSelectedAppMode());
			String deviceName = app.getString(R.string.shared_string_none);
			boolean deviceFound = false;
			if (!Algorithms.isEmpty(deviceId)) {
				AbstractDevice<?> device = plugin.getDevice(deviceId);
				if (device != null) {
					deviceName = device.getName();
					deviceFound = true;
				}
			}
			pref.setSummary(deviceName);
			if (deviceFound) {
				pref.setIcon(getActiveIcon(iconId));
			} else {
				pref.setIcon(getIcon(iconId));
			}
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		String key = preference.getKey();
		SensorWidgetDataFieldType sensorType = null;
		String deviceId = null;
		ApplicationMode appMode = getSelectedAppMode();
		if (key.equals(settings.SPEED_SENSOR_WRITE_TO_TRACK_DEVICE.getId())) {
			sensorType = SensorWidgetDataFieldType.BIKE_SPEED;
			deviceId = settings.SPEED_SENSOR_WRITE_TO_TRACK_DEVICE.getModeValue(appMode);
		} else if (key.equals(settings.CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE.getId())) {
			sensorType = SensorWidgetDataFieldType.BIKE_CADENCE;
			deviceId = settings.CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE.getModeValue(appMode);
		} else if (key.equals(settings.POWER_SENSOR_WRITE_TO_TRACK_DEVICE.getId())) {
			sensorType = SensorWidgetDataFieldType.BIKE_POWER;
			deviceId = settings.POWER_SENSOR_WRITE_TO_TRACK_DEVICE.getModeValue(appMode);
		} else if (key.equals(settings.HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE.getId())) {
			sensorType = SensorWidgetDataFieldType.HEART_RATE;
			deviceId = settings.HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE.getModeValue(appMode);
		} else if (key.equals(settings.TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE.getId())) {
			sensorType = SensorWidgetDataFieldType.TEMPERATURE;
			deviceId = settings.TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE.getModeValue(appMode);
		}
		SelectExternalDeviceFragment.showInstance(requireActivity().getSupportFragmentManager(), this, sensorType, deviceId);
		return true;
	}

	@Override
	public void selectNewDevice(@Nullable AbstractDevice<?> device, SensorWidgetDataFieldType requestedWidgetDataFieldType) {
		String deviceId = null;
		if (device != null) {
			deviceId = device.getDeviceId();
		}
		ApplicationMode appMode = getSelectedAppMode();
		switch (requestedWidgetDataFieldType) {
			case BIKE_SPEED:
				settings.SPEED_SENSOR_WRITE_TO_TRACK_DEVICE.setModeValue(appMode, deviceId);
				setupSpeedSensor();
				break;
			case BIKE_CADENCE:
				settings.CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE.setModeValue(appMode, deviceId);
				setupCadenceSensor();
				break;
			case BIKE_POWER:
				settings.POWER_SENSOR_WRITE_TO_TRACK_DEVICE.setModeValue(appMode, deviceId);
				setupBikePowerSensor();
				break;
			case HEART_RATE:
				settings.HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE.setModeValue(appMode, deviceId);
				setupHeartRateSensor();
				break;
			case TEMPERATURE:
				settings.TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE.setModeValue(appMode, deviceId);
				setupTemperatureSensor();
				break;
		}
	}
}
