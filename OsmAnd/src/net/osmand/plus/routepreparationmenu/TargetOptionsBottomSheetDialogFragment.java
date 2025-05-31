package net.osmand.plus.routepreparationmenu;

import android.app.Activity;
import android.os.Bundle;

import androidx.annotation.Nullable;

import net.osmand.plus.OsmandApplication;
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
import net.osmand.util.Algorithms;

public class TargetOptionsBottomSheetDialogFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = "TargetOptionsBottomSheetDialogFragment";

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.shared_string_options)));
		OsmandApplication app = requiredMyApplication();
		TargetPointsHelper targetsHelper = app.getTargetPointsHelper();
		BaseBottomSheetItem sortDoorToDoorItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_sort_door_to_door))
				.setTitle(getString(R.string.intermediate_items_sort_by_distance))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					MapActivity mapActivity = getMapActivity();
					if (mapActivity != null) {
						WaypointDialogHelper.sortAllTargets(mapActivity);
					}
					dismiss();
				})
				.create();
		items.add(sortDoorToDoorItem);

		BaseBottomSheetItem reorderStartAndFinishItem = new SimpleBottomSheetItem.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_sort_reverse_order))
				.setTitle(getString(R.string.switch_start_finish))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					MapActivity activity = getMapActivity();
					if (activity != null) {
						WaypointDialogHelper.switchStartAndFinish(activity, true);
					}
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
						MapActivity mapActivity = getMapActivity();
						if (mapActivity != null) {
							WaypointDialogHelper.reverseAllPoints(mapActivity);
						}
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
					MapActivity activity = getMapActivity();
					if (activity != null) {
						WaypointDialogHelper.clearAllIntermediatePoints(activity);
					}
					dismiss();
				})
				.create();
		items.add(clearIntermediatesItem);
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_close;
	}

	private void openAddPointDialog(MapActivity mapActivity) {
		Bundle args = new Bundle();
		args.putString(AddPointBottomSheetDialog.POINT_TYPE_KEY, PointType.INTERMEDIATE.name());
		AddPointBottomSheetDialog fragment = new AddPointBottomSheetDialog();
		fragment.setArguments(args);
		fragment.setUsedOnMap(false);
		fragment.show(mapActivity.getSupportFragmentManager(), AddPointBottomSheetDialog.TAG);
	}

	private void onWaypointItemClick() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			openAddPointDialog(mapActivity);
		}
	}

	@Nullable
	private MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}
}
