package net.osmand.plus.activities;

import net.osmand.plus.OsmandApplication;
import android.app.ActionBar;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockListActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

public abstract class OsmandListActivity extends SherlockListActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		setActionBarSettings();
	}

	protected void setActionBarSettings() {
		getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
	}

	protected OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
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
	
	public void createMenuItem(Menu m, int id, int titleRes, int iconLight, int iconDark, int menuItemType) {
		com.actionbarsherlock.view.MenuItem menuItem = m.add(0, id, 0, titleRes);
		int res = isLightActionBar() ? iconLight : iconDark;
		if(res != 0) {
			menuItem = menuItem.setIcon(res);
		}
		menuItem.setShowAsActionFlags(menuItemType)
				.setOnMenuItemClickListener(new OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
						return onOptionsItemSelected(item);
					}
				});

	}
	
	public boolean isLightActionBar() {
		return ((OsmandApplication) getApplication()).getSettings().isLightActionBar();
	}
}
