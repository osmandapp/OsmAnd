package net.osmand.plus.osmedit;

import android.os.Bundle;
import android.view.MenuItem;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActionBarProgressActivity;


public class LocalOpenstreetmapActivity extends ActionBarProgressActivity {


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.local_openstreetmap);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				finish();
				return true;

		}
		return false;
	}
}
