package net.osmand.plus.plugins.externalsensors.dialogs;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.base.BottomSheetBehaviourDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.DividerSpaceItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class PairNewDeviceBottomSheet extends BottomSheetBehaviourDialogFragment {

	public static final String TAG = PairNewDeviceBottomSheet.class.getSimpleName();
	public static final int BOTTOM_SHEET_HEIGHT_DP = 427;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.ant_plus_pair_new_sensor)));

		BaseBottomSheetItem pairBluetooth = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_bluetooth))
				.setTitle(getString(R.string.ant_plus_pair_new_sensor_ble))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					dismiss();
					callMapActivity(mapActivity ->
							ExternalDevicesSearchFragment.Companion.showInstance(
							mapActivity.getSupportFragmentManager(), true, false));
				})
				.create();
		items.add(pairBluetooth);

		BaseBottomSheetItem pairAntItem = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_ant_plus))
				.setTitle(getString(R.string.ant_plus_pair_new_sensor_ant_plus))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					dismiss();
					callMapActivity(mapActivity ->
							ExternalDevicesSearchFragment.Companion.showInstance(
							mapActivity.getSupportFragmentManager(), false, true)
					);
				})
				.create();
		items.add(pairAntItem);

		items.add(new DividerItem(getContext()));

		BaseBottomSheetItem helpItem = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_help))
				.setTitle(getString(R.string.ant_plus_help_title))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					dismiss();
					callActivity(activity -> AndroidUtils.openUrl(activity, R.string.docs_external_sensors, nightMode));
				})
				.create();
		items.add(helpItem);
		BaseBottomSheetItem cancelItem = new BottomSheetItemButton.Builder()
				.setButtonType(DialogButtonType.SECONDARY)
				.setTitle(getString(R.string.shared_string_cancel))
				.setLayoutId(R.layout.bottom_sheet_button)
				.setOnClickListener(v -> dismiss())
				.create();
		items.add(cancelItem);
		int padding = getDimensionPixelSize(R.dimen.content_padding_small);
		items.add(new DividerSpaceItem(getContext(), padding));
	}

	@Override
	protected int getPeekHeight() {
		return dpToPx(BOTTOM_SHEET_HEIGHT_DP);
	}

	protected void hideBottomSheet() {
		MapActivity mapActivity = (MapActivity) getActivity();
		if (mapActivity != null) {
			FragmentManager manager = mapActivity.getSupportFragmentManager();
			manager.beginTransaction()
					.hide(this)
					.commitAllowingStateLoss();
		}
	}

	protected boolean hideButtonsContainer() {
		return true;
	}

	public static void showInstance(@NonNull FragmentManager fragmentManager) {
		if (AndroidUtils.isFragmentCanBeAdded(fragmentManager, TAG)) {
			PairNewDeviceBottomSheet fragment = new PairNewDeviceBottomSheet();
			fragment.setUsedOnMap(false);
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
	}
}