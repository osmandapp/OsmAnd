package net.osmand.plus.monitoring;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import net.osmand.AndroidUtils;
import net.osmand.Location;
import net.osmand.ValueHolder;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmAndLocationProvider;
import net.osmand.plus.OsmAndTaskManager.OsmAndTaskRunnable;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.OsmandPlugin;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.SavingTrackHelper.SaveGpxResult;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.views.OsmandMapLayer.DrawSettings;
import net.osmand.plus.views.OsmandMapTileView;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import java.lang.ref.WeakReference;
import java.util.List;

import gnu.trove.list.array.TIntArrayList;

public class OsmandMonitoringPlugin extends OsmandPlugin {

	public static final String ID = "osmand.monitoring";
	public static final int REQUEST_LOCATION_PERMISSION_FOR_GPX_RECORDING = 208;

	private MapActivity mapActivity;
	private final OsmandSettings settings;
	private TextInfoWidget monitoringControl;
	private final LiveMonitoringHelper liveMonitoringHelper;
	private boolean isSaving;
	private boolean showDialogWhenActivityResumed;

	public OsmandMonitoringPlugin(OsmandApplication app) {
		super(app);
		liveMonitoringHelper = new LiveMonitoringHelper(app);
		final List<ApplicationMode> am = ApplicationMode.allPossibleValues();
		ApplicationMode.regWidgetVisibility("monitoring", am.toArray(new ApplicationMode[0]));
		settings = app.getSettings();
		pluginPreferences.add(settings.SAVE_TRACK_TO_GPX);
		pluginPreferences.add(settings.SAVE_TRACK_INTERVAL);
		pluginPreferences.add(settings.SAVE_TRACK_MIN_DISTANCE);
		pluginPreferences.add(settings.SAVE_TRACK_PRECISION);
		pluginPreferences.add(settings.AUTO_SPLIT_RECORDING);
		pluginPreferences.add(settings.DISABLE_RECORDING_ONCE_APP_KILLED);
		pluginPreferences.add(settings.SAVE_HEADING_TO_GPX);
		pluginPreferences.add(settings.SHOW_TRIP_REC_NOTIFICATION);
		pluginPreferences.add(settings.TRACK_STORAGE_DIRECTORY);
		pluginPreferences.add(settings.LIVE_MONITORING);
		pluginPreferences.add(settings.LIVE_MONITORING_URL);
		pluginPreferences.add(settings.LIVE_MONITORING_INTERVAL);
		pluginPreferences.add(settings.LIVE_MONITORING_MAX_INTERVAL_TO_SEND);
	}

	@Override
	public void disable(OsmandApplication app) {
		super.disable(app);
		app.getNotificationHelper().refreshNotifications();
	}

	@Override
	public void updateLocation(Location location) {
		liveMonitoringHelper.updateLocation(location);
	}

	@Override
	public int getLogoResourceId() {
		return R.drawable.ic_action_gps_info;
	}

	@Override
	public Drawable getAssetResourceImage() {
		return app.getUIUtilities().getIcon(R.drawable.trip_recording);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public CharSequence getDescription() {
		return app.getString(R.string.record_plugin_description);
	}

	@Override
	public String getName() {
		return app.getString(R.string.record_plugin_name);
	}


	@Override
	public String getHelpFileName() {
		return "feature_articles/trip-recording-plugin.html";
	}

	@Override
	public void registerLayers(MapActivity activity) {
		registerWidget(activity);
	}

	private void registerWidget(MapActivity activity) {
		MapInfoLayer layer = activity.getMapLayers().getMapInfoLayer();
		monitoringControl = createMonitoringControl(activity);

		layer.registerSideWidget(monitoringControl,
				R.drawable.ic_action_play_dark, R.string.map_widget_monitoring, "monitoring", false, 30);
		layer.recreateControls();
	}

	@Override
	public void updateLayers(OsmandMapTileView mapView, MapActivity activity) {
		if (isActive()) {
			if (monitoringControl == null) {
				registerWidget(activity);
			}
		} else {
			if (monitoringControl != null) {
				MapInfoLayer layer = activity.getMapLayers().getMapInfoLayer();
				layer.removeSideWidget(monitoringControl);
				layer.recreateControls();
				monitoringControl = null;
			}
		}
	}

	public static final int[] SECONDS = new int[]{0, 1, 2, 3, 5, 10, 15, 20, 30, 60, 90};
	public static final int[] MINUTES = new int[]{2, 3, 5};
	public static final int[] MAX_INTERVAL_TO_SEND_MINUTES = new int[]{1, 2, 5, 10, 15, 20, 30, 60, 90, 2 * 60, 3 * 60, 4 * 60, 6 * 60, 12 * 60, 24 * 60};

	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.MONITORING_SETTINGS;
	}

