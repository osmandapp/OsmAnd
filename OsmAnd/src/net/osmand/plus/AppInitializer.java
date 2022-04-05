package net.osmand.plus;

import static net.osmand.plus.AppVersionUpgradeOnInit.LAST_APP_VERSION;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.getPendingIntent;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceForLocalIndex;
import static net.osmand.plus.liveupdates.LiveUpdatesHelper.preferenceLastCheck;
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
import android.content.res.Resources;
import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;

import net.osmand.IProgress;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.map.OsmandRegions;
import net.osmand.map.OsmandRegions.RegionTranslation;
import net.osmand.map.WorldRegion;
import net.osmand.osm.AbstractPoiType;
import net.osmand.osm.MapPoiTypes;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.download.LocalIndexHelper;
import net.osmand.plus.download.LocalIndexInfo;
import net.osmand.plus.download.ui.AbstractLoadLocalIndexTask;
import net.osmand.plus.helpers.AnalyticsHelper;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.helpers.LauncherShortcutsHelper;
import net.osmand.plus.helpers.LockHelper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.inapp.InAppPurchaseHelperImpl;
import net.osmand.plus.liveupdates.LiveUpdatesHelper;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.myplaces.FavouritesFileHelper;
import net.osmand.plus.notifications.NotificationHelper;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.monitoring.LiveMonitoringHelper;
import net.osmand.plus.plugins.monitoring.OsmandMonitoringPlugin;
import net.osmand.plus.plugins.openplacereviews.OprAuthHelper;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper;
import net.osmand.plus.plugins.rastermaps.DownloadTilesHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.quickaction.QuickActionRegistry;
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
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SavingTrackHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.voice.CommandPlayerException;
import net.osmand.plus.wikivoyage.data.TravelDbHelper;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.plus.wikivoyage.data.TravelObfHelper;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;
import net.osmand.util.OpeningHoursParser;

import org.apache.commons.logging.Log;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import btools.routingapp.IBRouterService;

/**
 *
 */
public class AppInitializer implements IProgress {

	public static final String LATEST_CHANGES_URL = "https://osmand.net/blog/osmand-android-4-1-released";

	private static final String EXCEPTION_FILE_SIZE = "EXCEPTION_FS"; //$NON-NLS-1$
	private static final Log LOG = PlatformUtil.getLog(AppInitializer.class);

	private final OsmandApplication app;
	private final AppVersionUpgradeOnInit appVersionUpgrade;

	private boolean initSettings = false;
	private boolean activityChangesShowed = false;
	private long startTime;
	private long startBgTime;
	private boolean appInitializing = true;
	private final List<String> warnings = new ArrayList<>();
	private String taskName;
	private final List<AppInitializeListener> listeners = new ArrayList<>();
	private SharedPreferences startPrefs;

	public enum InitEvents {
		FAVORITES_INITIALIZED, NATIVE_INITIALIZED,
		NATIVE_OPEN_GL_INITIALIZED,
		TASK_CHANGED, MAPS_INITIALIZED, POI_TYPES_INITIALIZED, ASSETS_COPIED, INIT_RENDERERS,
		RESTORE_BACKUPS, INDEX_REGION_BOUNDARIES, SAVE_GPX_TRACKS, LOAD_GPX_TRACKS, ROUTING_CONFIG_INITIALIZED
	}

	public interface AppInitializeListener {

		@WorkerThread
		void onStart(AppInitializer init);

		@UiThread
		void onProgress(AppInitializer init, InitEvents event);

		@UiThread
		void onFinish(AppInitializer init);
	}

	public interface LoadRoutingFilesCallback {
		void onRoutingFilesLoaded();
	}


