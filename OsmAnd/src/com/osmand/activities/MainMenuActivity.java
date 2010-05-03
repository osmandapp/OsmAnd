package com.osmand.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.osmand.R;

public class MainMenuActivity extends Activity {

	private Button showMap;
	private Button exitButton;
	private Button settingsButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);  
		setContentView(R.layout.menu);
		
		showMap = (Button) findViewById(R.id.MapButton);
		showMap.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				final Intent settings = new Intent(MainMenuActivity.this, MapActivity.class);
				startActivity(settings);
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
		exitButton = (Button) findViewById(R.id.ExitButton);
		exitButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				MainMenuActivity.this.finish();
			}
		});
	}
}
