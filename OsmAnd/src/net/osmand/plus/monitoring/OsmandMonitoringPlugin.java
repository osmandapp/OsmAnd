package net.osmand.plus.monitoring;

import java.util.EnumSet;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.ContextMenuAdapter;
import net.osmand.plus.ContextMenuAdapter.OnContextMenuClick;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndTaskManager.OsmAndTaskRunnable;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.views.MapInfoLayer;
import net.osmand.plus.views.MonitoringInfoControl;
import net.osmand.plus.views.MonitoringInfoControl.MonitoringInfoControlServices;
import net.osmand.plus.views.MonitoringInfoControl.ValueHolder;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.mapwidgets.BaseMapWidget;
import net.osmand.plus.views.mapwidgets.TextInfoWidget;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.view.View;

public class OsmandMonitoringPlugin extends OsmandPlugin implements MonitoringInfoControlServices {
	private static final String ID = "osmand.monitoring";
	private OsmandSettings settings;
	private OsmandApplication app;
	private BaseMapWidget monitoringControl;

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
		return app.getString(R.string.osmand_monitoring_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.osmand_monitoring_plugin_name);
	}

	@Override
	public void registerLayers(MapActivity activity) {
		MapInfoLayer layer = activity.getMapLayers().getMapInfoLayer();
		monitoringControl = createMonitoringControl(activity, layer.getPaintText(), layer.getPaintSubText());
		
		layer.getMapInfoControls().registerSideWidget(monitoringControl,
				R.drawable.monitoring_rec_big, R.string.map_widget_monitoring, "monitoring", false,
				EnumSet.of(ApplicationMode.BICYCLE, ApplicationMode.PEDESTRIAN), EnumSet.noneOf(ApplicationMode.class), 18);
		layer.recreateControls();
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if(monitoringControl == null) {
			registerLayers(activity);
		}
		MonitoringInfoControl lock = activity.getMapLayers().getMapInfoLayer().getMonitoringInfoControl();
		if(lock != null && !lock.getMonitorActions().contains(this)) {
			lock.addMonitorActions(this);
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
		adapter.item(R.string.context_menu_item_add_waypoint).icons(R.drawable.ic_action_gnew_label_dark, R.drawable.ic_action_gnew_label_light)
		.listen(listener).reg();
	}
	
	public static final int[] SECONDS = new int[] {1, 2, 3, 5, 10, 15, 30, 60, 90};
	public static final int[] MINUTES = new int[] {2, 3, 5};
	
	@Override
	public void settingsActivityCreate(final SettingsActivity activity, PreferenceScreen screen) {
		Preference grp = new Preference(activity);
		grp.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			
			@Override
			public boolean onPreferenceClick(Preference preference) {
				activity.startActivity(new Intent(activity, SettingsMonitoringActivity.class));
				return true;
			}
		});
		grp.setTitle(R.string.monitoring_settings);
		grp.setSummary(R.string.monitoring_settings_descr);
		grp.setKey("monitoring_settings");
		screen.addPreference(grp);
		
	}

	

	/**
	 * creates (if it wasn't created previously) the control to be added on a MapInfoLayer that shows a monitoring state (recorded/stopped)
	 */
	private BaseMapWidget createMonitoringControl(final MapActivity map, Paint paintText, Paint paintSubText) {
		final Drawable monitoringBig = map.getResources().getDrawable(R.drawable.monitoring_rec_big);
		final Drawable monitoringSmall = map.getResources().getDrawable(R.drawable.monitoring_rec_small);
		final Drawable monitoringInactive = map.getResources().getDrawable(R.drawable.monitoring_rec_inactive);
		monitoringControl = new TextInfoWidget(map, 0, paintText, paintSubText) {
			long lastUpdateTime;
			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				boolean visible = true;
				String txt = map.getString(R.string.monitoring_control_start);
				String subtxt = null;
				Drawable d = monitoringInactive;
				long last = lastUpdateTime;
				if (settings.SAVE_TRACK_TO_GPX.get()) {
					float dist = app.getSavingTrackHelper().getDistance();
					last = app.getSavingTrackHelper().getLastTimeUpdated();
					String ds = OsmAndFormatter.getFormattedDistance(dist, map.getMyApplication());
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
		monitoringControl.updateInfo(null);

		// monitoringControl.addView(child);
		monitoringControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				boolean wasTrackMonitored = settings.SAVE_TRACK_TO_GPX.get();
				if (!wasTrackMonitored) {
					app.getSavingTrackHelper().startNewSegment();
				}
				settings.SAVE_TRACK_TO_GPX.set(!wasTrackMonitored);
				final Intent serviceIntent = new Intent(map, NavigationService.class);
				if (wasTrackMonitored) {
					if (app.getNavigationService() != null && !app.getNavigationService().startedForNavigation()) {
						app.stopService(serviceIntent);
					}
				} else {
					if (app.getNavigationService() == null) {
						app.getSettings().SERVICE_OFF_INTERVAL.set(0);
						app.startService(serviceIntent);
					}
				}
				
				monitoringControl.updateInfo(null);
			}
		});
		return monitoringControl;
	}

	@Override
	public void addMonitorActions(final ContextMenuAdapter qa, final MonitoringInfoControl li, final OsmandMapTileView view) {
		final Intent serviceIntent = new Intent(view.getContext(), NavigationService.class);
		final boolean off = !view.getSettings().SAVE_TRACK_TO_GPX.get();
		qa.item(off ? R.string.monitoring_mode_off : R.string.monitoring_mode_on
				).icon(  off ? R.drawable.monitoring_rec_inactive : R.drawable.monitoring_rec_big).listen(new OnContextMenuClick() {

			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				if (off) {
					final ValueHolder<Integer> vs = new ValueHolder<Integer>();
					vs.value = view.getSettings().SAVE_TRACK_INTERVAL.get();
					li.showIntervalChooseDialog(view, view.getContext().getString(R.string.save_track_interval) + " : %s", 
							view.getContext().getString(R.string.save_track_to_gpx), SECONDS, MINUTES,
							vs, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							view.getSettings().SAVE_TRACK_INTERVAL.set(vs.value);
							view.getSettings().SAVE_TRACK_TO_GPX.set(true);
							if (view.getApplication().getNavigationService() == null) {
								view.getSettings().SERVICE_OFF_INTERVAL.set(0);
								view.getContext().startService(serviceIntent);
							}
						}
					});
				} else {
					view.getSettings().SAVE_TRACK_TO_GPX.set(false);
					if (view.getApplication().getNavigationService() != null && !view.getApplication().getNavigationService().startedForNavigation()) {
						view.getContext().stopService(serviceIntent);
					}
				}
			}
		}).reg();
		
		final boolean bgoff = view.getApplication().getNavigationService() == null;
		int msgId = !bgoff? R.string.bg_service_sleep_mode_on : R.string.bg_service_sleep_mode_off;
		int draw = !bgoff? R.drawable.monitoring_rec_big : R.drawable.monitoring_rec_inactive;
		qa.item(msgId).icon(draw).listen(new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				if (view.getApplication().getNavigationService() == null) {
					final ValueHolder<Integer> vs = new ValueHolder<Integer>();
					vs.value = view.getSettings().SERVICE_OFF_INTERVAL.get();
					li.showIntervalChooseDialog(view, view.getContext().getString(R.string.gps_wakeup_interval), 
							view.getContext().getString(R.string.background_router_service), SettingsMonitoringActivity.BG_SECONDS, 
							SettingsMonitoringActivity.BG_MINUTES,
							vs, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							view.getSettings().SERVICE_OFF_INTERVAL.set(vs.value);
							view.getContext().startService(serviceIntent);	
						}
					});
				} else {
					view.getContext().stopService(serviceIntent);
				}
			}
		}).position(0).reg();

		qa.item(R.string.save_current_track_widget).icon(R.drawable.monitoring_rec_inactive).listen(
					new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				app.getTaskManager().runInBackground(new OsmAndTaskRunnable<Void, Void, Void>() {

					@Override
					protected Void doInBackground(Void... params) {
						SavingTrackHelper helper = app.getSavingTrackHelper();
						helper.saveDataToGpx();
						helper.close();
						return null;
					}

				}, (Void) null);
			}
		}).position(-1).reg();

		final boolean liveoff = !view.getSettings().LIVE_MONITORING.get();
		qa.item(liveoff ? R.string.live_monitoring_mode_off : R.string.live_monitoring_mode_on).icon(
				liveoff ? R.drawable.monitoring_rec_inactive: R.drawable.monitoring_rec_big).listen( 
						new OnContextMenuClick() {
			@Override
			public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
				if (liveoff) {
					final ValueHolder<Integer> vs = new ValueHolder<Integer>();
					vs.value = view.getSettings().LIVE_MONITORING_INTERVAL.get();
					li.showIntervalChooseDialog(view, view.getContext().getString(R.string.live_monitoring_interval) + " : %s", 
							view.getContext().getString(R.string.live_monitoring), SECONDS, MINUTES,
							vs, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							view.getSettings().LIVE_MONITORING_INTERVAL.set(vs.value);
							view.getSettings().LIVE_MONITORING.set(true);
						}
					});
				} else {
					view.getSettings().LIVE_MONITORING.set(false);
				}
			}
		}).reg();
	}

}