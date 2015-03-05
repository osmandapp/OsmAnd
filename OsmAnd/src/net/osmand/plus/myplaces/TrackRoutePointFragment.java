package net.osmand.plus.myplaces;

import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.R;
import net.osmand.plus.activities.TrackActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Denis
 * on 05.03.2015.
 */
public class TrackRoutePointFragment extends SelectedGPXFragment {
	GpxSelectionHelper.GpxDisplayGroup group;
	@Override
	public void setContent() {
		List<GpxSelectionHelper.GpxDisplayGroup> groups = filterGroups();
		lightContent = app.getSettings().isLightContent();


		List<GpxSelectionHelper.GpxDisplayItem> items = new ArrayList<>();
		for (GpxSelectionHelper.GpxDisplayGroup group : groups) {
			if (group.getType() != GpxSelectionHelper.GpxDisplayItemType.TRACK_ROUTE_POINTS){
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

		MenuItem item = menu.add(R.string.settings).setIcon(R.drawable.ic_overflow_menu_light).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				saveAsFavorites(group);
				return true;
			}
		});
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	}
}
