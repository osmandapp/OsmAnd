package net.osmand.plus.mapmarkers;

import android.os.Bundle;
import android.view.View;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;

public class PlanRouteOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "PlanRouteOptionsBottomSheetDialogFragment";

	public static final String SELECT_ALL_KEY = "select_all";

	private PlanRouteOptionsFragmentListener listener;

	public void setListener(PlanRouteOptionsFragmentListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.shared_string_options)));

		if (!AndroidUiHelper.isOrientationPortrait(getActivity())) {
			boolean selectAll = getArguments().getBoolean(SELECT_ALL_KEY);

			BaseBottomSheetItem selectItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(selectAll ? R.drawable.ic_action_select_all : R.drawable.ic_action_deselect_all))
					.setTitle(getString(selectAll ? R.string.shared_string_select_all : R.string.shared_string_deselect_all))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if (listener != null) {
								listener.selectOnClick();
							}
							dismiss();
						}
					})
					.create();
			items.add(selectItem);
		}

		BaseBottomSheetItem navigateItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark))
				.setTitle(getString(R.string.shared_string_navigation))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.navigateOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(navigateItem);

		BaseBottomSheetItem roundTripItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(getMyApplication().getSettings().ROUTE_MAP_MARKERS_ROUND_TRIP.get())
				.setDescription(getString(R.string.make_round_trip_descr))
				.setIcon(getContentIcon(R.drawable.ic_action_trip_round))
				.setTitle(getString(R.string.make_round_trip))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_switch_56dp)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.makeRoundTripOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(roundTripItem);

		items.add(new SubtitleDividerItem(getContext()));

		items.add(new SubtitleItem(getString(R.string.sort_by)));

		BaseBottomSheetItem doorToDoorItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_sort_door_to_door))
				.setTitle(getString(R.string.intermediate_items_sort_by_distance))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.doorToDoorOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(doorToDoorItem);

		BaseBottomSheetItem reversItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_sort_reverse_order))
				.setTitle(getString(R.string.shared_string_reverse_order))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						if (listener != null) {
							listener.reverseOrderOnClick();
						}
						dismiss();
					}
				})
				.create();
		items.add(reversItem);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	interface PlanRouteOptionsFragmentListener {

		void selectOnClick();

		void navigateOnClick();

		void makeRoundTripOnClick();

		void doorToDoorOnClick();

		void reverseOrderOnClick();
	}
}
