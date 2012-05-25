package net.osmand.plus.monitoring;

import org.apache.commons.logging.Log;

import net.osmand.LogUtil;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.SettingsActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;

public class OsmandMonitoringPlugin extends OsmandPlugin {
	private static final String ID = "osmand.monitoring";
	private OsmandSettings settings;
	private OsmandApplication app;
	private static final Log log = LogUtil.getLog(OsmandMonitoringPlugin.class);
	
	public OsmandMonitoringPlugin(OsmandApplication app) {
		this.app = app;
	}
	
	@Override
	public boolean init(OsmandApplication app) {
		settings = app.getSettings();
		settings.SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.CAR, true);
		settings.SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.BICYCLE, true);
		settings.SAVE_TRACK_TO_GPX.setModeDefaultValue(ApplicationMode.PEDESTRIAN, true);
		return true;
	}
	
	@Override
	public String getId() {
		return ID;
	}
	@Override
	public String getDescription() {
		return app.getString(R.string.osmand_monitoring_description);
	}
	@Override
	public String getName() {
		return app.getString(R.string.osmand_monitoring_name);
	}
	@Override
	public void registerLayers(MapActivity activity) {
	}
	
	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude, ContextMenuAdapter adapter,
			Object selectedObj) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos, boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.context_menu_item_add_waypoint) {
					mapActivity.getMapActions().addWaypoint(latitude, longitude);
				}
			}
		};
		adapter.registerItem(R.string.context_menu_item_add_waypoint, 0, listener, -1);
	}
	

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		Preference offlineData = screen.findPreference("index_settings");
		if(offlineData == null) {
			log.error("OsmandMonitoringPlugin: Index settings preference not found !!!");
		} else {
			offlineData.setSummary(offlineData.getSummary() + " "+ app.getString(R.string.gpx_index_settings_descr));
		}
		PreferenceScreen grp = screen.getPreferenceManager().createPreferenceScreen(activity);
		grp.setTitle(R.string.monitor_preferences);
		grp.setSummary(R.string.monitor_preferences_descr);
		grp.setKey("monitor_settings");
		((PreferenceCategory) screen.findPreference("profile_dep_cat")).addPreference(grp);

		PreferenceCategory cat = new PreferenceCategory(activity);
		cat.setTitle(R.string.save_track_to_gpx);
		grp.addPreference(cat);

		cat.addPreference(activity.createCheckBoxPreference(settings.SAVE_TRACK_TO_GPX, R.string.save_track_to_gpx,
				R.string.save_track_to_gpx_descrp));

		cat.addPreference(activity.createTimeListPreference(settings.SAVE_TRACK_INTERVAL, new int[] { 1, 2, 3, 5, 10, 15, 20, 30 },
				new int[] { 1, 2, 3, 5 }, 1, R.string.save_track_interval, R.string.save_track_interval_descr));

		Preference pref = new Preference(activity);
		pref.setTitle(R.string.save_current_track);
		pref.setSummary(R.string.save_current_track_descr);
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {

			@Override
			public boolean onPreferenceClick(Preference preference) {
				SavingTrackHelper helper = new SavingTrackHelper(activity);
				if (helper.hasDataToSave()) {
					saveCurrentTracks(activity);
				} else {
					helper.close();
				}
				return true;
			}
		});
		cat.addPreference(pref);

		cat = new PreferenceCategory(activity);
		cat.setTitle(R.string.live_monitoring);
		grp.addPreference(cat);

		cat.addPreference(activity.createCheckBoxPreference(settings.LIVE_MONITORING, R.string.live_monitoring,
				R.string.live_monitoring_descr));
		cat.addPreference(activity.createEditTextPreference(settings.LIVE_MONITORING_URL, R.string.live_monitoring_url,
				R.string.live_monitoring_url_descr));

		cat.addPreference(activity.createTimeListPreference(settings.LIVE_MONITORING_INTERVAL, new int[] { 1, 2, 3, 5, 10, 15, 20, 30 },
				new int[] { 1, 2, 3, 5 }, 1, R.string.live_monitoring_interval, R.string.live_monitoring_interval_descr));

	}
	
	private void saveCurrentTracks(final SettingsActivity activity) {
		activity.progressDlg = ProgressDialog.show(activity, activity.getString(R.string.saving_gpx_tracks), activity.getString(R.string.saving_gpx_tracks), true);
		final ProgressDialogImplementation impl = new ProgressDialogImplementation(activity.progressDlg);
		impl.setRunnable("SavingGPX", new Runnable() { //$NON-NLS-1$
					@Override
					public void run() {
						try {
							SavingTrackHelper helper = new SavingTrackHelper(activity);
							helper.saveDataToGpx();
							helper.close();
						} finally {
							if (activity.progressDlg != null) {
								activity.progressDlg.dismiss();
								activity.progressDlg = null;
							}
						}
					}
				});
		impl.run();
	}
}
