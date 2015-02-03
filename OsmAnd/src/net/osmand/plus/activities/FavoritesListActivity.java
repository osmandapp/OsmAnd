package net.osmand.plus.activities;

import android.view.MenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import android.os.Bundle;


public class FavoritesListActivity extends OsmandActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_fragment_layout);
		if (savedInstanceState == null) {
			// During initial setup, plug in the details fragment.
			FavoritesListFragment details = new FavoritesListFragment();
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