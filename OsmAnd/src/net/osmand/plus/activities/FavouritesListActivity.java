package net.osmand.plus.activities;

import net.osmand.plus.OsmandApplication;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class FavouritesListActivity extends SherlockFragmentActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			FavouritesListFragment details = new FavouritesListFragment();
			details.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction().add(android.R.id.content, details).commit();
		}
	}

	@Override
	public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
		int itemId = item.getItemId();
		switch (itemId) {
		case android.R.id.home:
			finish();
			return true;

		}
		return false;
	}
}