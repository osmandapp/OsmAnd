package net.osmand.plus.profiles;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

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
				getSupportActionBar().setTitle(intent.getStringExtra("profile_name"));
			}
		}

//        if (savedInstanceState == null) {
//            SettingsProfileFragment profileFragment = new SettingsProfileFragment();
//            profileFragment.setArguments(getIntent().getExtras());
//            getSupportFragmentManager().beginTransaction().add(android.R.id.content, profileFragment).commit();
//        }
	}
}
