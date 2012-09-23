package net.osmand.plus;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.osmand.Algoritms;
import net.osmand.FavouritePoint;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.LogUtil;
import net.osmand.Version;
import net.osmand.access.AccessibilityMode;
import net.osmand.access.AccessibleToast;
import net.osmand.plus.activities.DayNightHelper;
import net.osmand.plus.activities.LiveMonitoringHelper;
import net.osmand.plus.activities.OsmandIntents;
import net.osmand.plus.activities.SavingTrackHelper;
import net.osmand.plus.activities.SettingsActivity;
import net.osmand.plus.render.NativeOsmandLibrary;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.routing.RoutingHelper;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.voice.CommandPlayerException;
import net.osmand.plus.voice.CommandPlayerFactory;
import net.osmand.render.RenderingRulesStorage;
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
import android.content.res.Configuration;
import android.os.Build;
import android.os.Handler;
import android.text.format.DateFormat;
import android.view.accessibility.AccessibilityManager;
import android.widget.Toast;

import com.bidforfix.andorid.BidForFixHelper;

public class OsmandApplication extends Application {
	public static final String EXCEPTION_PATH = ResourceManager.APP_DIR + "exception.log"; //$NON-NLS-1$
	private static final org.apache.commons.logging.Log LOG = LogUtil.getLog(OsmandApplication.class);

	ResourceManager manager = null;
	PoiFiltersHelper poiFilters = null;
	RoutingHelper routingHelper = null;
	FavouritesDbHelper favorites = null;
	CommandPlayer player = null;
	
	OsmandSettings osmandSettings = null;
	
	DayNightHelper daynightHelper;
	NavigationService navigationService;
	RendererRegistry rendererRegistry;
	BidForFixHelper bidforfix;
	
	// start variables
	private ProgressDialogImplementation startDialog;
	private List<String> startingWarnings;
	private Handler uiHandler;
	private GPXFile gpxFileToDisplay;
	private SavingTrackHelper savingTrackHelper;
	private LiveMonitoringHelper liveMonitoringHelper;

	private boolean applicationInitializing = false;
	private Locale prefferedLocale = null;
	

