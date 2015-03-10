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
public class TrackSegmentFragment extends SelectedGPXFragment {

	@Override
	protected GpxDisplayItemType filterType() {
		return GpxSelectionHelper.GpxDisplayItemType.TRACK_SEGMENT;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		MenuItem item = menu.add(R.string.shared_string_show_on_map).setIcon(R.drawable.ic_show_on_map).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				selectSplitDistance();
				return true;
			}
		});
		MenuItemCompat.setShowAsAction(item, MenuItemCompat.SHOW_AS_ACTION_ALWAYS);
	}
}
