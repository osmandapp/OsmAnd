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
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.helpers.FontCache;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.settings.backend.OsmandSettings.OsmandPreference;
import net.osmand.plus.settings.fragments.OnPreferenceChanged;

public class SpeedCamerasBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = SpeedCamerasBottomSheet.class.getName();
	public static final String SPEED_CAMERA_KEY_NAME = "speed_camera";
	private OsmandApplication app;
	private OsmandSettings settings;

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
		settings = app.getSettings();
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		View root = UiUtilities.getInflater(app, nightMode).inflate(R.layout.bottom_sheet_speed_cameras, null);
		((ImageView) root.findViewById(R.id.icon)).setImageDrawable(app.getUIUtilities().getIcon(R.drawable.img_speed_camera_warning));
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
		OsmandPreference<Boolean> speedCamUninstalled = settings.SPEED_CAMERAS_UNINSTALLED;
		speedCamUninstalled.set(true);
		settings.SPEAK_SPEED_CAMERA.set(false);
		settings.SHOW_CAMERAS.set(false);
		app.getPoiTypes().forbidPoiType(SPEED_CAMERA_KEY_NAME);
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof OnPreferenceChanged) {
			((OnPreferenceChanged) targetFragment).onPreferenceChanged(speedCamUninstalled.getId());
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
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
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
