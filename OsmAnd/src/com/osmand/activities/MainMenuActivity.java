package com.osmand.activities;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;
import java.text.MessageFormat;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

import com.osmand.LogUtil;
import com.osmand.ProgressDialogImplementation;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.Version;
import com.osmand.activities.search.SearchActivity;
import com.osmand.voice.CommandPlayer;

public class MainMenuActivity extends Activity {

	private static final String FIRST_TIME_APP_RUN = "FIRST_TIME_APP_RUN"; //$NON-NLS-1$
	private static boolean applicationAlreadyStarted = false;
	private static final String EXCEPTION_PATH = "/osmand/exception.log"; //$NON-NLS-1$
	private static final String EXCEPTION_FILE_SIZE = "/osmand/exception.log"; //$NON-NLS-1$
	
	
	private Button showMap;
	private Button settingsButton;
	private Button searchButton;
	private Button favouritesButton;
	private ProgressDialog progressDlg;
	

	
	public void startApplication(){
		if(!applicationAlreadyStarted){
			// Algoritms.removeAllFiles(new File(Environment.getExternalStorageDirectory(), "/osmand/tiles/Mapnik/18"));
			progressDlg = ProgressDialog.show(this, getString(R.string.loading_data), getString(R.string.reading_indexes), true);
			final ProgressDialogImplementation impl = new ProgressDialogImplementation(progressDlg);
			impl.setRunnable("Initializing app", new Runnable(){ //$NON-NLS-1$
				@Override
				public void run() {
					try {
						// initializing voice prolog subsystem
						
						List<String> warnings = ResourceManager.getResourceManager().reloadIndexes(impl);
						impl.startTask(getString(R.string.voice_data_initializing), -1);
						String w = CommandPlayer.init(MainMenuActivity.this);
						if(w != null){
							warnings.add(w);
						}
						SavingTrackHelper helper = new SavingTrackHelper(MainMenuActivity.this);
						if (helper.hasDataToSave()) {
							impl.startTask(getString(R.string.saving_gpx_tracks), -1);
							warnings.addAll(helper.saveDataToGpx());
						}
						helper.close();
						showWarnings(warnings);
					} finally {
						if(progressDlg != null){
							progressDlg.dismiss();
							progressDlg = null;
						}
					}
				}
			});
			impl.run();
			applicationAlreadyStarted = true;
			
			Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());
			
			long size = getPreferences(MODE_WORLD_READABLE).getLong(EXCEPTION_FILE_SIZE, 0);
			final File file = new File(Environment.getExternalStorageDirectory(), EXCEPTION_PATH);
			if(file.exists() && file.length() > 0){
				if(size != file.length()){
					String msg = MessageFormat.format(getString(R.string.previous_run_crashed),
							EXCEPTION_PATH);
					Builder builder = new AlertDialog.Builder(MainMenuActivity.this);
					builder.setMessage(msg).setNeutralButton(getString(R.string.close), null);
					builder.setPositiveButton(R.string.send_report, new DialogInterface.OnClickListener(){
						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = new Intent(Intent.ACTION_SEND);
							intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"osmand.app@gmail.com"}); //$NON-NLS-1$
							intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
							intent.setType("vnd.android.cursor.dir/email");  //$NON-NLS-1$
							intent.putExtra(Intent.EXTRA_SUBJECT, "OsmAnd bug"); //$NON-NLS-1$
							StringBuilder text = new StringBuilder();
							text.append("\nDevice : ").append(Build.DEVICE); //$NON-NLS-1$
							text.append("\nBrand : ").append(Build.BRAND); //$NON-NLS-1$
							text.append("\nModel : ").append(Build.MODEL); //$NON-NLS-1$
							text.append("\nProduct : ").append(Build.PRODUCT); //$NON-NLS-1$
							text.append("\nBuild : ").append(Build.DISPLAY); //$NON-NLS-1$
							text.append("\nVersion : ").append(Build.VERSION.RELEASE); //$NON-NLS-1$
							text.append("\nApp Version : ").append(Version.APP_NAME_VERSION); //$NON-NLS-1$
							try {
								PackageInfo info = getPackageManager().getPackageInfo(getPackageName(),	0);
								if (info != null) {
									text.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode); //$NON-NLS-1$ //$NON-NLS-2$
								}
							} catch (NameNotFoundException e) {
							}
							intent.putExtra(Intent.EXTRA_TEXT, text.toString());
							startActivity(Intent.createChooser(intent, getString(R.string.send_report)));
						}
						
					});
					builder.show();
					getPreferences(MODE_WORLD_READABLE).edit().putLong(EXCEPTION_FILE_SIZE, file.length()).commit();
				}
				
			} else {
				if(size > 0){
					getPreferences(MODE_WORLD_READABLE).edit().putLong(EXCEPTION_FILE_SIZE, 0).commit();
				}
			}
		}
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.menu);

		showMap = (Button) findViewById(R.id.MapButton);
		showMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent mapIndent = new Intent(MainMenuActivity.this, MapActivity.class);
				startActivityForResult(mapIndent, 0);
			}
		});
		settingsButton = (Button) findViewById(R.id.SettingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent settings = new Intent(MainMenuActivity.this, SettingsActivity.class);
				startActivity(settings);
			}
		});
		
		favouritesButton = (Button) findViewById(R.id.FavoritesButton);
		favouritesButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent settings = new Intent(MainMenuActivity.this, FavouritesActivity.class);
				startActivity(settings);
			}
		});
		
		searchButton = (Button) findViewById(R.id.SearchButton);
		searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent search = new Intent(MainMenuActivity.this, SearchActivity.class);
				startActivity(search);
			}
		});
		
		
