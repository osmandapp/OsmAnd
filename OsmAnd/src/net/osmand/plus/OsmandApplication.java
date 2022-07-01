package net.osmand.plus;

import static net.osmand.IndexConstants.ROUTING_FILE_EXT;
import static net.osmand.plus.settings.backend.ApplicationMode.valueOfStringKey;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.app.CarToast;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.TileSourceManager;
import net.osmand.map.WorldRegion;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.RestartActivity;
import net.osmand.plus.activities.actions.OsmAndDialogs;
import net.osmand.plus.api.SQLiteAPI;
import net.osmand.plus.api.SQLiteAPIImpl;
import net.osmand.plus.auto.NavigationCarAppService;
import net.osmand.plus.auto.NavigationScreen;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadService;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AnalyticsHelper;
import net.osmand.plus.helpers.AndroidApiLocationServiceHelper;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.DayNightHelper;
import net.osmand.plus.helpers.GmsLocationServiceHelper;
import net.osmand.plus.helpers.LauncherShortcutsHelper;
import net.osmand.plus.helpers.LocaleHelper;
import net.osmand.plus.helpers.LocationServiceHelper;
import net.osmand.plus.helpers.LockHelper;
import net.osmand.plus.helpers.RateUsHelper;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.myplaces.FavouritesHelper;
import net.osmand.plus.notifications.NotificationHelper;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.accessibility.AccessibilityMode;
import net.osmand.plus.plugins.accessibility.AccessibilityPlugin;
import net.osmand.plus.plugins.monitoring.LiveMonitoringHelper;
import net.osmand.plus.plugins.openplacereviews.OprAuthHelper;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper;
import net.osmand.plus.plugins.rastermaps.DownloadTilesHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.quickaction.QuickActionRegistry;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.render.TravelRendererHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routing.AvoidRoadsHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.FileSettingsHelper;
import net.osmand.plus.settings.enums.DrivingRegion;
import net.osmand.plus.settings.enums.LocationSource;
import net.osmand.plus.settings.enums.MetricsConstants;
import net.osmand.plus.track.helpers.GpsFilterHelper;
import net.osmand.plus.track.helpers.GpxDbHelper;
import net.osmand.plus.track.helpers.GpxDisplayHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.track.helpers.SavingTrackHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.mapwidgets.AverageSpeedComputer;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.router.GeneralRouter;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.search.SearchUICore;
import net.osmand.util.Algorithms;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import btools.routingapp.BRouterServiceConnection;
import btools.routingapp.IBRouterService;

public class OsmandApplication extends MultiDexApplication {

	public static final String EXCEPTION_PATH = "exception.log";
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(OsmandApplication.class);

	final AppInitializer appInitializer = new AppInitializer(this);
	Handler uiHandler;
	OsmandSettings osmandSettings;
	OsmAndAppCustomization appCustomization;
	NavigationService navigationService;
	DownloadService downloadService;
	OsmandAidlApi aidlApi;

	NavigationCarAppService navigationCarAppService;
	NavigationSession carNavigationSession;

	private final SQLiteAPI sqliteAPI = new SQLiteAPIImpl(this);
	private final OsmAndTaskManager taskManager = new OsmAndTaskManager(this);
	private final UiUtilities iconsCache = new UiUtilities(this);
	private final LocaleHelper localeHelper = new LocaleHelper(this);

