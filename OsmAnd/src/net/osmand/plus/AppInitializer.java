package net.osmand.plus;

import static net.osmand.IndexConstants.SETTINGS_DIR;
import static net.osmand.plus.AppVersionUpgradeOnInit.LAST_APP_VERSION;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getPendingIntent;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceForLocalIndex;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLastSuccessfulUpdateCheck;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceTimeOfDayToUpdate;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceUpdateFrequency;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.runLiveUpdate;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.setAlarmForPendingIntent;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.gpx.GPXUtilities;
import net.osmand.map.OsmandRegions;
import net.osmand.map.OsmandRegions.RegionTranslation;
import net.osmand.map.WorldRegion;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.download.local.LocalIndexHelper;
import net.osmand.plus.download.local.LocalItem;
import net.osmand.plus.feedback.AnalyticsHelper;
import net.osmand.plus.feedback.FeedbackHelper;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.helpers.LauncherShortcutsHelper;
import net.osmand.plus.helpers.LockHelper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.inapp.InAppPurchaseHelperImpl;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.KeyEventHelper;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.TimeOfDay;
import net.osmand.plus.liveupdates.LiveUpdatesHelper.UpdateFrequency;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.myplaces.tracks.filters.SmartFolderHelper;
import net.osmand.plus.notifications.NotificationHelper;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.monitoring.LiveMonitoringHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper;
import net.osmand.plus.plugins.rastermaps.DownloadTilesHelper;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.render.TravelRendererHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routing.AvoidRoadsHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.FileSettingsHelper;
import net.osmand.plus.track.helpers.GpsFilterHelper;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDisplayHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.mapwidgets.utils.AverageGlideComputer;
import net.osmand.plus.views.mapwidgets.utils.AverageSpeedComputer;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.voice.CommandPlayerException;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.plus.wikivoyage.data.TravelObfHelper;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;
import net.osmand.util.CollectionUtils;
import net.osmand.util.OpeningHoursParser;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import btools.routingapp.IBRouterService;

public class AppInitializer implements IProgress {

	private static final String EXCEPTION_FILE_SIZE = "EXCEPTION_FS";
	private static final Log LOG = PlatformUtil.getLog(AppInitializer.class);
	private static final int MAX_OPENGL_FAILURES = 3;
	private static final int MAX_OPENGL_DISABLE = 6;

	private final OsmandApplication app;
	private final AppVersionUpgradeOnInit appVersionUpgrade;

	private final List<String> warnings = new ArrayList<>();
	private List<AppInitializeListener> listeners = new ArrayList<>();

	private boolean initSettings;
	private boolean activityChangesShowed;
	private long startTime;
	private long startBgTime;
	private boolean appInitializing = true;
	private String taskName;
	private SharedPreferences startPrefs;

	public enum InitEvents {
		FAVORITES_INITIALIZED, NATIVE_INITIALIZED, NATIVE_OPEN_GL_INITIALIZED, TASK_CHANGED,
		MAPS_INITIALIZED, POI_TYPES_INITIALIZED, POI_FILTERS_INITIALIZED, ASSETS_COPIED,
		INIT_RENDERERS, RESTORE_BACKUPS, INDEX_REGION_BOUNDARIES, SAVE_GPX_TRACKS, LOAD_GPX_TRACKS,
		ROUTING_CONFIG_INITIALIZED
	}

	static {
		//Set old time format of GPX for Android 6.0 and lower
		GPXUtilities.GPX_TIME_OLD_FORMAT = Build.VERSION.SDK_INT <= Build.VERSION_CODES.M;
	}

	public interface AppInitializeListener {

		@WorkerThread
		default void onStart(@NonNull AppInitializer init) {

		}

		@UiThread
		default void onProgress(@NonNull AppInitializer init, @NonNull InitEvents event) {

		}

		@UiThread
		default void onFinish(@NonNull AppInitializer init) {

		}
	}

	public interface LoadRoutingFilesCallback {
		void onRoutingFilesLoaded();
	}

