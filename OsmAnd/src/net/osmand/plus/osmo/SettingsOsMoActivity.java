package net.osmand.plus.osmo;


import net.osmand.access.AccessibleToast;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.actions.ShareDialog;
import net.osmand.plus.osmo.OsMoService.SessionInfo;
import net.osmand.util.Algorithms;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.widget.Toast;

public class SettingsOsMoActivity extends SettingsBaseActivity {

	private Preference debugPref;
	private Preference trackerId;
	private CheckBoxPreference sendLocationsref;
	
	public static final int[] SECONDS = new int[] {1, 2, 3, 5, 10, 15, 30, 60, 90};
	public static final int[] MINUTES = new int[] {2, 3, 5};
	
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getSupportActionBar().setTitle(R.string.osmo_settings);
		PreferenceScreen grp = getPreferenceScreen();
		
		
		
		trackerId = new Preference(this);
		trackerId.setTitle(R.string.osmo_tracker_id);
		trackerId.setSummary(R.string.osmo_tracker_id_descr);
		trackerId.setOnPreferenceClickListener(this);
		grp.addPreference(trackerId);
		
		sendLocationsref = createCheckBoxPreference(settings.OSMO_AUTO_SEND_LOCATIONS);
		sendLocationsref.setTitle(R.string.osmo_auto_send_locations);
		sendLocationsref.setSummary(R.string.osmo_auto_send_locations_descr);
		grp.addPreference(sendLocationsref);
		
		grp.addPreference(createTimeListPreference(settings.OSMO_SAVE_TRACK_INTERVAL, SECONDS,
				MINUTES, 1000, R.string.osmo_track_interval, R.string.osmo_track_interval_descr));
		
		debugPref = new Preference(this);
		debugPref.setTitle(R.string.osmo_settings_debug);
		debugPref.setOnPreferenceClickListener(this);
		updateDebugPref();
		grp.addPreference(debugPref);
    }

	private void updateDebugPref() {
		final OsMoPlugin plugin = OsMoPlugin.getEnabledPlugin(OsMoPlugin.class);
		OsMoService service = plugin.getService();
		OsMoTracker tracker = plugin.getTracker();
		StringBuilder s = new StringBuilder();
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
			String err = service.getLastRegistrationError();
			if(err == null) {
				err = "...";
			}
			s.append(getString(R.string.osmo_io_error) + err).append("\n");
		}
		s.append(getString(R.string.osmo_locations_sent,
				tracker.getLocationsSent(),
				tracker.getBufferLocationsSize())).append("\n");
		s.append(getString(R.string.osmo_settings_uuid)).append(" : ")
		.append(getMyApplication().getSettings().OSMO_DEVICE_KEY.get().toUpperCase()).append("\n");
		debugPref.setSummary(s.toString().trim());
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
				ShareDialog dlg = new ShareDialog(this);
				dlg.setTitle(getString(R.string.osmo_tracker_id));
				dlg.viewContent(ci.trackerId);
				dlg.shareURLOrText(ci.trackerId, getString(R.string.osmo_tracker_id_share, ci.trackerId), null);
				dlg.showDialog();
			}
		}
		return super.onPreferenceClick(preference);
	}
	
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean p = super.onPreferenceChange(preference, newValue);
		String id = preference.getKey();
		if (id.equals(settings.OSMO_AUTO_SEND_LOCATIONS.getId())) {
			if ((Boolean) newValue) {
				final OsMoPlugin plugin = OsMoPlugin.getEnabledPlugin(OsMoPlugin.class);
				plugin.getTracker().enableTracker();
			}
		}
		return p;
	}


	public void updateAllSettings() {
		super.updateAllSettings();
	}
	
	
}
