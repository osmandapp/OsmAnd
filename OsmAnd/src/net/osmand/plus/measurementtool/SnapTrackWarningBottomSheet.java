package net.osmand.plus.measurementtool;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;

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

	public static final String TAG = SnapTrackWarningBottomSheet.class.getSimpleName();
	private static final Log LOG = PlatformUtil.getLog(SnapTrackWarningBottomSheet.class);

	protected View mainView;
	protected GpxTrackAdapter adapter;
	private SnapTrackWarningListener listener;

	public void setListener(SnapTrackWarningListener listener) {
		this.listener = listener;
	}

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

	public static void showInstance(FragmentManager fm, SnapTrackWarningListener listener) {
		try {
			if (!fm.isStateSaved()) {
				SnapTrackWarningBottomSheet fragment = new SnapTrackWarningBottomSheet();
				fragment.setUsedOnMap(true);
				fragment.setRetainInstance(true);
				fragment.setListener(listener);
				fm.beginTransaction()
						.add(R.id.bottomFragmentContainer, fragment, TAG)
						.commitAllowingStateLoss();
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_continue;
	}

	@Override
	protected void onRightBottomButtonClick() {
		if (listener != null) {
			listener.continueButtonOnClick();
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
		if (listener != null) {
			listener.dismissButtonOnClick();
		}
	}

	interface SnapTrackWarningListener {

		void continueButtonOnClick();

		void dismissButtonOnClick();

	}
}