	public interface InitOpenglListener {
		void onOpenglInitialized();
	}

	public AppInitializer(@NonNull OsmandApplication app) {
		this.app = app;
		appVersionUpgrade = new AppVersionUpgradeOnInit(app);
	}

	public List<String> getWarnings() {
		return warnings;
	}

	public boolean isAppInitializing() {
		return appInitializing;
	}

	@SuppressLint({"CommitPrefEdits", "ApplySharedPref"})
	public void initVariables() {
		if (initSettings) {
			return;
		}
		ApplicationMode.onApplicationStart(app);
		startPrefs = app.getSharedPreferences(
				getLocalClassName(app.getAppCustomization().getMapActivity().getName()),
				Context.MODE_PRIVATE);
		appVersionUpgrade.upgradeVersion(startPrefs, LAST_APP_VERSION);
		initSettings = true;
	}

	public int getNumberOfStarts() {
		return appVersionUpgrade.getNumberOfStarts(startPrefs);
	}

	public long getFirstInstalledDays() {
		return appVersionUpgrade.getFirstInstalledDays(startPrefs);
	}

	public long getFirstInstalledTime() {
		return appVersionUpgrade.getFirstInstalledTime(startPrefs);
	}

	public long getUpdateVersionTime() {
		return appVersionUpgrade.getUpdateVersionTime(startPrefs);
	}

	public void resetFirstTimeRun() {
		appVersionUpgrade.resetFirstTimeRun(startPrefs);
	}

	public boolean isFirstTime() {
		initVariables();
		return appVersionUpgrade.isFirstTime();
	}

	public boolean isAppVersionChanged() {
		return appVersionUpgrade.isAppVersionChanged();
	}

	public int getPrevAppVersion() {
		return appVersionUpgrade.getPrevAppVersion();
	}

	public boolean checkAppVersionChanged() {
		initVariables();
		boolean showRecentChangesDialog = !isFirstTime() && isAppVersionChanged();
//		showRecentChangesDialog = true;
		if (showRecentChangesDialog && !activityChangesShowed) {
			activityChangesShowed = true;
			return true;
		}
		checkMapUpdates();

		return false;
	}

	private void checkMapUpdates() {
		long diff = System.currentTimeMillis() - app.getSettings().LAST_CHECKED_UPDATES.get();
		if (diff >= 2 * 24 * 60 * 60L && new Random().nextInt(5) == 0 &&
				app.getSettings().isInternetConnectionAvailable()) {
			app.getDownloadThread().runReloadIndexFiles();
		} else if (Version.isDeveloperVersion(app)) {
//			app.getDownloadThread().runReloadIndexFiles();
		}
	}

	public boolean checkPreviousRunsForExceptions(Activity activity, boolean writeFileSize) {
		initVariables();
		long size = activity.getPreferences(Context.MODE_PRIVATE).getLong(EXCEPTION_FILE_SIZE, 0);
		File file = app.getAppPath(FeedbackHelper.EXCEPTION_PATH);
		if (file.exists() && file.length() > 0) {
			if (size != file.length() && !isFirstTime()) {
				if (writeFileSize) {
					activity.getPreferences(Context.MODE_PRIVATE).edit().putLong(EXCEPTION_FILE_SIZE, file.length()).commit();
				}
				return true;
			}
		} else if (size > 0) {
			activity.getPreferences(Context.MODE_PRIVATE).edit().putLong(EXCEPTION_FILE_SIZE, 0).commit();
		}
		return false;
	}

	private void indexRegionsBoundaries(List<String> warnings) {
		File file = app.getAppPath("regions.ocbf");
		try {
			if (file != null) {
				if (!file.exists()) {
					Algorithms.streamCopy(OsmandRegions.class.getResourceAsStream("regions.ocbf"),
							new FileOutputStream(file));
				}
				app.regions.prepareFile(file.getAbsolutePath());

			}
		} catch (Exception e) {
			warnings.add(e.getMessage());
			file.delete(); // recreate file
			LOG.error(e.getMessage(), e);
		}
	}

