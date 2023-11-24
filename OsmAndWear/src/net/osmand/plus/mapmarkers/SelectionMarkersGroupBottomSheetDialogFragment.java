package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.ShortDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class SelectionMarkersGroupBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "SelectionMarkersGroupBottomSheetDialogFragment";

	private AddMarkersGroupFragmentListener listener;

	public void setListener(AddMarkersGroupFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.add_group)));

		items.add(new ShortDescriptionItem(getString(R.string.add_group_descr)));

		BaseBottomSheetItem favoritesItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_favorite))
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
				.setTitle(getString(R.string.shared_string_gpx_waypoints))
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
	}

	@Override
	protected int getDismissButtonTextId() {
		return 	R.string.shared_string_close;
	}

	interface AddMarkersGroupFragmentListener {

		void favouritesOnClick();

		void waypointsOnClick();
	}
}