	@Override
	public void onCreate() {
		super.onCreate();
		
		long timeToStart = System.currentTimeMillis();
		osmandSettings = createOsmandSettingsInstance();
		routingHelper = new RoutingHelper(this, player);
		manager = new ResourceManager(this);
		daynightHelper = new DayNightHelper(this);
		bidforfix = new BidForFixHelper("osmand.net", getString(R.string.default_buttons_support), getString(R.string.default_buttons_cancel));
		savingTrackHelper = new SavingTrackHelper(this);
		liveMonitoringHelper = new LiveMonitoringHelper(this);
		uiHandler = new Handler();
		rendererRegistry = new RendererRegistry();
		checkPrefferedLocale();
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
			routingHelper.getVoiceRouter().onApplicationTerminate(getApplicationContext());
		}
		if (bidforfix != null) {
    		bidforfix.onDestroy();
    	}
	}

	public RendererRegistry getRendererRegistry() {
		return rendererRegistry;
	}
	
	/**
	 * Creates instance of OsmandSettings
	 * @return Reference to instance of OsmandSettings
	 */
	protected OsmandSettings createOsmandSettingsInstance() {
		return new OsmandSettings(this);
	}

	/**
	 * Application settings
	 * @return Reference to instance of OsmandSettings
	 */
	public OsmandSettings getSettings() {
		if(osmandSettings == null) {
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
	

	public PoiFiltersHelper getPoiFilters() {
		if (poiFilters == null) {
			poiFilters = new PoiFiltersHelper(this);
			poiFilters.updateFilters(true);
		}
		return poiFilters;
	}

	public void setGpxFileToDisplay(GPXFile gpxFileToDisplay, boolean showCurrentGpxFile) {
		this.gpxFileToDisplay = gpxFileToDisplay;
		osmandSettings.SHOW_CURRENT_GPX_TRACK.set(showCurrentGpxFile);
		if (gpxFileToDisplay == null) {
			getFavorites().setFavoritePointsFromGPXFile(null);
		} else {
			List<FavouritePoint> pts = new ArrayList<FavouritePoint>();
			for (WptPt p : gpxFileToDisplay.points) {
				FavouritePoint pt = new FavouritePoint();
				pt.setLatitude(p.lat);
				pt.setLongitude(p.lon);
				if (p.name == null) {
					p.name = "";
				}
				pt.setName(p.name);
				pts.add(pt);
			}
			gpxFileToDisplay.proccessPoints();
			getFavorites().setFavoritePointsFromGPXFile(pts);
		}
	}

	public GPXFile getGpxFileToDisplay() {
		return gpxFileToDisplay;
	}

	public FavouritesDbHelper getFavorites() {
		if (favorites == null) {
			favorites = new FavouritesDbHelper(this);
		}
		return favorites;
	}

	public ResourceManager getResourceManager() {
		return manager;
	}

	public DayNightHelper getDaynightHelper() {
		return daynightHelper;
	}

	@Override
	public void onLowMemory() {
		super.onLowMemory();
		manager.onLowMemory();
	}
	
	

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (prefferedLocale != null && !newConfig.locale.getLanguage().equals(prefferedLocale.getLanguage())) {
			super.onConfigurationChanged(newConfig);
			// ugly fix ! On devices after 4.0 screen is blinking when you rotate device!
			if(Build.VERSION.SDK_INT < 14 ){
				newConfig.locale = prefferedLocale;
			}
			getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());				
			Locale.setDefault(prefferedLocale);
		} else {
			super.onConfigurationChanged(newConfig);
		}
	}

	public void checkPrefferedLocale() {
		Configuration config = getBaseContext().getResources().getConfiguration();
		String lang = osmandSettings.PREFERRED_LOCALE.get();
		if (!"".equals(lang) && !config.locale.getLanguage().equals(lang)) {
			prefferedLocale = new Locale(lang);
			Locale.setDefault(prefferedLocale);
			config.locale = prefferedLocale;
			getBaseContext().getResources().updateConfiguration(config, getBaseContext().getResources().getDisplayMetrics());
		}

	}

	public static final int PROGRESS_DIALOG = 5;

	/**
	 * @param activity
	 *            that supports onCreateDialog({@link #PROGRESS_DIALOG}) and
	 *            returns @param progressdialog
	 * @param progressDialog
	 *            - it should be exactly the same as onCreateDialog
	 * @return
	 */
	public void checkApplicationIsBeingInitialized(Activity activity, ProgressDialog progressDialog) {
		// start application if it was previously closed
		startApplication();
		synchronized (OsmandApplication.this) {
			if (startDialog != null) {
				try {
					SpecialPhrases.setLanguage(this, osmandSettings);
				} catch (IOException e) {
					LOG.error("I/O exception", e);
					Toast error = Toast.makeText(this, "Error while reading the special phrases. Restart OsmAnd if possible", Toast.LENGTH_LONG);
					error.show();
				}
				
				progressDialog.setTitle(getString(R.string.loading_data));
				progressDialog.setMessage(getString(R.string.reading_indexes));
				activity.showDialog(PROGRESS_DIALOG);
				startDialog.setDialog(progressDialog);
			} else if (startingWarnings != null) {
				showWarnings(startingWarnings, activity);
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

	public void showDialogInitializingCommandPlayer(final Activity uiContext) {
		showDialogInitializingCommandPlayer(uiContext, true);
	}

	public void showDialogInitializingCommandPlayer(final Activity uiContext, boolean warningNoneProvider) {
		showDialogInitializingCommandPlayer(uiContext, warningNoneProvider, null);
	}

	public void showDialogInitializingCommandPlayer(final Activity uiContext, boolean warningNoneProvider, Runnable run) {
		String voiceProvider = osmandSettings.VOICE_PROVIDER.get();
		if (voiceProvider == null || OsmandSettings.VOICE_PROVIDER_NOT_USE.equals(voiceProvider)) {
			if (warningNoneProvider && voiceProvider == null) {
				Builder builder = new AlertDialog.Builder(uiContext);
				builder.setCancelable(true);
				builder.setNegativeButton(R.string.default_buttons_cancel, null);
				builder.setPositiveButton(R.string.default_buttons_ok,
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								Intent intent = new Intent(uiContext,
										SettingsActivity.class);
								intent.putExtra(
										SettingsActivity.INTENT_KEY_SETTINGS_SCREEN,
										SettingsActivity.SCREEN_NAVIGATION_SETTINGS);
								uiContext.startActivity(intent);
							}
						});
				builder.setTitle(R.string.voice_is_not_available_title);
				builder.setMessage(R.string.voice_is_not_available_msg);
				builder.show();
			}

		} else {
			if (player == null || !Algoritms.objectEquals(voiceProvider, player.getCurrentVoice())) {
				initVoiceDataInDifferentThread(uiContext, voiceProvider, run);
			}
		}

	}

	private void initVoiceDataInDifferentThread(final Activity uiContext, final String voiceProvider, final Runnable run) {
		final ProgressDialog dlg = ProgressDialog.show(uiContext,
				getString(R.string.loading_data),
				getString(R.string.voice_data_initializing));
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					if (player != null) {
						player.clear();
					}
					player = CommandPlayerFactory.createCommandPlayer(voiceProvider, OsmandApplication.this, uiContext);
					routingHelper.getVoiceRouter().setPlayer(player);
					dlg.dismiss();
					if (run != null && uiContext != null) {
						uiContext.runOnUiThread(run);
					}
				} catch (CommandPlayerException e) {
					dlg.dismiss();
					showWarning(uiContext, e.getError());
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
	
	public BidForFixHelper getBidForFix() {
		return bidforfix;
	}
	

	public synchronized void closeApplication() {
		if (applicationInitializing) {
			manager.close();
		}
		applicationInitializing = false;
	}

	public synchronized void startApplication() {
		if (applicationInitializing) {
			return;
		}
		applicationInitializing = true;
		startDialog = new ProgressDialogImplementation(this, null, false);

		startDialog.setRunnable("Initializing app", new Runnable() { //$NON-NLS-1$
					@Override
					public void run() {
						startApplicationBackground();
					}
				});
		startDialog.run();

		Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());

	}

	public String exportFavorites(File f) {
		GPXFile gpx = new GPXFile();
		for (FavouritePoint p : getFavorites().getFavouritePoints()) {
			if (p.isStored()) {
				WptPt pt = new WptPt();
				pt.lat = p.getLatitude();
				pt.lon = p.getLongitude();
				pt.name = p.getName() + "_" + p.getCategory();
				gpx.points.add(pt);
			}
		}
		return GPXUtilities.writeGpxFile(f, gpx, this);
	}

	private void startApplicationBackground() {
		List<String> warnings = new ArrayList<String>();
		try {
			if (!Version.isBlackberry(this)) {
				if (osmandSettings.NATIVE_RENDERING_FAILED.get()) {
					osmandSettings.NATIVE_RENDERING.set(false);
					osmandSettings.NATIVE_RENDERING_FAILED.set(false);
					warnings.add(getString(R.string.native_library_not_supported));
				} else if (osmandSettings.NATIVE_RENDERING.get()) {
					osmandSettings.NATIVE_RENDERING_FAILED.set(true);
					startDialog.startTask(getString(R.string.init_native_library), -1);
					RenderingRulesStorage storage = rendererRegistry.getCurrentSelectedRenderer();
					boolean initialized = NativeOsmandLibrary.getLibrary(storage) != null;
					osmandSettings.NATIVE_RENDERING_FAILED.set(false);
					if (!initialized) {
						LOG.info("Native library could not loaded!");
						osmandSettings.NATIVE_RENDERING.set(false);
					}
				}
			}
			warnings.addAll(manager.reloadIndexes(startDialog));
			player = null;
			if (savingTrackHelper.hasDataToSave()) {
				startDialog.startTask(getString(R.string.saving_gpx_tracks), -1);
				warnings.addAll(savingTrackHelper.saveDataToGpx());
			}
			savingTrackHelper.close();

			// restore backuped favorites to normal file
			final File appDir = getSettings().extendOsmandPath(ResourceManager.APP_DIR);
			File save = new File(appDir, FavouritesDbHelper.FILE_TO_SAVE);
			File bak = new File(appDir, FavouritesDbHelper.FILE_TO_BACKUP);
			if (bak.exists() && (!save.exists() || bak.lastModified() > save.lastModified())) {
				if (save.exists()) {
					save.delete();
				}
				bak.renameTo(save);
			}
		} finally {
			synchronized (OsmandApplication.this) {
				final ProgressDialog toDismiss;
				if (startDialog != null) {
					toDismiss = startDialog.getDialog();
				} else {
					toDismiss = null;
				}
				startDialog = null;

				if (toDismiss != null) {
					uiHandler.post(new Runnable() {
						@Override
						public void run() {
							if (toDismiss != null) {
								// TODO handling this dialog is bad, we need a better standard way
								toDismiss.dismiss();
								// toDismiss.getOwnerActivity().dismissDialog(PROGRESS_DIALOG);
							}
						}
					});
					showWarnings(warnings, toDismiss.getContext());
				} else {
					startingWarnings = warnings;
				}
			}
		}
	}

	protected void showWarnings(List<String> warnings, final Context uiContext) {
		if (warnings != null && !warnings.isEmpty()) {
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
			showWarning(uiContext, b.toString());
		}
	}

	private void showWarning(final Context uiContext, final String b) {
		uiHandler.post(new Runnable() {
			@Override
			public void run() {
				AccessibleToast.makeText(uiContext, b.toString(), Toast.LENGTH_LONG).show();
			}
		});
	}

	private class DefaultExceptionHandler implements UncaughtExceptionHandler {

		private UncaughtExceptionHandler defaultHandler;
		private PendingIntent intent;

		public DefaultExceptionHandler() {
			defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
			intent = PendingIntent.getActivity(OsmandApplication.this.getBaseContext(), 0,
		            new Intent(OsmandApplication.this.getBaseContext(), OsmandIntents.getMainMenuActivity()), 0);
		}

		@Override
		public void uncaughtException(final Thread thread, final Throwable ex) {
			File file = osmandSettings.extendOsmandPath(EXCEPTION_PATH);
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintStream printStream = new PrintStream(out);
				ex.printStackTrace(printStream);
				StringBuilder msg = new StringBuilder();
				msg.append("Version  " + Version.getFullVersion(OsmandApplication.this)+"\n"). //$NON-NLS-1$ 
					append(DateFormat.format("dd.MM.yyyy h:mm:ss", System.currentTimeMillis())).append("\n"). //$NON-NLS-1$//$NON-NLS-2$
					append("Exception occured in thread " + thread.toString() + " : \n"). //$NON-NLS-1$ //$NON-NLS-2$
					append(new String(out.toByteArray()));

				if (file.getParentFile().canWrite()) {
					BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
					writer.write(msg.toString());
					writer.close();
				}
				if(routingHelper.isFollowingMode()) {
					AlarmManager mgr = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
					mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 2000, intent);
					System.exit(2);
				}
				defaultHandler.uncaughtException(thread, ex);
			} catch (Exception e) {
				// swallow all exceptions
				android.util.Log.e(LogUtil.TAG, "Exception while handle other exception", e); //$NON-NLS-1$
			}

		}
	}
	
	public boolean accessibilityExtensions(){
		return osmandSettings.ACCESSIBILITY_EXTENSIONS.get();
	}

	public boolean accessibilityEnabled() {
		final AccessibilityMode mode = getSettings().ACCESSIBILITY_MODE.get();
		if (mode == AccessibilityMode.ON)
			return true;
		else if (mode == AccessibilityMode.OFF)
			return false;
		return ((AccessibilityManager)getSystemService(ACCESSIBILITY_SERVICE)).isEnabled();
	}

}
