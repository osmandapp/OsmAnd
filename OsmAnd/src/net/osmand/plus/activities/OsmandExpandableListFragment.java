package net.osmand.plus.activities;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import android.app.ActionBar;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;

import com.actionbarsherlock.app.SherlockExpandableListActivity;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

public abstract class OsmandExpandableListFragment extends SherlockFragment {
	
	
	public OsmandApplication getMyApplication() {
		return (OsmandApplication)getActivity().getApplication();
	}
	
	public 
	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
		com.android.internal.R.layout.list_content
		listView = new ExpandableListView(getActivity());
		return listView;
	}

	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconLight, int iconDark, int menuItemType) {
		int r = isLightActionBar() ? iconLight : iconDark;
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (r != 0) {
			menuItem.setIcon(r);
		}
		menuItem.setShowAsActionFlags(menuItemType).setOnMenuItemClickListener(new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(com.actionbarsherlock.view.MenuItem item) {
				return onOptionsItemSelected(item);
			}
		});
		return menuItem;
	}
	
	
	public boolean isLightActionBar() {
		return ((OsmandApplication) getApplication()).getSettings().isLightActionBar();
	}
}
