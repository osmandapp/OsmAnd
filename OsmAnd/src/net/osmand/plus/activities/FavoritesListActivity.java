package net.osmand.plus.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.fragment.app.FragmentManager;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;


public class FavoritesListActivity extends OsmandActionBarActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_fragment_layout);
		if (savedInstanceState == null) {
			FragmentManager fragmentManager = getSupportFragmentManager();
			if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, FavoritesListFragment.TAG)) {
				// During initial setup, plug in the details fragment.
				FavoritesListFragment fragment = new FavoritesListFragment();
				fragment.setArguments(getIntent().getExtras());
				fragmentManager.beginTransaction()
						.add(android.R.id.content, fragment, FavoritesListFragment.TAG)
						.commitAllowingStateLoss();
			}
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int itemId = item.getItemId();
		if (itemId == android.R.id.home) {
			finish();
			return true;
		}
		return false;
	}
}