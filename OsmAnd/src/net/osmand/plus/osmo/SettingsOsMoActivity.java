package net.osmand.plus.osmo;


import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.osmo.OsMoService.SessionInfo;
import net.osmand.util.Algorithms;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.text.format.DateFormat;
import android.util.TimeUtils;
import android.widget.ProgressBar;
import android.widget.Toast;

public class SettingsOsMoActivity extends SettingsBaseActivity {

	public static final String MORE_VALUE = "MORE_VALUE";
	public static final String DEFINE_EDIT = "DEFINE_EDIT";
	private Preference debugPref;
	private Preference trackerId;
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setTitle(R.string.osmo_settings);
		PreferenceScreen grp = getPreferenceScreen();
		
		
		trackerId = new Preference(this);
		trackerId.setTitle(R.string.osmo_tracker_id);
		trackerId.setSummary(R.string.osmo_tracker_id_descr);
		
		debugPref = new Preference(this);
		debugPref.setTitle(R.string.osmo_settings_debug);
		
		updateDebugPref();
		grp.addPreference(debugPref);
    }

	private void updateDebugPref() {
		final OsMoPlugin plugin = OsMoPlugin.getEnabledPlugin(OsMoPlugin.class);
		OsMoService service = plugin.getService();
		OsMoTracker tracker = plugin.getTracker();
		StringBuilder s = new StringBuilder();
		s.append(getString(R.string.osmo_settings_uuid)).append(" : ")
				.append(getMyApplication().getSettings().OSMO_DEVICE_KEY.get().toUpperCase()).append("\n");
		if(service.isConnected()) {
			int seconds = (int) ((System.currentTimeMillis() - service.getConnectionTime()) / 1000);
			s.append(getString(R.string.osmo_conn_successfull, Algorithms.formatDuration(seconds))).append("\n");
			SessionInfo si = service.getCurrentSessionInfo();
			if(si == null) {
				s.append(getString(R.string.osmo_auth_pending)).append("\n");
			} else {
				s.append(getString(R.string.osmo_session_token, si.token)).append("\n");
			}
		} else {
			s.append(getString(R.string.osmo_io_error) + service.getLastRegistrationError()).append("\n");
		}
		s.append(getString(R.string.osmo_locations_sent,
				tracker.getLocationsSent(),
				tracker.getBufferLocationsSize()));
		debugPref.setSummary(s.toString().trim());
		debugPref.setOnPreferenceClickListener(this);
	}
	
	@Override
	public boolean onPreferenceClick(Preference preference) {
		if (preference == debugPref) {
			updateDebugPref();
			return true;
		} else if(preference == trackerId) {
			final OsMoPlugin plugin = OsMoPlugin.getEnabledPlugin(OsMoPlugin.class);
			OsMoService service = plugin.getService();
			SessionInfo ci = service.getCurrentSessionInfo();
			if(ci == null || ci.trackerId == null) {
				AccessibleToast.makeText(this, R.string.osmo_auth_pending, Toast.LENGTH_SHORT).show();
			} else {
				Builder bld = new AlertDialog.Builder(this);
				bld.setTitle(R.string.osmo_tracker_id);
				bld.setMessage(ci.trackerId);
				bld.show();
			}
		}
		return super.onPreferenceClick(preference);
	}


	public void updateAllSettings() {
		super.updateAllSettings();
	}
	
	
}
