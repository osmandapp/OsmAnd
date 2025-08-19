package net.osmand.plus.routepreparationmenu;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerHalfItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.TargetPointsHelper;
import net.osmand.plus.helpers.WaypointDialogHelper;
import net.osmand.plus.routepreparationmenu.data.PointType;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.util.Algorithms;

public class TargetOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = TargetOptionsBottomSheetDialogFragment.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.shared_string_options)));
		TargetPointsHelper targetsHelper = app.getTargetPointsHelper();
		BaseBottomSheetItem sortDoorToDoorItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_sort_door_to_door))
				.setTitle(getString(R.string.intermediate_items_sort_by_distance))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					callMapActivity(WaypointDialogHelper::sortAllTargets);
					dismiss();
				})
				.create();
		items.add(sortDoorToDoorItem);

		BaseBottomSheetItem reorderStartAndFinishItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_sort_reverse_order))
				.setTitle(getString(R.string.switch_start_finish))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					callMapActivity(mapActivity -> WaypointDialogHelper.switchStartAndFinish(mapActivity, true));
					dismiss();
				})
				.create();
		items.add(reorderStartAndFinishItem);

		if (!Algorithms.isEmpty(targetsHelper.getIntermediatePoints())) {
			BaseBottomSheetItem reorderAllItems = new SimpleBottomSheetItem.Builder()
					.setIcon(getContentIcon(R.drawable.ic_action_sort_reverse_order))
					.setTitle(getString(R.string.reverse_all_points))
					.setLayoutId(R.layout.bottom_sheet_item_simple)
					.setOnClickListener(v -> {
						callMapActivity(WaypointDialogHelper::reverseAllPoints);
						dismiss();
					})
					.create();
			items.add(reorderAllItems);
		}

		items.add(new DividerHalfItem(getContext()));

		BaseBottomSheetItem[] addWaypointItem = new BaseBottomSheetItem[1];
		addWaypointItem[0] = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_plus))
				.setTitle(getString(R.string.add_intermediate_point))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> onWaypointItemClick())
				.create();
		items.add(addWaypointItem[0]);

		BaseBottomSheetItem clearIntermediatesItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_clear_all))
				.setTitle(getString(R.string.clear_all_intermediates))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setDisabled(app.getTargetPointsHelper().getIntermediatePoints().isEmpty())
				.setOnClickListener(v -> {
					callMapActivity(WaypointDialogHelper::clearAllIntermediatePoints);
					dismiss();
				})
				.create();
		items.add(clearIntermediatesItem);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	private void onWaypointItemClick() {
		callMapActivity(this::openAddPointDialog);
	}

	private void openAddPointDialog(@NonNull MapActivity mapActivity) {
		AddPointBottomSheetDialog.showInstance(mapActivity, PointType.INTERMEDIATE, false);
	}

	public static void showInstance(@NonNull FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			TargetOptionsBottomSheetDialogFragment fragment = new TargetOptionsBottomSheetDialogFragment();
			fragment.setUsedOnMap(true);
			fragment.show(manager, TAG);
		}
	}
}