	private void initPoiTypes() {
		app.poiTypes.setForbiddenTypes(app.settings.getForbiddenTypes());
		if (app.getAppPath(SETTINGS_DIR + "poi_types.xml").exists()) {
			app.poiTypes.init(app.getAppPath(SETTINGS_DIR + "poi_types.xml").getAbsolutePath());
		} else {
			app.poiTypes.init();
		}
		app.poiTypes.setPoiTranslator(new MapPoiTypesTranslator(app));
	}

	public void onCreateApplication() {
		// always update application mode to default
		OsmandSettings osmandSettings = app.getSettings();
		if (osmandSettings.FOLLOW_THE_ROUTE.get()) {
			ApplicationMode savedMode = osmandSettings.readApplicationMode();
			if (!osmandSettings.APPLICATION_MODE.get().getStringKey().equals(savedMode.getStringKey())) {
				osmandSettings.setApplicationMode(savedMode);
			}
		} else {
			osmandSettings.setApplicationMode(osmandSettings.DEFAULT_APPLICATION_MODE.get());
		}
		startTime = System.currentTimeMillis();
		getLazyRoutingConfig();
		app.applyTheme(app);
		startupInit(app.reconnectToBRouter(), IBRouterService.class);
		app.importHelper = startupInit(new ImportHelper(app), ImportHelper.class);
		app.backupHelper = startupInit(new BackupHelper(app), BackupHelper.class);
		app.inAppPurchaseHelper = startupInit(new InAppPurchaseHelperImpl(app), InAppPurchaseHelperImpl.class);
		app.poiTypes = startupInit(MapPoiTypes.getDefaultNoInit(), MapPoiTypes.class);
		app.transportRoutingHelper = startupInit(new TransportRoutingHelper(app), TransportRoutingHelper.class);
		app.routingHelper = startupInit(new RoutingHelper(app), RoutingHelper.class);
		app.routingOptionsHelper = startupInit(new RoutingOptionsHelper(app), RoutingOptionsHelper.class);
		app.resourceManager = startupInit(new ResourceManager(app), ResourceManager.class);
		app.locationProvider = startupInit(new OsmAndLocationProvider(app), OsmAndLocationProvider.class);
		app.daynightHelper = startupInit(new DayNightHelper(app), DayNightHelper.class);
		app.avoidSpecificRoads = startupInit(new AvoidSpecificRoads(app), AvoidSpecificRoads.class);
		app.avoidRoadsHelper = startupInit(new AvoidRoadsHelper(app), AvoidRoadsHelper.class);
		app.gpxDisplayHelper = startupInit(new GpxDisplayHelper(app), GpxDisplayHelper.class);
		app.savingTrackHelper = startupInit(new SavingTrackHelper(app), SavingTrackHelper.class);
		app.analyticsHelper = startupInit(new AnalyticsHelper(app), AnalyticsHelper.class);
		app.feedbackHelper = startupInit(new FeedbackHelper(app), FeedbackHelper.class);
		app.notificationHelper = startupInit(new NotificationHelper(app), NotificationHelper.class);
		app.liveMonitoringHelper = startupInit(new LiveMonitoringHelper(app), LiveMonitoringHelper.class);
		app.selectedGpxHelper = startupInit(new GpxSelectionHelper(app), GpxSelectionHelper.class);
		app.gpxDbHelper = startupInit(new GpxDbHelper(app), GpxDbHelper.class);
		app.favoritesHelper = startupInit(new FavouritesHelper(app), FavouritesHelper.class);
		app.waypointHelper = startupInit(new WaypointHelper(app), WaypointHelper.class);
		app.aidlApi = startupInit(new OsmandAidlApi(app), OsmandAidlApi.class);

		app.regions = startupInit(new OsmandRegions(), OsmandRegions.class);
		updateRegionVars();

		app.poiFilters = startupInit(new PoiFiltersHelper(app), PoiFiltersHelper.class);
		app.rendererRegistry = startupInit(new RendererRegistry(app), RendererRegistry.class);
		app.geocodingLookupService = startupInit(new GeocodingLookupService(app), GeocodingLookupService.class);
		app.targetPointsHelper = startupInit(new TargetPointsHelper(app), TargetPointsHelper.class);
		app.mapMarkersDbHelper = startupInit(new MapMarkersDbHelper(app), MapMarkersDbHelper.class);
		app.mapMarkersHelper = startupInit(new MapMarkersHelper(app), MapMarkersHelper.class);
		app.searchUICore = startupInit(new QuickSearchHelper(app), QuickSearchHelper.class);
		app.mapViewTrackingUtilities = startupInit(new MapViewTrackingUtilities(app), MapViewTrackingUtilities.class);
		app.osmandMap = startupInit(new OsmandMap(app), OsmandMap.class);

		app.travelHelper = startupInit(new TravelObfHelper(app), TravelHelper.class);
		app.travelRendererHelper = startupInit(new TravelRendererHelper(app), TravelRendererHelper.class);

		app.lockHelper = startupInit(new LockHelper(app), LockHelper.class);
		app.inputDeviceHelper = startupInit(new InputDevicesHelper(app), InputDevicesHelper.class);
		app.keyEventHelper = startupInit(new KeyEventHelper(app), KeyEventHelper.class);
		app.fileSettingsHelper = startupInit(new FileSettingsHelper(app), FileSettingsHelper.class);
		app.networkSettingsHelper = startupInit(new NetworkSettingsHelper(app), NetworkSettingsHelper.class);
		app.mapButtonsHelper = startupInit(new MapButtonsHelper(app), MapButtonsHelper.class);
		app.osmOAuthHelper = startupInit(new OsmOAuthHelper(app), OsmOAuthHelper.class);
		app.onlineRoutingHelper = startupInit(new OnlineRoutingHelper(app), OnlineRoutingHelper.class);
		app.launcherShortcutsHelper = startupInit(new LauncherShortcutsHelper(app), LauncherShortcutsHelper.class);
		app.gpsFilterHelper = startupInit(new GpsFilterHelper(app), GpsFilterHelper.class);
		app.downloadTilesHelper = startupInit(new DownloadTilesHelper(app), DownloadTilesHelper.class);
		app.averageSpeedComputer = startupInit(new AverageSpeedComputer(app), AverageSpeedComputer.class);
		app.averageGlideComputer = startupInit(new AverageGlideComputer(app), AverageGlideComputer.class);
		app.weatherHelper = startupInit(new WeatherHelper(app), WeatherHelper.class);
		app.dialogManager = startupInit(new DialogManager(), DialogManager.class);
		app.smartFolderHelper = startupInit(new SmartFolderHelper(app), SmartFolderHelper.class);

		initOpeningHoursParser();
	}