	// start variables
	ResourceManager resourceManager;
	OsmAndLocationProvider locationProvider;
	RendererRegistry rendererRegistry;
	DayNightHelper daynightHelper;
	PoiFiltersHelper poiFilters;
	MapPoiTypes poiTypes;
	RoutingHelper routingHelper;
	TransportRoutingHelper transportRoutingHelper;
	FavouritesHelper favoritesHelper;
	CommandPlayer player;
	GpxSelectionHelper selectedGpxHelper;
	GpxDisplayHelper gpxDisplayHelper;
	SavingTrackHelper savingTrackHelper;
	AnalyticsHelper analyticsHelper;
	NotificationHelper notificationHelper;
	LiveMonitoringHelper liveMonitoringHelper;
	TargetPointsHelper targetPointsHelper;
	MapMarkersHelper mapMarkersHelper;
	MapMarkersDbHelper mapMarkersDbHelper;
	WaypointHelper waypointHelper;
	RoutingOptionsHelper routingOptionsHelper;
	DownloadIndexesThread downloadIndexesThread;
	AvoidSpecificRoads avoidSpecificRoads;
	AvoidRoadsHelper avoidRoadsHelper;
	BRouterServiceConnection bRouterServiceConnection;
	OsmandRegions regions;
	GeocodingLookupService geocodingLookupService;
	QuickSearchHelper searchUICore;
	TravelHelper travelHelper;
	InAppPurchaseHelper inAppPurchaseHelper;
	MapViewTrackingUtilities mapViewTrackingUtilities;
	OsmandMap osmandMap;
	LockHelper lockHelper;
	FileSettingsHelper fileSettingsHelper;
	NetworkSettingsHelper networkSettingsHelper;
	GpxDbHelper gpxDbHelper;
	QuickActionRegistry quickActionRegistry;
	OsmOAuthHelper osmOAuthHelper;
	OprAuthHelper oprAuthHelper;
	MeasurementEditingContext measurementEditingContext;
	OnlineRoutingHelper onlineRoutingHelper;
	BackupHelper backupHelper;
	TravelRendererHelper travelRendererHelper;
	LauncherShortcutsHelper launcherShortcutsHelper;
	GpsFilterHelper gpsFilterHelper;
	DownloadTilesHelper downloadTilesHelper;
	AverageSpeedComputer averageSpeedComputer;

	private final Map<String, Builder> customRoutingConfigs = new ConcurrentHashMap<>();
	private File externalStorageDirectory;
	private boolean externalStorageDirectoryReadOnly;

	// Typeface
	
