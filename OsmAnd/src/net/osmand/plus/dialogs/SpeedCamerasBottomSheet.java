package net.osmand.plus.dialogs;

import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;

public class SpeedCamerasBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SpeedCamerasBottomSheet.class.getName();
	private OsmandApplication app;

	public static void showInstance(@NonNull FragmentManager fm) {
		if (!fm.isStateSaved()) {
			SpeedCamerasBottomSheet bottomSheet = new SpeedCamerasBottomSheet();
			bottomSheet.show(fm, TAG);
		}
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		app = requiredMyApplication();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View root = UiUtilities.getInflater(app, nightMode).inflate(R.layout.bottom_sheet_speed_cameras, null);
		((ImageView) root.findViewById(R.id.icon)).setImageResource(R.drawable.img_speed_camera_warning);
		((TextView) root.findViewById(R.id.description)).setText(getDescriptionText());
		items.add(new BaseBottomSheetItem.Builder().setCustomView(root).create());
	}

	@Override
	protected void onRightBottomButtonClick() {
		super.onRightBottomButtonClick();
	}

	@Override
	protected void onDismissButtonClickAction() {
		super.onDismissButtonClickAction();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_uninstall;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.keep_active;
	}

	private SpannableString getDescriptionText() {
		String keepActive = getString(R.string.keep_active);
		String uninstall = getString(R.string.shared_string_uninstall);
		String text = getString(R.string.speed_cameras_legal_descr, keepActive, uninstall);
		return UiUtilities.setWordsMediumFont(app, text, keepActive, uninstall);
	}
}
