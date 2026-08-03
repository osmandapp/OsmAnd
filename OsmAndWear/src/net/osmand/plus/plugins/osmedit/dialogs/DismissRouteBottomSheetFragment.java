package net.osmand.plus.plugins.osmedit.dialogs;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;

public class DismissRouteBottomSheetFragment extends MenuBottomSheetDialogFragment {

	public static final String TAG = DismissRouteBottomSheetFragment.class.getSimpleName();

	private Runnable onStopAction;
	private OnDismissListener dismissListener;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		BaseBottomSheetItem descriptionItem = new BottomSheetItemWithDescription.Builder()
				.setDescription(getString(R.string.stop_routing_confirm))
				.setTitle(getString(R.string.cancel_route))
				.setLayoutId(R.layout.bottom_sheet_item_list_title_with_descr)
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
		return R.string.shared_string_no;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_yes;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.PRIMARY;
	}

	@Override
	public int getSecondDividerHeight() {
		return getResources().getDimensionPixelSize(R.dimen.bottom_sheet_icon_margin);
	}

	@Override
	protected void onRightBottomButtonClick() {
		MapActivity mapActivity = getMapActivity();
		if (mapActivity != null) {
			mapActivity.getMapActions().stopNavigationWithoutConfirm();
		}
		if (onStopAction != null) {
			onStopAction.run();
		}
		dismiss();
	}

	@Override
	public void onDismiss(@NonNull DialogInterface dialog) {
		super.onDismiss(dialog);
		FragmentActivity activity = getActivity();
		if (activity != null && !activity.isChangingConfigurations() && dismissListener != null) {
			dismissListener.onDismiss(dialog);
		}
	}

	@Nullable
	public MapActivity getMapActivity() {
		Activity activity = getActivity();
		if (activity instanceof MapActivity) {
			return (MapActivity) activity;
		}
		return null;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager, @Nullable OnDismissListener listener, @Nullable Runnable onStopAction) {
		if (!fragmentManager.isStateSaved()) {
			DismissRouteBottomSheetFragment fragment = new DismissRouteBottomSheetFragment();
			fragment.dismissListener = listener;
			fragment.onStopAction = onStopAction;
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}
}