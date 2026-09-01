package net.osmand.plus.routepreparationmenu;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.routepreparationmenu.cards.RouteDirectionsCard;
import net.osmand.plus.utils.AndroidUtils;

public class RouteDetailsFilterBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = RouteDetailsFilterBottomSheet.class.getSimpleName();
	public static final String REQUEST_KEY = "route_details_filter_request";
	public static final String RESULT_FILTER_MASK_KEY = "route_details_filter_mask";

	private static final String SELECTED_MASK_KEY = "selected_mask";
	private static final String WARNING_COUNT_KEY = "warning_count";
	private static final String POI_COUNT_KEY = "poi_count";
	private static final String FAVORITE_COUNT_KEY = "favorite_count";

	private int selectedMask;
	private int warningCount;
	private int poiCount;
	private int favoriteCount;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Bundle args = requireArguments();
		selectedMask = savedInstanceState != null
				? savedInstanceState.getInt(SELECTED_MASK_KEY, RouteDirectionsCard.FILTER_ALL)
				: args.getInt(SELECTED_MASK_KEY, RouteDirectionsCard.FILTER_ALL);
		warningCount = args.getInt(WARNING_COUNT_KEY);
		poiCount = args.getInt(POI_COUNT_KEY);
		favoriteCount = args.getInt(FAVORITE_COUNT_KEY);
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_MASK_KEY, selectedMask);
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.filter_screen_title)));
		addFilterItem(RouteDirectionsCard.FILTER_TRAFFIC_WARNINGS, R.string.way_alarms,
				warningCount);
		addFilterItem(RouteDirectionsCard.FILTER_POI, R.string.points_of_interests, poiCount);
		addFilterItem(RouteDirectionsCard.FILTER_FAVORITES,
				R.string.shared_string_my_favorites, favoriteCount);
	}

	private void addFilterItem(int filter, int titleId, int count) {
		if (count == 0) {
			return;
		}
		BottomSheetItemWithCompoundButton[] item = new BottomSheetItemWithCompoundButton[1];
		String title = getString(R.string.ltr_or_rtl_combine_via_space,
				getString(titleId), "(" + count + ")");
		item[0] = (BottomSheetItemWithCompoundButton) new BottomSheetItemWithCompoundButton.Builder()
				.setChecked((selectedMask & filter) != 0)
				.setOnCheckedChangeListener((buttonView, checked) -> setFilterEnabled(filter, checked))
				.setTitle(title)
				.setLayoutId(R.layout.bottom_sheet_item_with_switch_no_icon)
				.setOnClickListener(v -> item[0].setChecked(!item[0].isChecked()))
				.create();
		items.add(item[0]);
	}

	private void setFilterEnabled(int filter, boolean enabled) {
		if (enabled) {
			selectedMask |= filter;
		} else {
			selectedMask &= ~filter;
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.apply_filters;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Bundle result = new Bundle();
		result.putInt(RESULT_FILTER_MASK_KEY, selectedMask);
		getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
		dismiss();
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, int selectedMask,
	                                int warningCount, int poiCount, int favoriteCount) {
		if (!AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			return;
		}
		Bundle args = new Bundle();
		args.putInt(SELECTED_MASK_KEY, selectedMask);
		args.putInt(WARNING_COUNT_KEY, warningCount);
		args.putInt(POI_COUNT_KEY, poiCount);
		args.putInt(FAVORITE_COUNT_KEY, favoriteCount);
		RouteDetailsFilterBottomSheet fragment = new RouteDetailsFilterBottomSheet();
		fragment.setArguments(args);
		fragment.setUsedOnMap(true);
		fragment.show(fragmentManager, TAG);
	}
}
