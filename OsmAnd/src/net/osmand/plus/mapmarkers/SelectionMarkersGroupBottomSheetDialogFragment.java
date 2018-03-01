package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class SelectionMarkersGroupBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "SelectionMarkersGroupBottomSheetDialogFragment";

	private AddMarkersGroupFragmentListener listener;

	public void setListener(AddMarkersGroupFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public View createMenuItems(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.add_group)));

		items.add(new DescriptionItem(getString(R.string.add_group_descr)));

		BaseBottomSheetItem favoritesItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_fav_dark))
				.setTitle(getString(R.string.favourites_group))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.favouritesOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(favoritesItem);

		BaseBottomSheetItem waypointsItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_polygom_dark))
				.setTitle(getString(R.string.track_waypoints))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.waypointsOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(waypointsItem);

		return null;
	}

	@Override
	protected int getCloseRowTextId() {
		return 	R.string.shared_string_close;
	}

	interface AddMarkersGroupFragmentListener {

		void favouritesOnClick();

		void waypointsOnClick();
	}
}
