package net.osmand.plus.plugins.aprs;

import androidx.annotation.NonNull;

import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;
import net.osmand.plus.settings.preferences.ListPreferenceEx;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

public class AprsSettingsFragment extends BaseSettingsFragment {

	private final AprsPlugin plugin = PluginsHelper.requirePlugin(AprsPlugin.class);

	@Override
	protected void setupPreferences() {
		setupFrequency();
		setupCustomFrequency();
		setupExpiry();
		setupRadius();
		setupHamlib();
	}

	private void setupFrequency() {
		String[] entries = {"144.390 MHz (US)", "144.800 MHz (EU)", "145.175 MHz", "145.570 MHz",
				"144.930 MHz", "144.640 MHz", "Custom"};
		Float[] values = {144.390f, 144.800f, 145.175f, 145.570f, 144.930f, 144.640f, -1f};
		ListPreferenceEx pref = findPreference(plugin.APRS_FREQUENCY.getId());
		if (pref != null) {
			pref.setEntries(entries);
			pref.setEntryValues(values);
			pref.setDescription(R.string.aprs_frequency_description);
		}
	}

	private void setupCustomFrequency() {
		EditTextPreferenceEx pref = findPreference(plugin.APRS_CUSTOM_FREQUENCY.getId());
		if (pref != null) {
			pref.setDescription(R.string.aprs_custom_frequency_description);
		}
	}

	private void setupExpiry() {
		Integer[] values = new Integer[24];
		String[] entries = new String[24];
		for (int i = 0; i < 24; i++) {
			values[i] = 5 + i * 5;
			entries[i] = values[i] + " min";
		}
		ListPreferenceEx pref = findPreference(plugin.APRS_EXPIRY_MINUTES.getId());
		if (pref != null) {
			pref.setEntries(entries);
			pref.setEntryValues(values);
			pref.setDescription(R.string.aprs_expiry_description);
		}
	}

	private void setupRadius() {
		Integer[] values = {50, 100, 150, 200, 250, 300, 350, 400, 450, 500};
		String[] entries = new String[values.length];
		for (int i = 0; i < values.length; i++) {
			entries[i] = values[i] + " km";
		}
		ListPreferenceEx pref = findPreference(plugin.APRS_RADIUS_KM.getId());
		if (pref != null) {
			pref.setEntries(entries);
			pref.setEntryValues(values);
			pref.setDescription(R.string.aprs_radius_description);
		}
	}

	private void setupHamlib() {
		SwitchPreferenceEx enabled = findPreference(plugin.APRS_HAMLIB_ENABLED.getId());
		if (enabled != null) {
			enabled.setDescription(R.string.aprs_hamlib_enabled_description);
		}
		EditTextPreferenceEx host = findPreference(plugin.APRS_HAMLIB_HOST.getId());
		if (host != null) {
			host.setDescription(R.string.aprs_hamlib_host_description);
		}
		EditTextPreferenceEx port = findPreference(plugin.APRS_HAMLIB_PORT.getId());
		if (port != null) {
			port.setDescription(R.string.aprs_hamlib_port_description);
		}
	}
}
