package net.osmand.plus.activities;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.osmand.Algoritms;
import net.osmand.FavouritePoint;
import net.osmand.LogUtil;
import net.osmand.GPXUtilities.GPXFileResult;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.PoiFiltersHelper;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.render.RendererRegistry;
import net.osmand.plus.voice.CommandPlayer;
import net.osmand.plus.voice.CommandPlayerException;
import net.osmand.plus.voice.CommandPlayerFactory;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Application;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.widget.Toast;

public class OsmandApplication extends Application {
	public static final String EXCEPTION_PATH = ResourceManager.APP_DIR + "exception.log"; //$NON-NLS-1$
	private static final org.apache.commons.logging.Log LOG = LogUtil.getLog(OsmandApplication.class);
	
	ResourceManager manager = null; 
	PoiFiltersHelper poiFilters = null;
	RoutingHelper routingHelper = null;
	FavouritesDbHelper favorites = null;
	CommandPlayer player = null;
	OsmandSettings osmandSettings;
	DayNightHelper daynightHelper;
	NavigationService navigationService;
	RendererRegistry rendererRegistry;
	
	
	// start variables
	private ProgressDialogImplementation startDialog;
	private List<String> startingWarnings;
	private ProgressDialog progressDlg;
	private Handler uiHandler;
	private GPXFileResult gpxFileToDisplay;
	
	private boolean applicationInitializing = false;
	private Locale prefferedLocale = null;

	
    public void	onCreate(){
    	super.onCreate();
    	long timeToStart = System.currentTimeMillis();
    	osmandSettings = OsmandSettings.getOsmandSettings(this);
    	routingHelper = new RoutingHelper(osmandSettings, OsmandApplication.this, player);
    	manager = new ResourceManager(this);
    	daynightHelper = new DayNightHelper(this);
    	uiHandler = new Handler();
    	rendererRegistry = new RendererRegistry();
    	checkPrefferedLocale();
    	startApplication();
    	if(LOG.isDebugEnabled()){
    		LOG.debug("Time to start application " + (System.currentTimeMillis() - timeToStart) + " ms. Should be less < 800 ms");
    	}
	}
    
    public RendererRegistry getRendererRegistry() {
		return rendererRegistry;
	}
    
    public OsmandSettings getSettings() {
		return osmandSettings;
	}
    
	public PoiFiltersHelper getPoiFilters() {
    	if(poiFilters == null){
    		poiFilters = new PoiFiltersHelper(this);
    	}
		return poiFilters;
	}
	
	public void setGpxFileToDisplay(GPXFileResult gpxFileToDisplay) {
		this.gpxFileToDisplay = gpxFileToDisplay;
		if(gpxFileToDisplay == null){
			getFavorites().setFavoritePointsFromGPXFile(null);
		} else {
			List<FavouritePoint> pts = new ArrayList<FavouritePoint>();
			for (WptPt p : gpxFileToDisplay.wayPoints) {
				FavouritePoint pt = new FavouritePoint();
				pt.setLatitude(p.lat);
				pt.setLongitude(p.lon);
				pt.setName(p.name);
				pts.add(pt);
			}
			getFavorites().setFavoritePointsFromGPXFile(pts);
		}
	}
	
	public GPXFileResult getGpxFileToDisplay() {
		return gpxFileToDisplay;
	}
    
