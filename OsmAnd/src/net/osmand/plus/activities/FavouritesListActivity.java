package net.osmand.plus.activities;

import android.view.MenuItem;
import net.osmand.plus.OsmandApplication;
import android.os.Bundle;


public class FavouritesListActivity extends OsmandActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			FavouritesListFragment details = new FavouritesListFragment();
			details.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, details).commit();
		}
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