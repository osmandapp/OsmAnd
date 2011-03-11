package net.osmand.activities;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.List;

import net.osmand.Algoritms;
import net.osmand.FavouritesDbHelper;
import net.osmand.LogUtil;
import net.osmand.OsmandSettings;
import net.osmand.PoiFiltersHelper;
import net.osmand.ProgressDialogImplementation;
import net.osmand.R;
import net.osmand.ResourceManager;
import net.osmand.voice.CommandPlayer;
import android.app.AlertDialog;
import android.app.Application;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
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
	
	
	// start variables
	private ProgressDialogImplementation startDialog;
	private List<String> startingWarnings;
	private ProgressDialog progressDlg;
	private Handler uiHandler;
	private DayNightHelper daynightHelper;
	
	
    public void	onCreate(){
    	super.onCreate();
    	routingHelper = new RoutingHelper(OsmandSettings.getApplicationMode(OsmandSettings.getPrefs(OsmandApplication.this)), OsmandApplication.this, player);
    	manager = new ResourceManager(this);
    	daynightHelper = new DayNightHelper(this);
    	uiHandler = new Handler();
    	startApplication();
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
	
	public ProgressDialog checkApplicationIsBeingInitialized(Context uiContext){
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
		String voiceProvider = OsmandSettings.getVoiceProvider(OsmandSettings.getPrefs(this));
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
		}
		if(player == null 
				|| !Algoritms.objectEquals(voiceProvider, player.getCurrentVoice())){
			initVoiceDataInDifferentThread(uiContext);
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
		return player.init(OsmandSettings.getVoiceProvider(OsmandSettings.getPrefs(this)));
	}

	public void startApplication() {
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
			File file = new File(Environment.getExternalStorageDirectory(), EXCEPTION_PATH);
			try {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				PrintStream printStream = new PrintStream(out);
				ex.printStackTrace(printStream);
				StringBuilder msg = new StringBuilder();
				msg.append("Exception occured in thread " + thread.toString() + " : "). //$NON-NLS-1$ //$NON-NLS-2$
						append(DateFormat.format("MMMM dd, yyyy h:mm:ss", System.currentTimeMillis())).append("\n"). //$NON-NLS-1$//$NON-NLS-2$
						append(new String(out.toByteArray()));

				if (Environment.getExternalStorageDirectory().canRead()) {
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