	private void initOpeningHoursParser() {
		OpeningHoursParser.setAdditionalString("off", app.getString(R.string.day_off_label));
		OpeningHoursParser.setAdditionalString("is_open", app.getString(R.string.poi_dialog_opening_hours));
		OpeningHoursParser.setAdditionalString("is_open_24_7", app.getString(R.string.shared_string_is_open_24_7));
		OpeningHoursParser.setAdditionalString("will_open_at", app.getString(R.string.will_open_at));
		OpeningHoursParser.setAdditionalString("open_from", app.getString(R.string.open_from));
		OpeningHoursParser.setAdditionalString("will_close_at", app.getString(R.string.will_close_at));
		OpeningHoursParser.setAdditionalString("open_till", app.getString(R.string.open_till));
		OpeningHoursParser.setAdditionalString("will_open_tomorrow_at", app.getString(R.string.will_open_tomorrow_at));
		OpeningHoursParser.setAdditionalString("will_open_on", app.getString(R.string.will_open_on));
	}

	private void updateRegionVars() {
		app.regions.setTranslator(new RegionTranslation() {

			@Override
			public String getTranslation(String id) {
				if (WorldRegion.AFRICA_REGION_ID.equals(id)) {
					return app.getString(R.string.index_name_africa);
				} else if (WorldRegion.AUSTRALIA_AND_OCEANIA_REGION_ID.equals(id)) {
					return app.getString(R.string.index_name_oceania);
				} else if (WorldRegion.ASIA_REGION_ID.equals(id)) {
					return app.getString(R.string.index_name_asia);
				} else if (WorldRegion.CENTRAL_AMERICA_REGION_ID.equals(id)) {
					return app.getString(R.string.index_name_central_america);
				} else if (WorldRegion.EUROPE_REGION_ID.equals(id)) {
					return app.getString(R.string.index_name_europe);
				} else if (WorldRegion.RUSSIA_REGION_ID.equals(id)) {
					return app.getString(R.string.index_name_russia);
				} else if (WorldRegion.NORTH_AMERICA_REGION_ID.equals(id)) {
					return app.getString(R.string.index_name_north_america);
				} else if (WorldRegion.SOUTH_AMERICA_REGION_ID.equals(id)) {
					return app.getString(R.string.index_name_south_america);
				} else if (WorldRegion.ANTARCTICA_REGION_ID.equals(id)) {
					return app.getString(R.string.index_name_antarctica);
				}
				return null;
			}
		});
		app.regions.setLocale(app.getLanguage(), app.getLocaleHelper().getCountry());
	}