	public AppInitializer(OsmandApplication app) {
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
		final File file = app.getAppPath(OsmandApplication.EXCEPTION_PATH);
		if (file.exists() && file.length() > 0) {
			if (size != file.length() && !isFirstTime()) {
				if (writeFileSize) {
					activity.getPreferences(Context.MODE_PRIVATE).edit().putLong(EXCEPTION_FILE_SIZE, file.length()).commit();
				}
				return true;
			}
		} else {
			if (size > 0) {
				activity.getPreferences(Context.MODE_PRIVATE).edit().putLong(EXCEPTION_FILE_SIZE, 0).commit();
			}
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
		app.poiTypes.setForbiddenTypes(app.osmandSettings.getForbiddenTypes());
		if (app.getAppPath(IndexConstants.SETTINGS_DIR + "poi_types.xml").exists()) {
			app.poiTypes.init(app.getAppPath(IndexConstants.SETTINGS_DIR + "poi_types.xml").getAbsolutePath());
		} else {
			app.poiTypes.init();
		}

		final Resources resources = app.getLocaleHelper().getLocalizedResources("en");

		app.poiTypes.setPoiTranslator(new MapPoiTypes.PoiTranslator() {

			@Override
			public String getTranslation(AbstractPoiType type) {
				AbstractPoiType baseLangType = type.getBaseLangType();
				if (baseLangType != null) {
					String translation = getTranslation(baseLangType);
					String langTranslation = " (" + app.getLangTranslation(type.getLang()).toLowerCase() + ")";
					if (translation != null) {
						return translation + langTranslation;
					} else {
						return app.poiTypes.getBasePoiName(baseLangType) + langTranslation;
					}
				}
				return getTranslation(type.getIconKeyName());
			}

			@Override
			public String getTranslation(String keyName) {
				try {
					Field f = R.string.class.getField("poi_" + keyName);
					if (f != null) {
						Integer in = (Integer) f.get(null);
						String val = app.getString(in);
						if (val != null) {
							int ind = val.indexOf(';');
							if (ind > 0) {
								return val.substring(0, ind);
							}
						}
						return val;
					}
				} catch (Throwable e) {
					LOG.info("No translation: " + keyName);
				}
				return null;
			}

			@Override
			public String getSynonyms(AbstractPoiType type) {
				AbstractPoiType baseLangType = type.getBaseLangType();
				if (baseLangType != null) {
					return getSynonyms(baseLangType);
				}
				return getSynonyms(type.getIconKeyName());
			}


			@Override
			public String getSynonyms(String keyName) {
				try {
					Field f = R.string.class.getField("poi_" + keyName);
					if (f != null) {
						Integer in = (Integer) f.get(null);
						String val = app.getString(in);
						if (val != null) {
							int ind = val.indexOf(';');
							if (ind > 0) {
								return val.substring(ind + 1);
							}
							return "";
						}
						return val;
					}
				} catch (Exception e) {
				}
				return "";
			}

			@Override
			public String getAllLanguagesTranslationSuffix() {
				return app.getString(R.string.shared_string_all_languages).toLowerCase();
			}

			@Override
			public String getEnTranslation(AbstractPoiType type) {
				AbstractPoiType baseLangType = type.getBaseLangType();
				if (baseLangType != null) {
					return getEnTranslation(baseLangType) + " (" + app.getLangTranslation(type.getLang()).toLowerCase() + ")";
				}
				return getEnTranslation(type.getIconKeyName());
			}

			@Override
			public String getEnTranslation(String keyName) {
				if (resources == null) {
					return Algorithms.capitalizeFirstLetter(
							keyName.replace('_', ' '));
				}
				try {
					Field f = R.string.class.getField("poi_" + keyName);
					if (f != null) {
						Integer in = (Integer) f.get(null);
						String val = resources.getString(in);
						if (val != null) {
							int ind = val.indexOf(';');
							if (ind > 0) {
								return val.substring(0, ind);
							}
						}
						return val;
					}
				} catch (Exception e) {
				}
				return null;
			}
		});
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
		app.backupHelper = startupInit(new BackupHelper(app), BackupHelper.class);
		app.inAppPurchaseHelper = startupInit(new InAppPurchaseHelperImpl(app), InAppPurchaseHelperImpl.class);
		app.poiTypes = startupInit(MapPoiTypes.getDefaultNoInit(), MapPoiTypes.class);
		app.transportRoutingHelper = startupInit(new TransportRoutingHelper(app), TransportRoutingHelper.class);
		app.routingHelper = startupInit(new RoutingHelper(app), RoutingHelper.class);
		app.routingOptionsHelper = startupInit(new RoutingOptionsHelper(app), RoutingOptionsHelper.class);
		app.resourceManager = startupInit(new ResourceManager(app), ResourceManager.class);
		app.daynightHelper = startupInit(new DayNightHelper(app), DayNightHelper.class);
		app.locationProvider = startupInit(new OsmAndLocationProvider(app), OsmAndLocationProvider.class);
		app.avoidSpecificRoads = startupInit(new AvoidSpecificRoads(app), AvoidSpecificRoads.class);
		app.avoidRoadsHelper = startupInit(new AvoidRoadsHelper(app), AvoidRoadsHelper.class);
		app.savingTrackHelper = startupInit(new SavingTrackHelper(app), SavingTrackHelper.class);
		app.analyticsHelper = startupInit(new AnalyticsHelper(app), AnalyticsHelper.class);
		app.notificationHelper = startupInit(new NotificationHelper(app), NotificationHelper.class);
		app.liveMonitoringHelper = startupInit(new LiveMonitoringHelper(app), LiveMonitoringHelper.class);
		app.selectedGpxHelper = startupInit(new GpxSelectionHelper(app, app.savingTrackHelper), GpxSelectionHelper.class);
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

		// TODO TRAVEL_OBF_HELPER check ResourceManager and use TravelObfHelper
		TravelHelper travelHelper = !TravelDbHelper.checkIfDbFileExists(app) ? new TravelObfHelper(app) : new TravelDbHelper(app);
		app.travelHelper = startupInit(travelHelper, TravelHelper.class);
		app.travelRendererHelper = startupInit(new TravelRendererHelper(app), TravelRendererHelper.class);

		app.lockHelper = startupInit(new LockHelper(app), LockHelper.class);
		app.fileSettingsHelper = startupInit(new FileSettingsHelper(app), FileSettingsHelper.class);
		app.networkSettingsHelper = startupInit(new NetworkSettingsHelper(app), NetworkSettingsHelper.class);
		app.quickActionRegistry = startupInit(new QuickActionRegistry(app.getSettings()), QuickActionRegistry.class);
		app.osmOAuthHelper = startupInit(new OsmOAuthHelper(app), OsmOAuthHelper.class);
		app.oprAuthHelper = startupInit(new OprAuthHelper(app), OprAuthHelper.class);
		app.onlineRoutingHelper = startupInit(new OnlineRoutingHelper(app), OnlineRoutingHelper.class);
		app.launcherShortcutsHelper = startupInit(new LauncherShortcutsHelper(app), LauncherShortcutsHelper.class);
		app.gpsFilterHelper = startupInit(new GpsFilterHelper(app), GpsFilterHelper.class);
		app.downloadTilesHelper = startupInit(new DownloadTilesHelper(app), DownloadTilesHelper.class);

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
		loadRoutingFiles(app, new LoadRoutingFilesCallback() {
			@Override
			public void onRoutingFilesLoaded() {
				notifyEvent(InitEvents.ROUTING_CONFIG_INITIALIZED);
			}
		});
	}

	public static void loadRoutingFiles(@NonNull final OsmandApplication app, @Nullable final LoadRoutingFilesCallback callback) {
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


	public synchronized void initVoiceDataInDifferentThread(@NonNull final Context context,
	                                                        @NonNull final ApplicationMode applicationMode,
	                                                        @NonNull final String voiceProvider,
	                                                        @Nullable final Runnable onFinishInitialization,
	                                                        boolean showProgress) {
		String progressTitle = app.getString(R.string.loading_data);
		String progressMessage = app.getString(R.string.voice_data_initializing);
		final ProgressDialog progressDialog = showProgress && context instanceof Activity
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
			app.getRendererRegistry().initRenderers(this);
			notifyEvent(InitEvents.INIT_RENDERERS);
			// native depends on renderers
			initOpenGl();
			notifyEvent(InitEvents.NATIVE_OPEN_GL_INITIALIZED);

			// init poi types before indexes and before POI
			initPoiTypes();
			notifyEvent(InitEvents.POI_TYPES_INITIALIZED);
			app.resourceManager.reloadIndexesOnStart(this, warnings);
			app.travelHelper.initializeDataOnAppStartup();
			// native depends on renderers
			initNativeCore();
			notifyEvent(InitEvents.NATIVE_INITIALIZED);
			app.favoritesHelper.loadFavorites();
			app.gpxDbHelper.loadGpxItems();
			notifyEvent(InitEvents.FAVORITES_INITIALIZED);
			app.poiFilters.reloadAllPoiFilters();
			app.poiFilters.loadSelectedPoiFilters();
			notifyEvent(InitEvents.POI_TYPES_INITIALIZED);
			indexRegionsBoundaries(warnings);
			notifyEvent(InitEvents.INDEX_REGION_BOUNDARIES);
			app.selectedGpxHelper.loadGPXTracks(this);
			notifyEvent(InitEvents.LOAD_GPX_TRACKS);
			saveGPXTracks();
			notifyEvent(InitEvents.SAVE_GPX_TRACKS);
			// restore backuped favorites to normal file
			restoreBackupForFavoritesFiles();
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
				app.showToastMessage(AndroidUtils.formatWarnings(warnings).toString());
			}
		}
	}

