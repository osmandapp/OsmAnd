package net.osmand.plus;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.osmand.CallbackWithObject;
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
import net.osmand.plus.dialogs.RateUsBottomSheetDialog;
import net.osmand.plus.download.DownloadIndexesThread;
import net.osmand.plus.helpers.AvoidSpecificRoads;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.inapp.InAppHelper;
import net.osmand.plus.mapcontextmenu.other.RoutePreferencesMenu;
import net.osmand.plus.monitoring.LiveMonitoringHelper;
import net.osmand.plus.poi.PoiFiltersHelper;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.search.QuickSearchHelper;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import btools.routingapp.BRouterServiceConnection;
import btools.routingapp.IBRouterService;

public class OsmandApplication extends MultiDexApplication {
	public static final String EXCEPTION_PATH = "exception.log"; //$NON-NLS-1$
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(OsmandApplication.class);

	public static final String SHOW_PLUS_VERSION_INAPP_PARAM = "show_plus_version_inapp";

	final AppInitializer appInitializer = new AppInitializer(this);
	OsmandSettings osmandSettings = null;
	OsmAndAppCustomization appCustomization;
	private final SQLiteAPI sqliteAPI = new SQLiteAPIImpl(this);
	private final OsmAndTaskManager taskManager = new OsmAndTaskManager(this);
	private final IconsCache iconsCache = new IconsCache(this);
	Handler uiHandler;

	NavigationService navigationService;

	OsmandAidlApi aidlApi;

	// start variables
	ResourceManager resourceManager;
	OsmAndLocationProvider locationProvider;
	RendererRegistry rendererRegistry;
	DayNightHelper daynightHelper;
	PoiFiltersHelper poiFilters;
	MapPoiTypes poiTypes;
	RoutingHelper routingHelper;
	FavouritesDbHelper favorites;
	CommandPlayer player;
	GpxSelectionHelper selectedGpxHelper;
	GPXDatabase gpxDatabase;
	SavingTrackHelper savingTrackHelper;
	NotificationHelper notificationHelper;
	LiveMonitoringHelper liveMonitoringHelper;
	TargetPointsHelper targetPointsHelper;
	MapMarkersHelper mapMarkersHelper;
	WaypointHelper waypointHelper;
	DownloadIndexesThread downloadIndexesThread;
	AvoidSpecificRoads avoidSpecificRoads;
	BRouterServiceConnection bRouterServiceConnection;
	OsmandRegions regions;
	GeocodingLookupService geocodingLookupService;
	QuickSearchHelper searchUICore;

	RoutingConfiguration.Builder defaultRoutingConfig;
	private Locale preferredLocale = null;
	private Locale defaultLocale;
	private File externalStorageDirectory;
	private boolean externalStorageDirectoryReadOnly;

	private String firstSelectedVoiceProvider;
	
	// Typeface
	
