package net.osmand.plus.osmedit;


import net.osmand.plus.OsmAndAppCustomization;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.myplaces.FavoritesActivity;
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
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getToolbar().setTitle(R.string.osm_settings);
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
				OsmAndAppCustomization appCustomization = getMyApplication().getAppCustomization();
				final Intent favorites = new Intent(SettingsOsmEditingActivity.this,
						appCustomization.getFavoritesActivity());
				favorites.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				getMyApplication().getSettings().FAVORITES_TAB.set(R.string.osm_edits);
				startActivity(favorites);
				return true;
			}
		});
		grp.addPreference(pref);
    }



}