	private void checkLiveUpdatesAlerts() {
		OsmandSettings settings = app.getSettings();
		if (!settings.IS_LIVE_UPDATES_ON.get()) {
			return;
		}
		LocalIndexHelper helper = new LocalIndexHelper(app);
		List<LocalIndexInfo> fullMaps = helper.getLocalFullMaps(new AbstractLoadLocalIndexTask() {
			@Override
			public void loadFile(LocalIndexInfo... loaded) {
			}
		});
		AlarmManager alarmMgr = (AlarmManager) app.getSystemService(Context.ALARM_SERVICE);
		for (LocalIndexInfo fm : fullMaps) {
			String fileName = fm.getFileName();
			if (!preferenceForLocalIndex(fileName, settings).get()) {
				continue;
			}
			int updateFrequencyOrd = preferenceUpdateFrequency(fileName, settings).get();
			LiveUpdatesHelper.UpdateFrequency updateFrequency =
					LiveUpdatesHelper.UpdateFrequency.values()[updateFrequencyOrd];
			long lastCheck = preferenceLastCheck(fileName, settings).get();

			if (System.currentTimeMillis() - lastCheck > updateFrequency.getTime() * 2) {
				runLiveUpdate(app, fileName, false, null);
				PendingIntent alarmIntent = getPendingIntent(app, fileName);
				int timeOfDayOrd = preferenceTimeOfDayToUpdate(fileName, settings).get();
				LiveUpdatesHelper.TimeOfDay timeOfDayToUpdate =
						LiveUpdatesHelper.TimeOfDay.values()[timeOfDayOrd];
				setAlarmForPendingIntent(alarmIntent, alarmMgr, updateFrequency, timeOfDayToUpdate);
			}
		}
	}

