package com.osmand.activities;

import java.util.concurrent.Callable;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;

import com.osmand.R;
import com.osmand.ResourceManager;
import com.osmand.services.LongRunningActionCallback;
import com.osmand.services.LongRunningActionDispatcher;

public class MainMenuActivity extends Activity implements
		LongRunningActionCallback {

	private Button showMap;
	private Button exitButton;
	private Button settingsButton;
	private ProgressBar loadProgress;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.menu);
		
		

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
				MainMenuActivity.this.finish();
			}
		});

		loadProgress = (ProgressBar) findViewById(R.id.LoadProgressBar);
		loadProgress.setHorizontalScrollBarEnabled(true);

//		startLongRunningOperation();

	}

	private LongRunningActionDispatcher dispatcher;

	@SuppressWarnings("unchecked")
	private void startLongRunningOperation() {
		this.dispatcher = new LongRunningActionDispatcher(this, this);
		dispatcher.startLongRunningAction(new Callable() {
			public Void  call() throws Exception {
				ResourceManager.getResourceManager().indexingPoi();
				return null;
			}
		}, "Dialog Title", "Dialog message");
	}

	@Override
	public void onLongRunningActionFinished(Exception error) {
		// TODO Auto-generated method stub

	}

}
