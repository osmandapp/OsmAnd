package net.osmand.plus.plugins.externalsensors;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;

import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.plugins.externalsensors.devices.sensors.SensorWidgetDataFieldType;
import net.osmand.plus.plugins.externalsensors.dialogs.SelectExternalDeviceFragment;
import net.osmand.plus.plugins.externalsensors.dialogs.SelectExternalDeviceFragment.SelectDeviceListener;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.CommonPreference;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.CustomObjectPreference;
import net.osmand.util.Algorithms;

public class ExternalSettingsWriteToTrackSettingsFragment extends BaseSettingsFragment implements SelectDeviceListener {

	public static final String TAG = ExternalSettingsWriteToTrackSettingsFragment.class.getSimpleName();

	private ExternalSensorsPlugin plugin;

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
	}

	@Override
	protected void setupPreferences() {
		for (ExternalSensorTrackDataType widgetType : ExternalSensorTrackDataType.values()) {
			setupSensorSettings(widgetType);
		}
	}

	private void setupSensorSettings(@NonNull ExternalSensorTrackDataType dataType) {
		CustomObjectPreference pref = findPreference(dataType.getPreferenceId());
		if (pref == null) {
			pref = new CustomObjectPreference(requireContext());
			pref.setKey(dataType.getPreferenceId());
			pref.setCustomObject(dataType);
			pref.setTitle(dataType.getTitleId());
			pref.setLayoutResource(R.layout.preference_with_descr);
			addOnPreferencesScreen(pref);
		}
		CommonPreference<String> deviceIdPref = plugin.getWriteToTrackDeviceIdPref(dataType);
		String deviceId = deviceIdPref.getModeValue(getSelectedAppMode());
		String deviceName = getString(R.string.shared_string_none);
		boolean deviceFound = false;

		if (Algorithms.isEmpty(deviceId)) {
			deviceName = getString(R.string.shared_string_none);
		} else if (plugin.getDevice(deviceId) != null) {
			AbstractDevice<?> device = plugin.getDevice(deviceId);
			if (device != null) {
				deviceName = device.getName();
				deviceFound = true;
			}
		} else {
			AbstractDevice<?> connectedDevice = plugin.getAnyDevice(dataType.getSensorType());
			if (connectedDevice != null) {
				deviceName = String.format(getString(R.string.any_connected_with_device), connectedDevice.getName());
				deviceFound = true;
			} else {
				deviceName = getString(R.string.any_connected);
			}
		}
		pref.setSummary(deviceName);
		SensorWidgetDataFieldType sensorType = dataType.getSensorType();
		if (deviceFound) {
			pref.setIcon(getActiveIcon(sensorType.disconnectedIconId));
		} else {
			pref.setIcon(getContentIcon(sensorType.disconnectedIconId));
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference instanceof CustomObjectPreference) {
			CustomObjectPreference customPref = (CustomObjectPreference) preference;
			if (customPref.getCustomObject() instanceof ExternalSensorTrackDataType) {
				ExternalSensorTrackDataType dataType = (ExternalSensorTrackDataType) customPref.getCustomObject();
				ApplicationMode appMode = getSelectedAppMode();
				SensorWidgetDataFieldType sensorType = dataType.getSensorType();
				String deviceId = plugin.getWriteToTrackDeviceIdPref(dataType).getModeValue(appMode);
				SelectExternalDeviceFragment.showInstance(requireActivity().getSupportFragmentManager(), this, sensorType, deviceId, true);
			}
		}
		return true;
	}

	@Override
	public void selectNewDevice(@Nullable String deviceId, @NonNull SensorWidgetDataFieldType requestedWidgetDataFieldType) {
		ApplicationMode appMode = getSelectedAppMode();
		switch (requestedWidgetDataFieldType) {
			case BIKE_SPEED:
				plugin.SPEED_SENSOR_WRITE_TO_TRACK_DEVICE_ID.setModeValue(appMode, deviceId);
				setupSensorSettings(ExternalSensorTrackDataType.BIKE_SPEED);
				break;
			case BIKE_CADENCE:
				plugin.CADENCE_SENSOR_WRITE_TO_TRACK_DEVICE_ID.setModeValue(appMode, deviceId);
				setupSensorSettings(ExternalSensorTrackDataType.BIKE_CADENCE);
				break;
			case BIKE_POWER:
				plugin.POWER_SENSOR_WRITE_TO_TRACK_DEVICE_ID.setModeValue(appMode, deviceId);
				setupSensorSettings(ExternalSensorTrackDataType.BIKE_POWER);
				break;
			case HEART_RATE:
				plugin.HEART_RATE_SENSOR_WRITE_TO_TRACK_DEVICE_ID.setModeValue(appMode, deviceId);
				setupSensorSettings(ExternalSensorTrackDataType.HEART_RATE);
				break;
			case TEMPERATURE:
				plugin.TEMPERATURE_SENSOR_WRITE_TO_TRACK_DEVICE_ID.setModeValue(appMode, deviceId);
				setupSensorSettings(ExternalSensorTrackDataType.TEMPERATURE);
				break;
		}
	}
}
