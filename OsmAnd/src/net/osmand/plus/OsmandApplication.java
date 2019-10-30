package net.osmand.plus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.AndroidUtils;
import net.osmand.CallbackWithObject;
import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibilityPlugin;
import net.osmand.aidl.OsmandAidlApi;
import net.osmand.data.LatLon;
import net.osmand.map.OsmandRegions;
import net.osmand.map.WorldRegion;
import net.osmand.osm.MapPoiTypes;
import net.osmand.osm.io.NetworkUtils;
import net.osmand.plus.AppInitializer.AppInitializeListener;
import net.osmand.plus.access.AccessibilityMode;
import net.osmand.plus.activities.DayNightHelper;
import net.osmand.plus.activities.ExitActivity;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.api.SQLiteAPI;
import net.osmand.plus.api.SQLiteAPIImpl;
import net.osmand.plus.base.MapViewTrackingUtilities;
import net.osmand.plus.dialogs.CrashBottomSheetDialogFragment;
import net.osmand.plus.dialogs.RateUsBottomSheetDialog;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.download.DownloadService;
import net.osmand.plus.download.IndexItem;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.LockHelper;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.inapp.InAppPurchaseHelper;
import net.osmand.plus.mapcontextmenu.other.RoutePreferencesMenu;
import net.osmand.plus.mapmarkers.MapMarkersDbHelper;
import net.osmand.plus.monitoring.LiveMonitoringHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routepreparationmenu.RoutingOptionsHelper;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.routing.TransportRoutingHelper;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.wikivoyage.data.TravelDbHelper;
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
import java.util.Locale;
import java.util.UUID;

import btools.routingapp.BRouterServiceConnection;
import btools.routingapp.IBRouterService;

public class OsmandApplication extends MultiDexApplication {
	public static final String EXCEPTION_PATH = "exception.log";
	public static final String OSMAND_PRIVACY_POLICY_URL = "https://osmand.net/help-online/privacy-policy";
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(OsmandApplication.class);

	final AppInitializer appInitializer = new AppInitializer(this);
	OsmandSettings osmandSettings = null;
	OsmAndAppCustomization appCustomization;
	private final SQLiteAPI sqliteAPI = new SQLiteAPIImpl(this);
	private final OsmAndTaskManager taskManager = new OsmAndTaskManager(this);
	private final UiUtilities iconsCache = new UiUtilities(this);
	Handler uiHandler;

	NavigationService navigationService;
	DownloadService downloadService;

	OsmandAidlApi aidlApi;

	// start variables
	ResourceManager resourceManager;
	OsmAndLocationProvider locationProvider;
	RendererRegistry rendererRegistry;
	DayNightHelper daynightHelper;
	PoiFiltersHelper poiFilters;
	MapPoiTypes poiTypes;
	RoutingHelper routingHelper;
	TransportRoutingHelper transportRoutingHelper;
	FavouritesDbHelper favorites;
	CommandPlayer player;
	GpxSelectionHelper selectedGpxHelper;
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
	BRouterServiceConnection bRouterServiceConnection;
	OsmandRegions regions;
	GeocodingLookupService geocodingLookupService;
	QuickSearchHelper searchUICore;
	TravelDbHelper travelDbHelper;
	InAppPurchaseHelper inAppPurchaseHelper;
	MapViewTrackingUtilities mapViewTrackingUtilities;
	LockHelper lockHelper;
	SettingsHelper settingsHelper;
	GpxDbHelper gpxDbHelper;

	private RoutingConfiguration.Builder routingConfig;
	private Locale preferredLocale = null;
	private Locale defaultLocale;
	private File externalStorageDirectory;
	private boolean externalStorageDirectoryReadOnly;

	private String firstSelectedVoiceProvider;
	
	// Typeface
	
