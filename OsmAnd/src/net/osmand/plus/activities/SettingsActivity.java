package net.osmand.plus.activities;


import java.io.File;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;

public class SettingsActivity extends SettingsBaseActivity {

	public static final String INTENT_KEY_SETTINGS_SCREEN = "INTENT_KEY_SETTINGS_SCREEN";
	public static final int SCREEN_GENERAL_SETTINGS = 1;
	public static final int SCREEN_NAVIGATION_SETTINGS = 2;

	private static final int PLUGINS_SELECTION_REQUEST = 1;
	

	private Preference bidforfix;
	private Preference plugins;
	private Preference localIndexes;
	private Preference general;
	private Preference routing;
	
	// FIXME
	public ProgressDialog progressDlg;
	public static CharSequence SCREEN_ID_GENERAL_SETTINGS;
	public static CharSequence SCREEN_ID_NAVIGATION_SETTINGS;

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
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
			String pref = null;
			if(s == SCREEN_GENERAL_SETTINGS){
				startActivity(new Intent(this, SettingsGeneralActivity.class));
			} else if(s == SCREEN_NAVIGATION_SETTINGS){
				startActivity(new Intent(this, SettingsNavigationActivity.class));
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
				startActivity(new Intent(this, OsmandIntents.getDownloadIndexActivity()));
			} else {
				startActivity(new Intent(this, OsmandIntents.getLocalIndexActivity()));
			}
			return true;
		} else if (preference == bidforfix) {
			startActivity(new Intent(this, OsmandBidForFixActivity.class));
		} else if (preference == general) {
			startActivity(new Intent(this, SettingsGeneralActivity.class));
			return true;
		} else if (preference == routing) {
			startActivity(new Intent(this, SettingsNavigationActivity.class));
			return true;
		} else if (preference == plugins) {
			startActivityForResult(new Intent(this, OsmandIntents.getPluginsActivity()), PLUGINS_SELECTION_REQUEST);
			return true;
		} else {
			super.onPreferenceClick(preference);
		}
		return false;
	}

}
