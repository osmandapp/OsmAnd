package net.osmand.osmedit;


import net.osmand.activities.SettingsBaseActivity;
import net.osmand.plus.R;
import android.content.Intent;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.text.InputType;

public class SettingsOsmEditingActivity extends SettingsBaseActivity {

	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setTitle(R.string.osm_settings);
		PreferenceScreen grp = getPreferenceScreen();

		EditTextPreference userName = createEditTextPreference(settings.USER_NAME, R.string.user_name, R.string.user_name_descr);
		grp.addPreference(userName);
		EditTextPreference pwd = createEditTextPreference(settings.USER_PASSWORD, R.string.user_password, R.string.user_password_descr);
		pwd.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
		grp.addPreference(pwd);
		
		CheckBoxPreference poiEdit = createCheckBoxPreference(settings.OFFLINE_EDITION,
				R.string.offline_edition, R.string.offline_edition_descr);
		grp.addPreference(poiEdit);
		
		Preference pref = new Preference(this);
		pref.setTitle(R.string.local_openstreetmap_settings);
		pref.setSummary(R.string.local_openstreetmap_settings_descr);
		pref.setKey("local_openstreetmap_points");
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(Preference preference) {
				startActivity(new Intent(SettingsOsmEditingActivity.this, LocalOpenstreetmapActivity.class));
				return true;
			}
		});
		grp.addPreference(pref);
    }



}
