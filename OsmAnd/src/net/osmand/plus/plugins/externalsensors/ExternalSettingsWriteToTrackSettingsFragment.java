package net.osmand.plus.plugins.externalsensors;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.plugins.externalsensors.dialogs.SelectExternalDeviceFragment;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.CustomObjectPreference;
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
		for (WriteToGpxWidgetType widgetType : WriteToGpxWidgetType.values()) {
			setupSensorSettings(widgetType);
		}
	}


	private void setupSensorSettings(@NonNull WriteToGpxWidgetType widgetType) {
		CustomObjectPreference pref = findPreference(widgetType.getId());
		if (pref == null) {
			pref = new CustomObjectPreference(requireContext());
			pref.setKey(widgetType.getId());
			pref.setCustomObject(widgetType);
			pref.setTitle(widgetType.getTitleId());
			pref.setLayoutResource(R.layout.preference_with_descr);
			addOnPreferencesScreen(pref);
		}
		CommonPreference<String> prefSettings = plugin.getPrefSettingsForWidgetType(widgetType);
		String deviceId = prefSettings.getModeValue(getSelectedAppMode());
		String deviceName = app.getString(R.string.shared_string_none);
		boolean deviceFound = false;
		if (!Algorithms.isEmpty(deviceId) && !ExternalSensorsPlugin.DENY_WRITE_SENSOR_DATA_TO_TRACK_KEY.equals(deviceId)) {
			AbstractDevice<?> device = plugin.getDevice(deviceId);
			if (device != null) {
				deviceName = device.getName();
				deviceFound = true;
			}
		}
		pref.setSummary(deviceName);
		if (deviceFound) {
			pref.setIcon(getActiveIcon(widgetType.getIcon()));
		} else {
			pref.setIcon(getIcon(widgetType.getIcon()));
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference instanceof CustomObjectPreference) {
			CustomObjectPreference customPref = (CustomObjectPreference) preference;
			if (customPref.getCustomObject() instanceof WriteToGpxWidgetType) {
				WriteToGpxWidgetType widgetType = (WriteToGpxWidgetType) customPref.getCustomObject();
				ApplicationMode appMode = getSelectedAppMode();
				SensorWidgetDataFieldType sensorType = widgetType.getSensorType();
				String deviceId = plugin.getPrefSettingsForWidgetType(widgetType).getModeValue(appMode);
				SelectExternalDeviceFragment.showInstance(requireActivity().getSupportFragmentManager(), this, sensorType, deviceId);
			}
		}
		return true;
	}

	@Override
	public void selectNewDevice(@Nullable AbstractDevice<?> device, SensorWidgetDataFieldType requestedWidgetDataFieldType) {
		String deviceId = ExternalSensorsPlugin.DENY_WRITE_SENSOR_DATA_TO_TRACK_KEY;
		if (device != null) {
			deviceId = device.getDeviceId();
		}
		ApplicationMode appMode = getSelectedAppMode();
		switch (requestedWidgetDataFieldType) {
			case BIKE_SPEED:
				plugin.SPEED_SENSOR_WRITE_TO_TRACK_DEVICE.setModeValue(appMode, deviceId);
				setupSensorSettings(WriteToGpxWidgetType.BIKE_SPEED);
				break;
			case BIKE_CADENCE:
				plugin.CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE.setModeValue(appMode, deviceId);
				setupSensorSettings(WriteToGpxWidgetType.BIKE_CADENCE);
				break;
			case BIKE_POWER:
				plugin.POWER_SENSOR_WRITE_TO_TRACK_DEVICE.setModeValue(appMode, deviceId);
				setupSensorSettings(WriteToGpxWidgetType.BIKE_POWER);
				break;
			case HEART_RATE:
				plugin.HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE.setModeValue(appMode, deviceId);
				setupSensorSettings(WriteToGpxWidgetType.HEART_RATE);
				break;
			case TEMPERATURE:
				plugin.TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE.setModeValue(appMode, deviceId);
				setupSensorSettings(WriteToGpxWidgetType.TEMPERATURE);
				break;
		}
	}
}
