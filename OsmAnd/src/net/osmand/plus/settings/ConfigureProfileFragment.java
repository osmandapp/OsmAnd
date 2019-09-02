package net.osmand.plus.settings;

import android.content.Intent;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;
import android.widget.Toast;

import net.osmand.aidl.OsmandAidlApi.ConnectedApp;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.List;

import static net.osmand.plus.profiles.EditProfileFragment.MAP_CONFIG;
import static net.osmand.plus.profiles.EditProfileFragment.OPEN_CONFIG_ON_MAP;
import static net.osmand.plus.profiles.EditProfileFragment.SELECTED_ITEM;

public class ConfigureProfileFragment extends BaseSettingsFragment {

	public static final String TAG = "ConfigureProfileFragment";

	@Override
	protected int getPreferencesResId() {
		return R.xml.configure_profile;
	}

	@Override
	protected int getToolbarResId() {
		return R.layout.profile_preference_toolbar_big;
	}

	protected String getToolbarTitle() {
		return getString(R.string.configure_profile);
	}

	@Override
	protected void setupPreferences() {
		Preference generalSettings = findPreference("general_settings");
		Preference navigationSettings = findPreference("navigation_settings");

		generalSettings.setIcon(getContentIcon(R.drawable.ic_action_settings));
		navigationSettings.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark));

		setupConfigureMapPref();
		setupConnectedAppsPref();
		setupOsmandPluginsPref();
	}

	private void setupConnectedAppsPref() {
		List<ConnectedApp> connectedApps = getMyApplication().getAidlApi().getConnectedApps();
		for (ConnectedApp connectedApp : connectedApps) {
			SwitchPreference preference = new SwitchPreference(getContext());
			preference.setKey(connectedApp.getPack());
			preference.setTitle(connectedApp.getName());
			preference.setIcon(connectedApp.getIcon());
			preference.setChecked(connectedApp.isEnabled());
			preference.setLayoutResource(R.layout.preference_dialog_and_switch);

			getPreferenceScreen().addPreference(preference);
		}
	}

	private void setupOsmandPluginsPref() {
		List<OsmandPlugin> plugins = OsmandPlugin.getVisiblePlugins();
		for (OsmandPlugin plugin : plugins) {
			SwitchPreferenceEx preference = new SwitchPreferenceEx(getContext());
			preference.setKey(plugin.getId());
			preference.setTitle(plugin.getName());
			preference.setIcon(getContentIcon(plugin.getLogoResourceId()));
			preference.setChecked(plugin.isActive());
			preference.setLayoutResource(R.layout.preference_dialog_and_switch);

			Intent intent = new Intent(getContext(), PluginActivity.class);
			intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, plugin.getId());
			preference.setIntent(intent);

			getPreferenceScreen().addPreference(preference);
		}
	}

	private void setupConfigureMapPref() {
		Preference configureMap = findPreference("configure_map");
		configureMap.setIcon(getContentIcon(R.drawable.ic_action_layers_dark));

		Intent intent = new Intent(getActivity(), MapActivity.class);
		intent.putExtra(OPEN_CONFIG_ON_MAP, MAP_CONFIG);
		intent.putExtra(SELECTED_ITEM, getSelectedAppMode().getStringKey());
		configureMap.setIntent(intent);
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference.getKey().equals("configure_map")) {
			getActivity().getSupportFragmentManager().beginTransaction().remove(this).commit();
			return false;
		}

		return super.onPreferenceClick(preference);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();
		OsmandPlugin plugin = OsmandPlugin.getPlugin(key);
		if (plugin != null) {
			Toast.makeText(getActivity(), "Change " + plugin.getId(), Toast.LENGTH_LONG).show();
			return OsmandPlugin.enablePlugin(getActivity(), app, plugin, (Boolean) newValue);
		}
		ConnectedApp connectedApp = getMyApplication().getAidlApi().getConnectedApp(key);
		if (connectedApp != null) {
			return getMyApplication().getAidlApi().switchEnabled(connectedApp);
		}

		return super.onPreferenceChange(preference, newValue);
	}
}