package net.osmand.plus.mapmarkers;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;

import net.osmand.plus.mapmarkers.MapMarkersHelper;
import net.osmand.plus.mapmarkers.MapMarkersHelper.MapMarkersSortByDef;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class OrderByBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public final static String TAG = "OrderByBottomSheetDialogFragment";

	private OrderByFragmentListener listener;

	public void setListener(OrderByFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Drawable distanceIcon = getContentIcon(R.drawable.ic_action_markers_dark);
		Drawable dateIcon = getContentIcon(R.drawable.ic_action_sort_by_date);

		items.add(new TitleItem(getString(R.string.sort_by)));

		BaseBottomSheetItem byNameItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_sort_by_name))
				.setTitle(getString(R.string.shared_string_name))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.onMapMarkersOrderByModeChanged(MapMarkersHelper.BY_NAME);
						}
						dismiss();
					}
				})
				.create();
		items.add(byNameItem);

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem distNearestItem = new SimpleBottomSheetItem.Builder()
				.setIcon(distanceIcon)
				.setTitle(getString(R.string.distance_nearest))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.onMapMarkersOrderByModeChanged(MapMarkersHelper.BY_DISTANCE_ASC);
						}
						dismiss();
					}
				})
				.create();
		items.add(distNearestItem);

		BaseBottomSheetItem distFarthestItem = new SimpleBottomSheetItem.Builder()
				.setIcon(distanceIcon)
				.setTitle(getString(R.string.distance_farthest))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.onMapMarkersOrderByModeChanged(MapMarkersHelper.BY_DISTANCE_DESC);
						}
						dismiss();
					}
				})
				.create();
		items.add(distFarthestItem);

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem dateAscItem = new SimpleBottomSheetItem.Builder()
				.setIcon(dateIcon)
				.setTitle(getString(R.string.date_added) + " (" + getString(R.string.ascendingly) + ")")
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.onMapMarkersOrderByModeChanged(MapMarkersHelper.BY_DATE_ADDED_ASC);
						}
						dismiss();
					}
				})
				.create();
		items.add(dateAscItem);

		BaseBottomSheetItem dateDescItem = new SimpleBottomSheetItem.Builder()
				.setIcon(dateIcon)
				.setTitle(getString(R.string.date_added) + " (" + getString(R.string.descendingly) + ")")
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.onMapMarkersOrderByModeChanged(MapMarkersHelper.BY_DATE_ADDED_DESC);
						}
						dismiss();
					}
				})
				.create();
		items.add(dateDescItem);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	interface OrderByFragmentListener {
		void onMapMarkersOrderByModeChanged(@MapMarkersSortByDef int sortByMode);
	}
}
