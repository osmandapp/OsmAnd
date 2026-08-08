package net.osmand.plus.plugins.traffic;

import androidx.preference.Preference;

import net.osmand.plus.R;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.settings.preferences.EditTextPreferenceEx;

public class TrafficSettingsFragment extends BaseSettingsFragment {

	private final TrafficPlugin plugin = PluginsHelper.requirePlugin(TrafficPlugin.class);

	@Override
	protected void setupPreferences() {
		EditTextPreferenceEx apiKey = findPreference(plugin.TOMTOM_API_KEY.getId());
		if (apiKey != null) {
			apiKey.setDescription(R.string.traffic_api_key_description);
			apiKey.setSummary(plugin.TOMTOM_API_KEY.get());
		}
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean changed = super.onPreferenceChange(preference, newValue);
		if (changed) {
			String key = preference.getKey();
			if (plugin.TRAFFIC_ROUTING.getId().equals(key) || plugin.TOMTOM_API_KEY.getId().equals(key)) {
				app.getRoutingHelper().onSettingsChanged(getSelectedAppMode(), true);
			}
		}
		return changed;
	}
}
