package net.osmand.plus;

import static net.osmand.IndexConstants.ROUTING_FILE_EXT;
import static net.osmand.plus.settings.backend.ApplicationMode.valueOfStringKey;
import static net.osmand.shared.settings.enums.MetricsConstants.KILOMETERS_AND_METERS;
import static net.osmand.shared.settings.enums.MetricsConstants.MILES_AND_FEET;
import static net.osmand.shared.settings.enums.MetricsConstants.MILES_AND_METERS;

import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.app.ActivityCompat.OnRequestPermissionsResultCallback;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ProcessLifecycleOwner;
import androidx.multidex.MultiDex;
import androidx.multidex.MultiDexApplication;

import net.osmand.PlatformUtil;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.map.WorldRegion.RegionParams;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.RestartActivity;
import net.osmand.plus.api.SQLiteAPI;
import net.osmand.plus.api.SQLiteAPIImpl;
import net.osmand.plus.auto.NavigationCarAppService;
import net.osmand.plus.auto.NavigationSession;
import net.osmand.plus.auto.screens.NavigationScreen;
import net.osmand.plus.avoidroads.AvoidRoadsHelper;
import net.osmand.plus.backup.BackupHelper;
import net.osmand.plus.backup.NetworkSettingsHelper;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.base.dialog.DialogManager;
import net.osmand.plus.configmap.routes.RouteLayersHelper;
import net.osmand.plus.configmap.tracks.TrackSortModesHelper;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadService;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.exploreplaces.ExplorePlacesOnlineProvider;
import net.osmand.plus.exploreplaces.ExplorePlacesProvider;
import net.osmand.plus.feedback.AnalyticsHelper;
import net.osmand.plus.feedback.FeedbackHelper;
import net.osmand.plus.feedback.RateUsHelper;
import net.osmand.plus.feedback.RateUsState;
import net.osmand.plus.help.HelpArticlesHelper;
import net.osmand.plus.helpers.*;
import net.osmand.plus.importfiles.ImportHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.keyevent.InputDevicesHelper;
import net.osmand.plus.keyevent.KeyEventHelper;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.measurementtool.MeasurementEditingContext;
import net.osmand.plus.myplaces.favorites.FavouritesHelper;
import net.osmand.plus.notifications.NotificationHelper;
import net.osmand.plus.onlinerouting.OnlineRoutingHelper;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.plugins.PluginsHelper;
import net.osmand.plus.plugins.accessibility.AccessibilityMode;
import net.osmand.plus.plugins.accessibility.AccessibilityPlugin;
import net.osmand.plus.plugins.monitoring.LiveMonitoringHelper;
import net.osmand.plus.plugins.monitoring.SavingTrackHelper;
import net.osmand.plus.plugins.osmedit.oauth.OsmOAuthHelper;
import net.osmand.plus.plugins.rastermaps.DownloadTilesHelper;
import net.osmand.plus.plugins.weather.OfflineForecastHelper;
import net.osmand.plus.plugins.weather.WeatherHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.quickaction.MapButtonsHelper;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.render.TravelRendererHelper;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.plus.search.history.SearchHistoryHelper;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.OsmAndAppCustomization;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.backup.FileSettingsHelper;
import net.osmand.plus.settings.enums.DrivingRegion;
import net.osmand.plus.settings.enums.LocationSource;
import net.osmand.plus.shared.OsmAndContextImpl;
import net.osmand.plus.simulation.OsmAndLocationSimulation;
import net.osmand.plus.track.clickable.ClickableWayHelper;
import net.osmand.plus.track.helpers.GpsFilterHelper;
import net.osmand.plus.track.helpers.GpxDisplayHelper;
import net.osmand.plus.track.helpers.GpxSelectionHelper;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.views.OsmandMap;
import net.osmand.plus.views.PointImageUtils;
import net.osmand.plus.views.corenative.NativeCoreContext;
import net.osmand.plus.views.mapwidgets.utils.AverageGlideComputer;
import net.osmand.plus.views.mapwidgets.utils.AverageSpeedComputer;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.voice.VoiceProviderDialog;
import net.osmand.plus.wikivoyage.data.TravelHelper;
import net.osmand.router.GeneralRouter;
import net.osmand.router.RoutingConfiguration;
import net.osmand.router.RoutingConfiguration.Builder;
import net.osmand.search.SearchUICore;
import net.osmand.shared.gpx.GpxDbHelper;
import net.osmand.shared.gpx.RouteActivityHelper;
import net.osmand.shared.gpx.SmartFolderHelper;
import net.osmand.shared.io.KFile;
import net.osmand.shared.settings.enums.MetricsConstants;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import btools.routingapp.BRouterServiceConnection;
import btools.routingapp.IBRouterService;

