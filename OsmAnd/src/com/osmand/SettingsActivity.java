package com.osmand;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckBox;

public class SettingsActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);
		
		final CheckBox useInternet = (CheckBox)findViewById(R.id.use_internet_to_download_tile);
		useInternet.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				DefaultLauncherConstants.loadMissingImages = useInternet.isChecked();
			}
			
		});
		useInternet.setChecked(DefaultLauncherConstants.loadMissingImages);
		
		final CheckBox showGPSText = (CheckBox)findViewById(R.id.show_gps_coordinates_text);
		showGPSText.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				DefaultLauncherConstants.showGPSCoordinates = showGPSText.isChecked();
			}
			
		});
		showGPSText.setChecked(DefaultLauncherConstants.showGPSCoordinates);
    }

}
