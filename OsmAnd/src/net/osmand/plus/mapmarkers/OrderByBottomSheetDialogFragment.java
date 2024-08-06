package net.osmand.plus.mapmarkers;

import static net.osmand.plus.mapmarkers.MapMarkersComparator.BY_DATE_ADDED_ASC;
import static net.osmand.plus.mapmarkers.MapMarkersComparator.BY_DATE_ADDED_DESC;
import static net.osmand.plus.mapmarkers.MapMarkersComparator.BY_DISTANCE_ASC;
import static net.osmand.plus.mapmarkers.MapMarkersComparator.BY_DISTANCE_DESC;
import static net.osmand.plus.mapmarkers.MapMarkersComparator.BY_NAME;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.mapmarkers.MapMarkersComparator.MapMarkersSortByDef;

public class OrderByBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "OrderByBottomSheetDialogFragment";

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
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.onMapMarkersOrderByModeChanged(BY_NAME);
					}
					dismiss();
				})
				.create();
		items.add(byNameItem);

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem distNearestItem = new SimpleBottomSheetItem.Builder()
				.setIcon(distanceIcon)
				.setTitle(getString(R.string.distance_nearest))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.onMapMarkersOrderByModeChanged(BY_DISTANCE_ASC);
					}
					dismiss();
				})
				.create();
		items.add(distNearestItem);

		BaseBottomSheetItem distFarthestItem = new SimpleBottomSheetItem.Builder()
				.setIcon(distanceIcon)
				.setTitle(getString(R.string.distance_farthest))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.onMapMarkersOrderByModeChanged(BY_DISTANCE_DESC);
					}
					dismiss();
				})
				.create();
		items.add(distFarthestItem);

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem dateAscItem = new SimpleBottomSheetItem.Builder()
				.setIcon(dateIcon)
				.setTitle(getString(R.string.date_added) + " (" + getString(R.string.ascendingly) + ")")
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.onMapMarkersOrderByModeChanged(BY_DATE_ADDED_ASC);
					}
					dismiss();
				})
				.create();
		items.add(dateAscItem);

		BaseBottomSheetItem dateDescItem = new SimpleBottomSheetItem.Builder()
				.setIcon(dateIcon)
				.setTitle(getString(R.string.date_added) + " (" + getString(R.string.descendingly) + ")")
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.onMapMarkersOrderByModeChanged(BY_DATE_ADDED_DESC);
					}
					dismiss();
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
