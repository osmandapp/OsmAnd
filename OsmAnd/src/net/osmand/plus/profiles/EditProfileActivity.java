package net.osmand.plus.profiles;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;
import net.osmand.util.Algorithms;

public class EditProfileActivity extends OsmandActionBarActivity {

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_fragment_layout);

        if (savedInstanceState == null) {
            EditProfileFragment editProfileFragment = new EditProfileFragment();
	        editProfileFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(android.R.id.content,
	            editProfileFragment, "editProfileFragment").commit();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		MenuItem m = menu.add(0, 0, 0, R.string.action_delete).setIcon(R.drawable.ic_action_delete_dark);
		MenuItemCompat.setShowAsAction(m, MenuItem.SHOW_AS_ACTION_ALWAYS);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {



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
