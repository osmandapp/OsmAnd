package net.osmand.plus.base;

import android.app.Activity;
import android.graphics.Shader.TileMode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

import androidx.annotation.NonNull;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.OsmandActionBarActivity;

public abstract class OsmandExpandableListFragment extends BaseOsmAndFragment
		implements OnChildClickListener {

	protected ExpandableListView listView;
	protected ExpandableListAdapter adapter;

	@Override
	public View onCreateView(@NonNull android.view.LayoutInflater inflater, android.view.ViewGroup container, Bundle savedInstanceState) {
		View v = createView(inflater, container);
		listView = (ExpandableListView) v.findViewById(android.R.id.list);
		listView.setOnChildClickListener(this);
		if (this.adapter != null) {
			listView.setAdapter(this.adapter);
		}
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getExpandableListView().setBackgroundColor(
				getResources().getColor(
						getMyApplication().getSettings().isLightContent() ? R.color.list_background_color_light
								: R.color.list_background_color_dark));
	}

	public View createView(android.view.LayoutInflater inflater, android.view.ViewGroup container) {
		setHasOptionsMenu(true);
		return inflater.inflate(R.layout.expandable_list, container, false);
	}

	public void setAdapter(ExpandableListAdapter a) {
		this.adapter = a;
		if (listView != null) {
			listView.setAdapter(a);
		}

	}

	public ExpandableListAdapter getAdapter() {
		return adapter;
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

	public ExpandableListView getExpandableListView() {
		return listView;
	}

	public void setListView(ExpandableListView listView) {
		this.listView = listView;
		listView.setOnChildClickListener(this);
	}

	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconId, int menuItemType) {
		return createMenuItem(m, id, titleRes, iconId, menuItemType, false);
	}

	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconId, int menuItemType,
	                               boolean flipIconForRtl) {
		int color = isLightActionBar() ? R.color.active_buttons_and_links_text_light : R.color.active_buttons_and_links_text_dark;
		return createMenuItem(m, id, titleRes, iconId, menuItemType, false, color);
	}

	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconId, int menuItemType,
	                               boolean flipIconForRtl, int iconColor) {
		OsmandApplication app = requireMyApplication();
		Drawable d = iconId == 0 ? null : app.getUIUtilities().getIcon(iconId, iconColor);
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (d != null) {
			if (flipIconForRtl) {
				d = AndroidUtils.getDrawableForDirection(app, d);
			}
			menuItem.setIcon(d);
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


	public boolean isLightActionBar() {
		Activity activity = getActivity();
		return activity == null || ((OsmandApplication) activity.getApplication()).getSettings().isLightActionBar();
	}


	public void collapseTrees(final int count) {
		Activity activity = getActivity();
		if (activity != null) {
			activity.runOnUiThread(new Runnable() {
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

	public OsmandActionBarActivity getActionBarActivity() {
		if (getActivity() instanceof OsmandActionBarActivity) {
			return (OsmandActionBarActivity) getActivity();
		}
		return null;
	}
}
