package net.osmand.plus.osmo;


import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.widget.ProgressBar;

public class SettingsOsMoActivity extends SettingsBaseActivity {

	public static final String MORE_VALUE = "MORE_VALUE";
	public static final String DEFINE_EDIT = "DEFINE_EDIT";
	private Preference uuid;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setTitle(R.string.osmo_settings);
		PreferenceScreen grp = getPreferenceScreen();
		
		uuid = new Preference(this);
		uuid.setTitle(R.string.osmo_settings_uuid);
		uuid.setSummary(getMyApplication().getSettings().OSMO_DEVICE_KEY.get().toUpperCase());
		
		grp.addPreference(uuid);
		
		
    }
	
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == uuid) {
			OsMoPlugin.getEnabledPlugin(OsMoPlugin.class).getRegisterDeviceTask(this, new Runnable() {
				@Override
				public void run() {
					updateAllSettings();						
				}
			}).execute();
		}
		return super.onPreferenceClick(preference);
	}


	public void updateAllSettings() {
		super.updateAllSettings();
	}
	
	
}
