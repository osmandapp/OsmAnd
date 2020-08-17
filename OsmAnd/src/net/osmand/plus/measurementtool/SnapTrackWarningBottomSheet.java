package net.osmand.plus.measurementtool;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.helpers.GpxTrackAdapter;

import org.apache.commons.logging.Log;

public class SnapTrackWarningBottomSheet extends MenuBottomSheetDialogFragment {

	public static final int REQUEST_CODE = 1000;
	public static final int CANCEL_RESULT_CODE = 2;
	public static final int CONTINUE_RESULT_CODE = 3;

	public static final String TAG = SnapTrackWarningBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SnapTrackWarningBottomSheet.class);

	protected View mainView;
	protected GpxTrackAdapter adapter;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			activity.findViewById(R.id.snap_to_road_image_button).setVisibility(View.GONE);
		}
		OsmandApplication app = requiredMyApplication();
		items.add(new TitleItem(getString(R.string.route_between_points)));
		BaseBottomSheetItem description = new BottomSheetItemWithDescription.Builder()
				.setDescription(app.getString(R.string.rourte_between_points_warning_desc))
				.setDescriptionColorId(R.color.text_color_primary_light)
				.setLayoutId(R.layout.bottom_sheet_item_description_long)
				.create();
		items.add(description);
		items.add(new DividerSpaceItem(app, app.getResources().getDimensionPixelSize(R.dimen.content_padding_half)));
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_continue;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment fragment = getTargetFragment();
		if (fragment != null) {
			fragment.onActivityResult(REQUEST_CODE, CONTINUE_RESULT_CODE, null);
		}
		dismiss();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			activity.findViewById(R.id.snap_to_road_image_button).setVisibility(View.VISIBLE);
		}
		Fragment fragment = getTargetFragment();
		if (fragment != null) {
			fragment.onActivityResult(REQUEST_CODE, CANCEL_RESULT_CODE, null);
		}
	}

	public static void showInstance(FragmentManager fm, Fragment targetFragment) {
		try {
			if (!fm.isStateSaved()) {
				SnapTrackWarningBottomSheet fragment = new SnapTrackWarningBottomSheet();
				fragment.setTargetFragment(targetFragment, REQUEST_CODE);
				fm.beginTransaction()
						.add(R.id.bottomFragmentContainer, fragment, TAG)
						.commitAllowingStateLoss();
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}
