package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.profiles.data.ProfileDataUtils;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.search.SearchablePreferenceDialog;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.widgets.dialogbutton.DialogButtonType;

import java.util.Optional;

public class ResetProfilePrefsBottomSheet extends BasePreferenceBottomSheet implements SearchablePreferenceDialog {

	public static final String TAG = ResetProfilePrefsBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}

		ApplicationMode mode = getAppMode();

		items.add(new TitleItem(getTitle()));

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

		BaseBottomSheetItem resetAllSettings = new BottomSheetItemWithDescription.Builder()
				.setDescription(getDescription())
				.setLayoutId(R.layout.bottom_sheet_item_pref_info)
				.create();
		items.add(resetAllSettings);
	}

	@NonNull
	private String getTitle() {
		return getString(getAppMode().isCustomProfile() ? R.string.restore_all_profile_settings : R.string.reset_all_profile_settings);
	}

	@NonNull
	private String getDescription() {
		final boolean customProfile = getAppMode().isCustomProfile();
		final String restoreDescr = getString(customProfile ? R.string.shared_string_restore : R.string.shared_string_reset);
		final String description = getString(customProfile ? R.string.restore_all_profile_settings_descr : R.string.reset_all_profile_settings_descr);
		return description + "\n\n" + getString(R.string.reset_confirmation_descr, restoreDescr);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return getAppMode().isCustomProfile() ? R.string.shared_string_restore : R.string.shared_string_reset;
	}

	@Override
	protected void onRightBottomButtonClick() {
		Fragment targetFragment = getTargetFragment();
		if (targetFragment instanceof ResetAppModePrefsListener) {
			ResetAppModePrefsListener listener = (ResetAppModePrefsListener) targetFragment;
			listener.resetAppModePrefs(getAppMode());
		}
		dismiss();
	}

	@Override
	protected DialogButtonType getRightBottomButtonType() {
		return DialogButtonType.SECONDARY;
	}

	public static ResetProfilePrefsBottomSheet createInstance(final @NonNull ApplicationMode appMode,
															  final Optional<Fragment> target) {
		return BasePreferenceBottomSheetInitializer
				.initialize(new ResetProfilePrefsBottomSheet())
				.with(Optional.empty(), appMode, false, target);
	}

	@Override
	public void show(final FragmentManager fragmentManager) {
		show(fragmentManager, TAG);
	}

	@Override
	public String getSearchableInfo() {
		return String.join(", ", getTitle(), getDescription());
	}

	public interface ResetAppModePrefsListener {
		void resetAppModePrefs(ApplicationMode appMode);
	}
}