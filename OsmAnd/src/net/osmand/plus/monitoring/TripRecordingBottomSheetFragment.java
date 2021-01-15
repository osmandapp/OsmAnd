package net.osmand.plus.monitoring;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;

public class TripRecordingBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = TripRecordingBottomSheetFragment.class.getSimpleName();


	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved()) {
			TripRecordingBottomSheetFragment fragment = new TripRecordingBottomSheetFragment();
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final View itemView = UiUtilities.getInflater(getMapActivity(), nightMode).inflate(
				R.layout.trip_recording_fragment, null, false);
		BaseBottomSheetItem descriptionItem = new BottomSheetItemWithDescription.Builder()
				.setTitle(getString(R.string.map_widget_monitoring))
				.setCustomView(itemView)
				.create();

		items.add(descriptionItem);


		int padding = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		items.add(new DividerSpaceItem(requireContext(), padding));

	}


	@Override
	protected boolean useVerticalButtons() {
		return false;
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
		return UiUtilities.DialogButtonType.PRIMARY;
	}

	@Override
	public int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin);
	}

	@Override
	protected void onRightBottomButtonClick() {
		dismiss();
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);

	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}
}

