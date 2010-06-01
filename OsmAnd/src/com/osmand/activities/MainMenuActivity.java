package com.osmand.activities;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.app.AlertDialog.Builder;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.osmand.LogUtil;
import com.osmand.ProgressDialogImplementation;
import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.activities.search.SearchActivity;

public class MainMenuActivity extends Activity {

	private static boolean applicationAlreadyStarted = false;
	private static final String EXCEPTION_PATH = "/osmand/exception.log";
	private static final String EXCEPTION_FILE_SIZE = "/osmand/exception.log";
	
	private Button showMap;
	private Button exitButton;
	private Button settingsButton;
	private Button searchButton;
	private NotificationManager mNotificationManager;
	private int APP_NOTIFICATION_ID;
	

	
	public void startApplication(){
		if(!applicationAlreadyStarted){
			// TODO exception!!! has leaked window ?
			final ProgressDialog dlg = ProgressDialog.show(this, "Loading data", "Reading indices...", true);
			final ProgressDialogImplementation impl = new ProgressDialogImplementation(dlg);
			impl.setRunnable("Initializing app", new Runnable(){
				@Override
				public void run() {
					try {
						ResourceManager.getResourceManager().indexingPoi(impl);
						ResourceManager.getResourceManager().indexingAddresses(impl);
					} finally {
						dlg.dismiss();
					}
				}
			});
			impl.run();
			applicationAlreadyStarted = true;
			Thread.setDefaultUncaughtExceptionHandler(new DefaultExceptionHandler());
			
			long size = getPreferences(MODE_WORLD_READABLE).getLong(EXCEPTION_FILE_SIZE, 0);
			File file = new File(Environment.getExternalStorageDirectory(), EXCEPTION_PATH);
			if(file.exists() && file.length() > 0){
				if(size != file.length()){
					String msg = "Previous application run was crashed. Log file is in " + EXCEPTION_PATH +". ";
					msg += "Please raise the issue and attach log file.";
					Builder builder = new AlertDialog.Builder(MainMenuActivity.this);
					builder.setMessage(msg).setNeutralButton("Close", null).show();
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

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		Intent mapIndent = new Intent(MainMenuActivity.this, MapActivity.class);
		Notification notification = new Notification(R.drawable.icon, "",
				System.currentTimeMillis());
		notification.setLatestEventInfo(MainMenuActivity.this, "OsmAnd",
				"OsmAnd is running in background", PendingIntent.getActivity(
						this.getBaseContext(), 0, mapIndent,
						PendingIntent.FLAG_CANCEL_CURRENT));
		mNotificationManager.notify(APP_NOTIFICATION_ID, notification);

		showMap = (Button) findViewById(R.id.MapButton);
		showMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent mapIndent = new Intent(MainMenuActivity.this,
						MapActivity.class);
				startActivityForResult(mapIndent, 0);

			}
		});
		settingsButton = (Button) findViewById(R.id.SettingsButton);
		settingsButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent settings = new Intent(MainMenuActivity.this,
						SettingsActivity.class);
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
		
		exitButton = (Button) findViewById(R.id.ExitButton);
		exitButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mNotificationManager.cancel(APP_NOTIFICATION_ID);
				MainMenuActivity.this.finish();
				System.exit(0);
			}
		});
		
		startApplication();
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
				msg.append("Exception occured in thread " + thread.toString() + " : ").
				   append(DateFormat.format("MMMM dd, yyyy h:mm:ss", System.currentTimeMillis())).append("\n").
					append(new String(out.toByteArray()));

				if(Environment.getExternalStorageDirectory().canRead()){
					BufferedWriter writer = new BufferedWriter(new FileWriter(file, true));
					writer.write(msg.toString());
					writer.close();
				}
				defaultHandler.uncaughtException(thread, ex);
			} catch (Exception e) {
				// swallow all exceptions
				Log.e(LogUtil.TAG, "Exception while handle other exception", e);
			}
			
		}
	}

}