//		exitButton = (Button) findViewById(R.id.ExitButton);
//		exitButton.setVisibility(View.INVISIBLE);
//		exitButton.setOnClickListener(new OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				finishApplication();
//			}
//		}); 
		
		startApplication();
		
		SharedPreferences pref = getPreferences(MODE_WORLD_WRITEABLE);
		if(!pref.contains(FIRST_TIME_APP_RUN)){
			pref.edit().putBoolean(FIRST_TIME_APP_RUN, true).commit();
			Builder builder = new AlertDialog.Builder(this);
			builder.setMessage(R.string.first_time_msg);
			builder.setPositiveButton(R.string.first_time_download, new DialogInterface.OnClickListener(){

				@Override
				public void onClick(DialogInterface dialog, int which) {
					startActivity(new Intent(MainMenuActivity.this, DownloadIndexActivity.class));
				}
				
			});
			builder.setNegativeButton(R.string.first_time_continue, null);
			
			builder.show();
		}
	}
	
	@Override
	protected void onStop() {
		if(progressDlg != null){
			progressDlg.dismiss();
			progressDlg = null;
		}
		super.onStop();
	}
	
	protected void showWarnings(List<String> warnings) {
		if (!warnings.isEmpty()) {
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
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Toast.makeText(MainMenuActivity.this, b.toString(), Toast.LENGTH_LONG).show();

				}
			});
		}
	}
	
	protected void finishApplication(){
		ResourceManager.getResourceManager().close();
		applicationAlreadyStarted = false;
		MainMenuActivity.this.finish();
	}
	
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_SEARCH
                && event.getRepeatCount() == 0) {
			final Intent search = new Intent(MainMenuActivity.this, SearchActivity.class);
			startActivity(search);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
	
	private class DefaultExceptionHandler implements UncaughtExceptionHandler {
		
		private UncaughtExceptionHandler defaultHandler;

		public DefaultExceptionHandler(){
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
				   append(DateFormat.format("MMMM dd, yyyy h:mm:ss", System.currentTimeMillis())).append("\n").  //$NON-NLS-1$//$NON-NLS-2$
					append(new String(out.toByteArray()));

				if(Environment.getExternalStorageDirectory().canRead()){
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