    public FavouritesDbHelper getFavorites() {
    	if(favorites == null) {
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
		super.onConfigurationChanged(newConfig);
		if (prefferedLocale != null) {
			newConfig.locale = prefferedLocale;
			Locale.setDefault(prefferedLocale);
			getBaseContext().getResources().updateConfiguration(newConfig, getBaseContext().getResources().getDisplayMetrics());
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
	
	public ProgressDialog checkApplicationIsBeingInitialized(Context uiContext){
		// start application if it was previously closed
		startApplication();
		synchronized (OsmandApplication.this) {
			if(startDialog != null){
				progressDlg = ProgressDialog.show(uiContext, getString(R.string.loading_data), getString(R.string.reading_indexes), true);
				startDialog.setDialog(progressDlg);
				return progressDlg;
			}  else if(startingWarnings != null){
				showWarnings(startingWarnings, uiContext);
			}
		}
		return null;
	}
	
	public boolean isApplicationInitializing(){
		return startDialog != null;
	}
	
	public RoutingHelper getRoutingHelper() {
		return routingHelper;
	}
	
	public CommandPlayer getPlayer() {
		return player;
	}
	

	public void showDialogInitializingCommandPlayer(final Activity uiContext){
		String voiceProvider = osmandSettings.VOICE_PROVIDER.get();
		if(voiceProvider == null){
			Builder builder = new AlertDialog.Builder(uiContext);
			builder.setCancelable(true);
			builder.setNegativeButton(R.string.default_buttons_cancel, null);
			builder.setPositiveButton(R.string.default_buttons_ok, new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					uiContext.startActivity(new Intent(uiContext, SettingsActivity.class));
				}
			});
			builder.setTitle(R.string.voice_is_not_available_title);
			builder.setMessage(R.string.voice_is_not_available_msg);
			builder.show();
		} else {
			if(player == null 
					|| !Algoritms.objectEquals(voiceProvider, player.getCurrentVoice())){
				initVoiceDataInDifferentThread(uiContext);
			}
		}
		
	}

	private void initVoiceDataInDifferentThread(final Activity uiContext) {
		final ProgressDialog dlg = ProgressDialog.show(uiContext,
				getString(R.string.loading_data),
				getString(R.string.voice_data_initializing));
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					initCommandPlayer(uiContext);
					dlg.dismiss();
				} catch (CommandPlayerException e) {
					dlg.dismiss();
					showWarning(uiContext, e.getError());
				}
			}
		}).start();
	}
	
	public void initCommandPlayer(Activity ctx)
		throws CommandPlayerException
	{
		final String voiceProvider = osmandSettings.VOICE_PROVIDER.get();
		if (player == null || !Algoritms.objectEquals(voiceProvider, player.getCurrentVoice())) {
			if (player != null) {
				player.clear();
			}
			player = CommandPlayerFactory.createCommandPlayer(voiceProvider,OsmandApplication.this, ctx);
			routingHelper.getVoiceRouter().setPlayer(player);
		}
	}
	
	public NavigationService getNavigationService() {
		return navigationService;
	}
	
	public void setNavigationService(NavigationService navigationService) {
		this.navigationService = navigationService;
	}
	
	public synchronized void closeApplication(){
		if(applicationInitializing){
			manager.close();
		}
		applicationInitializing = false; 
	}
	

	public synchronized void startApplication() {
		if(applicationInitializing){
			return;
		}
		applicationInitializing = true;
		startDialog = new ProgressDialogImplementation(this, null, false);

		startDialog.setRunnable("Initializing app", new Runnable() { //$NON-NLS-1$

					@Override
					public void run() {
						List<String> warnings = null;
						try {
							warnings = manager.reloadIndexes(startDialog);
							player = null;
							SavingTrackHelper helper = new SavingTrackHelper(OsmandApplication.this);
							if (helper.hasDataToSave()) {
								startDialog.startTask(getString(R.string.saving_gpx_tracks), -1);
								warnings.addAll(helper.saveDataToGpx());
							}
							helper.close();

						} finally {
							synchronized (OsmandApplication.this) {
								startDialog = null;
								if (progressDlg != null) {
									progressDlg.dismiss();
									showWarnings(warnings, progressDlg.getContext());
									progressDlg = null;
								} else {
									startingWarnings = warnings;
								}
							}
						}
					}
				});
		startDialog.run();

		Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());

	}
	
	protected void showWarnings(List<String> warnings, final Context uiContext) {
		if (warnings != null && !warnings.isEmpty()) {
			final StringBuilder b = new StringBuilder();
			boolean f = true;
			for (String w : warnings) {
				if(f){
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
				Toast.makeText(uiContext, b.toString(), Toast.LENGTH_LONG).show();
			}
		});
	}
	

	private class DefaultExceptionHandler implements UncaughtExceptionHandler {

		private UncaughtExceptionHandler defaultHandler;

		public DefaultExceptionHandler() {
			defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
		}

		@Override
		public void uncaughtException(final Thread thread, final Throwable ex) {
			File file = osmandSettings.extendOsmandPath(EXCEPTION_PATH);
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintStream printStream = new PrintStream(out);
				ex.printStackTrace(printStream);
				StringBuilder msg = new StringBuilder();
				msg.append("Exception occured in thread " + thread.toString() + " : "). //$NON-NLS-1$ //$NON-NLS-2$
						append(DateFormat.format("MMMM dd, yyyy h:mm:ss", System.currentTimeMillis())).append("\n"). //$NON-NLS-1$//$NON-NLS-2$
						append(new String(out.toByteArray()));

				if (file.getParentFile().canWrite()) {
					BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
					writer.write(msg.toString());
					writer.close();
				}
				defaultHandler.uncaughtException(thread, ex);
			} catch (Exception e) {
				// swallow all exceptions
				Log.e(LogUtil.TAG, "Exception while handle other exception", e); //$NON-NLS-1$
			}

		}
	}
	
	
}
