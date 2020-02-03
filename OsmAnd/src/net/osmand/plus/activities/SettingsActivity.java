package net.osmand.plus.activities;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.liveupdates.OsmLiveActivity;

public class SettingsActivity extends SettingsBaseActivity {

	public static final String INTENT_KEY_SETTINGS_SCREEN = "INTENT_KEY_SETTINGS_SCREEN";
	public static final int SCREEN_GENERAL_SETTINGS = 1;
	public static final int SCREEN_NAVIGATION_SETTINGS = 2;

	private static final int PLUGINS_SELECTION_REQUEST = 1;
    private static final String CONTRIBUTION_VERSION_FLAG = "CONTRIBUTION_VERSION_FLAG";
	

	private Preference general;
	private Preference routing;
	private Preference subscription;
	private Preference privacy;


	@Override
    public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_pref);
		PreferenceScreen screen = getPreferenceScreen();
		general = (Preference) screen.findPreference("general_settings");
		general.setOnPreferenceClickListener(this);
		routing = (Preference) screen.findPreference("routing_settings");
		routing.setOnPreferenceClickListener(this);
		subscription = (Preference) screen.findPreference("subscription_settings");
		subscription.setOnPreferenceClickListener(this);
		privacy = (Preference) screen.findPreference("privacy_and_security");
		privacy.setOnPreferenceClickListener(this);

		getToolbar().setTitle(Version.getFullVersion(getMyApplication()));
		
		Intent intent = getIntent();
		if(intent != null && intent.getIntExtra(INTENT_KEY_SETTINGS_SCREEN, 0) != 0){
			int s = intent.getIntExtra(INTENT_KEY_SETTINGS_SCREEN, 0);
			if(s == SCREEN_GENERAL_SETTINGS){
				startActivity(new Intent(this, SettingsGeneralActivity.class));
			} else if(s == SCREEN_NAVIGATION_SETTINGS){
				startActivity(new Intent(this, SettingsNavigationActivity.class));
			} 
		}
		PreferenceCategory plugins = (PreferenceCategory) screen.findPreference("plugin_settings");
		for(OsmandPlugin op : OsmandPlugin.getEnabledPlugins()) {
			final Class<? extends Activity> sa = op.getSettingsActivity();
			if(sa != null) {
				Preference preference = new Preference(this);
				preference.setTitle(op.getName());
				preference.setKey(op.getId());
				preference.setOnPreferenceClickListener(new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						startActivity(new Intent(SettingsActivity.this, sa));
						return false;
					}
				});
				plugins.addPreference(preference);
			}
		}
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if ((requestCode == PLUGINS_SELECTION_REQUEST) && (resultCode == PluginsActivity.ACTIVE_PLUGINS_LIST_MODIFIED)) {
			finish();
			startActivity(getIntent());
		}
	}

	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == general) {
			startActivity(new Intent(this, SettingsGeneralActivity.class));
			return true;
		} else if (preference == routing) {
			startActivity(new Intent(this, SettingsNavigationActivity.class));
			return true;
		} else if (preference == subscription) {
			Intent intent = new Intent(this, OsmLiveActivity.class);
			intent.putExtra(OsmLiveActivity.SHOW_SETTINGS_ONLY_INTENT_PARAM, true);
			startActivity(intent);
			return true;
		} else if (preference == privacy) {
			Intent intent = new Intent(this, PrivacyAndSecurityActivity.class);
			startActivity(intent);
			return true;
		} else {
			super.onPreferenceClick(preference);
		}
		return false;
	}
	
}
