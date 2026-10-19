package net.osmand.plus.dialogs;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.activities.RestartActivity;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.OsmandSettings;

public class SpeedCamerasUninstallRestartBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SpeedCamerasUninstallRestartBottomSheet.class.getSimpleName();

	public static void showInstance(@NonNull FragmentManager fm) {
		if (!fm.isStateSaved()) {
			SpeedCamerasUninstallRestartBottomSheet bottomSheet = new SpeedCamerasUninstallRestartBottomSheet();
			bottomSheet.show(fm, TAG);
		}
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		items.add(new TitleItem(getString(R.string.uninstall_speed_cameras)));
		items.add(new LongDescriptionItem(getString(R.string.speed_cameras_restart_descr)));
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_cancel;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_uninstall_and_restart;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY_HARMFUL;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = requiredMyApplication();
		OsmandSettings settings = app.getSettings();
		settings.SPEED_CAMERAS_UNINSTALLED.set(true);
		settings.SPEAK_SPEED_CAMERA.set(false);
		settings.SHOW_CAMERAS.set(false);

		FragmentActivity activity = getActivity();
		if (activity != null) {
			RestartActivity.doRestartSilent(activity);
		}
	}
}