	private <T> T startupInit(T object, Class<T> class1) {
		long t = System.currentTimeMillis();
		if (t - startTime > 7) {
			System.err.println("Startup service " + class1.getName() + " took too long " + (t - startTime) + " ms");
		}
		startTime = t;
		return object;
	}

	@SuppressLint("StaticFieldLeak")
	private void getLazyRoutingConfig() {
		loadRoutingFiles(app, () -> notifyEvent(InitEvents.ROUTING_CONFIG_INITIALIZED));
	}

	public static void loadRoutingFiles(@NonNull OsmandApplication app, @Nullable LoadRoutingFilesCallback callback) {
		new AsyncTask<Void, Void, Map<String, RoutingConfiguration.Builder>>() {

			@Override
			protected Map<String, RoutingConfiguration.Builder> doInBackground(Void... voids) {
				Map<String, String> defaultAttributes = getDefaultAttributes();
				Map<String, RoutingConfiguration.Builder> customConfigs = new HashMap<>();

				File routingFolder = app.getAppPath(IndexConstants.ROUTING_PROFILES_DIR);
				if (routingFolder.isDirectory()) {
					File[] fl = routingFolder.listFiles();
					if (fl != null && fl.length > 0) {
						for (File f : fl) {
							if (f.isFile() && f.getName().endsWith(IndexConstants.ROUTING_FILE_EXT) && f.canRead()) {
								try {
									String fileName = f.getName();
									RoutingConfiguration.Builder builder = new RoutingConfiguration.Builder(defaultAttributes);
									RoutingConfiguration.parseFromInputStream(new FileInputStream(f), fileName, builder);

									customConfigs.put(fileName, builder);
								} catch (XmlPullParserException | IOException e) {
									Algorithms.removeAllFiles(f);
									LOG.error(e.getMessage(), e);
								}
							}
						}
					}
				}
				return customConfigs;
			}

			@Override
			protected void onPostExecute(Map<String, RoutingConfiguration.Builder> customConfigs) {
				if (!customConfigs.isEmpty()) {
					app.getCustomRoutingConfigs().putAll(customConfigs);
				}
				app.avoidSpecificRoads.initRouteObjects(false);
				if (callback != null) {
					callback.onRoutingFilesLoaded();
				}
			}

			private Map<String, String> getDefaultAttributes() {
				Map<String, String> defaultAttributes = new HashMap<>();
				RoutingConfiguration.Builder builder = RoutingConfiguration.getDefault();
				for (Map.Entry<String, String> entry : builder.getAttributes().entrySet()) {
					String key = entry.getKey();
					if (!"routerName".equals(key)) {
						defaultAttributes.put(key, entry.getValue());
					}
				}
				return defaultAttributes;
			}

		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}


	public synchronized void initVoiceDataInDifferentThread(@NonNull Context context,
	                                                        @NonNull ApplicationMode applicationMode,
	                                                        @NonNull String voiceProvider,
	                                                        @Nullable Runnable onFinishInitialization,
	                                                        boolean showProgress) {
		String progressTitle = app.getString(R.string.loading_data);
		String progressMessage = app.getString(R.string.voice_data_initializing);
		ProgressDialog progressDialog = showProgress && context instanceof Activity
				? ProgressDialog.show(context, progressTitle, progressMessage)
				: null;

		new Thread(() -> {
			try {
				if (app.player != null) {
					app.player.clear();
				}
				app.player = CommandPlayer.createCommandPlayer(app, applicationMode, voiceProvider);
				app.getRoutingHelper().getVoiceRouter().setPlayer(app.player);
			} catch (CommandPlayerException e) {
				app.showToastMessage(e.getError());
				LOG.error("Failed to create CommandPlayer", e);
			} finally {
				if (progressDialog != null) {
					progressDialog.dismiss();
				}
				if (onFinishInitialization != null) {
					((OsmandApplication) context.getApplicationContext()).runInUIThread(onFinishInitialization);
				}
			}
		}).start();
	}

	private void startApplicationBackground() {
		try {
			notifyStart();
			startBgTime = System.currentTimeMillis();
			app.getRendererRegistry().initRenderers();
			notifyEvent(InitEvents.INIT_RENDERERS);
			// native depends on renderers
			initOpenGl();

			// init poi types before indexes and before POI
			initPoiTypes();
			notifyEvent(InitEvents.POI_TYPES_INITIALIZED);
			app.resourceManager.reloadIndexesOnStart(this, warnings);
			app.travelHelper.initializeDataOnAppStartup();
			app.travelRendererHelper.updateVisibilityPrefs();
			// native depends on renderers
			initNativeCore();
			app.favoritesHelper.loadFavorites();
			app.gpxDbHelper.loadGpxItems();
			notifyEvent(InitEvents.FAVORITES_INITIALIZED);
			app.poiFilters.reloadAllPoiFilters();
			app.poiFilters.loadSelectedPoiFilters();
			notifyEvent(InitEvents.POI_FILTERS_INITIALIZED);
			indexRegionsBoundaries(warnings);
			notifyEvent(InitEvents.INDEX_REGION_BOUNDARIES);
			app.selectedGpxHelper.loadGPXTracks(this);
			notifyEvent(InitEvents.LOAD_GPX_TRACKS);
			saveGPXTracks();
			notifyEvent(InitEvents.SAVE_GPX_TRACKS);
			// restore backuped favorites to normal file -> this is obsolete with new favorite concept
			//restoreBackupForFavoritesFiles();
			notifyEvent(InitEvents.RESTORE_BACKUPS);
			app.mapMarkersHelper.syncAllGroups();
			app.searchUICore.initSearchUICore();

			checkLiveUpdatesAlerts();
		} catch (RuntimeException e) {
			e.printStackTrace();
			warnings.add(e.getMessage());
		} finally {
			appInitializing = false;
			notifyFinish();
			if (!Algorithms.isEmpty(warnings)) {
				String warning = AndroidUtils.formatWarnings(warnings).toString();
				if (PluginsHelper.isDevelopment()) {
					app.showToastMessage(warning);
				} else {
					LOG.error(warning);
				}
			}
		}
	}

	private void checkLiveUpdatesAlerts() {
		OsmandSettings settings = app.getSettings();
		if (!settings.IS_LIVE_UPDATES_ON.get()) {
			return;
		}
		LocalIndexHelper helper = new LocalIndexHelper(app);
		List<LocalItem> fullMaps = helper.getLocalFullMaps(null);
		AlarmManager alarmMgr = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
		for (LocalItem item : fullMaps) {
			String fileName = item.getFileName();
			if (!preferenceForLocalIndex(fileName, settings).get()) {
				continue;
			}
			int updateFrequencyOrd = preferenceUpdateFrequency(fileName, settings).get();
			UpdateFrequency updateFrequency = UpdateFrequency.values()[updateFrequencyOrd];
			long lastCheck = preferenceLastSuccessfulUpdateCheck(fileName, settings).get();

			if (System.currentTimeMillis() - lastCheck > updateFrequency.intervalMillis * 2) {
				runLiveUpdate(app, fileName, false, null);
				PendingIntent alarmIntent = getPendingIntent(app, fileName);
				int timeOfDayOrd = preferenceTimeOfDayToUpdate(fileName, settings).get();
				TimeOfDay timeOfDayToUpdate = TimeOfDay.values()[timeOfDayOrd];
				setAlarmForPendingIntent(alarmIntent, alarmMgr, updateFrequency, timeOfDayToUpdate);
			}
		}
	}

	private void saveGPXTracks() {
		if (app.savingTrackHelper.hasDataToSave()) {
			long timeUpdated = app.savingTrackHelper.getLastTrackPointTime();
			if (System.currentTimeMillis() - timeUpdated >= 1000 * 60 * 30) {
				startTask(app.getString(R.string.saving_gpx_tracks), -1);
				try {
					warnings.addAll(app.savingTrackHelper.saveDataToGpx(app.getAppCustomization().getTracksDir()).getWarnings());
				} catch (RuntimeException e) {
					warnings.add(e.getMessage());
				}
			} else {
				app.savingTrackHelper.loadGpxFromDatabase();
			}
		}
		if (app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get() && PluginsHelper.isActive(OsmandMonitoringPlugin.class)) {
			app.startNavigationService(NavigationService.USED_BY_GPX);
		}
	}

	private final ExecutorService initOpenglSingleThreadExecutor = Executors.newSingleThreadExecutor();

	@SuppressLint("StaticFieldLeak")
	public void initOpenglAsync(@Nullable InitOpenglListener listener) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... voids) {
				initOpenGl();
				return null;
			}

