package net.osmand.plus.activities;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnMenuItemClickListener;

public abstract class OsmandExpandableListFragment extends SherlockFragment implements OnChildClickListener {
	
	
	private ExpandableListView listView;
	private ExpandableListAdapter adapter;


	public OsmandApplication getMyApplication() {
		return (OsmandApplication)getActivity().getApplication();
	}
	
	@Override
	public View onCreateView(android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
		View v = createView(inflater, container);
		listView = (ExpandableListView) v.findViewById(android.R.id.list);
		listView.setOnChildClickListener(this);
		if(this.adapter != null) {
			listView.setAdapter(this.adapter);
		}
		return v;
	}

	public View createView(android.view.LayoutInflater inflater, android.view.ViewGroup container) {
		setHasOptionsMenu(true);
		return inflater.inflate(R.layout.expandable_list, container, false);
	}
	
	public void setAdapter(ExpandableListAdapter a) {
		this.adapter = a;
		if(listView != null) {
			listView.setAdapter(a);
		}
		
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

	public ExpandableListView getExpandableListView() { return listView; }

	public void setListView(ExpandableListView listView) {
		this.listView = listView;
		listView.setOnChildClickListener(this);
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
		return ((OsmandApplication) getActivity().getApplication()).getSettings().isLightActionBar();
	}
	
	
	public void collapseTrees(final int count) {
		getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				synchronized (adapter) {
					final ExpandableListView expandableListView = getExpandableListView();
					for (int i = 0; i < adapter.getGroupCount(); i++) {
						int cp = adapter.getChildrenCount(i);
						if (cp < count) {
							expandableListView.expandGroup(i);
						} else {
							expandableListView.collapseGroup(i);
						}
					}
				}
			}
		});

	}
}
