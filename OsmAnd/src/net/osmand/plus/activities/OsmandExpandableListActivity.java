package net.osmand.plus.activities;

import android.app.ActionBar;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;


public abstract class OsmandExpandableListActivity extends
		ActionBarProgressActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}
	
	protected void onStart() {
		super.onStart();
		getExpandableListView().setBackgroundColor(
				getResources().getColor(
						getMyApplication().getSettings().isLightContent() ? R.color.list_background_color_light
								: R.color.list_background_color_dark));
	};


	public OsmandApplication getMyApplication() {
		return (OsmandApplication) getApplication();
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

	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconLight, int iconDark, int menuItemType) {
		int r = isLightActionBar() ? iconLight : iconDark;
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (r != 0) {
			menuItem.setIcon(r);
		}
		menuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				return onOptionsItemSelected(item);
			}
		});
		menuItem.setShowAsAction(menuItemType);
		return menuItem;
	}

	public MenuItem createMenuItem(Menu m, int id, int titleRes, int icon, int menuItemType) {
		return createMenuItem(m, id, titleRes, icon, icon, menuItemType);
	}

	public void fixBackgroundRepeat(View view) {
		Drawable bg = view.getBackground();
		if (bg != null) {
			if (bg instanceof BitmapDrawable) {
				BitmapDrawable bmp = (BitmapDrawable) bg;
				// bmp.mutate(); // make sure that we aren't sharing state anymore
				bmp.setTileModeXY(TileMode.REPEAT, TileMode.REPEAT);
			}
		}
	}


	public void setListAdapter(OsmandBaseExpandableListAdapter adapter) {
		((ExpandableListView) findViewById(android.R.id.list)).setAdapter(adapter);
	}

	public ExpandableListView getExpandableListView() {
		return (ExpandableListView) findViewById(android.R.id.list);
	}

	public void setOnChildClickListener(ExpandableListView.OnChildClickListener childClickListener) {
		((ExpandableListView) findViewById(android.R.id.list)).setOnChildClickListener(childClickListener);
	}

	public boolean isLightActionBar() {
		return ((OsmandApplication) getApplication()).getSettings().isLightActionBar();
	}
}
