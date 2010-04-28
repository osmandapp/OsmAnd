package com.osmand;

import java.io.File;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;

public class StartActivity extends Activity {
    /** Called when the activity is first created. */
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// SurfaceView surf = (SurfaceView) findViewById(R.id.SurfaceView01);
		setContentView(new OsmandMapTileView(this, new File(Environment.getExternalStorageDirectory(), "osmand/tiles/Mapnik")));
		// setContentView(R.layout.main);
	}
    
    
}