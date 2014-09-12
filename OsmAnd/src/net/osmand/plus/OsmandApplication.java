package net.osmand.plus;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.osmand.IndexConstants;
import net.osmand.PlatformUtil;
import net.osmand.access.AccessibilityPlugin;
import net.osmand.access.AccessibleAlertBuilder;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.access.AccessibilityMode;
import net.osmand.plus.activities.*;
import net.osmand.plus.download.DownloadIndexFragment;
import net.osmand.plus.api.SQLiteAPI;
import net.osmand.plus.api.SQLiteAPIImpl;
import net.osmand.plus.helpers.WaypointHelper;
import net.osmand.plus.monitoring.LiveMonitoringHelper;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.resources.ResourceManager;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.sherpafy.SherpafyCustomization;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.voice.CommandPlayerException;
import net.osmand.plus.voice.CommandPlayerFactory;
import net.osmand.render.RenderingRulesStorage;
import net.osmand.router.RoutingConfiguration;
import net.osmand.util.Algorithms;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Application;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.accessibility.AccessibilityManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import btools.routingapp.BRouterServiceConnection;
import btools.routingapp.IBRouterService;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.app.SherlockExpandableListActivity;
import com.actionbarsherlock.app.SherlockListActivity;


public class OsmandApplication extends Application {
	public static final String EXCEPTION_PATH = "exception.log"; //$NON-NLS-1$
	private static final org.apache.commons.logging.Log LOG = PlatformUtil.getLog(OsmandApplication.class);


	ResourceManager resourceManager = null;
	PoiFiltersHelper poiFilters = null;
	RoutingHelper routingHelper = null;
	FavouritesDbHelper favorites = null;
	CommandPlayer player = null;

	OsmandSettings osmandSettings = null;

	OsmAndAppCustomization appCustomization;
	DayNightHelper daynightHelper;
	NavigationService navigationService;
	RendererRegistry rendererRegistry;
	OsmAndLocationProvider locationProvider;
	OsmAndTaskManager taskManager;

	// start variables
	private ProgressImplementation startDialog;
	private Handler uiHandler;
	private GpxSelectionHelper selectedGpxHelper;
	private SavingTrackHelper savingTrackHelper;
	private LiveMonitoringHelper liveMonitoringHelper;
	private TargetPointsHelper targetPointsHelper;
	private RoutingConfiguration.Builder defaultRoutingConfig;
	private WaypointHelper waypointHelper;

	private boolean applicationInitializing = false;
	private Locale preferredLocale = null;

	SQLiteAPI sqliteAPI;
	BRouterServiceConnection bRouterServiceConnection;

	MapActivity mapActivity;
	DownloadIndexFragment downloadActivity;
	
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
		new Toast(this); // activate in UI thread to avoid further exceptions
		sqliteAPI = new SQLiteAPIImpl(this);
		try {
			bRouterServiceConnection = BRouterServiceConnection.connect(this);
		} catch(Exception e) {
			e.printStackTrace();
		}

		if(Version.isSherpafy(this)) {
			appCustomization = new SherpafyCustomization();
		} else {
			appCustomization = new OsmAndAppCustomization();
		}

		appCustomization.setup(this);

		osmandSettings = appCustomization.getOsmandSettings();
		// always update application mode to default
		if(!osmandSettings.FOLLOW_THE_ROUTE.get()){
			osmandSettings.APPLICATION_MODE.set(osmandSettings.DEFAULT_APPLICATION_MODE.get());
		}


		applyTheme(this);
		
		routingHelper = new RoutingHelper(this, player);
		taskManager = new OsmAndTaskManager(this);
		resourceManager = new ResourceManager(this);
		daynightHelper = new DayNightHelper(this);
		locationProvider = new OsmAndLocationProvider(this);
		savingTrackHelper = new SavingTrackHelper(this);
		liveMonitoringHelper = new LiveMonitoringHelper(this);
		selectedGpxHelper = new GpxSelectionHelper(this);
		favorites = new FavouritesDbHelper(this);
		waypointHelper = new WaypointHelper(this);
		uiHandler = new Handler();
		rendererRegistry = new RendererRegistry();
		targetPointsHelper = new TargetPointsHelper(this);
//		if(!osmandSettings.FOLLOW_THE_ROUTE.get()) {
//			targetPointsHelper.clearPointToNavigate(false);
//		}
		
