package net.osmand.plus.base;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.enums.ThemeUsageContext;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;

public abstract class OsmandExpandableListFragment extends BaseFullScreenFragment implements OnChildClickListener {

	protected ExpandableListView listView;
	protected ExpandableListAdapter adapter;

	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		updateNightMode();
		View v = createView(inflater, container);
		listView = v.findViewById(android.R.id.list);
		listView.setOnChildClickListener(this);
		if (this.adapter != null) {
			listView.setAdapter(this.adapter);
		}
		return v;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		getExpandableListView().setBackgroundColor(ColorUtilities.getListBgColor(app, nightMode));
	}

	public View createView(android.view.LayoutInflater inflater, android.view.ViewGroup container) {
		setHasOptionsMenu(true);
		return inflater.inflate(R.layout.expandable_list, container, false);
	}

	public void setAdapter(ExpandableListAdapter adapter) {
		this.adapter = adapter;
		if (listView != null) {
			listView.setAdapter(adapter);
		}
	}

	public ExpandableListAdapter getAdapter() {
		return adapter;
	}

	public ExpandableListView getExpandableListView() {
		return listView;
	}

	public void setListView(ExpandableListView listView) {
		this.listView = listView;
		listView.setOnChildClickListener(this);
	}

	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconId, int menuItemType) {
		int color = ColorUtilities.getActiveButtonsAndLinksTextColorId(isNightMode());
		return createMenuItem(m, id, titleRes, iconId, menuItemType, false, color);
	}

	public MenuItem createMenuItem(Menu m, int id, int titleRes, int iconId, int menuItemType,
	                               boolean flipIconForRtl, int iconColor) {
		Drawable drawable = iconId == 0 ? null : uiUtilities.getIcon(iconId, iconColor);
		MenuItem menuItem = m.add(0, id, 0, titleRes);
		if (drawable != null) {
			if (flipIconForRtl) {
				drawable = AndroidUtils.getDrawableForDirection(app, drawable);
			}
			menuItem.setIcon(drawable);
		}
		menuItem.setOnMenuItemClickListener(this::onOptionsItemSelected);
		menuItem.setShowAsAction(menuItemType);
		return menuItem;
	}

	public boolean isNightMode() {
		Activity activity = getActivity();
		if (activity != null) {
			OsmandApplication app = (OsmandApplication) activity.getApplication();
			return app.getDaynightHelper().isNightMode(ThemeUsageContext.APP);
		}
		return false;
	}

	public void collapseTrees(int count) {
		Activity activity = getActivity();
		if (activity != null) {
			activity.runOnUiThread(() -> {
				synchronized (adapter) {
					ExpandableListView expandableListView = getExpandableListView();
					for (int i = 0; i < adapter.getGroupCount(); i++) {
						int cp = adapter.getChildrenCount(i);
						if (cp < count) {
							expandableListView.expandGroup(i);
						} else {
							expandableListView.collapseGroup(i);
						}
					}
				}
			});
		}
	}
}
