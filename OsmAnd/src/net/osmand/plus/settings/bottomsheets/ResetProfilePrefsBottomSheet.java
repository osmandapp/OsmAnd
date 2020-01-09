package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithCompoundButton;
import net.osmand.plus.base.bottomsheetmenu.BottomSheetItemWithDescription;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.BaseSettingsFragment;

public class ResetProfilePrefsBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = ResetProfilePrefsBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}

		items.add(new TitleItem(getString(R.string.reset_all_profile_settings)));

		ApplicationMode mode = getAppMode();
		int profileColor = mode.getIconColorInfo().getColor(nightMode);
		int colorNoAlpha = ContextCompat.getColor(ctx, profileColor);

		Drawable backgroundIcon = UiUtilities.getColoredSelectableDrawable(ctx, colorNoAlpha, 0.3f);
		Drawable[] layers = {new ColorDrawable(UiUtilities.getColorWithAlpha(colorNoAlpha, 0.10f)), backgroundIcon};

		BaseBottomSheetItem profileItem = new BottomSheetItemWithCompoundButton.Builder()
				.setChecked(true)
				.setCompoundButtonColorId(profileColor)
				.setButtonTintList(ColorStateList.valueOf(getResolvedColor(profileColor)))
				.setDescription(BaseSettingsFragment.getAppModeDescription(ctx, mode))
				.setIcon(getIcon(mode.getIconRes(), profileColor))
				.setTitle(mode.toHumanString(ctx))
				.setBackground(new LayerDrawable(layers))
				.setLayoutId(R.layout.preference_profile_item_with_radio_btn)
				.create();
		items.add(profileItem);

		StringBuilder description = new StringBuilder(getString(R.string.reset_confirmation_descr, getString(R.string.shared_string_reset)));
		description.append("\n\n");
		description.append(getString(R.string.reset_all_profile_settings_descr));

		BaseBottomSheetItem resetAllSettings = new BottomSheetItemWithDescription.Builder()
				.setDescription(description)
				.setLayoutId(R.layout.bottom_sheet_item_pref_info)
				.create();
		items.add(resetAllSettings);
	}

	@Override
	protected int getRightBottomButtonTextId() {
		return R.string.shared_string_reset;
	}

	@Override
	protected void onRightBottomButtonClick() {
		OsmandApplication app = getMyApplication();
		if (app != null) {
			app.getSettings().resetPreferencesForProfile(getAppMode());
		}
		dismiss();
	}

	@Override
	protected UiUtilities.DialogButtonType getRightBottomButtonType() {
		return UiUtilities.DialogButtonType.SECONDARY;
	}

	public static boolean showInstance(@NonNull FragmentManager fragmentManager, String key, Fragment target,
	                                   boolean usedOnMap, @NonNull ApplicationMode appMode) {
		try {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, key);

			ResetProfilePrefsBottomSheet fragment = new ResetProfilePrefsBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.show(fragmentManager, TAG);
			return true;
		} catch (RuntimeException e) {
			return false;
		}
	}
}