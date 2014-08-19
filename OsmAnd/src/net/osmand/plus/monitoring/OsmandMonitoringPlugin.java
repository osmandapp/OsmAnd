package net.osmand.plus.monitoring;

import gnu.trove.list.array.TIntArrayList;

import java.util.List;

import net.osmand.Location;
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
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceScreen;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

public class OsmandMonitoringPlugin extends OsmandPlugin implements MonitoringInfoControlServices {
	private static final String ID = "osmand.monitoring";
	private OsmandSettings settings;
	private OsmandApplication app;
	private BaseMapWidget monitoringControl;
	private LiveMonitoringHelper liveMonitoringHelper;
	private boolean ADD_BG_TO_ACTION = true;

	public OsmandMonitoringPlugin(OsmandApplication app) {
		this.app = app;
		liveMonitoringHelper = new LiveMonitoringHelper(app);
		final List<ApplicationMode> am = ApplicationMode.allPossibleValues();
		ApplicationMode.regWidget("monitoring", am.toArray(new ApplicationMode[am.size()]));
	}
	
	@Override
	public void updateLocation(Location location) {
		liveMonitoringHelper.updateLocation(location);
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
				R.drawable.monitoring_rec_big, R.string.map_widget_monitoring, "monitoring", false, 18);
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
	
