package net.osmand.plus.dialogs;

import android.os.Bundle;
import android.text.SpannableString;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class SpeedCamerasBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SpeedCamerasBottomSheet.class.getName();
	private OsmandApplication app;

	public static void showInstance(@NonNull FragmentManager fm, @Nullable Fragment targetFragment) {
		if (!fm.isStateSaved()) {
			SpeedCamerasBottomSheet bottomSheet = new SpeedCamerasBottomSheet();
			bottomSheet.setTargetFragment(targetFragment, 0);
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
		View root = UiUtilities.getInflater(app, nightMode).inflate(R.layout.bottom_sheet_icon_title_description, null);
		((ImageView) root.findViewById(R.id.icon)).setImageDrawable(app.getUIUtilities().getIcon(R.drawable.img_speed_camera_warning));
		((TextView) root.findViewById(R.id.title)).setText(R.string.speed_camera_pois);
		((TextView) root.findViewById(R.id.description)).setText(getDescriptionText());
		items.add(new BaseBottomSheetItem.Builder().setCustomView(root).create());
	}

	@Override
	protected void onRightBottomButtonClick() {
		setDialogShowed();
		dismiss();
	}

	@Override
	protected void onDismissButtonClickAction() {
		FragmentManager fm = getFragmentManager();
		if (fm != null) {
			SpeedCamerasUninstallRestartBottomSheet.showInstance(fm);
		}
		setDialogShowed();
	}

	@Override
	protected int getDismissButtonTextId() {
		return R.string.shared_string_uninstall;
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.keep_active;
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return getDismissButtonType();
	}

	private SpannableString getDescriptionText() {
		String keepActive = getString(R.string.keep_active);
		String uninstall = getString(R.string.shared_string_uninstall);
		String text = getString(R.string.speed_cameras_legal_descr, keepActive, uninstall);
		return UiUtilities.createCustomFontSpannable(FontCache.getRobotoMedium(app), text, keepActive, uninstall);
	}

	private void setDialogShowed() {
		app.getSettings().SPEED_CAMERAS_ALERT_SHOWED.set(true);
	}
}
