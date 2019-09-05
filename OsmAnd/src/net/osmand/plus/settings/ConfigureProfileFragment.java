package net.osmand.plus.settings;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import net.osmand.aidl.OsmandAidlApi;
import net.osmand.aidl.OsmandAidlApi.ConnectedApp;
import net.osmand.plus.OsmandApplication;
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

	@Override
	protected String getToolbarTitle() {
		return getString(R.string.configure_profile);
	}

	@Override
	protected void setupPreferences() {
		Preference generalSettings = findPreference("general_settings");
		Preference navigationSettings = findPreference("navigation_settings");
		Preference pluginSettings = findPreference("plugin_settings");

		generalSettings.setIcon(getContentIcon(R.drawable.ic_action_settings));
		navigationSettings.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark));

		setupConfigureMapPref();
		setupConnectedAppsPref();
		setupOsmandPluginsPref();
	}

	private void setupConfigureMapPref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		Preference configureMap = findPreference("configure_map");
		configureMap.setIcon(getContentIcon(R.drawable.ic_action_layers_dark));

		Intent intent = new Intent(ctx, MapActivity.class);
		intent.putExtra(OPEN_CONFIG_ON_MAP, MAP_CONFIG);
		intent.putExtra(SELECTED_ITEM, getSelectedAppMode().getStringKey());
		configureMap.setIntent(intent);
	}

	private void setupConnectedAppsPref() {
		OsmandApplication app = getMyApplication();
		if (app == null) {
			return;
		}
		List<ConnectedApp> connectedApps = app.getAidlApi().getConnectedApps();
		for (ConnectedApp connectedApp : connectedApps) {
			SwitchPreference preference = new SwitchPreference(app);
			preference.setPersistent(false);
			preference.setKey(connectedApp.getPack());
			preference.setTitle(connectedApp.getName());
			preference.setIcon(connectedApp.getIcon());
			preference.setChecked(connectedApp.isEnabled());
			preference.setLayoutResource(R.layout.preference_switch);

			getPreferenceScreen().addPreference(preference);
		}
	}

	private void setupOsmandPluginsPref() {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}
		List<OsmandPlugin> plugins = OsmandPlugin.getVisiblePlugins();
		for (OsmandPlugin plugin : plugins) {
			SwitchPreferenceEx preference = new SwitchPreferenceEx(ctx);
			preference.setPersistent(false);
			preference.setKey(plugin.getId());
			preference.setTitle(plugin.getName());
			preference.setIcon(getPluginIcon(plugin));
			preference.setChecked(plugin.isActive());
			preference.setLayoutResource(R.layout.preference_dialog_and_switch);
			preference.setIntent(getPluginIntent(plugin));

			getPreferenceScreen().addPreference(preference);
		}
	}

	private Drawable getPluginIcon(OsmandPlugin plugin) {
		int iconResId = plugin.getLogoResourceId();
		return plugin.isActive() ? getActiveIcon(iconResId) : getContentIcon(iconResId);
	}

	private Intent getPluginIntent(OsmandPlugin plugin) {
		Intent intent;
		final Class<? extends Activity> settingsActivity = plugin.getSettingsActivity();
		if (settingsActivity != null && !plugin.needsInstallation()) {
			intent = new Intent(getContext(), settingsActivity);
		} else {
			intent = new Intent(getContext(), PluginActivity.class);
			intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, plugin.getId());
		}
		return intent;
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		String key = preference.getKey();

		OsmandPlugin plugin = OsmandPlugin.getPlugin(key);
		if (plugin != null) {
			if (newValue instanceof Boolean) {
				if ((plugin.isActive() || !plugin.needsInstallation())) {
					if (OsmandPlugin.enablePlugin(getActivity(), app, plugin, (Boolean) newValue)) {
						updateAllSettings();
						return true;
					}
				} else if (plugin.needsInstallation() && preference.getIntent() != null) {
					startActivity(preference.getIntent());
				}
			}
			return false;
		}

		OsmandAidlApi aidlApi = app.getAidlApi();
		ConnectedApp connectedApp = aidlApi.getConnectedApp(key);
		if (connectedApp != null) {
			return aidlApi.switchEnabled(connectedApp);
		}

		return super.onPreferenceChange(preference, newValue);
	}
}