	public static final int[] SECONDS = new int[] {0, 1, 2, 3, 5, 10, 15, 30, 60, 90};
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
				final boolean globalRecord = settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
				if (globalRecord || settings.SAVE_TRACK_TO_GPX.get()) {
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
					if(globalRecord) {
						d = monitoringBig;
					}
				}
				setText(txt, subtxt);
				setImageDrawable(d);
				if (last != lastUpdateTime && globalRecord) {
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
				controlDialog(map);
			}

			
		});
		return monitoringControl;
	}

	private void controlDialog(final MapActivity map) {
		final boolean wasTrackMonitored = settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
		
		Builder bld = new AlertDialog.Builder(map);
		final TIntArrayList items = new TIntArrayList();
		if(wasTrackMonitored) {
			items.add(R.string.gpx_monitoring_stop);
			items.add(R.string.gpx_start_new_segment);
			if(settings.LIVE_MONITORING.get()) {
				items.add(R.string.live_monitoring_stop);
			} else if(!settings.LIVE_MONITORING_URL.getProfileDefaultValue().equals(settings.LIVE_MONITORING_URL.get())){
				items.add(R.string.live_monitoring_start);
			}
		} else {
			items.add(R.string.gpx_monitoring_start);
		}
		items.add(R.string.save_current_track);
		String[] strings = new String[items.size()];
		for(int i =0; i < strings.length; i++) {
			strings[i] = app.getString(items.get(i));
		}
		bld.setItems(strings, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				int item = items.get(which);
				if(item == R.string.save_current_track){
					app.getTaskManager().runInBackground(new OsmAndTaskRunnable<Void, Void, Void>() {

						@Override
						protected Void doInBackground(Void... params) {
							SavingTrackHelper helper = app.getSavingTrackHelper();
							helper.saveDataToGpx(app.getAppCustomization().getTracksDir());
							helper.close();
							return null;
						}

					}, (Void) null);
				} else if(item == R.string.gpx_monitoring_start) {
					startGPXMonitoring();
				} else if(item == R.string.gpx_monitoring_stop) {
					settings.SAVE_GLOBAL_TRACK_TO_GPX.set(false);
					if (app.getNavigationService() != null) {
						app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_GPX);
					}
				} else if(item == R.string.gpx_start_new_segment) {
					app.getSavingTrackHelper().startNewSegment();
				} else if(item == R.string.live_monitoring_stop) {
					settings.LIVE_MONITORING.set(false);
				} else if(item == R.string.live_monitoring_start) {
					final ValueHolder<Integer> vs = new ValueHolder<Integer>();
					vs.value = settings.LIVE_MONITORING_INTERVAL.get();
					showIntervalChooseDialog(map, app.getString(R.string.live_monitoring_interval) + " : %s", 
							app.getString(R.string.save_track_to_gpx), SECONDS, MINUTES,
							vs, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							settings.LIVE_MONITORING_INTERVAL.set(vs.value);
							settings.LIVE_MONITORING.set(true);
						}
					});
				}
				monitoringControl.updateInfo(null);
			}

			private void startGPXMonitoring() {
				app.getSavingTrackHelper().startNewSegment();
				final ValueHolder<Integer> vs = new ValueHolder<Integer>();
				vs.value = settings.SAVE_GLOBAL_TRACK_INTERVAL.get();
				showIntervalChooseDialog(map, app.getString(R.string.save_track_interval) + " : %s", 
						app.getString(R.string.save_track_to_gpx), SECONDS, MINUTES,
						vs, new OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						settings.SAVE_GLOBAL_TRACK_INTERVAL.set(vs.value);
						settings.SAVE_GLOBAL_TRACK_TO_GPX.set(true);
						if (app.getNavigationService() == null) {
							settings.SERVICE_OFF_INTERVAL.set(0);
						}
						app.startNavigationService(NavigationService.USED_BY_GPX);
					}
				});
				
			}
		});
		bld.show();
	}
	
	public static void showIntervalChooseDialog(final Context uiCtx, final String patternMsg,
			String title, final int[] seconds, final int[] minutes, final ValueHolder<Integer> v, OnClickListener onclick){
		Builder dlg = new AlertDialog.Builder(uiCtx);
		dlg.setTitle(title);
		LinearLayout ll = new LinearLayout(uiCtx);
		final TextView tv = new TextView(uiCtx);
		tv.setPadding(7, 3, 7, 0);
		tv.setText(String.format(patternMsg, uiCtx.getString(R.string.int_continuosly)));
		SeekBar sp = new SeekBar(uiCtx);
		sp.setPadding(7, 5, 7, 0);
		final int secondsLength = seconds.length;
    	final int minutesLength = minutes.length;
    	sp.setMax(secondsLength + minutesLength - 1);
		sp.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
			}
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
				String s;
				if(progress == 0) {
					s = uiCtx.getString(R.string.int_continuosly);
					v.value = 0;
				} else {
					if(progress < secondsLength) {
						s = seconds[progress] + " " + uiCtx.getString(R.string.int_seconds);
						v.value = seconds[progress] * 1000;
					} else {
						s = minutes[progress - secondsLength] + " " + uiCtx.getString(R.string.int_min);
						v.value = minutes[progress - secondsLength] * 60 * 1000;
					}
				}
				tv.setText(String.format(patternMsg, s));
				
			}
		});
		
		for (int i = 0; i < secondsLength + minutesLength - 1; i++) {
			if (i < secondsLength) {
				if (v.value <= seconds[i] * 1000) {
					sp.setProgress(i);
					break;
				}
			} else {
				if (v.value <= minutes[i - secondsLength] * 1000 * 60) {
					sp.setProgress(i);
					break;
				}
			}
		}
		
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.addView(tv);
		ll.addView(sp);
		dlg.setView(ll);
		dlg.setPositiveButton(R.string.default_buttons_ok, onclick);
		dlg.setNegativeButton(R.string.default_buttons_cancel, null);
		dlg.show();
	}
	
	
	@Override
	public void addMonitorActions(final ContextMenuAdapter qa, final MonitoringInfoControl li, final OsmandMapTileView view) {
		if (ADD_BG_TO_ACTION) {
			final Intent serviceIntent = new Intent(view.getContext(), NavigationService.class);
			final boolean bgoff = view.getApplication().getNavigationService() == null;
			int msgId = !bgoff ? R.string.bg_service_sleep_mode_on : R.string.bg_service_sleep_mode_off;
			int draw = !bgoff ? R.drawable.monitoring_rec_big : R.drawable.monitoring_rec_inactive;
			qa.item(msgId).icon(draw).listen(new OnContextMenuClick() {
				@Override
				public void onContextMenuClick(int itemId, int pos, boolean isChecked, DialogInterface dialog) {
					if (view.getApplication().getNavigationService() == null) {
						final ValueHolder<Integer> vs = new ValueHolder<Integer>();
						vs.value = view.getSettings().SERVICE_OFF_INTERVAL.get();
						showIntervalChooseDialog(view.getContext(), app.getString(R.string.gps_wakeup_interval),
								app.getString(R.string.background_router_service),
								SettingsMonitoringActivity.BG_SECONDS, SettingsMonitoringActivity.BG_MINUTES, vs,
								new OnClickListener() {
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
		}
	}

}