package net.osmand.plus.base;

import android.app.ActionBar;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.ActionBarProgressActivity;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;


public abstract class OsmandListActivity extends
		ActionBarProgressActivity implements AdapterView.OnItemClickListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		((OsmandApplication) getApplication()).applyTheme(this);
		super.onCreate(savedInstanceState);
		getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
	}

	@Override
	protected void onStart() {
		super.onStart();
		OsmandApplication app = getMyApplication();
		boolean nightMode = !app.getSettings().isLightContent();
		getListView().setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
		getListView().setDivider(app.getUIUtilities().getIcon(R.drawable.divider_solid, ColorUtilities.getDividerColorId(nightMode)));
		getListView().setDividerHeight(AndroidUtils.dpToPx(app, 1));
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

	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconDark, int menuItemType) {
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (iconDark != 0) {
			menuItem.setIcon(getMyApplication().getUIUtilities().getIcon(iconDark));
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

	public void setListAdapter(ListAdapter adapter) {
		((ListView) findViewById(android.R.id.list)).setAdapter(adapter);
		setOnItemClickListener(this);

	}

	public ListView getListView() {
		return findViewById(android.R.id.list);
	}

	public ListAdapter getListAdapter() {
		ListAdapter adapter = getListView().getAdapter();
		if (adapter instanceof HeaderViewListAdapter) {
			return ((HeaderViewListAdapter) adapter).getWrappedAdapter();
		} else {
			return adapter;
		}
	}

	public void setOnItemClickListener(AdapterView.OnItemClickListener childClickListener) {
		((ListView) findViewById(android.R.id.list)).setOnItemClickListener(childClickListener);
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

	}
}
