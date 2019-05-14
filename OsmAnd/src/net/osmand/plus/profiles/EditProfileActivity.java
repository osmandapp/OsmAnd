package net.osmand.plus.profiles;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuItem;

import android.view.View;
import android.view.inputmethod.InputMethodManager;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;

public class EditProfileActivity extends OsmandActionBarActivity {

	public static final int DELETE_ID = 1010;
	public static final String FRAGMENT_TAG = "editProfileFragment";

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		getMyApplication().applyTheme(this);
		super.onCreate(savedInstanceState);
		setContentView(R.layout.single_fragment_layout);

        if (savedInstanceState == null) {
            EditProfileFragment editProfileFragment = new EditProfileFragment();
	        editProfileFragment.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(android.R.id.content,
	            editProfileFragment, FRAGMENT_TAG).commit();
        }
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem m = menu.add(0, DELETE_ID,0, R.string.action_delete).setIcon(R.drawable.ic_action_delete_dark);
		MenuItemCompat.setShowAsAction(m, MenuItem.SHOW_AS_ACTION_ALWAYS);
		super.onCreateOptionsMenu(menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		int itemId = item.getItemId();
		switch (itemId) {
			case android.R.id.home:
				finish();
				return true;
			case DELETE_ID:
				((EditProfileFragment)getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG)).onDeleteProfileClick();
				return true;

		}
		return false;
	}
}
