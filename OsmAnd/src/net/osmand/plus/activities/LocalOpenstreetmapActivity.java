package net.osmand.plus.activities;

import net.osmand.plus.OsmandSettings;
import net.osmand.plus.R;

import android.app.ExpandableListActivity;
import android.os.Bundle;

public class LocalOpenstreetmapActivity extends ExpandableListActivity {

	private OsmandSettings settings;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.local_openstreetmap);
		settings = OsmandSettings.getOsmandSettings(this);
	}
	
}
