package net.osmand.plus.osmo;


import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;

public class SettingsOsMoActivity extends SettingsBaseActivity {

	public static final String MORE_VALUE = "MORE_VALUE";
	public static final String DEFINE_EDIT = "DEFINE_EDIT";
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setTitle(R.string.online_map_settings);
		PreferenceScreen grp = getPreferenceScreen();
		
		Preference pref = new Preference(this);
		pref.setTitle(R.string.osmo_settings_uuid);
		pref.setSummary(OsMoPlugin.getUUID(this));
		
		grp.addPreference(pref);
		
		
    }


	public void updateAllSettings() {
		super.updateAllSettings();
	}
	
	
}
