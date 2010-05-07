package com.osmand.activities;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.osmand.R;
import com.osmand.ResourceManager;

public class MainMenuActivity extends Activity {

	private Button showMap;
	private Button exitButton;
	private Button settingsButton;
	private NotificationManager mNotificationManager;
	private int APP_NOTIFICATION_ID;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.menu);

		mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		Intent mapIndent = new Intent(MainMenuActivity.this, MapActivity.class);
		Notification notification = new Notification(R.drawable.icon, "Notify",
				System.currentTimeMillis());
		notification.setLatestEventInfo(MainMenuActivity.this, "OsmAnd",
				"OsmAnd are running in background", PendingIntent.getActivity(
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
		exitButton = (Button) findViewById(R.id.ExitButton);
		exitButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				mNotificationManager.cancel(APP_NOTIFICATION_ID);
				MainMenuActivity.this.finish();
			}
		});

		// add notification

		final ProgressDialog dlg = ProgressDialog.show(this, "Loading data",
				"Reading indices...");
		new Thread() {
			@Override
			public void run() {
				try {
					dlg.setMessage("Indexing poi...");
					ResourceManager.getResourceManager().indexingPoi(dlg);
				} finally {
					dlg.dismiss();
				}
			}
		}.start();

	}

}