	private void restoreBackupForFavoritesFiles() {
		final File appDir = app.getAppPath(null);
		File save = new File(appDir, FavouritesFileHelper.FILE_TO_SAVE);
		File bak = new File(appDir, FavouritesFileHelper.FILE_TO_BACKUP);
		if (bak.exists() && (!save.exists() || bak.lastModified() > save.lastModified())) {
			if (save.exists()) {
				save.delete();
			}
			bak.renameTo(save);
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
		if (app.getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get() && OsmandPlugin.isActive(OsmandMonitoringPlugin.class)) {
			app.startNavigationService(NavigationService.USED_BY_GPX);
		}
	}

	private void initOpenGl() {
		if (!"qnx".equals(System.getProperty("os.name"))) {
			OsmandSettings osmandSettings = app.getSettings();
			if (osmandSettings.USE_OPENGL_RENDER.get()) {
				boolean success = false;
				if (!osmandSettings.OPENGL_RENDER_FAILED.get()) {
					osmandSettings.OPENGL_RENDER_FAILED.set(true);
					success = NativeCoreContext.tryCatchInit(app);
					if (success) {
						osmandSettings.OPENGL_RENDER_FAILED.set(false);
					}
				}
				if (!success) {
					// try next time once again ?
					osmandSettings.OPENGL_RENDER_FAILED.set(false);
					warnings.add("Native OpenGL library is not supported. Please try again after exit");
				}
			}
		}
	}

	private void initNativeCore() {
		if (!"qnx".equals(System.getProperty("os.name"))) {
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
				NativeOsmandLibrary lib = NativeOsmandLibrary.getLibrary(storage, app);
				boolean initialized = lib != null;
				osmandSettings.NATIVE_RENDERING_FAILED.set(false);
				if (!initialized) {
					LOG.info("Native library could not be loaded!");
				} else {
					File ls = app.getAppPath("fonts");
					lib.loadFontData(ls);
				}

			}
			app.getResourceManager().initMapBoundariesCacheNative();
		}
	}

	public void notifyStart() {
		for (AppInitializeListener listener : listeners) {
			listener.onStart(AppInitializer.this);
		}
	}

	public void notifyFinish() {
		app.uiHandler.post(new Runnable() {

			@Override
			public void run() {
				for (AppInitializeListener l : listeners) {
					l.onFinish(AppInitializer.this);
				}
			}
		});
	}

	public void notifyEvent(final InitEvents event) {
		if (event != InitEvents.TASK_CHANGED) {
			long time = System.currentTimeMillis();
			System.out.println("Initialized " + event + " in " + (time - startBgTime) + " ms");
			startBgTime = time;
		}
		app.uiHandler.post(new Runnable() {

			@Override
			public void run() {
				for (AppInitializeListener l : listeners) {
					l.onProgress(AppInitializer.this, event);
				}
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


	private boolean applicationBgInitializing = false;


	public synchronized void startApplication() {
		if (applicationBgInitializing) {
			return;
		}
		applicationBgInitializing = true;
		new Thread(new Runnable() { //$NON-NLS-1$
			@Override
			public void run() {
				try {
					startApplicationBackground();
				} finally {
					applicationBgInitializing = false;
				}
			}
		}, "Initializing app").start();
	}


	public void addListener(AppInitializeListener listener) {
		this.listeners.add(listener);
		if (!appInitializing) {
			listener.onFinish(this);
		}
	}


	@Override
	public void setGeneralProgress(String genProgress) {
	}

	public void removeListener(AppInitializeListener listener) {
		this.listeners.remove(listener);
	}

	private String getLocalClassName(String cls) {
		final String pkg = app.getPackageName();
		int packageLen = pkg.length();
		if (!cls.startsWith(pkg) || cls.length() <= packageLen
				|| cls.charAt(packageLen) != '.') {
			return cls;
		}
		return cls.substring(packageLen + 1);
	}
}