	@Override
	public void onCreate() {
		long timeToStart = System.currentTimeMillis();
		if (Version.getAppName(this).equals("OsmAnd~")) {
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
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("enable_proxy", false)) {
			NetworkUtils.setProxy(osmandSettings.PROXY_HOST.get(), osmandSettings.PROXY_PORT.get());
		}
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
		
		checkPreferredLocale();
		appInitializer.onCreateApplication();
//		if(!osmandSettings.FOLLOW_THE_ROUTE.get()) {
//			targetPointsHelper.clearPointToNavigate(false);
//		}

		InAppHelper.initialize(this);
		initRemoteConfig();
		startApplication();
		System.out.println("Time to start application " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms");
		timeToStart = System.currentTimeMillis();
		OsmandPlugin.initPlugins(this);
		System.out.println("Time to init plugins " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms");
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
		}.execute();
	}
	
	public IconsCache getIconsCache() {
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
        getNotificationHelper().removeNotifications();
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

	public GPXDatabase getGpxDatabase() {
		return gpxDatabase;
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

	public GeocodingLookupService getGeocodingLookupService() {
		return geocodingLookupService;
	}

	public QuickSearchHelper getSearchUICore() {
		return searchUICore;
	}

	public CommandPlayer getPlayer() {
		return player;
	}

	public void initVoiceCommandPlayer(final Activity uiContext, ApplicationMode applicationMode,
									   boolean warningNoneProvider, Runnable run, boolean showDialog, boolean force) {
		String voiceProvider = osmandSettings.VOICE_PROVIDER.get();
		if (voiceProvider == null || OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(voiceProvider)) {
			if (warningNoneProvider && voiceProvider == null) {
				final AlertDialog.Builder builder = new AlertDialog.Builder(uiContext);

				View view = uiContext.getLayoutInflater().inflate(R.layout.select_voice_first, null);

				((ImageView) view.findViewById(R.id.icon))
						.setImageDrawable(getIconsCache().getIcon(R.drawable.ic_action_volume_up, getSettings().isLightContent()));

				view.findViewById(R.id.spinner).setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(final View v) {
						RoutePreferencesMenu.selectVoiceGuidance((MapActivity) uiContext, new CallbackWithObject<String>() {
							@Override
							public boolean processResult(String result) {
								boolean acceptableValue = !RoutePreferencesMenu.MORE_VALUE.equals(firstSelectedVoiceProvider);
								if (acceptableValue) {
									((TextView) v.findViewById(R.id.selectText))
											.setText(RoutePreferencesMenu.getVoiceProviderName(uiContext, result));
									firstSelectedVoiceProvider = result;
								}
								return acceptableValue;
							}
						});
					}
				});

				((ImageView) view.findViewById(R.id.dropDownIcon))
						.setImageDrawable(getIconsCache().getIcon(R.drawable.ic_action_arrow_drop_down, getSettings().isLightContent()));

				builder.setCancelable(true);
				builder.setNegativeButton(R.string.shared_string_cancel, null);
				builder.setPositiveButton(R.string.shared_string_apply, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if (!Algorithms.isEmpty(firstSelectedVoiceProvider)) {
							RoutePreferencesMenu.applyVoiceProvider((MapActivity) uiContext, firstSelectedVoiceProvider);
						}
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
		if (osmandSettings.USE_MAP_MARKERS.get()) {
			targetPointsHelper.removeAllWayPoints(false, false);
		}
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
		Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());
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
						.append("\n") //$NON-NLS-1$
						.append(DateFormat.format("dd.MM.yyyy h:mm:ss", System.currentTimeMillis()));
				try {
					PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
					if (info != null) {
						msg.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode); //$NON-NLS-1$ //$NON-NLS-2$
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
					mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
					System.exit(2);
				}
				defaultHandler.uncaughtException(thread, ex);
			} catch (Exception e) {
				// swallow all exceptions
				android.util.Log.e(PlatformUtil.TAG, "Exception while handle other exception", e); //$NON-NLS-1$
			}

		}
	}

	
	public TargetPointsHelper getTargetPointsHelper() {
		return targetPointsHelper;
	}

	public MapMarkersHelper getMapMarkersHelper() {
		return mapMarkersHelper;
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
		if (osmandSettings.OSMAND_THEME.get() == OsmandSettings.OSMAND_DARK_THEME) {
			t = R.style.OsmandDarkTheme;
		} else if (osmandSettings.OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME) {
			t = R.style.OsmandLightTheme;
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
	
	public RoutingConfiguration.Builder getDefaultRoutingConfig() {
		if(defaultRoutingConfig == null) {
			defaultRoutingConfig = appInitializer.getLazyDefaultRoutingConfig();
		}
		return defaultRoutingConfig;
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
	

	public void startNavigationService(int intent, int interval) {
		final Intent serviceIntent = new Intent(this, NavigationService.class);
		
		if (getNavigationService() != null) {
			intent |= getNavigationService().getUsedBy();
			interval = Math.min(getNavigationService().getServiceOffInterval(), interval);
			getNavigationService().stopSelf();
			
		}
		serviceIntent.putExtra(NavigationService.USAGE_INTENT, intent);
		serviceIntent.putExtra(NavigationService.USAGE_OFF_INTERVAL, interval);
		startService(serviceIntent);
		//getNotificationHelper().showNotifications();
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
	
	public void logEvent(Activity ctx, String event) {
		try {
			if (Version.isGooglePlayEnabled(this) && Version.isFreeVersion(this)
					&& !osmandSettings.DO_NOT_SEND_ANONYMOUS_APP_USAGE.get()
					&& !osmandSettings.FULL_VERSION_PURCHASED.get()
					&& !osmandSettings.LIVE_UPDATES_PURCHASED.get()) {
				Class<?> cl = Class.forName("com.google.firebase.analytics.FirebaseAnalytics");
				Method mm = cl.getMethod("getInstance", Context.class);
				Object inst = mm.invoke(null, ctx == null ? this : ctx);
				Method log = cl.getMethod("logEvent", String.class, Bundle.class);
				log.invoke(inst, event, new Bundle());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void initRemoteConfig() {
		try {
			if (Version.isGooglePlayEnabled(this) && Version.isFreeVersion(this)) {
				Class<?> cl = Class.forName("com.google.firebase.remoteconfig.FirebaseRemoteConfig");
				Method mm = cl.getMethod("getInstance");
				Object inst = mm.invoke(null);
				Method log = cl.getMethod("setDefaults", Map.class);
				Map<String, Object> defaults = new HashMap<>();
				defaults.put(SHOW_PLUS_VERSION_INAPP_PARAM, Boolean.TRUE);
				log.invoke(inst, defaults);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void fetchRemoteParams() {
		try {
			if(Version.isGooglePlayEnabled(this) && Version.isFreeVersion(this)) {
				Class<?> cl = Class.forName("com.google.firebase.remoteconfig.FirebaseRemoteConfig");
				Method mm = cl.getMethod("getInstance");
				Object inst = mm.invoke(null);
				Method log = cl.getMethod("fetch");
				log.invoke(inst);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void activateFetchedRemoteParams() {
		try {
			if (Version.isGooglePlayEnabled(this) && Version.isFreeVersion(this)) {
				Class<?> cl = Class.forName("com.google.firebase.remoteconfig.FirebaseRemoteConfig");
				Method mm = cl.getMethod("getInstance");
				Object inst = mm.invoke(null);
				Method log = cl.getMethod("activateFetched");
				log.invoke(inst);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean getRemoteBoolean(String key, boolean defaultValue) {
		try {
			if (Version.isGooglePlayEnabled(this) && Version.isFreeVersion(this)) {
				Class<?> cl = Class.forName("com.google.firebase.remoteconfig.FirebaseRemoteConfig");
				Method mm = cl.getMethod("getInstance");
				Object inst = mm.invoke(null);
				Method log = cl.getMethod("getBoolean", String.class);
				Boolean res = (Boolean)log.invoke(inst, key);
				return res == null ? defaultValue : res;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return defaultValue;
	}
}
