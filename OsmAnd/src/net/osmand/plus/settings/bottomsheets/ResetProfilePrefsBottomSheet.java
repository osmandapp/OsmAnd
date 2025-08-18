package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

public class ResetProfilePrefsBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = ResetProfilePrefsBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}

		ApplicationMode mode = getAppMode();
		boolean customProfile = mode.isCustomProfile();

		String title = getString(customProfile ? R.string.restore_all_profile_settings : R.string.reset_all_profile_settings);
		items.add(new TitleItem(title));

		int colorNoAlpha = mode.getProfileColor(nightMode);

		Drawable backgroundIcon = UiUtilities.getColoredSelectableDrawable(ctx, colorNoAlpha, 0.3f);
		Drawable[] layers = {new ColorDrawable(ColorUtilities.getColorWithAlpha(colorNoAlpha, 0.10f)), backgroundIcon};

		BaseBottomSheetItem profileItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(true)
				.setCompoundButtonColor(colorNoAlpha)
				.setButtonTintList(ColorStateList.valueOf(colorNoAlpha))
				.setDescription(ProfileDataUtils.getAppModeDescription(ctx, mode))
				.setIcon(getPaintedIcon(mode.getIconRes(), colorNoAlpha))
				.setTitle(mode.toHumanString())
				.setBackground(new LayerDrawable(layers))
				.setLayoutId(R.layout.preference_profile_item_with_radio_btn)
				.create();
		items.add(profileItem);

		String restoreDescr = getString(customProfile ? R.string.shared_string_restore : R.string.shared_string_reset);
		String description = getString(customProfile ? R.string.restore_all_profile_settings_descr : R.string.reset_all_profile_settings_descr);

		StringBuilder stringBuilder = new StringBuilder(description);
		stringBuilder.append("\n\n");
		stringBuilder.append(getString(R.string.reset_confirmation_descr, restoreDescr));

		BaseBottomSheetItem resetAllSettings = new BottomSheetItemWithDescription.Builder()
				.setDescription(stringBuilder)
				.setLayoutId(R.layout.bottom_sheet_item_pref_info)
				.create();
		items.add(resetAllSettings);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return getAppMode().isCustomProfile() ? R.string.shared_string_restore : R.string.shared_string_reset;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof ResetAppModePrefsListener listener) {
			listener.resetAppModePrefs(getAppMode());
		}
		dismiss();
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	public static boolean showInstance(@NonNull FragmentManager manager,
	                                   @NonNull ApplicationMode appMode,
	                                   @Nullable Fragment target) {
		return showInstance(manager, appMode, target, false);
	}

	public static boolean showInstance(@NonNull FragmentManager manager,
	                                   @NonNull ApplicationMode appMode,
	                                   @Nullable Fragment target,
	                                   boolean usedOnMap) {
		if (AndroidUtils.isFragmentCanBeAdded(manager, TAG)) {
			ResetProfilePrefsBottomSheet fragment = new ResetProfilePrefsBottomSheet();
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(manager, TAG);
			return true;
		}
		return false;
	}

	public interface ResetAppModePrefsListener {
		void resetAppModePrefs(ApplicationMode appMode);
	}
}