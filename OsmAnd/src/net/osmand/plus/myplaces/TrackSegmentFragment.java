package net.osmand.plus.myplaces;

import android.os.Bundle;
import android.support.v4.view.MenuItemCompat;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.TrackActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 04.03.2015.
 */
public class TrackSegmentFragment extends SelectedGPXFragment {
	GpxSelectionHelper.GpxDisplayGroup group;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = super.onCreateView(inflater, container, savedInstanceState);
		ListView listView = (ListView) view.findViewById(android.R.id.list);
		listView.setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
				long packedPos = ((AdapterView.AdapterContextMenuInfo) menuInfo).position;
				showContextMenu(adapter.getItem((int)packedPos));
			}
		});
		return view;
	}

	@Override
	public void setContent() {
		List<GpxSelectionHelper.GpxDisplayGroup> groups = filterGroups();
		lightContent = app.getSettings().isLightContent();


		List<GpxSelectionHelper.GpxDisplayItem> items = new ArrayList<>();
		for (GpxSelectionHelper.GpxDisplayGroup group : groups) {
			if (group.getType() != GpxSelectionHelper.GpxDisplayItemType.TRACK_SEGMENT){
				continue;
			}
			this.group = group;
			for (GpxSelectionHelper.GpxDisplayItem item : group.getModifiableList()) {
				items.add(item);
			}
		}
		adapter = new SelectedGPXAdapter(items);
		setListAdapter(adapter);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		menu.clear();
		((TrackActivity) getActivity()).getClearToolbar(false);
		if (isArgumentTrue(ARG_TO_HIDE_CONFIG_BTN)){
			return;
		}

		MenuItem item = menu.add(R.string.showed_on_map).setIcon(R.drawable.ic_show_on_map).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				selectSplitDistance(group);
				return true;
			}
		});
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	}
}