	@Override
	public void onCreate() {
		if (RestartActivity.isRestartProcess(this)) {
			return;
		}
		long timeToStart = System.currentTimeMillis();
		if (Version.isDeveloperVersion(this)) {
			try {
				Class.forName("net.osmand.plus.base.EnableStrictMode").newInstance();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		super.onCreate();
		createInUiThread();
		uiHandler = new Handler();
		appCustomization = new OsmAndAppCustomization();
		appCustomization.setup(this);
		osmandSettings = appCustomization.getOsmandSettings();
		appInitializer.initVariables();
		if (appInitializer.isAppVersionChanged() && appInitializer.getPrevAppVersion() < AppVersionUpgradeOnInit.VERSION_2_3) {
			osmandSettings.freezeExternalStorageDirectory();
		} else if (appInitializer.isFirstTime()) {
			osmandSettings.initExternalStorageDirectory();
		}
		externalStorageDirectory = osmandSettings.getExternalStorageDirectory();
		if (!FileUtils.isWritable(externalStorageDirectory)) {
			externalStorageDirectoryReadOnly = true;
			externalStorageDirectory = osmandSettings.getInternalAppPath();
		}

		Algorithms.removeAllFiles(getAppPath(IndexConstants.TEMP_DIR));
		if (appInitializer.isAppVersionChanged()) {
			// Reset mapillary tile sources
			File tilesPath = getAppPath(IndexConstants.TILES_INDEX_DIR);
			File mapillaryVectorTilesPath = new File(tilesPath, TileSourceManager.getMapillaryVectorSource().getName());
			Algorithms.removeAllFiles(mapillaryVectorTilesPath);
			// Remove travel sqlite db files
			removeSqliteDbTravelFiles();
		}

		localeHelper.checkPreferredLocale();
		appInitializer.onCreateApplication();
		osmandMap.getMapLayers().createLayers(osmandMap.getMapView());
		startApplication();
		System.out.println("Time to start application " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms");

		timeToStart = System.currentTimeMillis();
		OsmandPlugin.initPlugins(this);
		OsmandPlugin.createLayers(this, null);
		System.out.println("Time to init plugins " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms");

		osmandMap.getMapLayers().updateLayers(null);

		SearchUICore.setDebugMode(OsmandPlugin.isDevelopment());
		BackupHelper.DEBUG = true;//OsmandPlugin.isDevelopment();
	}

	public boolean isPlusVersionInApp() {
		return true;
	}

	public boolean isExternalStorageDirectoryReadOnly() {
		return externalStorageDirectoryReadOnly;
	}

	private void removeSqliteDbTravelFiles() {
		File[] files = getAppPath(IndexConstants.WIKIVOYAGE_INDEX_DIR).listFiles();
		if (files != null) {
			for (File file : files) {
				if (file.getName().endsWith(IndexConstants.BINARY_WIKIVOYAGE_MAP_INDEX_EXT)) {
					file.delete();
				}
			}
		}
	}

	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		MultiDex.install(this);
	}

	public AppInitializer getAppInitializer() {
		return appInitializer;
	}
	
	public MapPoiTypes getPoiTypes() {
		return poiTypes;
	}

	private void createInUiThread() {
		new Toast(this); // activate in UI thread to avoid further exceptions
		new AsyncTask<View, Void, Void>() {
			@Override
			protected Void doInBackground(View... params) {
				return null;
			}

			protected void onPostExecute(Void result) {
			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	@NonNull
	public UiUtilities getUIUtilities() {
		return iconsCache;
	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		if (routingHelper != null) {
			routingHelper.getVoiceRouter().onApplicationTerminate();
		}
		if (RateUsHelper.shouldShowRateDialog(this)) {
			osmandSettings.RATE_US_STATE.set(RateUsHelper.RateUsState.IGNORED);
		}
		getNotificationHelper().removeNotifications(false);
	}

	public RendererRegistry getRendererRegistry() {
		return rendererRegistry;
	}
	
	public OsmAndTaskManager getTaskManager() {
		return taskManager;
	}
	
	public AvoidSpecificRoads getAvoidSpecificRoads() {
		return avoidSpecificRoads;
	}

	public AvoidRoadsHelper getAvoidRoadsHelper() {
		return avoidRoadsHelper;
	}

	public OsmAndLocationProvider getLocationProvider() {
		return locationProvider;
	}
	
	public OsmAndAppCustomization getAppCustomization() {
		return appCustomization;
	}

	public QuickActionRegistry getQuickActionRegistry() {
		return quickActionRegistry;
	}

	public LocationServiceHelper createLocationServiceHelper() {
		LocationSource source = osmandSettings.LOCATION_SOURCE.get();
		if (source == LocationSource.GOOGLE_PLAY_SERVICES) {
			return new GmsLocationServiceHelper(this);
		}
		return new AndroidApiLocationServiceHelper(this);
	}

	public void setAppCustomization(OsmAndAppCustomization appCustomization) {
		this.appCustomization = appCustomization;
		this.appCustomization.setup(this);
	}

	/**
	 * Application settings
	 *
	 * @return Reference to instance of OsmandSettings
	 */
	public OsmandSettings getSettings() {
		if (osmandSettings == null) {
			LOG.error("Trying to access settings before they were created");
		}
		return osmandSettings;
	}

	public void setOsmandSettings(OsmandSettings osmandSettings) {
		this.osmandSettings = osmandSettings;
		OsmandPlugin.initPlugins(this);
	}

	public SavingTrackHelper getSavingTrackHelper() {
		return savingTrackHelper;
	}
	
	public NotificationHelper getNotificationHelper() {
		return notificationHelper;
	}

	public LiveMonitoringHelper getLiveMonitoringHelper() {
		return liveMonitoringHelper;
	}

	public WaypointHelper getWaypointHelper() {
		return waypointHelper;
	}

	public PoiFiltersHelper getPoiFilters() {
		return poiFilters;
	}

	public GpxSelectionHelper getSelectedGpxHelper() {
		return selectedGpxHelper;
	}

	public GpxDisplayHelper getGpxDisplayHelper() {
		return gpxDisplayHelper;
	}

	public GpxDbHelper getGpxDbHelper() {
		return gpxDbHelper;
	}

	public FavouritesHelper getFavoritesHelper() {
		return favoritesHelper;
	}

	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	public DayNightHelper getDaynightHelper() {
		return daynightHelper;
	}

	public LockHelper getLockHelper() {
		return lockHelper;
	}

	public FileSettingsHelper getFileSettingsHelper() {
		return fileSettingsHelper;
	}

	public NetworkSettingsHelper getNetworkSettingsHelper() {
		return networkSettingsHelper;
	}

	public OsmOAuthHelper getOsmOAuthHelper() {
		return osmOAuthHelper;
	}

	public OprAuthHelper getOprAuthHelper() {
		return oprAuthHelper;
	}

	public LocaleHelper getLocaleHelper() {
		return localeHelper;
	}

	public synchronized DownloadIndexesThread getDownloadThread() {
		if (downloadIndexesThread == null) {
			downloadIndexesThread = new DownloadIndexesThread(this);
		}
		return downloadIndexesThread;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		resourceManager.onLowMemory();
	}

	@Override
	public void onConfigurationChanged(@NonNull Configuration newConfig) {
		Locale preferredLocale = localeHelper.getPreferredLocale();
		if (preferredLocale != null && !newConfig.locale.getLanguage().equals(preferredLocale.getLanguage())) {
			super.onConfigurationChanged(newConfig);
			// ugly fix ! On devices after 4.0 screen is blinking when you rotate device!
			if (Build.VERSION.SDK_INT < 14) {
				newConfig.locale = preferredLocale;
			}
			getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
			Locale.setDefault(preferredLocale);
		} else {
			super.onConfigurationChanged(newConfig);
		}
	}

	public void checkApplicationIsBeingInitialized(AppInitializeListener listener) {
		// start application if it was previously closed
		startApplication();
		if (listener != null) {
			appInitializer.addListener(listener);
		}
	}
	
	public void unsubscribeInitListener(AppInitializeListener listener) {
		if (listener != null) {
			appInitializer.removeListener(listener);
		}
	}
	
	public boolean isApplicationInitializing() {
		return appInitializer.isAppInitializing();
	}

	public RoutingHelper getRoutingHelper() {
		return routingHelper;
	}

	public MeasurementEditingContext getMeasurementEditingContext() {
		return measurementEditingContext;
	}

	public void setMeasurementEditingContext(MeasurementEditingContext context) {
		this.measurementEditingContext = context;
	}

	public OnlineRoutingHelper getOnlineRoutingHelper() {
		return onlineRoutingHelper;
	}

	public BackupHelper getBackupHelper() {
		return backupHelper;
	}

	public TransportRoutingHelper getTransportRoutingHelper() {
		return transportRoutingHelper;
	}

	public RoutingOptionsHelper getRoutingOptionsHelper() {
		return routingOptionsHelper;
	}

	public GeocodingLookupService getGeocodingLookupService() {
		return geocodingLookupService;
	}

	public QuickSearchHelper getSearchUICore() {
		return searchUICore;
	}

	public TravelHelper getTravelHelper() {
		return travelHelper;
	}

	public TravelRendererHelper getTravelRendererHelper() {
		return travelRendererHelper;
	}

	public InAppPurchaseHelper getInAppPurchaseHelper() {
		return inAppPurchaseHelper;
	}

	public LauncherShortcutsHelper getLauncherShortcutsHelper() {
		return launcherShortcutsHelper;
	}

	public GpsFilterHelper getGpsFilterHelper() {
		return gpsFilterHelper;
	}

	@NonNull
	public DownloadTilesHelper getDownloadTilesHelper() {
		return downloadTilesHelper;
	}

	@NonNull
	public AverageSpeedComputer getAverageSpeedComputer() {
		return averageSpeedComputer;
	}

	public CommandPlayer getPlayer() {
		return player;
	}

	public void initVoiceCommandPlayer(@NonNull Context context,
	                                   @NonNull ApplicationMode appMode,
	                                   @Nullable Runnable onCommandPlayerCreated,
	                                   boolean warnNoProvider,
	                                   boolean showProgress,
	                                   boolean forceInitialization,
	                                   boolean applyAllModes) {
		String voiceProvider = osmandSettings.VOICE_PROVIDER.getModeValue(appMode);
		if (OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(voiceProvider)) {
			osmandSettings.VOICE_MUTE.setModeValue(appMode, true);
		} else if (Algorithms.isEmpty(voiceProvider)) {
			if (warnNoProvider && context instanceof MapActivity) {
				OsmAndDialogs.showVoiceProviderDialog((MapActivity) context, appMode, applyAllModes);
			}
		} else {
			if (player == null || !voiceProvider.equals(player.getCurrentVoice()) || forceInitialization) {
				appInitializer.initVoiceDataInDifferentThread(context, appMode, voiceProvider,
						onCommandPlayerCreated, showProgress);
			}
		}
	}

	public NavigationService getNavigationService() {
		return navigationService;
	}

	public void setNavigationService(NavigationService navigationService) {
		this.navigationService = navigationService;
	}

	public interface NavigationSessionListener {
		void onNavigationSessionChanged(@Nullable NavigationSession navigationSession);
	}

	private NavigationSessionListener navigationSessionListener;

	public void setNavigationSessionListener(@Nullable NavigationSessionListener navigationSessionListener) {
		this.navigationSessionListener = navigationSessionListener;
	}

	@Nullable
	public NavigationCarAppService getNavigationCarAppService() {
		return navigationCarAppService;
	}

	public void setNavigationCarAppService(@Nullable NavigationCarAppService navigationCarAppService) {
		this.navigationCarAppService = navigationCarAppService;
	}

	@Nullable
	public NavigationSession getCarNavigationSession() {
		return carNavigationSession;
	}

	public void setCarNavigationSession(@Nullable NavigationSession carNavigationSession) {
		NavigationService navigationService = this.navigationService;
		if (carNavigationSession == null) {
			if (navigationService != null) {
				navigationService.stopIfNeeded(this, NavigationService.USED_BY_CAR_APP);
			}
		} else {
			startNavigationService(NavigationService.USED_BY_CAR_APP);
		}
		this.carNavigationSession = carNavigationSession;
		NavigationSessionListener navigationSessionListener = this.navigationSessionListener;
		if (navigationSessionListener != null) {
			navigationSessionListener.onNavigationSessionChanged(carNavigationSession);
		}
	}

	public void refreshCarScreen() {
		NavigationSession carNavigationSession = getCarNavigationSession();
		if (carNavigationSession != null) {
			NavigationScreen navigationScreen = carNavigationSession.getNavigationScreen();
			if (navigationScreen != null) {
				navigationScreen.invalidate();
			}
		}
	}

	public DownloadService getDownloadService() {
		return downloadService;
	}

	public void setDownloadService(DownloadService downloadService) {
		this.downloadService = downloadService;
	}

	public OsmandAidlApi getAidlApi() {
		return aidlApi;
	}

	public void stopNavigation() {
		if (locationProvider.getLocationSimulation().isRouteAnimating()) {
			locationProvider.getLocationSimulation().stop();
		}
		routingHelper.getVoiceRouter().interruptRouteCommands();
		routingHelper.clearCurrentRoute(null, new ArrayList<LatLon>());
		routingHelper.setRoutePlanningMode(false);
		osmandSettings.LAST_ROUTING_APPLICATION_MODE = osmandSettings.APPLICATION_MODE.get();
		osmandSettings.setApplicationMode(valueOfStringKey(osmandSettings.LAST_USED_APPLICATION_MODE.get(), ApplicationMode.DEFAULT));
		targetPointsHelper.removeAllWayPoints(false, false);
	}

	public void startApplication() {
		UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
		if (!(uncaughtExceptionHandler instanceof DefaultExceptionHandler)) {
			Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());
		}
		if (NetworkUtils.getProxy() == null && osmandSettings.ENABLE_PROXY.get()) {
			NetworkUtils.setProxy(osmandSettings.PROXY_HOST.get(), osmandSettings.PROXY_PORT.get());
		}
		appInitializer.startApplication();
	}

	private class DefaultExceptionHandler implements UncaughtExceptionHandler {

		private final UncaughtExceptionHandler defaultHandler;
		private final PendingIntent intent;

		public DefaultExceptionHandler() {
			defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
			intent = PendingIntent.getActivity(OsmandApplication.this.getBaseContext(), 0,
					new Intent(OsmandApplication.this.getBaseContext(),
							getAppCustomization().getMapActivity()), 0);
		}

		@Override
		public void uncaughtException(final Thread thread, final Throwable ex) {
			File file = getAppPath(EXCEPTION_PATH);
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintStream printStream = new PrintStream(out);
				ex.printStackTrace(printStream);
				StringBuilder msg = new StringBuilder();
				msg.append("Version  ")
						.append(Version.getFullVersion(OsmandApplication.this))
						.append("\n")
						.append(DateFormat.format("dd.MM.yyyy h:mm:ss", System.currentTimeMillis()));
				try {
					PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
					if (info != null) {
						msg.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode);
					}
				} catch (Throwable e) {
				}
				msg.append("\n")
						.append("Exception occured in thread ")
						.append(thread.toString())
						.append(" : \n")
						.append(out.toString());

				if (file.getParentFile().canWrite()) {
					BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
					writer.write(msg.toString());
					writer.close();
				}
				if (routingHelper.isFollowingMode()) {
					AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
					if (Build.VERSION.SDK_INT >= 19) {
						mgr.setExact(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
					} else {
						mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
					}
					System.exit(2);
				}
				defaultHandler.uncaughtException(thread, ex);
			} catch (Exception e) {
				// swallow all exceptions
				android.util.Log.e(PlatformUtil.TAG, "Exception while handle other exception", e);
			}
		}
	}

	public TargetPointsHelper getTargetPointsHelper() {
		return targetPointsHelper;
	}

	public MapMarkersHelper getMapMarkersHelper() {
		return mapMarkersHelper;
	}

	public MapMarkersDbHelper getMapMarkersDbHelper() {
		return mapMarkersDbHelper;
	}

	public void showShortToastMessage(final int msgId, final Object... args) {
		uiHandler.post(() -> {
			Toast.makeText(OsmandApplication.this, getString(msgId, args), Toast.LENGTH_SHORT).show();
			NavigationSession carNavigationSession = this.carNavigationSession;
			if (carNavigationSession != null && carNavigationSession.hasStarted()) {
				CarToast.makeText(carNavigationSession.getCarContext(),
						getString(msgId, args), CarToast.LENGTH_SHORT).show();
			}
		});
	}

	public void showShortToastMessage(final String msg) {
		uiHandler.post(() -> {
			Toast.makeText(OsmandApplication.this, msg, Toast.LENGTH_SHORT).show();
			NavigationSession carNavigationSession = this.carNavigationSession;
			if (carNavigationSession != null && carNavigationSession.hasStarted()) {
				CarToast.makeText(carNavigationSession.getCarContext(),
						msg, CarToast.LENGTH_SHORT).show();
			}
		});
	}

	public void showToastMessage(final int msgId, final Object... args) {
		uiHandler.post(() -> {
			Toast.makeText(OsmandApplication.this, getString(msgId, args), Toast.LENGTH_LONG).show();
			NavigationSession carNavigationSession = this.carNavigationSession;
			if (carNavigationSession != null && carNavigationSession.hasStarted()) {
				CarToast.makeText(carNavigationSession.getCarContext(),
						getString(msgId, args), CarToast.LENGTH_LONG).show();
			}
		});
	}

	public void showToastMessage(@Nullable String text) {
		if (!Algorithms.isEmpty(text)) {
			uiHandler.post(() -> {
				Toast.makeText(OsmandApplication.this, text, Toast.LENGTH_LONG).show();
				NavigationSession carNavigationSession = this.carNavigationSession;
				if (carNavigationSession != null && carNavigationSession.hasStarted()) {
					CarToast.makeText(carNavigationSession.getCarContext(),
							text, CarToast.LENGTH_LONG).show();
				}
			});
		}
	}

	public SQLiteAPI getSQLiteAPI() {
		return sqliteAPI;
	}

	public void runInUIThread(Runnable run) {
		uiHandler.post(run);
	}

	public void runInUIThread(Runnable run, long delay) {
		uiHandler.postDelayed(run, delay);
	}
	
	public void runMessageInUIThreadAndCancelPrevious(final int messageId, final Runnable run, long delay) {
		Message msg = Message.obtain(uiHandler, new Runnable() {
			
			@Override
			public void run() {
				if (!uiHandler.hasMessages(messageId)) {
					run.run();
				}
			}
		});
		msg.what = messageId;
		uiHandler.removeMessages(messageId);
		uiHandler.sendMessageDelayed(msg, delay);
	}
	
	public File getAppPath(@Nullable String path) {
		if (path == null) {
			path = "";
		}
		return new File(externalStorageDirectory, path);
	}

	public void setExternalStorageDirectory(int type, String directory) {
		osmandSettings.setExternalStorageDirectory(type, directory);
		externalStorageDirectory = osmandSettings.getExternalStorageDirectory();
		externalStorageDirectoryReadOnly = false;
		getResourceManager().resetStoreDirectory();
	}

	public void applyTheme(Context c) {
		int themeResId;
		boolean doNotUseAnimations = osmandSettings.DO_NOT_USE_ANIMATIONS.get();
		if (!osmandSettings.isLightContent()) {
			if (doNotUseAnimations) {
				themeResId = R.style.OsmandDarkTheme_NoAnimation;
			} else {
				themeResId = R.style.OsmandDarkTheme;
			}
		} else {
			if (doNotUseAnimations) {
				themeResId = R.style.OsmandLightTheme_NoAnimation;
			} else {
				themeResId = R.style.OsmandLightTheme;
			}
		}
		localeHelper.setLanguage(c);
		c.setTheme(themeResId);
	}

	IBRouterService reconnectToBRouter() {
		try {
			bRouterServiceConnection = BRouterServiceConnection.connect(this);
			if (bRouterServiceConnection != null) {
				return bRouterServiceConnection.getBrouterService();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public IBRouterService getBRouterService() {
		if (bRouterServiceConnection == null) {
			return null;
		}
		IBRouterService s = bRouterServiceConnection.getBrouterService();
		if (s != null && !s.asBinder().isBinderAlive()) {
			s = reconnectToBRouter();
		}
		return s;
	}

	public String getLanguage() {
		return localeHelper.getLanguage();
	}

	@Override
	public AssetManager getAssets() {
		return getResources() != null ? getResources().getAssets() : super.getAssets();
	}

	@Override
	public Resources getResources() {
		Resources localizedResources = localeHelper.getLocalizedResources();
		return localizedResources != null ? localizedResources : super.getResources();
	}

	public List<RoutingConfiguration.Builder> getAllRoutingConfigs() {
		List<RoutingConfiguration.Builder> builders = new ArrayList<>(customRoutingConfigs.values());
		builders.add(0, getDefaultRoutingConfig());
		return builders;
	}

	public synchronized RoutingConfiguration.Builder getDefaultRoutingConfig() {
		return RoutingConfiguration.getDefault();
	}

	public Map<String, RoutingConfiguration.Builder> getCustomRoutingConfigs() {
		return customRoutingConfigs;
	}

	public RoutingConfiguration.Builder getCustomRoutingConfig(String key) {
		return customRoutingConfigs.get(key);
	}

	@NonNull
	public RoutingConfiguration.Builder getRoutingConfigForMode(ApplicationMode mode) {
		RoutingConfiguration.Builder builder = null;
		String routingProfileKey = mode.getRoutingProfile();
		if (!Algorithms.isEmpty(routingProfileKey)) {
			int index = routingProfileKey.indexOf(ROUTING_FILE_EXT);
			if (index != -1) {
				String configKey = routingProfileKey.substring(0, index + ROUTING_FILE_EXT.length());
				builder = customRoutingConfigs.get(configKey);
			}
		}
		return builder != null ? builder : getDefaultRoutingConfig();
	}

	@Nullable
	public GeneralRouter getRouter(ApplicationMode mode) {
		Builder builder = getRoutingConfigForMode(mode);
		return getRouter(builder, mode);
	}

	@Nullable
	public GeneralRouter getRouter(Builder builder, ApplicationMode am) {
		GeneralRouter router = builder.getRouter(am.getRoutingProfile());
		if (router == null) {
			router = builder.getRouter(am.getDefaultRoutingProfile());
		}
		return router;
	}

	public OsmandRegions getRegions() {
		return regions;
	}

	public boolean accessibilityEnabled() {
		return accessibilityEnabledForMode(getSettings().APPLICATION_MODE.get());
	}

	public boolean accessibilityEnabledForMode(ApplicationMode appMode) {
		final AccessibilityMode mode = getSettings().ACCESSIBILITY_MODE.getModeValue(appMode);
		if (!OsmandPlugin.isActive(AccessibilityPlugin.class)) {
			return false;
		}
		if (mode == AccessibilityMode.ON) {
			return true;
		} else if (mode == AccessibilityMode.OFF) {
			return false;
		}
		return systemAccessibilityEnabled();
	}

	public boolean systemAccessibilityEnabled() {
		return ((AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE)).isEnabled();
	}

	public String getVersionName() {
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			return info.versionName;
		} catch (NameNotFoundException e) {
			return "";
		}
	}

	public int getVersionCode() {
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			return info.versionCode;
		} catch (NameNotFoundException e) {
			return 0;
		}
	}

	public void startNavigationService(int intent) {
		final Intent serviceIntent = new Intent(this, NavigationService.class);
		if (getNavigationService() != null) {
			intent |= getNavigationService().getUsedBy();
			getNavigationService().stopSelf();
			
		}
		serviceIntent.putExtra(NavigationService.USAGE_INTENT, intent);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(serviceIntent);
		} else {
			startService(serviceIntent);
		}
		//getNotificationHelper().showNotifications();
	}

	public void startDownloadService() {
		final Intent serviceIntent = new Intent(this, DownloadService.class);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(serviceIntent);
		} else {
			startService(serviceIntent);
		}
	}

	public String getLangTranslation(String l) {
		try {
			java.lang.reflect.Field f = R.string.class.getField("lang_" + l);
			if (f != null) {
				Integer in = (Integer) f.get(null);
				return getString(in);
			}
		} catch (Exception e) {
			System.err.println(e.getMessage());
		}
		return l;
	}

	public void setupDrivingRegion(WorldRegion reg) {
		DrivingRegion drg = null;
		WorldRegion.RegionParams params = reg.getParams();
//		boolean americanSigns = "american".equals(params.getRegionRoadSigns());
		boolean leftHand = "yes".equals(params.getRegionLeftHandDriving());
		MetricsConstants mc1 = "miles".equals(params.getRegionMetric()) ?
				MetricsConstants.MILES_AND_FEET : MetricsConstants.KILOMETERS_AND_METERS;
		MetricsConstants mc2 = "miles".equals(params.getRegionMetric()) ?
				MetricsConstants.MILES_AND_METERS : MetricsConstants.KILOMETERS_AND_METERS;
		for (DrivingRegion r : DrivingRegion.values()) {
			if (r.leftHandDriving == leftHand && (r.defMetrics == mc1 || r.defMetrics == mc2)) {
				drg = r;
				break;
			}
		}
		if (drg != null) {
			osmandSettings.DRIVING_REGION.set(drg);
		}
	}

	public String getUserAndroidId() {
		String userAndroidId = osmandSettings.USER_ANDROID_ID.get();
		if (!Algorithms.isEmpty(userAndroidId)) {
			return userAndroidId;
		}
		userAndroidId = UUID.randomUUID().toString();
		osmandSettings.USER_ANDROID_ID.set(userAndroidId);
		return userAndroidId;
	}

	public void logEvent(String event) {
		try {
			analyticsHelper.addEvent(event, AnalyticsHelper.EVENT_TYPE_APP_USAGE);
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	public void logMapDownloadEvent(String event, IndexItem item) {
		try {
			analyticsHelper.addEvent("map_download_" + event + ": " + item.getFileName(), AnalyticsHelper.EVENT_TYPE_MAP_DOWNLOAD);
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	public void logMapDownloadEvent(String event, IndexItem item, long time) {
		try {
			analyticsHelper.addEvent("map_download_" + event + ": " + item.getFileName() + " in " + time + " msec", AnalyticsHelper.EVENT_TYPE_MAP_DOWNLOAD);
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	public MapViewTrackingUtilities getMapViewTrackingUtilities() {
		return mapViewTrackingUtilities;
	}

	public OsmandMap getOsmandMap() {
		return osmandMap;
	}

	public void sendCrashLog() {
		File file = getAppPath(OsmandApplication.EXCEPTION_PATH);
		sendCrashLog(file);
	}

	public void sendCrashLog(File file) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.putExtra(Intent.EXTRA_EMAIL, new String[] {"crash@osmand.net"});
		intent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(this, file));
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
		intent.setType("vnd.android.cursor.dir/email");
		intent.putExtra(Intent.EXTRA_SUBJECT, "OsmAnd bug");
		intent.putExtra(Intent.EXTRA_TEXT, getDeviceInfo());
		Intent chooserIntent = Intent.createChooser(intent, getString(R.string.send_report));
		chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		AndroidUtils.startActivityIfSafe(this, intent, chooserIntent);
	}

	public void sendSupportEmail(String screenName) {
		Intent emailIntent = new Intent(Intent.ACTION_SEND)
				.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				.putExtra(Intent.EXTRA_EMAIL, new String[] {"support@osmand.net"})
				.putExtra(Intent.EXTRA_SUBJECT, screenName)
				.putExtra(Intent.EXTRA_TEXT, getDeviceInfo());
		emailIntent.setSelector(new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")));
		AndroidUtils.startActivityIfSafe(this, emailIntent);
	}

	public String getDeviceInfo() {
		StringBuilder text = new StringBuilder();
		text.append("Device : ").append(Build.DEVICE);
		text.append("\nBrand : ").append(Build.BRAND);
		text.append("\nModel : ").append(Build.MODEL);
		text.append("\nProduct : ").append(Build.PRODUCT);
		text.append("\nBuild : ").append(Build.DISPLAY);
		text.append("\nVersion : ").append(Build.VERSION.RELEASE);
		text.append("\nApp Version : ").append(Version.getAppName(this));
		try {
			PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
			if (info != null) {
				text.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode);
			}
		} catch (PackageManager.NameNotFoundException e) {
			LOG.error("", e);
		}
		return text.toString();
	}
}
