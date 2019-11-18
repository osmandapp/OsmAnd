package net.osmand.plus.profiles;

import android.os.Bundle;
import android.view.MenuItem;

import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;

public class SettingsProfileActivity extends OsmandActionBarActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_fragment_layout);
		getSupportActionBar().setElevation(5.0f);
		getSupportActionBar().setTitle(R.string.application_profiles);
		if (savedInstanceState == null) {
			SettingsProfileFragment profileFragment = new SettingsProfileFragment();
			profileFragment.setArguments(getIntent().getExtras());
			getSupportFragmentManager().beginTransaction()
					.add(android.R.id.content, profileFragment, SettingsProfileFragment.class.getName()).commit();
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

	@Override
	protected void onResume() {
		super.onResume();
	}


}