		checkPreferredLocale();
		startApplication();
		if (LOG.isDebugEnabled()) {
			LOG.debug("Time to start application " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms");
		}
		timeToStart = System.currentTimeMillis();
		OsmandPlugin.initPlugins(this);

		if (LOG.isDebugEnabled()) {
			LOG.debug("Time to init plugins " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms");
		}


	}
	
	@Override
	public void onTerminate() {
		super.onTerminate();
		if (routingHelper != null) {
			routingHelper.getVoiceRouter().onApplicationTerminate(this);
		}
	}

	public RendererRegistry getRendererRegistry() {
		return rendererRegistry;
	}
	
	public OsmAndTaskManager getTaskManager() {
		return taskManager;
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

	public LiveMonitoringHelper getLiveMonitoringHelper() {
		return liveMonitoringHelper;
	}

	public WaypointHelper getWaypointHelper() {
		return waypointHelper;
	}

	public PoiFiltersHelper getPoiFilters() {
		if (poiFilters == null) {
			poiFilters = new PoiFiltersHelper(this);
		}
		return poiFilters;
	}


	public GpxSelectionHelper getSelectedGpxHelper() {
		return selectedGpxHelper;
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
		String lang = osmandSettings.PREFERRED_LOCALE.get();
		if (!"".equals(lang) && !config.locale.getLanguage().equals(lang)) {
			preferredLocale = new Locale(lang);
			Locale.setDefault(preferredLocale);
			config.locale = preferredLocale;
			getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
		}
		String clang = "".equals(lang) ? config.locale.getLanguage() : lang;
		resourceManager.getOsmandRegions().setLocale(clang);

	}

	public static final int PROGRESS_DIALOG = 5;

	public void checkApplicationIsBeingInitialized(Activity activity, ProgressDialog progressDialog) {
		// start application if it was previously closed
		startApplication();
		synchronized (OsmandApplication.this) {
			if (startDialog != null) {
				progressDialog.setTitle(getString(R.string.loading_data));
				progressDialog.setMessage(getString(R.string.reading_indexes));
				activity.showDialog(PROGRESS_DIALOG);
				startDialog.setDialog(progressDialog);
			} else {
				progressDialog.dismiss();
			}
		}
	}
	
	public void checkApplicationIsBeingInitialized(Activity activity, TextView tv, ProgressBar progressBar,
			Runnable onClose) {
		// start application if it was previously closed
		startApplication();
		synchronized (OsmandApplication.this) {
			if (startDialog != null ) {
				tv.setText(getString(R.string.loading_data));
				startDialog.setProgressBar(tv, progressBar, onClose);
			} else if (onClose != null) {
				onClose.run();
			}
		}
	}

	public boolean isApplicationInitializing() {
		return startDialog != null;
	}

	public RoutingHelper getRoutingHelper() {
		return routingHelper;
	}

	public CommandPlayer getPlayer() {
		return player;
	}

	public void initVoiceCommandPlayer(final Activity uiContext) {
		showDialogInitializingCommandPlayer(uiContext, true, null, false);
	}

	public void showDialogInitializingCommandPlayer(final Activity uiContext, boolean warningNoneProvider) {
		showDialogInitializingCommandPlayer(uiContext, warningNoneProvider, null, true);
	}

	public void showDialogInitializingCommandPlayer(final Activity uiContext, boolean warningNoneProvider, Runnable run, boolean showDialog) {
		String voiceProvider = osmandSettings.VOICE_PROVIDER.get();
		if (voiceProvider == null || OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(voiceProvider)) {
			if (warningNoneProvider && voiceProvider == null) {
				Builder builder = new AccessibleAlertBuilder(uiContext);
				LinearLayout ll = new LinearLayout(uiContext);
				ll.setOrientation(LinearLayout.VERTICAL);
				final TextView tv = new TextView(uiContext);
				tv.setPadding(7, 3, 7, 0);
				tv.setText(R.string.voice_is_not_available_msg);
				tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
				ll.addView(tv);
				
				final CheckBox cb = new CheckBox(uiContext);
				cb.setText(R.string.remember_choice);
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
				lp.setMargins(7, 10, 7, 0);
				cb.setLayoutParams(lp);
				ll.addView(cb);
				
				builder.setCancelable(true);
				builder.setNegativeButton(R.string.default_buttons_cancel, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						if(cb.isChecked()) {
							osmandSettings.VOICE_PROVIDER.set(OsmandSettings.VOICE_PROVIDER_NOT_USE);
						}
					}
				});
				builder.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						Intent intent = new Intent(uiContext, SettingsActivity.class);
						intent.putExtra(SettingsActivity.INTENT_KEY_SETTINGS_SCREEN, SettingsActivity.SCREEN_GENERAL_SETTINGS);
						uiContext.startActivity(intent);
					}
				});
				
				
				builder.setTitle(R.string.voice_is_not_available_title);
				builder.setView(ll);
				//builder.setMessage(R.string.voice_is_not_available_msg);
				builder.show();
			}

		} else {
			if (player == null || !Algorithms.objectEquals(voiceProvider, player.getCurrentVoice())) {
				initVoiceDataInDifferentThread(uiContext, voiceProvider, run, showDialog);
			}
		}

	}

	private void initVoiceDataInDifferentThread(final Activity uiContext, final String voiceProvider, final Runnable run, boolean showDialog) {
		final ProgressDialog dlg = showDialog ? ProgressDialog.show(uiContext, getString(R.string.loading_data),
				getString(R.string.voice_data_initializing)) : null;
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (player != null) {
						player.clear();
					}
					player = CommandPlayerFactory.createCommandPlayer(voiceProvider, OsmandApplication.this, uiContext);
					routingHelper.getVoiceRouter().setPlayer(player);
					if(dlg != null) {
						dlg.dismiss();
					}
					if (run != null && uiContext != null) {
						uiContext.runOnUiThread(run);
					}
				} catch (CommandPlayerException e) {
					if(dlg != null) {
						dlg.dismiss();
					}
					showToastMessage(e.getError());
				}
			}
		}).start();
	}

	public NavigationService getNavigationService() {
		return navigationService;
	}

	public void setNavigationService(NavigationService navigationService) {
		this.navigationService = navigationService;
	}


	private void fullExit() {
		// http://stackoverflow.com/questions/2092951/how-to-close-android-application
		System.runFinalizersOnExit(true);
		System.exit(0);
	}

	public synchronized void closeApplication(final Activity activity) {
		if (getNavigationService() != null) {
			Builder bld = new AlertDialog.Builder(activity);
			bld.setMessage(R.string.background_service_is_enabled_question);
			bld.setPositiveButton(R.string.default_buttons_yes, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					closeApplicationAnyway(activity, true);
				}
			});
			bld.setNegativeButton(R.string.default_buttons_no, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					closeApplicationAnyway(activity, false);
				}
			});
			bld.show();
		} else {
			closeApplicationAnyway(activity, true);
		}
	}

	private void closeApplicationAnyway(final Activity activity, boolean disableService) {
		if (applicationInitializing) {
			resourceManager.close();
		}
		applicationInitializing = false;

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

	public synchronized void startApplication() {
		if (applicationInitializing) {
			return;
		}
		applicationInitializing = true;
		startDialog = new ProgressImplementation(this, null, false);

		startDialog.setRunnable("Initializing app", new Runnable() { //$NON-NLS-1$
					@Override
					public void run() {
						startApplicationBackground();
					}
				});
		startDialog.run();

		Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());

	}
	

	private void startApplicationBackground() {
		List<String> warnings = new ArrayList<String>();
		try {
			favorites.loadFavorites();
			try {
				SpecialPhrases.setLanguage(this, osmandSettings);
			} catch (IOException e) {
				LOG.error("I/O exception", e);
				warnings.add("Error while reading the special phrases. Restart OsmAnd if possible");
			}
			
			if (!Version.isBlackberry(this) || !"qnx".equals(System.getProperty("os.name"))) {
				if (osmandSettings.NATIVE_RENDERING_FAILED.get()) {
					osmandSettings.SAFE_MODE.set(true);
					osmandSettings.NATIVE_RENDERING_FAILED.set(false);
					warnings.add(getString(R.string.native_library_not_supported));
				} else {
					osmandSettings.SAFE_MODE.set(false);
					osmandSettings.NATIVE_RENDERING_FAILED.set(true);
					startDialog.startTask(getString(R.string.init_native_library), -1);
					RenderingRulesStorage storage = rendererRegistry.getCurrentSelectedRenderer();
					boolean initialized = NativeOsmandLibrary.getLibrary(storage, this) != null;
					osmandSettings.NATIVE_RENDERING_FAILED.set(false);
					if (!initialized) {
						LOG.info("Native library could not be loaded!");
					}
				}
			}
			warnings.addAll(resourceManager.reloadIndexes(startDialog));
			player = null;
			if (savingTrackHelper.hasDataToSave()) {
				long timeUpdated = savingTrackHelper.getLastTrackPointTime();
				if (System.currentTimeMillis() - timeUpdated >= 45000) {
					startDialog.startTask(getString(R.string.saving_gpx_tracks), -1);
					try {
						warnings.addAll(savingTrackHelper.saveDataToGpx(appCustomization.getTracksDir()));
					} catch (RuntimeException e) {
						warnings.add(e.getMessage());
					}
				}
			}
			if(getSettings().SAVE_GLOBAL_TRACK_TO_GPX.get()){
				startNavigationService(NavigationService.USED_BY_GPX);
			}
			// restore backuped favorites to normal file
			final File appDir = getAppPath(null);
			File save = new File(appDir, FavouritesDbHelper.FILE_TO_SAVE);
			File bak = new File(appDir, FavouritesDbHelper.FILE_TO_BACKUP);
			if (bak.exists() && (!save.exists() || bak.lastModified() > save.lastModified())) {
				if (save.exists()) {
					save.delete();
				}
				bak.renameTo(save);
			}
		} catch (RuntimeException e) {
			e.printStackTrace();
			warnings.add(e.getMessage());
		} finally {
			synchronized (OsmandApplication.this) {
				final ProgressDialog toDismiss;
				final Runnable pb;
				if (startDialog != null) {
					toDismiss = startDialog.getDialog();
					pb = startDialog.getFinishRunnable();
				} else {
					toDismiss = null;
					pb = null;
				}
				startDialog = null;

				if (toDismiss != null || pb != null) {
					uiHandler.post(new Runnable() {
						@Override
						public void run() {
							if(pb != null) {
								pb.run();
							}
							if (toDismiss != null) {
								// TODO handling this dialog is bad, we need a better standard way
								toDismiss.dismiss();
								// toDismiss.getOwnerActivity().dismissDialog(PROGRESS_DIALOG);
							}
						}
					});
				}
				if (warnings != null && !warnings.isEmpty()) {
					showToastMessage(formatWarnings(warnings).toString());
				}
			}
		}
	}

	private StringBuilder formatWarnings(List<String> warnings) {
		final StringBuilder b = new StringBuilder();
		boolean f = true;
		for (String w : warnings) {
			if (f) {
				f = false;
			} else {
				b.append('\n');
			}
			b.append(w);
		}
		return b;
	}


	private class DefaultExceptionHandler implements UncaughtExceptionHandler {

		private UncaughtExceptionHandler defaultHandler;
		private PendingIntent intent;

		public DefaultExceptionHandler() {
			defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
			intent = PendingIntent.getActivity(OsmandApplication.this.getBaseContext(), 0,
					new Intent(OsmandApplication.this.getBaseContext(),
							getAppCustomization().getMainMenuActivity()), 0);
		}

		@Override
		public void uncaughtException(final Thread thread, final Throwable ex) {
			File file = getAppPath(EXCEPTION_PATH);
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintStream printStream = new PrintStream(out);
				ex.printStackTrace(printStream);
				StringBuilder msg = new StringBuilder();
				msg.append("Version  " + Version.getFullVersion(OsmandApplication.this) + "\n"). //$NON-NLS-1$ 
						append(DateFormat.format("dd.MM.yyyy h:mm:ss", System.currentTimeMillis()));
				try {
					PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
					if (info != null) {
						msg.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode); //$NON-NLS-1$ //$NON-NLS-2$
					}
				} catch (Throwable e) {
				}
				msg.append("\n"). //$NON-NLS-1$//$NON-NLS-2$
						append("Exception occured in thread " + thread.toString() + " : \n"). //$NON-NLS-1$ //$NON-NLS-2$
						append(new String(out.toByteArray()));

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

	public void showShortToastMessage(final int msgId, final Object... args) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				AccessibleToast.makeText(OsmandApplication.this, getString(msgId, args), Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void showShortToastMessage(final String msg) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				AccessibleToast.makeText(OsmandApplication.this, msg, Toast.LENGTH_SHORT).show();
			}
		});
	}

	public void showToastMessage(final int msgId, final Object... args) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				AccessibleToast.makeText(OsmandApplication.this, getString(msgId, args), Toast.LENGTH_LONG).show();
			}
		});
	}

	public void showToastMessage(final String msg) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				AccessibleToast.makeText(OsmandApplication.this, msg, Toast.LENGTH_LONG).show();				
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

		return new File(getAppCustomization().getExternalStorageDir(), IndexConstants.APP_DIR + path);
	}

	public void applyTheme(Context c) {
		int t = R.style.OsmandLightDarkActionBarTheme;
		if (osmandSettings.OSMAND_THEME.get() == OsmandSettings.OSMAND_DARK_THEME) {
			t = R.style.OsmandDarkTheme;
		} else if (osmandSettings.OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_THEME) {
			t = R.style.OsmandLightTheme;
		} else if (osmandSettings.OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_DARK_ACTIONBAR_THEME) {
			t = R.style.OsmandLightDarkActionBarTheme;
		}
		setLanguage(c);
		c.setTheme(t);
		if (osmandSettings.OSMAND_THEME.get() == OsmandSettings.OSMAND_LIGHT_DARK_ACTIONBAR_THEME
				&& Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			ActionBar ab = null;
			if (c instanceof SherlockActivity) {
				ab = ((SherlockActivity) c).getSupportActionBar();
			} else if (c instanceof SherlockListActivity) {
				ab = ((SherlockListActivity) c).getSupportActionBar();
			} else if (c instanceof SherlockExpandableListActivity) {
				ab = ((SherlockExpandableListActivity) c).getSupportActionBar();
			}
			if (ab != null) {
				BitmapDrawable bg = (BitmapDrawable) getResources().getDrawable(R.drawable.bg_striped);
				bg.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
				ab.setBackgroundDrawable(bg);
			}
		}
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
			}
		}
	}
	
	public RoutingConfiguration.Builder getDefaultRoutingConfig() {
		if (defaultRoutingConfig == null) {
			File routingXml = getAppPath(IndexConstants.ROUTING_XML_FILE);
			if (routingXml.exists() && routingXml.canRead()) {
				try {
					defaultRoutingConfig = RoutingConfiguration.parseFromInputStream(new FileInputStream(routingXml));
				} catch (XmlPullParserException e) {
					throw new IllegalStateException(e);
				} catch (IOException e) {
					throw new IllegalStateException(e);
				}
			} else {
				defaultRoutingConfig = RoutingConfiguration.getDefault();
			}
		}
		return defaultRoutingConfig;
	}
	
	public boolean accessibilityExtensions() {
		return (Build.VERSION.SDK_INT < 14) ? getSettings().ACCESSIBILITY_EXTENSIONS.get() : false;
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

	public void startNavigationService(int intent) {
		final Intent serviceIntent = new Intent(this, NavigationService.class);
		serviceIntent.putExtra(NavigationService.USAGE_INTENT, intent);
		if (getNavigationService() == null) {
			if (intent != NavigationService.USED_BY_GPX) {
				//for only-USED_BY_GPX case use pre-configured SERVICE_OFF_INTERVAL
				//other cases always use "continuous":
				getSettings().SERVICE_OFF_INTERVAL.set(0);
			}
			startService(serviceIntent);
		} else {
			//additional cases always use "continuous"
			//TODO: fallback to custom USED_BY_GPX interval in case all other sleep mode purposes have been stopped
			getSettings().SERVICE_OFF_INTERVAL.set(0);
			getNavigationService().addUsageIntent(intent);
		}		
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public void setMapActivity(MapActivity mapActivity) {
		this.mapActivity = mapActivity;
	}
	
	public void setDownloadActivity(DownloadIndexFragment downloadActivity) {
		this.downloadActivity = downloadActivity;
	}
	
	public DownloadIndexFragment getDownloadActivity() {
		return downloadActivity;
	}
}
