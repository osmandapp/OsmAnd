package net.osmand.plus.plugins.monitoring;

import static net.osmand.IndexConstants.GPX_FILE_EXT;
import static net.osmand.aidlapi.OsmAndCustomizationConstants.PLUGIN_OSMAND_MONITORING;
import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_DISTANCE;
import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_DOWNHILL;
import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_TIME;
import static net.osmand.plus.views.mapwidgets.WidgetType.TRIP_RECORDING_UPHILL;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.Location;
import net.osmand.PlatformUtil;
import net.osmand.data.ValueHolder;
import net.osmand.plus.plugins.monitoring.actions.FinishTripRecordingAction;
import net.osmand.plus.plugins.monitoring.actions.SaveRecordedTripAndContinueAction;
import net.osmand.plus.plugins.monitoring.actions.StartNewTripSegmentAction;
import net.osmand.plus.plugins.monitoring.actions.TripRecordingAction;
import net.osmand.plus.quickaction.QuickActionType;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmAndTaskManager.OsmAndTaskRunnable;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.dashboard.tools.DashFragmentData;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingDistanceWidget;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidget.TripRecordingDownhillWidget;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidget.TripRecordingUphillWidget;
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingTimeWidget;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.WidgetsAvailabilityHelper;
import net.osmand.plus.settings.controllers.BatteryOptimizationController;
import net.osmand.plus.settings.fragments.SettingsScreenType;
import net.osmand.plus.track.data.GPXInfo;
import net.osmand.plus.track.fragments.TrackMenuFragment;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.mapwidgets.MapWidgetInfo;
import net.osmand.plus.views.mapwidgets.WidgetGroup;
import net.osmand.plus.views.mapwidgets.WidgetInfoCreator;
import net.osmand.plus.views.mapwidgets.WidgetType;
import net.osmand.plus.views.mapwidgets.WidgetsPanel;
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
		pluginPreferences.add(settings.SHOW_TRIP_REC_NOTIFICATION);
		pluginPreferences.add(settings.SHOW_TRIP_REC_START_DIALOG);
		pluginPreferences.add(settings.TRACK_STORAGE_DIRECTORY);
		pluginPreferences.add(settings.LIVE_MONITORING);
		pluginPreferences.add(settings.LIVE_MONITORING_URL);
		pluginPreferences.add(settings.LIVE_MONITORING_INTERVAL);
		pluginPreferences.add(settings.LIVE_MONITORING_MAX_INTERVAL_TO_SEND);
	}

	private void registerWidgetsVisibility() {
		for (WidgetType widget : WidgetGroup.TRIP_RECORDING.getWidgets()) {
			ApplicationMode[] appModes = widget == TRIP_RECORDING_DISTANCE ? null : new ApplicationMode[] {};
			WidgetsAvailabilityHelper.regWidgetVisibility(widget, appModes);
		}
	}

	@Override
	public void disable(@NonNull OsmandApplication app) {
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
	public CharSequence getDescription(boolean linksEnabled) {
		String docsUrl = app.getString(R.string.docs_plugin_trip_recording);
		String description = app.getString(R.string.record_plugin_description, docsUrl);
		return linksEnabled ? UiUtilities.createUrlSpannable(app, description, docsUrl) : description;
	}

	@Override
	public String getName() {
		return app.getString(R.string.record_plugin_name);
	}


	public static final int[] SECONDS = {0, 1, 2, 3, 5, 10, 15, 20, 30, 60, 90};
	public static final int[] MINUTES = {2, 3, 5};
	public static final int[] MAX_INTERVAL_TO_SEND_MINUTES = {1, 2, 5, 10, 15, 20, 30, 60, 90, 2 * 60, 3 * 60, 4 * 60, 6 * 60, 12 * 60, 24 * 60};

	@Nullable
	@Override
	public SettingsScreenType getSettingsScreenType() {
		return SettingsScreenType.MONITORING_SETTINGS;
	}

	@Override
	public String getPrefsDescription() {
		return app.getString(R.string.monitoring_prefs_descr);
	}

	@Override
	public void mapActivityResume(@NonNull MapActivity activity) {
		this.mapActivity = activity;
		if (showDialogWhenActivityResumed) {
			showDialogWhenActivityResumed = false;
			askShowTripRecordingDialog(mapActivity);
		}
	}

	@Override
	public void mapActivityPause(@NonNull MapActivity activity) {
		this.distanceWidget = null;
		this.timeWidget = null;
		this.uphillWidget = null;
		this.downhillWidget = null;
		this.mapActivity = null;
	}

	@Override
	public void createWidgets(@NonNull MapActivity mapActivity, @NonNull List<MapWidgetInfo> widgetsInfos, @NonNull ApplicationMode appMode) {
		WidgetInfoCreator creator = new WidgetInfoCreator(app, appMode);

		MapWidget distanceWidget = createMapWidgetForParams(mapActivity, TRIP_RECORDING_DISTANCE);
		widgetsInfos.add(creator.createWidgetInfo(distanceWidget));

		MapWidget timeWidget = createMapWidgetForParams(mapActivity, TRIP_RECORDING_TIME);
		widgetsInfos.add(creator.createWidgetInfo(timeWidget));

		MapWidget uphillWidget = createMapWidgetForParams(mapActivity, TRIP_RECORDING_UPHILL);
		widgetsInfos.add(creator.createWidgetInfo(uphillWidget));

		MapWidget downhillWidget = createMapWidgetForParams(mapActivity, TRIP_RECORDING_DOWNHILL);
		widgetsInfos.add(creator.createWidgetInfo(downhillWidget));
	}

	@Nullable
	@Override
	protected MapWidget createMapWidgetForParams(@NonNull MapActivity mapActivity, @NonNull WidgetType widgetType, @Nullable String customId, @Nullable WidgetsPanel widgetsPanel) {
		switch (widgetType) {
			case TRIP_RECORDING_DISTANCE:
				return new TripRecordingDistanceWidget(mapActivity, customId, widgetsPanel);
			case TRIP_RECORDING_TIME:
				return new TripRecordingTimeWidget(mapActivity, customId, widgetsPanel);
			case TRIP_RECORDING_UPHILL:
				return new TripRecordingUphillWidget(mapActivity, customId, widgetsPanel);
			case TRIP_RECORDING_DOWNHILL:
				return new TripRecordingDownhillWidget(mapActivity, customId, widgetsPanel);
		}
		return null;
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

	public void askShowTripRecordingDialog(@NonNull FragmentActivity activity) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (hasDataToSave() || isRecordingTrack()) {
			TripRecordingBottomSheet.showInstance(fragmentManager);
		} else {
			askStartRecording(activity);
		}
	}

	public void askStartRecording(@NonNull FragmentActivity activity) {
		BatteryOptimizationController.askShowDialog(activity, true, this::askStartRecordingStep2);
	}

	private void askStartRecordingStep2(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (!manager.isStateSaved()) {
			if (settings.SHOW_TRIP_REC_START_DIALOG.get()) {
				TripRecordingStartingBottomSheet.showInstance(manager);
			} else {
				startRecording(activity);
			}
		}
	}

	public void startRecording(@Nullable FragmentActivity activity) {
		app.getSavingTrackHelper().startNewSegment();
		setRecordingTrack(true);
		app.startNavigationService(NavigationService.USED_BY_GPX);

		if (activity != null) {
			AndroidUtils.requestNotificationPermissionIfNeeded(activity);
		}
	}

	public boolean finishRecording() {
		if (mapActivity != null && hasDataToSave()) {
			saveCurrentTrack(null, mapActivity);
			app.getNotificationHelper().refreshNotifications();
			return true;
		}
		return false;
	}

	public void saveCurrentTrack() {
		saveCurrentTrack(null, null, true, false);
	}

	public void saveCurrentTrack(@Nullable Runnable onComplete) {
		saveCurrentTrack(onComplete, null, true, false);
	}

	public void saveCurrentTrack(@Nullable Runnable onComplete, @Nullable FragmentActivity activity) {
		saveCurrentTrack(onComplete, activity, true, false);
	}

	public void saveCurrentTrack(@Nullable Runnable onComplete, @Nullable FragmentActivity activity,
	                             boolean stopRecording, boolean openTrack) {
		if (stopRecording) {
			stopRecording();
		}
		WeakReference<FragmentActivity> activityRef = activity != null ? new WeakReference<>(activity) : null;

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

				FragmentActivity fragmentActivity = activityRef != null ? activityRef.get() : mapActivity;
				if (result != null && AndroidUtils.isActivityNotDestroyed(fragmentActivity)) {
					Map<String, GpxFile> gpxFilesByName = result.getGpxFilesByName();
					GpxFile gpxFile = null;
					File file = null;
					if (!Algorithms.isEmpty(gpxFilesByName)) {
						String name = gpxFilesByName.keySet().iterator().next();
						gpxFile = gpxFilesByName.get(name);
						file = new File(gpxFile.getPath());
					}
					boolean fileExists = file != null && file.exists();
					boolean gpxFileNonEmpty = gpxFile != null && (gpxFile.hasTrkPt() || gpxFile.hasWptPt());
					if (fileExists && gpxFileNonEmpty) {
						if (openTrack) {
							TrackMenuFragment.openTrack(fragmentActivity, file, null);
						} else {
							boolean showOnMap = app.getSelectedGpxHelper().getSelectedCurrentRecordingTrack() != null;
							if (showOnMap) {
								app.getSelectedGpxHelper().setGpxFileToDisplay(gpxFile);
							}
							FragmentManager fragmentManager = fragmentActivity.getSupportFragmentManager();
							SaveGPXBottomSheet.showInstance(fragmentManager, file);
						}
					}
				}
				if (onComplete != null) {
					onComplete.run();
				}
			}
		}, (Void) null);
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

	public void pauseOrResumeRecording() {
		if (isRecordingTrack()) {
			setRecordingTrack(false);
			NavigationService navigationService = app.getNavigationService();
			if (navigationService != null) {
				navigationService.stopIfNeeded(app, NavigationService.USED_BY_GPX);
			}
		} else {
			setRecordingTrack(true);
			app.startNavigationService(NavigationService.USED_BY_GPX);
			if (mapActivity != null) {
				AndroidUtils.requestNotificationPermissionIfNeeded(mapActivity);
			}
		}
	}

	public void stopRecording() {
		stopRecording(false);
	}

	public void stopRecording(boolean clearData) {
		setRecordingTrack(false);
		app.getSavingTrackHelper().onStopRecording(clearData);
		if (app.getNavigationService() != null) {
			app.getNavigationService().stopIfNeeded(app, NavigationService.USED_BY_GPX);
		}
	}

	public boolean isSaving() {
		return isSaving;
	}

	public void setRecordingTrack(boolean recording) {
		settings.SAVE_GLOBAL_TRACK_TO_GPX.set(recording);
	}

	public boolean isRecordingTrack() {
		return settings.SAVE_GLOBAL_TRACK_TO_GPX.get();
	}

	public boolean hasDataToSave() {
		return app.getSavingTrackHelper().hasDataToSave();
	}

	public boolean isLiveMonitoringEnabled() {
		return liveMonitoringHelper.isLiveMonitoringEnabled();
	}

	public void startGPXMonitoring(@Nullable FragmentActivity activity) {
		ValueHolder<Integer> vs = new ValueHolder<>();
		ValueHolder<Boolean> choice = new ValueHolder<>();

		vs.value = settings.SAVE_GLOBAL_TRACK_INTERVAL.get();
		choice.value = settings.SAVE_GLOBAL_TRACK_REMEMBER.get();

		if (choice.value || activity == null) {
			Runnable runnable = () -> {
				app.getSavingTrackHelper().startNewSegment();
				setRecordingTrack(true);
				settings.SAVE_GLOBAL_TRACK_INTERVAL.set(vs.value);
				settings.SAVE_GLOBAL_TRACK_REMEMBER.set(choice.value);

				if (activity != null) {
					AndroidUtils.requestNotificationPermissionIfNeeded(activity);
				}
				app.startNavigationService(NavigationService.USED_BY_GPX);
			};
			runnable.run();
		} else {
			askStartRecording(activity);
		}
	}

	@Override
	public DashFragmentData getCardFragment() {
		return DashTrackFragment.FRAGMENT_DATA;
	}

	@Override
	protected List<QuickActionType> getQuickActionTypes() {
		List<QuickActionType> quickActionTypes = new ArrayList<>();
		quickActionTypes.add(TripRecordingAction.TYPE);
		quickActionTypes.add(StartNewTripSegmentAction.TYPE);
		quickActionTypes.add(SaveRecordedTripAndContinueAction.TYPE);
		quickActionTypes.add(FinishTripRecordingAction.TYPE);
		return quickActionTypes;
	}
}