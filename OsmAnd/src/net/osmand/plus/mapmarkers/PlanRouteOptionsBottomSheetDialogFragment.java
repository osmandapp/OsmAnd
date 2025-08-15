package net.osmand.plus.mapmarkers;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleDividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.SubtitleItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.utils.AndroidUtils;

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

		if (!AndroidUiHelper.isOrientationPortrait(requireActivity())) {
			boolean selectAll = requireArguments().getBoolean(SELECT_ALL_KEY);

			BaseBottomSheetItem selectItem = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(selectAll ? R.drawable.ic_action_select_all : R.drawable.ic_action_deselect_all))
					.setTitle(getString(selectAll ? R.string.shared_string_select_all : R.string.shared_string_deselect_all))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(v -> {
						if (listener != null) {
							listener.selectOnClick();
						}
						dismiss();
					})
					.create();
			items.add(selectItem);
		}

		BaseBottomSheetItem navigateItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_gdirections_dark))
				.setTitle(getString(R.string.shared_string_navigation))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.navigateOnClick();
					}
					dismiss();
				})
				.create();
		items.add(navigateItem);

		BaseBottomSheetItem roundTripItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(settings.ROUTE_MAP_MARKERS_ROUND_TRIP.get())
				.setDescription(getString(R.string.make_round_trip_descr))
				.setIcon(getContentIcon(R.drawable.ic_action_trip_round))
				.setTitle(getString(R.string.make_round_trip))
				.setLayoutId(R.layout.bottom_sheet_item_with_descr_and_switch_56dp)
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.makeRoundTripOnClick();
					}
					dismiss();
				})
				.create();
		items.add(roundTripItem);

		items.add(new SubtitleDividerItem(getContext()));

		items.add(new SubtitleItem(getString(R.string.sort_by)));

		BaseBottomSheetItem doorToDoorItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_sort_door_to_door))
				.setTitle(getString(R.string.intermediate_items_sort_by_distance))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.doorToDoorOnClick();
					}
					dismiss();
				})
				.create();
		items.add(doorToDoorItem);

		BaseBottomSheetItem reversItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_sort_reverse_order))
				.setTitle(getString(R.string.shared_string_reverse_order))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					if (listener != null) {
						listener.reverseOrderOnClick();
					}
					dismiss();
				})
				.create();
		items.add(reversItem);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	public static void showInstance(@NonNull FragmentActivity activity, boolean selectAll,
	                                @NonNull PlanRouteOptionsFragmentListener listener) {
		FragmentManager fragmentManager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			Bundle args = new Bundle();
			args.putBoolean(SELECT_ALL_KEY, selectAll);

			PlanRouteOptionsBottomSheetDialogFragment fragment = new PlanRouteOptionsBottomSheetDialogFragment();
			fragment.setArguments(args);
			fragment.setUsedOnMap(true);
			fragment.setListener(listener);
			fragment.show(fragmentManager, TAG);
		}
	}

	public interface PlanRouteOptionsFragmentListener {

		void selectOnClick();

		void navigateOnClick();

		void makeRoundTripOnClick();

		void doorToDoorOnClick();

		void reverseOrderOnClick();
	}
}
