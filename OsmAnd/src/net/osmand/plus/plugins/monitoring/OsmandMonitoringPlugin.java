package net.osmand.plus.plugins.monitoring;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_OSMAND_MONITORING;
import static net.osmand.plus.views.mapwidgets.WidgetParams.TRIP_RECORDING_DISTANCE;
import static net.osmand.plus.views.mapwidgets.WidgetParams.TRIP_RECORDING_DOWNHILL;
import static net.osmand.plus.views.mapwidgets.WidgetParams.TRIP_RECORDING_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetParams.TRIP_RECORDING_UPHILL;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.ValueHolder;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmAndTaskManager.OsmAndTaskRunnable;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.helpers.GpxUiHelper;
import net.osmand.plus.helpers.GpxUiHelper.GPXInfo;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingDistanceWidget;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidget.TripRecordingDownhillWidget;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidget.TripRecordingUphillWidget;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingTimeWidget;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.fragments.BaseSettingsFragment.SettingsScreenType;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.track.helpers.SavingTrackHelper;
import net.osmand.plus.track.helpers.SavingTrackHelper.SaveGpxResult;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.layers.MapInfoLayer;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetParams;
import net.osmand.plus.views.mapwidgets.widgets.MapWidget;
import net.osmand.plus.views.mapwidgets.widgets.TextInfoWidget;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OsmandMonitoringPlugin extends OsmandPlugin {

	private static final Log LOG = PlatformUtil.getLog(OsmandMonitoringPlugin.class);
	public static final String OSMAND_SAVE_SERVICE_ACTION = "OSMAND_SAVE_SERVICE_ACTION";
	public static final int REQUEST_LOCATION_PERMISSION_FOR_GPX_RECORDING = 208;

	private final OsmandSettings settings;
	private final LiveMonitoringHelper liveMonitoringHelper;

	private MapActivity mapActivity;
	private boolean isSaving;
	private boolean showDialogWhenActivityResumed;

	private TextInfoWidget distanceWidget;
	private TextInfoWidget timeWidget;
	private TextInfoWidget uphillWidget;
	private TextInfoWidget downhillWidget;

	public OsmandMonitoringPlugin(OsmandApplication app) {
		super(app);
		liveMonitoringHelper = new LiveMonitoringHelper(app);
		registerWidgetsVisibility();
		settings = app.getSettings();
		pluginPreferences.add(settings.SAVE_TRACK_TO_GPX);
		pluginPreferences.add(settings.SAVE_TRACK_INTERVAL);
		pluginPreferences.add(settings.SAVE_TRACK_MIN_DISTANCE);
		pluginPreferences.add(settings.SAVE_TRACK_PRECISION);
		pluginPreferences.add(settings.AUTO_SPLIT_RECORDING);
		pluginPreferences.add(settings.DISABLE_RECORDING_ONCE_APP_KILLED);
		pluginPreferences.add(settings.SAVE_HEADING_TO_GPX);
		pluginPreferences.add(settings.SHOW_TRIP_REC_NOTIFICATION);
		pluginPreferences.add(settings.SHOW_TRIP_REC_START_DIALOG);
		pluginPreferences.add(settings.TRACK_STORAGE_DIRECTORY);
		pluginPreferences.add(settings.LIVE_MONITORING);
		pluginPreferences.add(settings.LIVE_MONITORING_URL);
		pluginPreferences.add(settings.LIVE_MONITORING_INTERVAL);
		pluginPreferences.add(settings.LIVE_MONITORING_MAX_INTERVAL_TO_SEND);
	}

	private void registerWidgetsVisibility() {
		for (WidgetParams widget : WidgetGroup.TRIP_RECORDING.getWidgets()) {
			ApplicationMode[] appModes = widget == TRIP_RECORDING_DISTANCE ? null : new ApplicationMode[] {};
			ApplicationMode.regWidgetVisibility(widget.id, appModes);
		}
	}

	@Override
	public void disable(OsmandApplication app) {
		super.disable(app);
		app.getNotificationHelper().refreshNotifications();
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		app.getLauncherShortcutsHelper().updateLauncherShortcuts();
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
		return PLUGIN_OSMAND_MONITORING;
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
	public void registerLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		if (mapActivity != null) {
			registerWidgets(mapActivity);
		}
	}

	@Override
	public void updateLayers(@NonNull Context context, @Nullable MapActivity mapActivity) {
		if (mapActivity != null) {
			if (isActive()) {
				registerWidgets(mapActivity);
			} else {
				unregisterWidgets(mapActivity);
			}
		}
	}

	private void registerWidgets(@NonNull MapActivity mapActivity) {
		MapInfoLayer layer = mapActivity.getMapLayers().getMapInfoLayer();
		if (distanceWidget == null) {
			distanceWidget = (TextInfoWidget) createMapWidgetForParams(mapActivity, TRIP_RECORDING_DISTANCE);
			layer.registerWidget(TRIP_RECORDING_DISTANCE, distanceWidget);
		}
		if (timeWidget == null) {
			timeWidget = (TextInfoWidget) createMapWidgetForParams(mapActivity, TRIP_RECORDING_TIME);
			layer.registerWidget(TRIP_RECORDING_TIME, timeWidget);
		}
		if (uphillWidget == null) {
			uphillWidget = (TextInfoWidget) createMapWidgetForParams(mapActivity, TRIP_RECORDING_UPHILL);
			layer.registerWidget(TRIP_RECORDING_UPHILL, uphillWidget);
		}
		if (downhillWidget == null) {
			downhillWidget = (TextInfoWidget) createMapWidgetForParams(mapActivity, TRIP_RECORDING_DOWNHILL);
			layer.registerWidget(TRIP_RECORDING_DOWNHILL, downhillWidget);
		}
		layer.recreateControls();
	}


	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetParams params) {
		switch (params) {
			case TRIP_RECORDING_DISTANCE:
				return new TripRecordingDistanceWidget(mapActivity);
			case TRIP_RECORDING_TIME:
				return new TripRecordingTimeWidget(mapActivity);
			case TRIP_RECORDING_UPHILL:
				return new TripRecordingUphillWidget(mapActivity);
			case TRIP_RECORDING_DOWNHILL:
				return new TripRecordingDownhillWidget(mapActivity);
		}
		return null;
	}

	private void unregisterWidgets(@NonNull MapActivity mapActivity) {
		MapInfoLayer mapInfoLayer = mapActivity.getMapLayers().getMapInfoLayer();
		if (distanceWidget != null) {
			mapInfoLayer.removeSideWidget(distanceWidget);
			distanceWidget = null;
		}
		if (timeWidget != null) {
			mapInfoLayer.removeSideWidget(timeWidget);
			timeWidget = null;
		}
		if (uphillWidget != null) {
			mapInfoLayer.removeSideWidget(uphillWidget);
			uphillWidget = null;
		}
		if (downhillWidget != null) {
			mapInfoLayer.removeSideWidget(downhillWidget);
			downhillWidget = null;
		}
		mapInfoLayer.recreateControls();
	}

	public static final int[] SECONDS = new int[] {0, 1, 2, 3, 5, 10, 15, 20, 30, 60, 90};
	public static final int[] MINUTES = new int[] {2, 3, 5};
	public static final int[] MAX_INTERVAL_TO_SEND_MINUTES = new int[] {1, 2, 5, 10, 15, 20, 30, 60, 90, 2 * 60, 3 * 60, 4 * 60, 6 * 60, 12 * 60, 24 * 60};

	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.MONITORING_SETTINGS;
	}

	@Override
	public String getPrefsDescription() {
		return app.getString(R.string.monitoring_prefs_descr);
	}

	@Override
	public void mapActivityResume(MapActivity activity) {
		this.mapActivity = activity;
		if (showDialogWhenActivityResumed) {
			showDialogWhenActivityResumed = false;
			showTripRecordingDialog(mapActivity);
		}
	}

	@Override
	public void mapActivityPause(MapActivity activity) {
		this.distanceWidget = null;
		this.timeWidget = null;
		this.uphillWidget = null;
		this.downhillWidget = null;
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

	public SelectedGpxFile getCurrentTrack() {
		return app.getSavingTrackHelper().getCurrentTrack();
	}

	public boolean wasTrackMonitored() {
		return settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
	}

	public boolean hasDataToSave() {
		return app.getSavingTrackHelper().hasDataToSave();
	}

	public void showTripRecordingDialog(@NonNull Activity activity) {
		FragmentManager fragmentManager = ((FragmentActivity) activity).getSupportFragmentManager();
		if (hasDataToSave() || wasTrackMonitored()) {
			TripRecordingBottomSheet.showInstance(fragmentManager);
		} else {
			TripRecordingStartingBottomSheet.showTripRecordingDialog(fragmentManager, app);
		}
	}

	public void saveCurrentTrack() {
		saveCurrentTrack(null, null, true, false);
	}

	public void saveCurrentTrack(@Nullable final Runnable onComplete) {
		saveCurrentTrack(onComplete, null, true, false);
	}

	public void saveCurrentTrack(@Nullable final Runnable onComplete, @Nullable FragmentActivity activity) {
		saveCurrentTrack(onComplete, activity, true, false);
	}

	public void saveCurrentTrack(@Nullable final Runnable onComplete, @Nullable FragmentActivity activity,
	                             final boolean stopRecording, final boolean openTrack) {
		if (stopRecording) {
			stopRecording();
		}
		final WeakReference<FragmentActivity> activityRef = activity != null ? new WeakReference<>(activity) : null;

		app.getTaskManager().runInBackground(new OsmAndTaskRunnable<Void, Void, SaveGpxResult>() {

			@Override
			protected void onPreExecute() {
				isSaving = true;
				updateWidgets();
			}

			@Override
			protected SaveGpxResult doInBackground(Void... params) {
				try {
					SavingTrackHelper helper = app.getSavingTrackHelper();
					SaveGpxResult result = helper.saveDataToGpx(app.getAppCustomization().getTracksDir());
					helper.close();
					return result;
				} catch (Exception e) {
					LOG.error(e.getMessage(), e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(SaveGpxResult result) {
				isSaving = false;
				app.getNotificationHelper().refreshNotifications();
				updateWidgets();

				Map<String, GPXFile> gpxFilesByName = result.getGpxFilesByName();
				GPXFile gpxFile = null;
				File file = null;
				if (!Algorithms.isEmpty(gpxFilesByName)) {
					String gpxFileName = gpxFilesByName.keySet().iterator().next();
					gpxFile = gpxFilesByName.get(gpxFileName);
					file = getSavedGpxFile(gpxFileName + GPX_FILE_EXT);
				}

				boolean fileExists = file != null && file.exists();
				boolean gpxFileNonEmpty = gpxFile != null && (gpxFile.hasTrkPt() || gpxFile.hasWptPt());
				if (fileExists && gpxFileNonEmpty) {
					if (openTrack) {
						TrackMenuFragment.openTrack(mapActivity, file, null);
					} else {
						FragmentActivity fragmentActivity = activityRef != null ? activityRef.get() : null;
						if (AndroidUtils.isActivityNotDestroyed(fragmentActivity)) {
							FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
							SaveGPXBottomSheet.showInstance(fragmentManager, file.getAbsolutePath());
						}
					}
				}

				if (onComplete != null) {
					onComplete.run();
				}
			}
		}, (Void) null);
	}

	@Nullable
	private File getSavedGpxFile(@NonNull String relativeFileNameWithExt) {
		File recDir = app.getAppCustomization().getTracksDir();
		List<GPXInfo> gpxInfoList = new ArrayList<>();
		GpxUiHelper.readGpxDirectory(recDir, gpxInfoList, "", false);
		for (GPXInfo gpxInfo : gpxInfoList) {
			if (relativeFileNameWithExt.equals(gpxInfo.getFileName())) {
				return new File(recDir, relativeFileNameWithExt);
			}
		}

		return null;
	}

	public void updateWidgets() {
		if (distanceWidget != null) {
			distanceWidget.updateInfo(null);
		}
		if (timeWidget != null) {
			timeWidget.updateInfo(null);
		}
		if (uphillWidget != null) {
			uphillWidget.updateInfo(null);
		}
		if (downhillWidget != null) {
			downhillWidget.updateInfo(null);
		}
	}

	public void stopRecording() {
		settings.SAVE_GLOBAL_TRACK_TO_GPX.set(false);
		if (app.getNavigationService() != null) {
			app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_GPX);
		}
	}

	public boolean isSaving() {
		return isSaving;
	}

	public boolean isLiveMonitoringEnabled() {
		return liveMonitoringHelper.isLiveMonitoringEnabled();
	}

	public void startGPXMonitoring(final Activity map) {
		startGPXMonitoring(map, true);
	}

	public void startGPXMonitoring(final Activity map, final boolean showTrackSelection) {
		final ValueHolder<Integer> vs = new ValueHolder<>();
		final ValueHolder<Boolean> choice = new ValueHolder<>();
		vs.value = settings.SAVE_GLOBAL_TRACK_INTERVAL.get();
		choice.value = settings.SAVE_GLOBAL_TRACK_REMEMBER.get();
		final Runnable runnable = () -> {
			app.getSavingTrackHelper().startNewSegment();
			settings.SAVE_GLOBAL_TRACK_INTERVAL.set(vs.value);
			settings.SAVE_GLOBAL_TRACK_TO_GPX.set(true);
			settings.SAVE_GLOBAL_TRACK_REMEMBER.set(choice.value);
			app.startNavigationService(NavigationService.USED_BY_GPX);
		};
		if (choice.value || map == null) {
			runnable.run();
		} else if (map instanceof FragmentActivity) {
			FragmentActivity activity = (FragmentActivity) map;
			TripRecordingStartingBottomSheet.showTripRecordingDialog(activity.getSupportFragmentManager(), app);
		}
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashTrackFragment.FRAGMENT_DATA;
	}
}