			@Override
			protected void onPostExecute(Void unused) {
				if (listener != null) {
					listener.onOpenglInitialized();
				}
			}
		}.executeOnExecutor(initOpenglSingleThreadExecutor);
	}

	private void initOpenGl() {
		OsmandSettings settings = app.getSettings();

		if (!settings.USE_OPENGL_RENDER.get()) {
			return;
		}

		if (!Version.isOpenGlAvailable(app)) {
			settings.USE_OPENGL_RENDER.set(false);
		} else {
			int failedCounter = settings.OPENGL_RENDER_FAILED.get();
			if (failedCounter >= MAX_OPENGL_FAILURES && failedCounter % 2 == 1) {
				settings.OPENGL_RENDER_FAILED.set(settings.OPENGL_RENDER_FAILED.get() + 1);
				// show warnings before disable
				warnings.add("Native OpenGL library is not supported. Please try again after exit");
				if (failedCounter > MAX_OPENGL_DISABLE) {
					settings.USE_OPENGL_RENDER.set(false);
				}
			} else {
				try {
					settings.OPENGL_RENDER_FAILED.set(settings.OPENGL_RENDER_FAILED.get() + 1);
					NativeCoreContext.init(app);
					settings.OPENGL_RENDER_FAILED.set(0);
				} catch (Throwable throwable) {
					LOG.error("NativeCoreContext", throwable);
					app.getFeedbackHelper().saveExceptionSilent(Thread.currentThread(), throwable);
				}
			}

			notifyEvent(InitEvents.NATIVE_OPEN_GL_INITIALIZED);
		}
	}

	private void initNativeCore() {
		if (!Version.isQnxOperatingSystem()) {
			OsmandSettings osmandSettings = app.getSettings();
			if (osmandSettings.NATIVE_RENDERING_FAILED.get()) {
				osmandSettings.SAFE_MODE.set(true);
				osmandSettings.NATIVE_RENDERING_FAILED.set(false);
				warnings.add(app.getString(R.string.native_library_not_supported));
			} else {
				osmandSettings.SAFE_MODE.set(false);
				osmandSettings.NATIVE_RENDERING_FAILED.set(true);
				startTask(app.getString(R.string.init_native_library), -1);
				RenderingRulesStorage storage = app.getRendererRegistry().getCurrentSelectedRenderer();
				if (storage == null) {
					LOG.info("Current renderer could not be used!");
					osmandSettings.SAFE_MODE.set(true);
					warnings.add(app.getString(R.string.native_library_not_supported));
				}
				NativeOsmandLibrary lib = storage != null ? NativeOsmandLibrary.getLibrary(storage, app) : null;
				boolean initialized = lib != null;
				osmandSettings.NATIVE_RENDERING_FAILED.set(false);
				if (initialized) {
					File ls = app.getAppPath("fonts");
					lib.loadFontData(ls);
				} else {
					LOG.info("Native library could not be loaded!");
				}

			}
			app.getResourceManager().initMapBoundariesCacheNative();
		}
		notifyEvent(InitEvents.NATIVE_INITIALIZED);
	}

	public void notifyStart() {
		for (AppInitializeListener listener : listeners) {
			listener.onStart(this);
		}
	}

	public void notifyFinish() {
		app.uiHandler.post(() -> {
			for (AppInitializeListener listener : listeners) {
				listener.onFinish(this);
			}
		});
	}

	public void notifyEvent(@NonNull InitEvents event) {
		if (event != InitEvents.TASK_CHANGED) {
			long time = System.currentTimeMillis();
			System.out.println("Initialized " + event + " in " + (time - startBgTime) + " ms");
			startBgTime = time;
		}
		app.uiHandler.post(() -> {
			for (AppInitializeListener listener : listeners) {
				listener.onProgress(this, event);
			}
		});
	}

	@Override
	public void startTask(String taskName, int work) {
		this.taskName = taskName;
		notifyEvent(InitEvents.TASK_CHANGED);
	}

	@Override
	public void startWork(int work) {
	}

	@Override
	public void progress(int deltaWork) {
	}

	@Override
	public void remaining(int remainingWork) {
	}

	@Override
	public void finishTask() {
		taskName = null;
		notifyEvent(InitEvents.TASK_CHANGED);
	}

	public String getCurrentInitTaskName() {
		return taskName;
	}


	@Override
	public boolean isIndeterminate() {
		return true;
	}


	@Override
	public boolean isInterrupted() {
		return false;
	}


	private boolean applicationBgInitializing;

	public synchronized void startApplication() {
		if (applicationBgInitializing) {
			return;
		}
		applicationBgInitializing = true;
		new Thread(() -> {
			try {
				startApplicationBackground();
			} finally {
				applicationBgInitializing = false;
			}
		}, "Initializing app").start();
	}

	public void addListener(@NonNull AppInitializeListener listener) {
		listeners = CollectionUtils.addToList(listeners, listener);
		if (!appInitializing) {
			listener.onFinish(this);
		}
	}

	public void removeListener(@NonNull AppInitializeListener listener) {
		listeners = CollectionUtils.removeFromList(listeners, listener);
	}

	@Override
	public void setGeneralProgress(String genProgress) {
	}

	private String getLocalClassName(@NonNull String cls) {
		String pkg = app.getPackageName();
		int packageLen = pkg.length();
		if (!cls.startsWith(pkg) || cls.length() <= packageLen
				|| cls.charAt(packageLen) != '.') {
			return cls;
		}
		return cls.substring(packageLen + 1);
	}
}