	@Override
	public void onCreate() {
		long timeToStart = System.currentTimeMillis();
		if (Version.isDeveloperVersion(this)) {
			if (android.os.Build.VERSION.SDK_INT >= 9) {
				try {
					Class.forName("net.osmand.plus.base.EnableStrictMode").newInstance();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
		super.onCreate();
		createInUiThread();
		uiHandler = new Handler();
		appCustomization = new OsmAndAppCustomization();
		appCustomization.setup(this);
		osmandSettings = appCustomization.getOsmandSettings();
		appInitializer.initVariables();
		if (appInitializer.isAppVersionChanged() && appInitializer.getPrevAppVersion() < AppInitializer.VERSION_2_3) {
			osmandSettings.freezeExternalStorageDirectory();
		} else if (appInitializer.isFirstTime()) {
			osmandSettings.initExternalStorageDirectory();
		}
		externalStorageDirectory = osmandSettings.getExternalStorageDirectory();
		if (!OsmandSettings.isWritable(externalStorageDirectory)) {
			externalStorageDirectoryReadOnly = true;
			externalStorageDirectory = osmandSettings.getInternalAppPath();
		}

		Algorithms.removeAllFiles(this.getAppPath(IndexConstants.TEMP_DIR));

		checkPreferredLocale();
		appInitializer.onCreateApplication();
//		if(!osmandSettings.FOLLOW_THE_ROUTE.get()) {
//			targetPointsHelper.clearPointToNavigate(false);
//		}
		startApplication();
		System.out.println("Time to start application " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms");
		timeToStart = System.currentTimeMillis();
		OsmandPlugin.initPlugins(this);
		System.out.println("Time to init plugins " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms");

		SearchUICore.setDebugMode(OsmandPlugin.isDevelopment());
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
	
	public UiUtilities getUIUtilities() {
		return iconsCache;
	}
	
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		if (routingHelper != null) {
			routingHelper.getVoiceRouter().onApplicationTerminate();
		}
        if(RateUsBottomSheetDialog.shouldShow(this)) {
            osmandSettings.RATE_US_STATE.set(RateUsBottomSheetDialog.RateUsState.IGNORED);
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

	public OsmAndLocationProvider getLocationProvider() {
		return locationProvider;
	}
	
	public OsmAndAppCustomization getAppCustomization() {
		return appCustomization;
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
		//android.os.Process.killProcess(android.os.Process.myPid());
		this.osmandSettings = osmandSettings;
		resourceManager.getRenderer().updateSettings();
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

	public GpxDbHelper getGpxDbHelper() {
		return gpxDbHelper;
	}

	public FavouritesDbHelper getFavorites() {
		return favorites;
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

	public SettingsHelper getSettingsHelper() {
		return settingsHelper;
	}

	public synchronized DownloadIndexesThread getDownloadThread() {
		if(downloadIndexesThread == null) {
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
	public void onConfigurationChanged(Configuration newConfig) {
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

	public void checkPreferredLocale() {
		Configuration config = getBaseContext().getResources().getConfiguration();

		String pl = osmandSettings.PREFERRED_LOCALE.get();
		String[] split = pl.split("_");
		String lang = split[0];
		String country = (split.length > 1) ? split[1] : "";

		if(defaultLocale == null) {
			defaultLocale = Locale.getDefault();
		}
		if (!"".equals(lang) && !config.locale.equals(pl)) {
			if (!"".equals(country)) {
				preferredLocale = new Locale(lang, country);
			} else {
				preferredLocale = new Locale(lang);
			}
			Locale.setDefault(preferredLocale);
			config.locale = preferredLocale;
			getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
		} else if("".equals(lang) && defaultLocale != null && Locale.getDefault() != defaultLocale) {
			Locale.setDefault(defaultLocale);
			config.locale = defaultLocale;
			preferredLocale = null;
			getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
		}
		
	}

	public static final int PROGRESS_DIALOG = 5;

	public void checkApplicationIsBeingInitialized(Activity activity, AppInitializeListener listener) {
		// start application if it was previously closed
		startApplication();
		if(listener != null) {
			appInitializer.addListener(listener);
		}
	}
	
	public void unsubscribeInitListener(AppInitializeListener listener) {
		if(listener != null) {
			appInitializer.removeListener(listener);
		}		
	}
	
	public boolean isApplicationInitializing() {
		return appInitializer.isAppInitializing();
	}

	public RoutingHelper getRoutingHelper() {
		return routingHelper;
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

	public TravelDbHelper getTravelDbHelper() {
		return travelDbHelper;
	}

	public InAppPurchaseHelper getInAppPurchaseHelper() {
		return inAppPurchaseHelper;
	}

	public CommandPlayer getPlayer() {
		return player;
	}

	public void initVoiceCommandPlayer(final Activity uiContext, final ApplicationMode applicationMode,
	                                   boolean warningNoneProvider, Runnable run, boolean showDialog, boolean force) {
		String voiceProvider = osmandSettings.VOICE_PROVIDER.getModeValue(applicationMode);
		if (voiceProvider == null || OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(voiceProvider)) {
			if (warningNoneProvider && voiceProvider == null) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(uiContext);

				View view = uiContext.getLayoutInflater().inflate(R.layout.select_voice_first, null);

				((ImageView) view.findViewById(R.id.icon))
						.setImageDrawable(getUIUtilities().getIcon(R.drawable.ic_action_volume_up, getSettings().isLightContent()));

				view.findViewById(R.id.spinner).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(final View v) {
						routingOptionsHelper.selectVoiceGuidance((MapActivity) uiContext, new CallbackWithObject<String>() {
							@Override
							public boolean processResult(String result) {
								boolean acceptableValue = !RoutePreferencesMenu.MORE_VALUE.equals(firstSelectedVoiceProvider);
								if (acceptableValue) {
									((TextView) v.findViewById(R.id.selectText))
											.setText(routingOptionsHelper.getVoiceProviderName(uiContext, result));
									firstSelectedVoiceProvider = result;
								}
								return acceptableValue;
							}
						});
					}
				});

				((ImageView) view.findViewById(R.id.dropDownIcon))
						.setImageDrawable(getUIUtilities().getIcon(R.drawable.ic_action_arrow_drop_down, getSettings().isLightContent()));

				builder.setCancelable(true);
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (!Algorithms.isEmpty(firstSelectedVoiceProvider)) {
							routingOptionsHelper.applyVoiceProvider((MapActivity) uiContext, firstSelectedVoiceProvider);
						}
					}
				});
				builder.setNeutralButton(R.string.shared_string_do_not_use, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialogInterface, int i) {
						osmandSettings.VOICE_PROVIDER.setModeValue(applicationMode, OsmandSettings.VOICE_PROVIDER_NOT_USE);
					}
				});

				builder.setView(view);
				builder.show();
			}

		} else {
			if (player == null || !Algorithms.objectEquals(voiceProvider, player.getCurrentVoice()) || force) {
				appInitializer.initVoiceDataInDifferentThread(uiContext, applicationMode, voiceProvider, run, showDialog);
			}
		}

	}

	public NavigationService getNavigationService() {
		return navigationService;
	}

	public void setNavigationService(NavigationService navigationService) {
		this.navigationService = navigationService;
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
		osmandSettings.APPLICATION_MODE.set(osmandSettings.DEFAULT_APPLICATION_MODE.get());
		targetPointsHelper.removeAllWayPoints(false, false);
	}

	private void fullExit() {
		// http://stackoverflow.com/questions/2092951/how-to-close-android-application
		System.runFinalizersOnExit(true);
		System.exit(0);
	}

	public synchronized void closeApplication(final Activity activity) {
		if (getNavigationService() != null) {
			AlertDialog.Builder bld = new AlertDialog.Builder(activity);
			bld.setMessage(R.string.background_service_is_enabled_question);
			bld.setPositiveButton(R.string.shared_string_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					closeApplicationAnywayImpl(activity, true);
				}
			});
			bld.setNegativeButton(R.string.shared_string_no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					closeApplicationAnywayImpl(activity, false);
				}
			});
			bld.show();
		} else {
			closeApplicationAnywayImpl(activity, true);
		}
	}
	
	private void closeApplicationAnyway(final Activity activity, boolean disableService) {
		activity.finish();
		Intent newIntent = new Intent(activity, ExitActivity.class);
		newIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
		newIntent.putExtra(ExitActivity.DISABLE_SERVICE, disableService);
		startActivity(newIntent);
	}

	public void closeApplicationAnywayImpl(final Activity activity, boolean disableService) {
		if (appInitializer.isAppInitializing()) {
			resourceManager.close();
		}
		activity.finish();
		if (getNavigationService() == null) {
			fullExit();
		} else if (disableService) {
			final Intent serviceIntent = new Intent(this, NavigationService.class);
			stopService(serviceIntent);

			new Thread(new Runnable() {
				public void run() {
					//wait until the service has fully stopped
					while (getNavigationService() != null) {
						try {
							Thread.sleep(100);
						}
							catch (InterruptedException e) {
						}
					}

					fullExit();
				}
			}).start();
		}
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

		private UncaughtExceptionHandler defaultHandler;
		private PendingIntent intent;

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
						.append(new String(out.toByteArray()));

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
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(OsmandApplication.this, getString(msgId, args), Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void showShortToastMessage(final String msg) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(OsmandApplication.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void showToastMessage(final int msgId, final Object... args) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(OsmandApplication.this, getString(msgId, args), Toast.LENGTH_LONG).show();
			}
		});
	}

