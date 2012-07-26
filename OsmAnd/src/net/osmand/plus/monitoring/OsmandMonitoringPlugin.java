package net.osmand.plus.monitoring;

import java.util.EnumSet;

import net.osmand.LogUtil;
import net.osmand.OsmAndFormatter;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.activities.ApplicationMode;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MapInfoControl;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.TextInfoControl;

import org.apache.commons.logging.Log;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.view.View;

public class OsmandMonitoringPlugin extends OsmandPlugin {
	private static final String ID = "osmand.monitoring";
	private OsmandSettings settings;
	private OsmandApplication app;
	private static final Log log = LogUtil.getLog(OsmandMonitoringPlugin.class);
	private MapInfoControl monitoringControl;

	public OsmandMonitoringPlugin(OsmandApplication app) {
		this.app = app;
	}

	@Override
	public boolean init(OsmandApplication app) {
		settings = app.getSettings();
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
		return app.getString(R.string.monitor_preferences);
	}

	@Override
	public void registerLayers(MapActivity activity) {
		MapInfoLayer layer = activity.getMapLayers().getMapInfoLayer();
		monitoringControl = createMonitoringControl(activity, layer.getPaintText(), layer.getPaintSubText());
		
		layer.getMapInfoControls().registerSideWidget(monitoringControl,
				R.drawable.monitoring_rec_small, R.string.map_widget_monitoring, "monitoring", false,
				EnumSet.of(ApplicationMode.BICYCLE, ApplicationMode.PEDESTRIAN), EnumSet.noneOf(ApplicationMode.class), 18);
		layer.recreateControls();
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if(monitoringControl == null) {
			registerLayers(activity);
		}
	}

	@Override
	public void registerMapContextMenuActions(final MapActivity mapActivity, final double latitude, final double longitude,
			ContextMenuAdapter adapter, Object selectedObj) {
		OnContextMenuClick listener = new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int resId, int pos, boolean isChecked, DialogInterface dialog) {
				if (resId == R.string.context_menu_item_add_waypoint) {
					mapActivity.getMapActions().addWaypoint(latitude, longitude);
				}
			}
		};
		adapter.registerItem(R.string.context_menu_item_add_waypoint, R.drawable.list_activities_gpx_waypoint, listener, -1);
	}

	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		Preference offlineData = screen.findPreference("local_indexes");
		if (offlineData == null) {
			log.error("OsmandMonitoringPlugin: Index settings preference not found !");
		} else {
			offlineData.setSummary(offlineData.getSummary() + " " + app.getString(R.string.gpx_index_settings_descr));
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
				SavingTrackHelper helper = app.getSavingTrackHelper();
				if (helper.hasDataToSave()) {
					saveCurrentTracks(helper, activity);
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

	private void saveCurrentTracks(final SavingTrackHelper helper, final SettingsActivity activity) {
		activity.progressDlg = ProgressDialog.show(activity, activity.getString(R.string.saving_gpx_tracks),
				activity.getString(R.string.saving_gpx_tracks), true);
		final ProgressDialogImplementation impl = new ProgressDialogImplementation(activity.progressDlg);
		impl.setRunnable("SavingGPX", new Runnable() { //$NON-NLS-1$
					@Override
					public void run() {
						try {
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

	/**
	 * creates (if it wasn't created previously) the control to be added on a MapInfoLayer that shows a monitoring state (recorded/stopped)
	 */
	private MapInfoControl createMonitoringControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		final Drawable monitoringBig = map.getResources().getDrawable(R.drawable.monitoring_rec_big);
		final Drawable monitoringSmall = map.getResources().getDrawable(R.drawable.monitoring_rec_small);
		final Drawable monitoringInactive = map.getResources().getDrawable(R.drawable.monitoring_rec_inactive);
		monitoringControl = new TextInfoControl(map, 0, paintText, paintSubText) {
			long lastUpdateTime;
			@Override
			public boolean updateInfo() {
				boolean visible = true;
				String txt = "start";
				String subtxt = null;
				Drawable d = monitoringInactive;
				long last = lastUpdateTime;
				if (settings.SAVE_TRACK_TO_GPX.get()) {
					float dist = app.getSavingTrackHelper().getDistance();
					last = app.getSavingTrackHelper().getLastTimeUpdated();
					String ds = OsmAndFormatter.getFormattedDistance(dist, map);
					int ls = ds.lastIndexOf(' ');
					if (ls == -1) {
						txt = ds;
					} else {
						txt = ds.substring(0, ls);
						subtxt = ds.substring(ls + 1);
					}
					d = monitoringBig;
				}
				setText(txt, subtxt);
				setImageDrawable(d);
				if (last != lastUpdateTime) {
					lastUpdateTime = last;
					blink();
				}
				updateVisibility(visible);
				return true;
			}
			
			private void blink() {
				setImageDrawable(monitoringSmall);
				invalidate();
				postDelayed(new Runnable() {
					@Override
					public void run() {
						setImageDrawable(monitoringBig);
						invalidate();
					}
				}, 500);
			}
		};
		monitoringControl.updateInfo();

		// monitoringControl.addView(child);
		monitoringControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean isTrackMonitored = settings.SAVE_TRACK_TO_GPX.get();
				settings.SAVE_TRACK_TO_GPX.set(!isTrackMonitored);
				monitoringControl.updateInfo();
				app.getSavingTrackHelper().setOnPause(settings.SAVE_TRACK_TO_GPX.get());
			}
		});
		return monitoringControl;
	}

}