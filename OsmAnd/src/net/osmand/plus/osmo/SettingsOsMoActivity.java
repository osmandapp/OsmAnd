package net.osmand.plus.osmo;


import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.support.v7.app.AlertDialog;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.access.AccessibleToast;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.SettingsBaseActivity;
import net.osmand.plus.activities.actions.ShareDialog;
import net.osmand.plus.osmo.OsMoService.SessionInfo;
import net.osmand.util.Algorithms;

import java.util.List;

public class SettingsOsMoActivity extends SettingsBaseActivity {

	private Preference debugPref;
	private Preference trackerId;
//	private CheckBoxPreference sendLocationsref;
	
	public static final int[] SECONDS = new int[] {0, 1, 2, 3, 5, 10, 15, 30, 60, 90};
	public static final int[] MINUTES = new int[] {2, 3, 5};


	@Override
    public void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getToolbar().setTitle(R.string.osmo_settings);
		PreferenceScreen grp = getPreferenceScreen();
		
		
		
		trackerId = new Preference(this);
		trackerId.setTitle(R.string.osmo_tracker_id);
		trackerId.setSummary(R.string.osmo_tracker_id_descr);
		trackerId.setOnPreferenceClickListener(this);
		grp.addPreference(trackerId);
		
		CheckBoxPreference autoConnectref = createCheckBoxPreference(settings.OSMO_AUTO_CONNECT);
		autoConnectref.setTitle(R.string.osmo_auto_connect);
		autoConnectref.setSummary(R.string.osmo_auto_connect_descr);
		grp.addPreference(autoConnectref);
		
//		sendLocationsref = createCheckBoxPreference(settings.OSMO_AUTO_SEND_LOCATIONS);
//		sendLocationsref.setTitle(R.string.osmo_auto_send_locations);
//		sendLocationsref.setSummary(R.string.osmo_auto_send_locations_descr);
//		sendLocationsref.setEnabled(settings.OSMO_AUTO_CONNECT.get());
//		grp.addPreference(sendLocationsref);
		
		grp.addPreference(createTimeListPreference(settings.OSMO_SAVE_TRACK_INTERVAL, SECONDS,
				MINUTES, 1000, R.string.osmo_track_interval, R.string.osmo_track_interval_descr));
		
		CheckBoxPreference showGroupNotifiations = createCheckBoxPreference(settings.OSMO_SHOW_GROUP_NOTIFICATIONS);
		showGroupNotifiations.setTitle(R.string.osmo_show_group_notifications);
		showGroupNotifiations.setSummary(R.string.osmo_show_group_notifications_descr);
		grp.addPreference(showGroupNotifiations);
		
//		if (OsmandPlugin.isDevelopment()) {
			debugPref = new Preference(this);
			debugPref.setTitle(R.string.osmo_settings_debug);
			debugPref.setOnPreferenceClickListener(this);
			updateDebugPref();
			grp.addPreference(debugPref);
//		}
    }

	private void updateDebugPref() {
		final OsMoPlugin plugin = OsMoPlugin.getEnabledPlugin(OsMoPlugin.class);
		assert plugin != null;
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
			s.append(getString(R.string.osmo_io_error)).append(err).append("\n");
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
		final OsMoPlugin plugin = OsMoPlugin.getEnabledPlugin(OsMoPlugin.class);
		assert plugin != null;
		if (preference == debugPref) {
			updateDebugPref();
			AlertDialog.Builder bld = new AlertDialog.Builder(this);
			StringBuilder bs = new StringBuilder();
			List<String> hs = plugin.getService().getHistoryOfCommands();
			if(hs != null) {
				for(int i = hs.size() - 1 ; i >= 0; i--) {
					bs.append(hs.get(i)).append("\n");
				}
			}
			ScrollView sv = new ScrollView(this);
			TextView tv = new TextView(this);
			sv.addView(tv);
			final String log = bs.toString();
			tv.setText(log);
			tv.setPadding(5, 0, 5, 5);
			tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
			tv.setMovementMethod(LinkMovementMethod.getInstance());
			bld.setView(sv);
			bld.setNegativeButton(R.string.shared_string_ok, null);
			bld.setPositiveButton(R.string.shared_string_share, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					final Intent sendIntent = new Intent();
					sendIntent.setAction(Intent.ACTION_SEND);
					sendIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.share_fav_subject));
					sendIntent.putExtra(Intent.EXTRA_TEXT, log);
					sendIntent.setType("text/plain");
					startActivity(sendIntent);
				}
			});
			bld.show();
			return true;
		} else if(preference == trackerId) {
			OsMoService service = plugin.getService();
			SessionInfo ci = service.getCurrentSessionInfo();
			if(ci == null || ci.trackerId == null) {
				AccessibleToast.makeText(this, R.string.osmo_auth_pending, Toast.LENGTH_SHORT).show();
			} else {
				ShareDialog dlg = new ShareDialog(this);
				dlg.setTitle(getString(R.string.osmo_tracker_id));
				dlg.viewContent(ci.trackerId);
				String url = OsMoService.SHARE_TRACKER_URL+Uri.encode(ci.trackerId);
				dlg.shareURLOrText(ci.trackerId, getString(R.string.osmo_tracker_id_share, ci.trackerId, "", url), null);
				dlg.showDialog();
			}
		}
		return super.onPreferenceClick(preference);
	}

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		boolean p = super.onPreferenceChange(preference, newValue);
		String id = preference.getKey();
		if (id.equals(settings.OSMO_AUTO_CONNECT.getId())) {
			if ((Boolean) newValue) {
				final OsMoPlugin plugin = OsMoPlugin.getEnabledPlugin(OsMoPlugin.class);
				assert plugin != null;
				plugin.getService().connect(false);
			}
//			sendLocationsref.setEnabled(settings.OSMO_AUTO_CONNECT.get());
		}
		return p;
	}


	public void updateAllSettings() {
		super.updateAllSettings();
	}
	
	
}
