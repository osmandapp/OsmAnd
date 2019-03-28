package net.osmand.plus.profiles;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.view.MenuItem;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;

public class SelectedProfileActivity extends OsmandActionBarActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_fragment_layout);

		Intent intent = getIntent();
		if (intent.getExtras() != null) {
			if (getSupportActionBar() != null) {
				getSupportActionBar().setTitle(((AppProfile) intent.getParcelableExtra("profile")).getTitle());
				getSupportActionBar().setElevation(5.0f);
			}
		}

        if (savedInstanceState == null) {
            SelectedProfileFragment selectedProfileFragment = new SelectedProfileFragment();
	        selectedProfileFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, selectedProfileFragment).commit();
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
