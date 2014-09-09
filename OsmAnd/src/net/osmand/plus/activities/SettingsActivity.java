package net.osmand.plus.activities;


import java.io.File;
import java.util.Date;

import net.osmand.IndexConstants;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.TypedValue;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsActivity extends SettingsBaseActivity {

	public static final String INTENT_KEY_SETTINGS_SCREEN = "INTENT_KEY_SETTINGS_SCREEN";
	public static final int SCREEN_GENERAL_SETTINGS = 1;
	public static final int SCREEN_NAVIGATION_SETTINGS = 2;

	private static final int PLUGINS_SELECTION_REQUEST = 1;
    private static final String CONTRIBUTION_VERSION_FLAG = "CONTRIBUTION_VERSION_FLAG";
	

	private Preference bidforfix;
	private Preference plugins;
	private Preference localIndexes;
	private Preference general;
	private Preference routing;
	private Preference about;
	
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
			if(s == SCREEN_GENERAL_SETTINGS){
				startActivity(new Intent(this, SettingsGeneralActivity.class));
			} else if(s == SCREEN_NAVIGATION_SETTINGS){
				startActivity(new Intent(this, SettingsNavigationActivity.class));
			} 
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
		} else if (preference == bidforfix) {
			startActivity(new Intent(this, OsmandBidForFixActivity.class));
		} else if (preference == general) {
			startActivity(new Intent(this, SettingsGeneralActivity.class));
			return true;
		} else if (preference == routing) {
			startActivity(new Intent(this, SettingsNavigationActivity.class));
			return true;
		} else if (preference == about) {
			showAboutDialog();
			return true;
		} else if (preference == plugins) {
			startActivityForResult(new Intent(this, getMyApplication().getAppCustomization().getPluginsActivity()), PLUGINS_SELECTION_REQUEST);
			return true;
		} else {
			super.onPreferenceClick(preference);
		}
		return false;
	}

	private void showAboutDialog() {
		Builder bld = new AlertDialog.Builder(this);
		bld.setTitle(R.string.about_settings);
        ScrollView sv = new ScrollView(this);
        TextView tv = new TextView(this);
        sv.addView(tv);
		String version = Version.getFullVersion(getMyApplication());
		String vt = getString(R.string.about_version) +"\t";
		int st = vt.length();
		String edition = "";
        SharedPreferences prefs = getApplicationContext().getSharedPreferences("net.osmand.settings", MODE_WORLD_READABLE);
        if (prefs.contains(CONTRIBUTION_VERSION_FLAG)) {
            try {
                PackageManager pm = getPackageManager();
                ApplicationInfo appInfo = pm.getApplicationInfo(OsmandApplication.class.getPackage().getName(), 0);
                Date date = new Date(new File(appInfo.sourceDir).lastModified());
                edition = getString(R.string.local_index_installed) + " :\t" + DateFormat.getDateFormat(getApplicationContext()).format(date);
            } catch (Exception e) {
            }
            SpannableString content = new SpannableString(vt + version +"\n" +
    				edition +"\n\n"+
    				getString(R.string.about_content));
    		content.setSpan(new ClickableSpan() {
    			@Override
    			public void onClick(View widget) {
    				final Intent mapIntent = new Intent(SettingsActivity.this, ContributionVersionActivity.class);
    				startActivityForResult(mapIntent, 0);
    			}
    			
    		}, st, st + version.length(), 0);
    		tv.setText(content);
        } else {
        	tv.setText(vt + version +"\n\n" +
    				getString(R.string.about_content));
        }
        
		tv.setPadding(5, 0, 5, 5);
		tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
		tv.setMovementMethod(LinkMovementMethod.getInstance());
		bld.setView(sv);
		bld.setPositiveButton(R.string.default_buttons_ok, null);
		bld.show();
		
	}

}
