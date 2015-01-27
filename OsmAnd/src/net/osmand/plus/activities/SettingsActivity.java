package net.osmand.plus.activities;


import java.io.File;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import net.osmand.plus.Version;
import net.osmand.plus.development.OsmandDevelopmentPlugin;

public class SettingsActivity extends SettingsBaseActivity {

	public static final String INTENT_KEY_SETTINGS_SCREEN = "INTENT_KEY_SETTINGS_SCREEN";
	public static final int SCREEN_GENERAL_SETTINGS = 1;
	public static final int SCREEN_NAVIGATION_SETTINGS = 2;

	private static final int PLUGINS_SELECTION_REQUEST = 1;
    private static final String CONTRIBUTION_VERSION_FLAG = "CONTRIBUTION_VERSION_FLAG";
	

	private Preference plugins;
	private Preference localIndexes;
	private Preference general;
	private Preference routing;
	private Preference about;
	private Preference version;


	@Override
    public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.settings_pref);
		PreferenceScreen screen = getPreferenceScreen();
		localIndexes =(Preference) screen.findPreference("local_indexes");
		localIndexes.setOnPreferenceClickListener(this);
//		bidforfix = (Preference) screen.findPreference("bidforfix");
//		bidforfix.setOnPreferenceClickListener(this);
		plugins = (Preference) screen.findPreference("plugins");
		plugins.setOnPreferenceClickListener(this);
		general = (Preference) screen.findPreference("general_settings");
		general.setOnPreferenceClickListener(this);
		routing = (Preference) screen.findPreference("routing_settings");
		routing .setOnPreferenceClickListener(this);
		OsmandPlugin.onSettingsActivityCreate(this, screen);
		
		Intent intent = getIntent();
		if(intent != null && intent.getIntExtra(INTENT_KEY_SETTINGS_SCREEN, 0) != 0){
			int s = intent.getIntExtra(INTENT_KEY_SETTINGS_SCREEN, 0);
			if(s == SCREEN_GENERAL_SETTINGS){
				startActivity(new Intent(this, SettingsGeneralActivity.class));
			} else if(s == SCREEN_NAVIGATION_SETTINGS){
				startActivity(new Intent(this, SettingsNavigationActivity.class));
			} 
		}
		if ((Version.getBuildAppEdition(getMyApplication()).length() > 0
				|| Version.isDeveloperVersion(getMyApplication())) &&
				OsmandPlugin.getEnabledPlugin(OsmandDevelopmentPlugin.class) != null){
			version = new Preference(this);
			version.setOnPreferenceClickListener(this);
			version.setSummary(R.string.version_settings_descr);
			version.setTitle(R.string.version_settings);
			version.setKey("version");
			screen.addPreference(version);
		}
		about = new Preference(this);
		about.setOnPreferenceClickListener(this);
		about.setSummary(R.string.about_settings_descr);
		about.setTitle(R.string.about_settings);
		about.setKey("about");
		screen.addPreference(about);
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
		if (preference == localIndexes) {
			boolean empty = getMyApplication().getResourceManager().getIndexFileNames().isEmpty();
			if (empty) {
				File folder = getMyApplication().getAppPath(IndexConstants.BACKUP_INDEX_DIR);
				if (folder.exists() && folder.isDirectory()) {
					String[] l = folder.list();
					empty = l == null || l.length == 0;
				}
			}
			if (empty) {
				startActivity(new Intent(this, getMyApplication().getAppCustomization().getDownloadIndexActivity()));
			} else {
				startActivity(new Intent(this, getMyApplication().getAppCustomization().getDownloadActivity()));
			}
			return true;
		} else if (preference == general) {
			startActivity(new Intent(this, SettingsGeneralActivity.class));
			return true;
		} else if (preference == routing) {
			startActivity(new Intent(this, SettingsNavigationActivity.class));
			return true;
		} else if (preference == about) {
			MainMenuActivity.showAboutDialog(this, getMyApplication());
			return true;
		} else if (preference == plugins) {
			startActivityForResult(new Intent(this, getMyApplication().getAppCustomization().getPluginsActivity()), PLUGINS_SELECTION_REQUEST);
			return true;
		} else if (preference == version){
			final Intent mapIntent = new Intent(this, ContributionVersionActivity.class);
			this.startActivityForResult(mapIntent, 0);
		} else {
			super.onPreferenceClick(preference);
		}
		return false;
	}

	

}
