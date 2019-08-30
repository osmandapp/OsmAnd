package net.osmand.plus.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.FragmentManager;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import net.osmand.aidl.OsmandAidlApi.ConnectedApp;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.PluginActivity;
import net.osmand.plus.settings.preferences.SwitchPreferenceEx;

import java.util.List;

import static net.osmand.plus.profiles.SettingsProfileFragment.PROFILE_STRING_KEY;

public class ConfigureProfileFragment extends BaseSettingsFragment {

	public static final String TAG = "ConfigureProfileFragment";

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);

		return view;
	}

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
		PreferenceScreen screen = getPreferenceScreen();

		Preference generalSettings = findPreference("general_settings");
		Preference navigationSettings = findPreference("navigation_settings");
		Preference configureMap = findPreference("configure_map");

		generalSettings.setIcon(getContentIcon(R.drawable.ic_action_settings));
		navigationSettings.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark));
		configureMap.setIcon(getContentIcon(R.drawable.ic_action_layers_dark));

		List<ConnectedApp> connectedApps = getMyApplication().getAidlApi().getConnectedApps();
		List<OsmandPlugin> plugins = OsmandPlugin.getVisiblePlugins();

		for (ConnectedApp connectedApp : connectedApps) {
			SwitchPreference preference = new SwitchPreference(getContext());
			preference.setKey(connectedApp.getPack());
			preference.setTitle(connectedApp.getName());
			preference.setIcon(connectedApp.getIcon());
			preference.setChecked(connectedApp.isEnabled());
			preference.setLayoutResource(R.layout.preference_dialog_and_switch);

			screen.addPreference(preference);
		}
		for (OsmandPlugin plugin : plugins) {
			SwitchPreferenceEx preference = new SwitchPreferenceEx(getContext());
			preference.setKey(plugin.getId());
			preference.setTitle(plugin.getName());
			preference.setSummaryOn("");
			preference.setIcon(getContentIcon(plugin.getLogoResourceId()));
			preference.setChecked(plugin.isActive());
			preference.setLayoutResource(R.layout.preference_dialog_and_switch);

			Intent intent = new Intent(getContext(), PluginActivity.class);
			intent.putExtra(PluginActivity.EXTRA_PLUGIN_ID, plugin.getId());
			preference.setIntent(intent);

			screen.addPreference(preference);
		}
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

	public static boolean showInstance(FragmentManager fragmentManager, ApplicationMode mode) {
		try {
			Bundle args = new Bundle();
			args.putString(PROFILE_STRING_KEY, mode.getStringKey());

			ConfigureProfileFragment configureProfileFragment = new ConfigureProfileFragment();
			configureProfileFragment.setArguments(args);

			fragmentManager.beginTransaction()
					.add(R.id.fragmentContainer, configureProfileFragment, ConfigureProfileFragment.TAG)
					.addToBackStack(ConfigureProfileFragment.TAG)
					.commitAllowingStateLoss();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
}