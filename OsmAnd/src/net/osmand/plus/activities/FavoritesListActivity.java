package net.osmand.plus.activities;

import android.view.MenuItem;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;


public class FavoritesListActivity extends OsmandActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_fragment_layout);
		if (savedInstanceState == null) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			if (fragmentManager.findFragmentByTag(FavoritesListFragment.TAG) == null) {
				// During initial setup, plug in the details fragment.
				FavoritesListFragment details = new FavoritesListFragment();
				details.setArguments(getIntent().getExtras());
				fragmentManager.beginTransaction()
						.add(android.R.id.content, details, FavoritesListFragment.TAG)
						.commitAllowingStateLoss();
			}
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