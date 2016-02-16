package net.osmand.plus.myplaces;

import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayItemType;
import net.osmand.plus.R;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

/**
 * Created by Denis
 * on 04.03.2015.
 */
public class TrackPointFragment extends SelectedGPXFragment {
	
	@Override
	protected GpxDisplayItemType filterType() {
		return GpxSelectionHelper.GpxDisplayItemType.TRACK_POINTS;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		MenuItem item = menu.add(R.string.shared_string_add_to_favorites).setIcon(R.drawable.ic_action_fav_dark).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				saveAsFavorites(filterType());
				return true;
			}
		});
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);

		if (app.getSettings().USE_MAP_MARKERS.get()) {
			item = menu.add(R.string.shared_string_add_to_map_markers).setIcon(R.drawable.ic_action_flag_dark)
					.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							saveAsMapMarkers(GpxSelectionHelper.GpxDisplayItemType.TRACK_POINTS);
							return true;
						}
					});
			MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
		}
	}
}
