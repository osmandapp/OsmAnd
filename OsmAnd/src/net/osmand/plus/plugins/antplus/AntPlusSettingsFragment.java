package net.osmand.plus.plugins.antplus;

import androidx.annotation.NonNull;
import androidx.preference.Preference;

import net.osmand.plus.R;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.antplus.devices.HeartRateDevice;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class AntPlusSettingsFragment extends BaseSettingsFragment {

	@Override
	protected void setupPreferences() {
		AntPlusPlugin plugin = OsmandPlugin.getPlugin(AntPlusPlugin.class);
		if (plugin != null) {
			Preference developmentInfo = findPreference("antplus_info");
			developmentInfo.setIcon(getContentIcon(R.drawable.ic_action_info_dark));

			HeartRateDevice heartRateDevice = plugin.getDevice(HeartRateDevice.class);
			if (heartRateDevice != null) {
				setupHeartRatePref(heartRateDevice);
			}
		}
	}

	private void setupHeartRatePref(@NonNull HeartRateDevice heartRateDevice) {
		Preference heartRateCategory = findPreference("heart_rate");
		heartRateCategory.setIconSpaceReserved(false);

		SwitchPreferenceEx heartRateEnabled = findPreference(heartRateDevice.getDeviceEnabledPref().getId());
		heartRateEnabled.setIconSpaceReserved(false);

		SwitchPreferenceEx heartRateWriteGpx = findPreference(heartRateDevice.getWriteGpxPref().getId());
		heartRateWriteGpx.setIconSpaceReserved(false);
	}
}