public class OsmandApplication extends MultiDexApplication {

	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(OsmandApplication.class);

	final AppInitializer appInitializer = new AppInitializer(this);
	Handler uiHandler;
	OsmandSettings settings;
	OsmAndAppCustomization appCustomization;
	NavigationService navigationService;
	DownloadService downloadService;
	OsmandAidlApi aidlApi;

	NavigationCarAppService navigationCarAppService;
	NavigationSession carNavigationSession;
	OnRequestPermissionsResultCallback carAppPermissionListener;

	private final SQLiteAPI sqliteAPI = new SQLiteAPIImpl(this);
	private final OsmAndTaskManager taskManager = new OsmAndTaskManager(this);
	private final UiUtilities iconsCache = new UiUtilities(this);
	private final LocaleHelper localeHelper = new LocaleHelper(this);
	private final ToastHelper toastHelper = new ToastHelper(this);

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
	ColorPaletteHelper colorPaletteHelper;
	SavingTrackHelper savingTrackHelper;
	AnalyticsHelper analyticsHelper;
	FeedbackHelper feedbackHelper;
	NotificationHelper notificationHelper;
	LiveMonitoringHelper liveMonitoringHelper;
	TargetPointsHelper targetPointsHelper;
	MapMarkersHelper mapMarkersHelper;
	MapMarkersDbHelper mapMarkersDbHelper;
	WaypointHelper waypointHelper;
	RoutingOptionsHelper routingOptionsHelper;
	DownloadIndexesThread downloadIndexesThread;
	AvoidRoadsHelper avoidRoadsHelper;
	BRouterServiceConnection bRouterServiceConnection;
	OsmandRegions regions;
	GeocodingLookupService geocodingLookupService;
	QuickSearchHelper searchUICore;
	SearchHistoryHelper searchHistoryHelper;
	TravelHelper travelHelper;
	InAppPurchaseHelper inAppPurchaseHelper;
	MapViewTrackingUtilities mapViewTrackingUtilities;
	OsmandMap osmandMap;
	LockHelper lockHelper;
	KeyEventHelper keyEventHelper;
	InputDevicesHelper inputDeviceHelper;
	FileSettingsHelper fileSettingsHelper;
	NetworkSettingsHelper networkSettingsHelper;
	MapButtonsHelper mapButtonsHelper;
	OsmOAuthHelper osmOAuthHelper;
	MeasurementEditingContext measurementEditingContext;
	OnlineRoutingHelper onlineRoutingHelper;
	BackupHelper backupHelper;
	ImportHelper importHelper;
	TravelRendererHelper travelRendererHelper;
	LauncherShortcutsHelper launcherShortcutsHelper;
	GpsFilterHelper gpsFilterHelper;
	DownloadTilesHelper downloadTilesHelper;
	AverageSpeedComputer averageSpeedComputer;
	AverageGlideComputer averageGlideComputer;
	WeatherHelper weatherHelper;
	DialogManager dialogManager;
	RouteLayersHelper routeLayersHelper;
	Model3dHelper model3dHelper;
	TrackSortModesHelper trackSortModesHelper;
	ExplorePlacesOnlineProvider explorePlacesProvider;
	HelpArticlesHelper helpArticlesHelper;
	ClickableWayHelper clickableWayHelper;

	private final Map<String, Builder> customRoutingConfigs = new ConcurrentHashMap<>();
	private File externalStorageDirectory;
	private boolean externalStorageDirectoryReadOnly;
	private boolean appInForeground;
	private boolean androidAutoInForeground;
	private float density = 0f;
	// Typeface