	public void showToastMessage(final String msg) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(OsmandApplication.this, msg, Toast.LENGTH_LONG).show();				
			}
		});
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
				if(!uiHandler.hasMessages(messageId)) {
					run.run();
				}
			}
		});
		msg.what = messageId;
		uiHandler.removeMessages(messageId);
		uiHandler.sendMessageDelayed(msg, delay);
	}
	
	public File getAppPath(String path) {
		if(path == null) {
			path = "";
		}
		return new File(externalStorageDirectory, path);
	}
	
	public void setExternalStorageDirectory(int type, String directory){
		osmandSettings.setExternalStorageDirectory(type, directory);
		externalStorageDirectory = osmandSettings.getExternalStorageDirectory();
		externalStorageDirectoryReadOnly = false;
		getResourceManager().resetStoreDirectory();
	}

	public void applyTheme(Context c) {
		int t = R.style.OsmandDarkTheme;
		boolean doNotUseAnimations = osmandSettings.DO_NOT_USE_ANIMATIONS.get();
		if (osmandSettings.OSMAND_THEME.get() == OsmandSettings.OSMAND_DARK_THEME) {
			if (doNotUseAnimations) {
				t = R.style.OsmandDarkTheme_NoAnimation;
			} else {
				t = R.style.OsmandDarkTheme;
			}
		} else if (osmandSettings.OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME) {
			if (doNotUseAnimations) {
				t = R.style.OsmandLightTheme_NoAnimation;
			} else {
				t = R.style.OsmandLightTheme;
			}
		}
		setLanguage(c);
		c.setTheme(t);
	}
	
	public IBRouterService getBRouterService() {
		if(bRouterServiceConnection == null) {
			return null;
		}
		return bRouterServiceConnection.getBrouterService();
	}
	
	public void setLanguage(Context context) {
		if (preferredLocale != null) {
			Configuration config = context.getResources().getConfiguration();
			String lang = preferredLocale.getLanguage();
			if (!"".equals(lang) && !config.locale.getLanguage().equals(lang)) {
				preferredLocale = new Locale(lang);
				Locale.setDefault(preferredLocale);
				config.locale = preferredLocale;
				context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
			} else if("".equals(lang) && defaultLocale != null && Locale.getDefault() != defaultLocale) {
				Locale.setDefault(defaultLocale);
				config.locale = defaultLocale;
				getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
			}
		}
	}
	
	public String getLanguage() {
		String lang;
		if (preferredLocale != null) {
			lang = preferredLocale.getLanguage();
		} else {
			lang = Locale.getDefault().getLanguage();
		}
		if (lang != null && lang.length() > 3) {
			lang = lang.substring(0, 2).toLowerCase();
		}
		return lang;
	}
	
	public synchronized RoutingConfiguration.Builder getRoutingConfig() {
		RoutingConfiguration.Builder rc;
		if(routingConfig == null) {
			rc = new RoutingConfiguration.Builder();
		} else {
			rc = routingConfig;
		}
		return rc;
	}

	public void updateRoutingConfig(Builder update) {
			routingConfig = update;
	}

	public OsmandRegions getRegions() {
		return regions;
	}
	
	public boolean accessibilityEnabled() {
		final AccessibilityMode mode = getSettings().ACCESSIBILITY_MODE.get();
		if(OsmandPlugin.getEnabledPlugin(AccessibilityPlugin.class) == null) {
			return false;
		}
		if (mode == AccessibilityMode.ON) {
			return true;
		} else if (mode == AccessibilityMode.OFF) {
			return false;
		}
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


	public int navigationServiceGpsInterval(int interval) {
		// Issue 5632 Workaround: Keep GPS always on instead of using AlarmManager, as API>=19 restricts repeated AlarmManager reception
		// Maybe do not apply to API=19 devices, many still behave acceptably (often restriction not worse than 1/min)
		if ((Build.VERSION.SDK_INT > 19) && (getSettings().SAVE_GLOBAL_TRACK_INTERVAL.get() < 5 * 60000)) {
			return 0;
		}
		// Default: Save battery power by turning off GPS between measurements
		if (interval >= 30000) {
			return interval;
		// GPS continuous
		} else {
			return 0;
		}
	}


	public void startNavigationService(int intent, int interval) {
		final Intent serviceIntent = new Intent(this, NavigationService.class);
		
		if (getNavigationService() != null) {
			intent |= getNavigationService().getUsedBy();
			interval = Math.min(getNavigationService().getServiceOffInterval(), interval);
			getNavigationService().stopSelf();
			
		}
		serviceIntent.putExtra(NavigationService.USAGE_INTENT, intent);
		serviceIntent.putExtra(NavigationService.USAGE_OFF_INTERVAL, interval);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
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
			java.lang.reflect.Field f = R.string.class.getField("lang_"+l);
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
		OsmandSettings.DrivingRegion drg = null;
		WorldRegion.RegionParams params = reg.getParams();
		boolean americanSigns = "american".equals(params.getRegionRoadSigns());
		boolean leftHand = "yes".equals(params.getRegionLeftHandDriving());
		OsmandSettings.MetricsConstants mc1 = "miles".equals(params.getRegionMetric()) ?
				OsmandSettings.MetricsConstants.MILES_AND_FEET : OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS;
		OsmandSettings.MetricsConstants mc2 = "miles".equals(params.getRegionMetric()) ?
				OsmandSettings.MetricsConstants.MILES_AND_METERS : OsmandSettings.MetricsConstants.KILOMETERS_AND_METERS;
		for (OsmandSettings.DrivingRegion r : OsmandSettings.DrivingRegion.values()) {
			if (r.americanSigns == americanSigns && r.leftHandDriving == leftHand &&
					(r.defMetrics == mc1 || r.defMetrics == mc2)) {
				drg = r;
				break;
			}
		}
		if (drg != null) {
			osmandSettings.DRIVING_REGION.set(drg);
		}
	}

	@SuppressLint("HardwareIds")
	public String getUserAndroidId() {
		String userAndroidId = osmandSettings.USER_ANDROID_ID.get();
		if (!Algorithms.isEmpty(userAndroidId)) {
			return userAndroidId;
		}
		try {
			userAndroidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
		} catch (Exception e) {
			// ignore
		}
		if (userAndroidId == null || userAndroidId.length() < 16 || userAndroidId.equals("0000000000000000")) {
			userAndroidId = UUID.randomUUID().toString();
		}
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

	public void restartApp(final Context ctx) {
		AlertDialog.Builder bld = new AlertDialog.Builder(ctx);
		bld.setMessage(R.string.restart_is_required);
		bld.setPositiveButton(R.string.shared_string_ok, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				if (ctx instanceof MapActivity) {
					MapActivity.doRestart(ctx);
				} else {
					android.os.Process.killProcess(android.os.Process.myPid());
				}
			}
		});
		bld.show();
	}
	
	public MapViewTrackingUtilities getMapViewTrackingUtilities() {
		return mapViewTrackingUtilities;
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
		StringBuilder text = new StringBuilder();
		text.append("\nDevice : ").append(Build.DEVICE); 
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
			PlatformUtil.getLog(CrashBottomSheetDialogFragment.class).error("", e);
		}
		intent.putExtra(Intent.EXTRA_TEXT, text.toString());
		Intent chooserIntent = Intent.createChooser(intent, getString(R.string.send_report));
		chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startActivity(chooserIntent);
	}
}