	@Override
	public String getPrefsDescription() {
		return app.getString(R.string.monitoring_prefs_descr);
	}

	/**
	 * creates (if it wasn't created previously) the control to be added on a MapInfoLayer that shows a monitoring state (recorded/stopped)
	 */
	private TextInfoWidget createMonitoringControl(final MapActivity map) {
		monitoringControl = new TextInfoWidget(map) {
			long lastUpdateTime;

			@Override
			public boolean updateInfo(DrawSettings drawSettings) {
				if (isSaving) {
					setText(map.getString(R.string.shared_string_save), "");
					setIcons(R.drawable.widget_monitoring_rec_big_day, R.drawable.widget_monitoring_rec_big_night);
					return true;
				}
				String txt = map.getString(R.string.monitoring_control_start);
				String subtxt = null;
				int dn;
				int d;
				long last = lastUpdateTime;
				final boolean globalRecord = settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
				final boolean isRecording = app.getSavingTrackHelper().getIsRecording();
				float dist = app.getSavingTrackHelper().getDistance();

				//make sure widget always shows recorded track distance if unsaved track exists
				if (dist > 0) {
					last = app.getSavingTrackHelper().getLastTimeUpdated();
					String ds = OsmAndFormatter.getFormattedDistance(dist, map.getMyApplication());
					int ls = ds.lastIndexOf(' ');
					if (ls == -1) {
						txt = ds;
					} else {
						txt = ds.substring(0, ls);
						subtxt = ds.substring(ls + 1);
					}
				}

				final boolean liveMonitoringEnabled = liveMonitoringHelper.isLiveMonitoringEnabled();
				if (globalRecord) {
					//indicates global recording (+background recording)
					if (liveMonitoringEnabled) {
						dn = R.drawable.widget_live_monitoring_rec_big_night;
						d = R.drawable.widget_live_monitoring_rec_big_day;
					} else {
						dn = R.drawable.widget_monitoring_rec_big_night;
						d = R.drawable.widget_monitoring_rec_big_day;
					}
				} else if (isRecording) {
					//indicates (profile-based, configured in settings) recording (looks like is only active during nav in follow mode)
					if (liveMonitoringEnabled) {
						dn = R.drawable.widget_live_monitoring_rec_small_night;
						d = R.drawable.widget_live_monitoring_rec_small_day;
					} else {
						dn = R.drawable.widget_monitoring_rec_small_night;
						d = R.drawable.widget_monitoring_rec_small_day;
					}
				} else {
					dn = R.drawable.widget_monitoring_rec_inactive_night;
					d = R.drawable.widget_monitoring_rec_inactive_day;
				}

				setText(txt, subtxt);
				setIcons(d, dn);
				if ((last != lastUpdateTime) && (globalRecord || isRecording)) {
					lastUpdateTime = last;
					//blink implementation with 2 indicator states (global logging + profile/navigation logging)
					if (liveMonitoringEnabled) {
						dn = R.drawable.widget_live_monitoring_rec_small_night;
						d = R.drawable.widget_live_monitoring_rec_small_day;
					} else {
						dn = R.drawable.widget_monitoring_rec_small_night;
						d = R.drawable.widget_monitoring_rec_small_day;
					}
					setIcons(d, dn);

					map.getMyApplication().runInUIThread(new Runnable() {
						@Override
						public void run() {
							int dn;
							int d;
							if (globalRecord) {
								if (liveMonitoringEnabled) {
									dn = R.drawable.widget_live_monitoring_rec_big_night;
									d = R.drawable.widget_live_monitoring_rec_big_day;
								} else {
									dn = R.drawable.widget_monitoring_rec_big_night;
									d = R.drawable.widget_monitoring_rec_big_day;
								}
							} else {
								if (liveMonitoringEnabled) {
									dn = R.drawable.widget_live_monitoring_rec_small_night;
									d = R.drawable.widget_live_monitoring_rec_small_day;
								} else {
									dn = R.drawable.widget_monitoring_rec_small_night;
									d = R.drawable.widget_monitoring_rec_small_day;
								}
							}
							setIcons(d, dn);
						}
					}, 500);
				}
				return true;
			}
		};
		monitoringControl.updateInfo(null);

		// monitoringControl.addView(child);
		monitoringControl.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				controlDialog(map, true);
			}


		});
		return monitoringControl;
	}

	@Override
	public void mapActivityResume(MapActivity activity) {
		this.mapActivity = activity;
		if (showDialogWhenActivityResumed) {
			showDialogWhenActivityResumed = false;
			controlDialog(mapActivity, true);
		}
	}

	@Override
	public void mapActivityPause(MapActivity activity) {
		this.mapActivity = null;
	}

	@Override
	public void handleRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		if (requestCode == REQUEST_LOCATION_PERMISSION_FOR_GPX_RECORDING) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				showDialogWhenActivityResumed = true;
			} else {
				app.showToastMessage(R.string.no_location_permission);
			}
		}
	}

	public void controlDialog(final Activity activity, final boolean showTrackSelection) {
		final boolean wasTrackMonitored = settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
		final boolean nightMode;
		if (activity instanceof MapActivity) {
			nightMode = app.getDaynightHelper().isNightModeForMapControls();
		} else {
			nightMode = !app.getSettings().isLightContent();
		}
		AlertDialog.Builder bld = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
		final TIntArrayList items = new TIntArrayList();
		if (wasTrackMonitored) {
			items.add(R.string.gpx_monitoring_stop);
			items.add(R.string.gpx_start_new_segment);
			if (settings.LIVE_MONITORING.get()) {
				items.add(R.string.live_monitoring_stop);
			} else if (!settings.LIVE_MONITORING_URL.getProfileDefaultValue(settings.APPLICATION_MODE.get()).
					equals(settings.LIVE_MONITORING_URL.get())) {
				items.add(R.string.live_monitoring_start);
			}
		} else {
			items.add(R.string.gpx_monitoring_start);
		}
		if (app.getSavingTrackHelper().hasDataToSave()) {
			items.add(R.string.save_current_track);
			items.add(R.string.clear_recorded_data);
		}
		String[] strings = new String[items.size()];
		for (int i = 0; i < strings.length; i++) {
			strings[i] = app.getString(items.get(i));
		}
		final int[] holder = new int[]{0};
		final Runnable run = new Runnable() {
			public void run() {
				int which = holder[0];
				int item = items.get(which);
				if (item == R.string.save_current_track) {
					saveCurrentTrack(null, activity);
				} else if (item == R.string.gpx_monitoring_start) {
					if (!OsmAndLocationProvider.isLocationPermissionAvailable(activity)) {
						if (mapActivity != null) {
							ActivityCompat.requestPermissions(mapActivity,
									new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
									REQUEST_LOCATION_PERMISSION_FOR_GPX_RECORDING);
						} else {
							app.showToastMessage(R.string.no_location_permission);
						}
					} else if (app.getLocationProvider().checkGPSEnabled(activity)) {
						startGPXMonitoring(activity, showTrackSelection);
					}
				} else if (item == R.string.clear_recorded_data) {
					if (AndroidUtils.isActivityNotDestroyed(activity)) {
						AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
						builder.setTitle(R.string.clear_recorded_data);
						builder.setMessage(R.string.are_you_sure);
						builder.setNegativeButton(R.string.shared_string_cancel, null).setPositiveButton(
								R.string.shared_string_ok, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										app.getSavingTrackHelper().clearRecordedData(true);
										app.getNotificationHelper().refreshNotifications();
									}
								});
						builder.show();
					}
				} else if (item == R.string.gpx_monitoring_stop) {
					stopRecording();
				} else if (item == R.string.gpx_start_new_segment) {
					app.getSavingTrackHelper().startNewSegment();
				} else if (item == R.string.live_monitoring_stop) {
					settings.LIVE_MONITORING.set(false);
				} else if (item == R.string.live_monitoring_start) {
					final ValueHolder<Integer> vs = new ValueHolder<Integer>();
					vs.value = settings.LIVE_MONITORING_INTERVAL.get();
					TripRecordingBottomSheetFragment.showInstance(mapActivity.getSupportFragmentManager());
				}
				if (monitoringControl != null) {
					monitoringControl.updateInfo(null);
				}
			}
		};
		if (strings.length == 1) {
			run.run();
		} else {
			bld.setItems(strings, new OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					holder[0] = which;
					run.run();
				}
			});
			bld.show();
		}
	}

	public void saveCurrentTrack() {
		saveCurrentTrack(null, null);
	}

	public void saveCurrentTrack(@Nullable final Runnable onComplete) {
		saveCurrentTrack(onComplete, null);
	}

	public void saveCurrentTrack(@Nullable final Runnable onComplete, @Nullable Activity activity) {
		stopRecording();

		final WeakReference<Activity> activityRef = activity != null ? new WeakReference<>(activity) : null;

		app.getTaskManager().runInBackground(new OsmAndTaskRunnable<Void, Void, SaveGpxResult>() {

			@Override
			protected void onPreExecute() {
				isSaving = true;
				updateControl();
			}

			@Override
			protected SaveGpxResult doInBackground(Void... params) {
				try {
					SavingTrackHelper helper = app.getSavingTrackHelper();
					SaveGpxResult result = helper.saveDataToGpx(app.getAppCustomization().getTracksDir());
					helper.close();
					return result;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}

			@Override
			protected void onPostExecute(SaveGpxResult result) {
				isSaving = false;
				app.getNotificationHelper().refreshNotifications();
				updateControl();
				if (activityRef != null && !Algorithms.isEmpty(result.getFilenames())) {
					final Activity a = activityRef.get();
					if (a instanceof FragmentActivity && !a.isFinishing()) {
						SaveGPXBottomSheetFragment.showInstance(((FragmentActivity) a).getSupportFragmentManager(), result.getFilenames());
					}
				}

				if (onComplete != null) {
					onComplete.run();
				}
			}
		}, (Void) null);
	}

	public void updateControl() {
		if (monitoringControl != null) {
			monitoringControl.updateInfo(null);
		}
	}

	public void stopRecording() {
		settings.SAVE_GLOBAL_TRACK_TO_GPX.set(false);
		if (app.getNavigationService() != null) {
			app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_GPX);
		}
	}

	public void startGPXMonitoring(final Activity map) {
		startGPXMonitoring(map, true);
	}

	public void startGPXMonitoring(final Activity map, final boolean showTrackSelection) {
		final ValueHolder<Integer> vs = new ValueHolder<Integer>();
		final ValueHolder<Boolean> choice = new ValueHolder<Boolean>();
		vs.value = settings.SAVE_GLOBAL_TRACK_INTERVAL.get();
		choice.value = settings.SAVE_GLOBAL_TRACK_REMEMBER.get();
		final Runnable runnable = new Runnable() {
			public void run() {
				app.getSavingTrackHelper().startNewSegment();
				settings.SAVE_GLOBAL_TRACK_INTERVAL.set(vs.value);
				settings.SAVE_GLOBAL_TRACK_TO_GPX.set(true);
				settings.SAVE_GLOBAL_TRACK_REMEMBER.set(choice.value);
				app.startNavigationService(NavigationService.USED_BY_GPX);
			}
		};
		if (choice.value || map == null) {
			runnable.run();
		} else {
			TripRecordingBottomSheetFragment.showInstance(mapActivity.getSupportFragmentManager());
		}
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashTrackFragment.FRAGMENT_DATA;
	}

}
