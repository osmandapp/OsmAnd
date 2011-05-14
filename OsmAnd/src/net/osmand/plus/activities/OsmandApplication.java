package net.osmand.plus.activities;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;
import java.util.Locale;

import net.osmand.Algoritms;
import net.osmand.LogUtil;
import net.osmand.plus.FavouritesDbHelper;
import net.osmand.plus.NavigationService;
import net.osmand.plus.OsmandSettings;
import net.osmand.plus.PoiFiltersHelper;
import net.osmand.plus.ProgressDialogImplementation;
import net.osmand.plus.R;
import net.osmand.plus.ResourceManager;
import net.osmand.plus.voice.CommandPlayer;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
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
	
	ResourceManager manager = null; 
	PoiFiltersHelper poiFilters = null;
	RoutingHelper routingHelper = null;
	FavouritesDbHelper favorites = null;
	CommandPlayer player = null;
	OsmandSettings osmandSettings;
	
	
	// start variables
	private ProgressDialogImplementation startDialog;
	private List<String> startingWarnings;
	private ProgressDialog progressDlg;
	private Handler uiHandler;
	private DayNightHelper daynightHelper;
	private NavigationService navigationService;
	private boolean applicationInitializing = false;
	private Locale prefferedLocale = null;

	
    public void	onCreate(){
    	super.onCreate();
    	osmandSettings = OsmandSettings.getOsmandSettings(this);
    	routingHelper = new RoutingHelper(osmandSettings, OsmandApplication.this, player);
    	manager = new ResourceManager(this);
    	daynightHelper = new DayNightHelper(this);
    	uiHandler = new Handler();
    	checkPrefferedLocale();
    	startApplication();
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
	

	public void showDialogInitializingCommandPlayer(final Context uiContext){
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

	private void initVoiceDataInDifferentThread(Context uiContext) {
		final ProgressDialog dlg = ProgressDialog.show(uiContext, getString(R.string.loading_data), getString(R.string.voice_data_initializing));
		new Thread(new Runnable() {
			@Override
			public void run() {
				String w = null;
				try {
					w = initCommandPlayer();
				} finally {
					dlg.dismiss();
				}
				if(w != null){
					showWarning(dlg.getContext(), w);
				}
			}
		}).start();
	}
	
	public String initCommandPlayer() {
		if (player == null) {
			player = new CommandPlayer(OsmandApplication.this);
			routingHelper.getVoiceRouter().setPlayer(player);
		}
		return player.init(osmandSettings.VOICE_PROVIDER.get());
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
