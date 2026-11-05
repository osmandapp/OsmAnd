package net.osmand.plus.plugins.externalsensors.dialogs;

import android.net.Uri;
import android.os.Bundle;

import androidx.fragment.app.FragmentActivity;
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
					MapActivity mapActivity = (MapActivity) getActivity();
					if (mapActivity != null) {
						ExternalDevicesSearchFragment.Companion.showInstance(
								mapActivity.getSupportFragmentManager(), true, false);
					}
				})
				.create();
		items.add(pairBluetooth);

		BaseBottomSheetItem pairAntItem = new BottomSheetItemWithDescription.Builder()
				.setIcon(getContentIcon(R.drawable.ic_action_ant_plus))
				.setTitle(getString(R.string.ant_plus_pair_new_sensor_ant_plus))
				.setLayoutId(R.layout.bottom_sheet_item_simple_pad_32dp)
				.setOnClickListener(v -> {
					dismiss();
					MapActivity mapActivity = (MapActivity) getActivity();
					if (mapActivity != null) {
						ExternalDevicesSearchFragment.Companion.showInstance(
								mapActivity.getSupportFragmentManager(), false, true);
					}
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
					FragmentActivity activity = getActivity();
					boolean nightMode = getMyApplication().getDaynightHelper().isNightMode(false);
					AndroidUtils.openUrl(activity, Uri.parse(getString(R.string.docs_external_sensors)), nightMode);
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
		int padding = getResources().getDimensionPixelSize(R.dimen.content_padding_small);
		items.add(new DividerSpaceItem(getContext(), padding));
	}

	@Override
	protected int getPeekHeight() {
		return AndroidUtils.dpToPx(requiredMyApplication(), BOTTOM_SHEET_HEIGHT_DP);
	}


	public static void showInstance(FragmentManager fragmentManager) {
		if (!fragmentManager.isStateSaved()) {
			PairNewDeviceBottomSheet fragment = new PairNewDeviceBottomSheet();
			fragment.setRetainInstance(true);
			fragment.show(fragmentManager, TAG);
		}
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

}