	@Override
	public void onCreate() {
		if (RestartActivity.isRestartProcess(this)) {
			return;
		}
		long timeToStart = System.currentTimeMillis();
		enableStrictMode();
		super.onCreate();

		LifecycleObserver appLifecycleObserver = new DefaultLifecycleObserver() {
			@Override
			public void onStart(@NonNull LifecycleOwner owner) {
				appInForeground = true;
			}

			@Override
			public void onStop(@NonNull LifecycleOwner owner) {
				appInForeground = false;
			}
		};
		ProcessLifecycleOwner.get().getLifecycle().addObserver(appLifecycleObserver);

		createInUiThread();
		uiHandler = new Handler();
		appCustomization = new OsmAndAppCustomization();
		appCustomization.setup(this);
		settings = appCustomization.getOsmandSettings();
		appInitializer.initVariables();
		if (appInitializer.isAppVersionChanged() && appInitializer.getPrevAppVersion() < AppVersionUpgradeOnInit.VERSION_2_3) {
			settings.freezeExternalStorageDirectory();
		} else if (appInitializer.isFirstTime()) {
			settings.initExternalStorageDirectory();
		}
		externalStorageDirectory = settings.getExternalStorageDirectory();
		if (!FileUtils.isWritable(externalStorageDirectory)) {
			externalStorageDirectoryReadOnly = true;
			externalStorageDirectory = settings.getInternalAppPath();
		}
		FileUtils.removeUnnecessaryFiles(this);

		// Initialize shared library
		net.osmand.shared.util.PlatformUtil.INSTANCE.initialize(this, new OsmAndContextImpl(this));

		localeHelper.checkPreferredLocale();
		appInitializer.onCreateApplication();
		osmandMap.getMapLayers().createLayers(osmandMap.getMapView());
		startApplication();
		System.out.println("Time to start application " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms");

		timeToStart = System.currentTimeMillis();
		PluginsHelper.initPlugins(this);
		PluginsHelper.createLayers(this, null);
		System.out.println("Time to init plugins " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms");

		osmandMap.getMapLayers().updateLayers(null);

		if (appInitializer.isFirstTime()) {
			settings.resetLastGlobalPreferencesEditTime();
			settings.resetLastPreferencesEditTime(settings.APPLICATION_MODE.get());
		}

		SearchUICore.setDebugMode(PluginsHelper.isDevelopment());
		BackupHelper.DEBUG = true;//PluginsHelper.isDevelopment();
	}

	public boolean isPlusVersionInApp() {
		return true;
	}

	public boolean isExternalStorageDirectoryReadOnly() {
		return externalStorageDirectoryReadOnly;
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

	public boolean isAppInForeground() {
		return appInForeground || androidAutoInForeground;
	}

	public boolean isAppInForegroundOnRootDevice() {
		return appInForeground;
	}

	public boolean isAppInForegroundOnAndroidAuto() {
		return androidAutoInForeground;
	}

	private void createInUiThread() {
		new Toast(AndroidUtils.createDisplayContext(this)); // activate in UI thread to avoid further exceptions
		OsmAndTaskManager.executeTask(new AsyncTask<View, Void, Void>() {
			@Override
			protected Void doInBackground(View... params) {
				return null;
			}

			protected void onPostExecute(Void result) {
			}
		});
	}

	@NonNull
	public Handler getUiHandler() {
		return uiHandler;
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
			settings.RATE_US_STATE.set(RateUsState.IGNORED);
		}
		getNotificationHelper().removeNotifications(false);
	}

	public RendererRegistry getRendererRegistry() {
		return rendererRegistry;
	}

	public OsmAndTaskManager getTaskManager() {
		return taskManager;
	}

	public AvoidRoadsHelper getAvoidSpecificRoads() {
		return avoidRoadsHelper;
	}

	public OsmAndLocationProvider getLocationProvider() {
		return locationProvider;
	}

	public OsmAndAppCustomization getAppCustomization() {
		return appCustomization;
	}

	public MapButtonsHelper getMapButtonsHelper() {
		return mapButtonsHelper;
	}

	public LocationServiceHelper createLocationServiceHelper() {
		LocationSource source = settings.LOCATION_SOURCE.get();
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
		if (settings == null) {
			LOG.error("Trying to access settings before they were created");
		}
		return settings;
	}

	public void setSettings(OsmandSettings settings) {
		this.settings = settings;
		PluginsHelper.initPlugins(this);
	}

	public SavingTrackHelper getSavingTrackHelper() {
		return savingTrackHelper;
	}

	public AnalyticsHelper getAnalyticsHelper() {
		return analyticsHelper;
	}

	public FeedbackHelper getFeedbackHelper() {
		return feedbackHelper;
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
		return GpxDbHelper.INSTANCE;
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

	public KeyEventHelper getKeyEventHelper() {
		return keyEventHelper;
	}

	public InputDevicesHelper getInputDeviceHelper() {
		return inputDeviceHelper;
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
		Resources resources = getResources();
		resources.updateConfiguration(newConfig, resources.getDisplayMetrics());

		resources = getBaseContext().getResources();
		resources.updateConfiguration(newConfig, resources.getDisplayMetrics());

		DisplayMetrics displayMetrics = resources.getDisplayMetrics();
		if (density != displayMetrics.density) {
			density = displayMetrics.density;
			getUIUtilities().clearCache();
			PointImageUtils.clearCache();

			OsmandMap osmandMap = getOsmandMap();
			if (osmandMap != null) {
				osmandMap.getMapView().updateDisplayMetrics(displayMetrics, displayMetrics.widthPixels,
						displayMetrics.heightPixels - AndroidUtils.getStatusBarHeight(this));
			}
		}

		Locale preferredLocale = localeHelper.getPreferredLocale();
		if (preferredLocale != null && !Objects.equals(newConfig.locale.getLanguage(), preferredLocale.getLanguage())) {
			super.onConfigurationChanged(newConfig);
			Locale.setDefault(preferredLocale);
		} else {
			super.onConfigurationChanged(newConfig);
		}
	}

	public void checkApplicationIsBeingInitialized(@Nullable AppInitializeListener listener) {
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

	public ColorPaletteHelper getColorPaletteHelper() {
		return colorPaletteHelper;
	}

	public BackupHelper getBackupHelper() {
		return backupHelper;
	}

	public ImportHelper getImportHelper() {
		return importHelper;
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

	public SearchHistoryHelper getSearchHistoryHelper() {
		return searchHistoryHelper;
	}

	public TravelHelper getTravelHelper() {
		return travelHelper;
	}

	public ClickableWayHelper getClickableWayHelper() {
		return clickableWayHelper;
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

	public ExplorePlacesProvider getExplorePlacesProvider() {
		return explorePlacesProvider;
	}

	@NonNull
	public AverageGlideComputer getAverageGlideComputer() {
		return averageGlideComputer;
	}

	@NonNull
	public WeatherHelper getWeatherHelper() {
		return weatherHelper;
	}

	@NonNull
	public DialogManager getDialogManager() {
		return dialogManager;
	}

	@NonNull
	public SmartFolderHelper getSmartFolderHelper() {
		return SmartFolderHelper.INSTANCE;
	}

	@NonNull
	public RouteLayersHelper getRouteLayersHelper() {
		return routeLayersHelper;
	}

	@NonNull
	public RouteActivityHelper getRouteActivityHelper() {
		return RouteActivityHelper.INSTANCE;
	}

	@NonNull
	public OfflineForecastHelper getOfflineForecastHelper() {
		return weatherHelper.getOfflineForecastHelper();
	}

	@NonNull
	public Model3dHelper getModel3dHelper() {
		return model3dHelper;
	}

	@NonNull
	public TrackSortModesHelper getTrackSortModesHelper() {
		return trackSortModesHelper;
	}

	@NonNull
	public HelpArticlesHelper getHelpArticlesHelper() {
		return helpArticlesHelper;
	}

	public CommandPlayer getPlayer() {
		return player;
	}

	public void initVoiceCommandPlayer(@NonNull Context context, @NonNull ApplicationMode appMode,
			@Nullable Runnable onCommandPlayerCreated, boolean warnNoProvider,
			boolean showProgress, boolean forceInitialization, boolean applyAllModes) {
		String voiceProvider = settings.VOICE_PROVIDER.getModeValue(appMode);
		if (OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(voiceProvider)) {
			settings.VOICE_MUTE.setModeValue(appMode, true);
		} else if (Algorithms.isEmpty(voiceProvider)) {
			if (warnNoProvider && context instanceof MapActivity) {
				VoiceProviderDialog.showVoiceProviderDialog((MapActivity) context, appMode, applyAllModes);
			}
		} else {
			if (player == null || !voiceProvider.equals(player.getCurrentVoice()) || forceInitialization) {
				appInitializer.initVoiceDataInDifferentThread(context, appMode, voiceProvider, onCommandPlayerCreated, showProgress);
			}
		}
	}

	@Nullable
	public NavigationService getNavigationService() {
		return navigationService;
	}

	public void setNavigationService(NavigationService navigationService) {
		this.navigationService = navigationService;
	}

	@Nullable
	public NavigationCarAppService getNavigationCarAppService() {
		return navigationCarAppService;
	}

	public void setNavigationCarAppService(@Nullable NavigationCarAppService carAppService) {
		this.navigationCarAppService = carAppService;
	}

	@Nullable
	public OnRequestPermissionsResultCallback getCarAppPermissionListener() {
		return carAppPermissionListener;
	}

	public void setCarAppPermissionListener(@Nullable OnRequestPermissionsResultCallback callback) {
		this.carAppPermissionListener = callback;
	}

	@Nullable
	public NavigationSession getCarNavigationSession() {
		return carNavigationSession;
	}

	public void onCarNavigationSessionStart(@NonNull NavigationSession carNavigationSession) {
		androidAutoInForeground = true;
	}

	public void onCarNavigationSessionStop(@NonNull NavigationSession carNavigationSession) {
		androidAutoInForeground = false;
	}

	public void setCarNavigationSession(@Nullable NavigationSession carNavigationSession) {
		this.carNavigationSession = carNavigationSession;
		if (carNavigationSession != null) {
			List<OsmandPlugin> enabledPlugins = PluginsHelper.getEnabledPlugins();
			for (OsmandPlugin plugin : enabledPlugins) {
				plugin.onCarNavigationSessionCreated();
			}
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

	@Nullable
	public DownloadService getDownloadService() {
		return downloadService;
	}

	public void setDownloadService(@Nullable DownloadService downloadService) {
		this.downloadService = downloadService;
	}

	public OsmandAidlApi getAidlApi() {
		return aidlApi;
	}

	public void stopNavigation() {
		OsmAndLocationSimulation locationSimulation = locationProvider.getLocationSimulation();
		if (locationSimulation.isRouteAnimating() || locationSimulation.isLoadingRouteLocations()) {
			locationSimulation.stop();
		}
		routingHelper.getVoiceRouter().interruptRouteCommands();
		routingHelper.clearCurrentRoute(null, new ArrayList<LatLon>());
		routingHelper.setRoutePlanningMode(false);
		settings.LAST_ROUTING_APPLICATION_MODE = settings.APPLICATION_MODE.get();
		settings.setApplicationMode(valueOfStringKey(settings.LAST_USED_APPLICATION_MODE.get(), ApplicationMode.DEFAULT));
		targetPointsHelper.removeAllWayPoints(false, false);
	}

	public void startApplication() {
		feedbackHelper.setExceptionHandler();
		if (NetworkUtils.getProxy() == null && settings.isProxyEnabled()) {
			try {
				NetworkUtils.setProxy(settings.PROXY_HOST.get(), settings.PROXY_PORT.get());
			} catch (RuntimeException e) {
				showToastMessage(e.getMessage());
			}
		}
		appInitializer.startApplication();
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

	public ToastHelper getToastHelper() {
		return toastHelper;
	}

	public void showToastMessage(@Nullable String text) {
		toastHelper.showToast(text, true);
	}

	public void showToastMessage(@StringRes int textId, Object... args) {
		toastHelper.showToast(textId, true, args);
	}

	public void showShortToastMessage(@Nullable String text) {
		toastHelper.showToast(text, false);
	}

	public void showShortToastMessage(@StringRes int textId, Object... args) {
		toastHelper.showToast(textId, false, args);
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

	public void runInUIThreadAndCancelPrevious(int messageId, Runnable run, long delay) {
		Message msg = Message.obtain(uiHandler, () -> {
			if (!uiHandler.hasMessages(messageId)) {
				run.run();
			}
		});
		msg.what = messageId;
		uiHandler.removeMessages(messageId);
		uiHandler.sendMessageDelayed(msg, delay);
	}

	public void runMessageInUiThread(int messageId, long delay, @NonNull Runnable runnable) {
		Message message = Message.obtain(uiHandler, runnable);
		message.what = messageId;
		uiHandler.sendMessageDelayed(message, delay);
	}

	public boolean hasMessagesInUiThread(int messageId) {
		return uiHandler.hasMessages(messageId);
	}

	public void removeMessagesInUiThread(int messageId) {
		uiHandler.removeMessages(messageId);
	}

	@NonNull
	public File getAppPath(@Nullable String path) {
		String child = path != null ? path : "";
		return new File(externalStorageDirectory, child);
	}

	@NonNull
	public KFile getAppPathKt(@Nullable String path) {
		String child = path != null ? path : "";
		return new KFile(new KFile(externalStorageDirectory.getPath()), child);
	}

	@NonNull
	public File getAppInternalPath(@Nullable String path) {
		String child = path != null ? path : "";
		return new File(settings.getInternalAppPath(), child);
	}

	public void setExternalStorageDirectory(int type, String directory) {
		settings.setExternalStorageDirectory(type, directory);
		externalStorageDirectory = settings.getExternalStorageDirectory();
		externalStorageDirectoryReadOnly = false;
		getResourceManager().resetStoreDirectory();
	}

	public void applyTheme(@NonNull Context context) {
		int themeId;
		boolean noAnimation = settings.DO_NOT_USE_ANIMATIONS.get();
		if (!settings.isLightContent()) {
			themeId = noAnimation ? R.style.OsmandDarkTheme_NoAnimation : R.style.OsmandDarkTheme;
		} else {
			themeId = noAnimation ? R.style.OsmandLightTheme_NoAnimation : R.style.OsmandLightTheme;
		}
		localeHelper.setLanguage(context);
		context.setTheme(themeId);
	}

	@Nullable
	public synchronized IBRouterService reconnectToBRouter() {
		try {
			bRouterServiceConnection = BRouterServiceConnection.connect(this);
			// a delay is necessary as the service process needs time to start..
			Thread.sleep(800);
			if (bRouterServiceConnection != null) {
				return bRouterServiceConnection.getBrouterService();
			}
		} catch (Exception e) {
			LOG.error(e);
		}
		return null;
	}

	@Nullable
	public synchronized IBRouterService getBRouterService() {
		if (bRouterServiceConnection == null) {
			return null;
		}
		IBRouterService service = bRouterServiceConnection.getBrouterService();
		if (service != null && !service.asBinder().isBinderAlive()) {
			service = reconnectToBRouter();
		}
		return service;
	}

	@NonNull
	public String getLanguage() {
		String appLang = localeHelper.getLanguage();
		// assume english is default though it's not correct
		if (Algorithms.isEmpty(appLang)) {
			appLang = "en";
		}
		return appLang;
	}

	@Override
	public AssetManager getAssets() {
		return getResources() != null ? getResources().getAssets() : super.getAssets();
	}

	@Override
	public Resources getResources() {
		OsmandMap map = getOsmandMap();
		MapActivity a = null;
		if (map != null) {
			a = map.getMapView().getMapActivity();
		}
		Context mainContext = a == null ? this : a;
		Resources mainResources = a == null ? super.getResources() : a.getResources();
		Resources localizedResources = localeHelper.getLocalizedResources(mainContext, mainResources);
		return localizedResources != null ? localizedResources : mainResources;
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
		AccessibilityMode mode = getSettings().ACCESSIBILITY_MODE.getModeValue(appMode);
		if (!PluginsHelper.isActive(AccessibilityPlugin.class)) {
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

	public void startNavigationService(int usageIntent) {
		startNavigationService(this, usageIntent);
	}

	public void startNavigationService(@NonNull Context context, int usageIntent) {
		LOG.info(">>>> APP startNavigationService = " + usageIntent);
		Intent intent = new Intent(context, NavigationService.class);
		intent.putExtra(NavigationService.USAGE_INTENT, usageIntent);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			runInUIThread(() -> {
				try {
					if (isAppInForeground()) {
						LOG.info(">>>> APP startForegroundService = " + usageIntent);
						context.startForegroundService(intent);
					}
				} catch (IllegalStateException e) {
					LOG.error("Failed to start foreground service: " + e.getMessage(), e);
				}
			});
		} else {
			context.startService(intent);
		}
	}

	public void setupDrivingRegion(@NonNull WorldRegion worldRegion) {
		DrivingRegion drivingRegion = getDrivingRegion(worldRegion.getParams());
		if (drivingRegion != null) {
			settings.executePreservingPrefTimestamp(() -> settings.DRIVING_REGION.set(drivingRegion));
		}
	}

	@Nullable
	private DrivingRegion getDrivingRegion(@NonNull RegionParams params) {
//		boolean americanSigns = "american".equals(params.getRegionRoadSigns());
		boolean leftHand = "yes".equals(params.getRegionLeftHandDriving());
		MetricsConstants mc1 = "miles".equals(params.getRegionMetric()) ? MILES_AND_FEET : KILOMETERS_AND_METERS;
		MetricsConstants mc2 = "miles".equals(params.getRegionMetric()) ? MILES_AND_METERS : KILOMETERS_AND_METERS;
		for (DrivingRegion region : DrivingRegion.values()) {
			if (region.leftHandDriving == leftHand && (region.defMetrics == mc1 || region.defMetrics == mc2)) {
				return region;
			}
		}
		return null;
	}

	@NonNull
	public String getUserAndroidId() {
		String userAndroidId = settings.USER_ANDROID_ID.get();
		if (Algorithms.isEmpty(userAndroidId) || isUserAndroidIdExpired()) {
			userAndroidId = UUID.randomUUID().toString();
			settings.USER_ANDROID_ID.set(userAndroidId);
			Calendar calendar = Calendar.getInstance();
			calendar.add(Calendar.MONTH, 3);
			settings.USER_ANDROID_ID_EXPIRED_TIME.set(calendar.getTimeInMillis());
		}
		return userAndroidId;
	}

	public boolean isUserAndroidIdExpired() {
		long expiredTime = settings.USER_ANDROID_ID_EXPIRED_TIME.get();
		return expiredTime <= 0 || expiredTime <= System.currentTimeMillis();
	}

	public boolean isUserAndroidIdAllowed() {
		return settings.SEND_UNIQUE_USER_IDENTIFIER.get();
	}

	public void logEvent(@NonNull String event) {
		try {
			analyticsHelper.addEvent(event, AnalyticsHelper.EVENT_TYPE_APP_USAGE);
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	public void logRoutingEvent(@NonNull String event) {
		try {
			analyticsHelper.addEvent(event, AnalyticsHelper.EVENT_TYPE_ROUTING);
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	public void logMapDownloadEvent(@NonNull String event, @NonNull IndexItem item) {
		try {
			analyticsHelper.addEvent("map_download_" + event + ": " + item.getFileName(), AnalyticsHelper.EVENT_TYPE_MAP_DOWNLOAD);
		} catch (Exception e) {
			LOG.error(e);
		}
	}

	public void logMapDownloadEvent(@NonNull String event, @NonNull IndexItem item, long time) {
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

	public boolean useOpenGlRenderer() {
		return NativeCoreContext.isInit() && settings.USE_OPENGL_RENDER.get();
	}

	private void enableStrictMode() {
		if (Version.isDeveloperVersion(this)) {
			try {
				Class.forName("net.osmand.plus.base.EnableStrictMode").newInstance();
			} catch (Exception e) {
				LOG.error(e);
			}
		}
	}

	public void reInitPoiTypes() {
		appInitializer.reInitPoiTypes();
	}
}
