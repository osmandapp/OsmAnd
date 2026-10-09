package net.osmand.plus.plugins.externalsensors;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;
import androidx.preference.PreferenceScreen;

import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.externalsensors.devices.AbstractDevice;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.util.Algorithms;

import java.util.List;

public class AntPlusSettingsFragment extends BaseSettingsFragment {

	private ExternalSensorsPlugin plugin;
	@Override
	protected void setupPreferences() {
		plugin = PluginsHelper.getPlugin(ExternalSensorsPlugin.class);
		Preference developmentInfo = findPreference("antplus_info");
		developmentInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

		List<AbstractDevice<?>> devices = plugin.getDevices();
		if (!Algorithms.isEmpty(devices)) {
			setupCommonDevicesPrefs(devices);
		}
	}

	private void setupCommonDevicesPrefs(@NonNull List<AbstractDevice<?>> devices) {
		PreferenceScreen screen = getPreferenceScreen();
		for (AbstractDevice<?> device : devices) {
			PreferenceCategory category = new PreferenceCategory(screen.getContext());
			category.setTitle(plugin.getDeviceName(device));
			category.setLayoutResource(R.layout.preference_category_with_descr);
			category.setIconSpaceReserved(false);
			screen.addPreference(category);
			/*
			CommonPreference<Boolean> sensorEnabledPref = sensor.getSensorEnabledPref();
			if (sensorEnabledPref != null) {
				String deviceEnabledPrefId = sensorEnabledPref.getId();
				SwitchPreferenceEx deviceEnabledPref = createSwitchPreferenceEx(deviceEnabledPrefId, R.string.ant_read_data, R.layout.preference_with_descr_dialog_and_switch);
				deviceEnabledPref.setSummaryOn(R.string.shared_string_enabled);
				deviceEnabledPref.setSummaryOff(R.string.shared_string_disabled);
				deviceEnabledPref.setIconSpaceReserved(false);
				screen.addPreference(deviceEnabledPref);
			}
			CommonPreference<Boolean> sensorWriteGpxPref = sensor.getSensorWriteGpxPref();
			if (sensorWriteGpxPref != null) {
				String writePrefId = sensorWriteGpxPref.getId();
				SwitchPreferenceEx deviceWritePref = createSwitchPreferenceEx(writePrefId, R.string.ant_write_to_gpx, R.layout.preference_with_descr_dialog_and_switch);
				deviceWritePref.setSummaryOn(R.string.shared_string_enabled);
				deviceWritePref.setSummaryOff(R.string.shared_string_disabled);
				deviceWritePref.setIconSpaceReserved(false);
				screen.addPreference(deviceWritePref);
			}
			 */
		}
	}
}
