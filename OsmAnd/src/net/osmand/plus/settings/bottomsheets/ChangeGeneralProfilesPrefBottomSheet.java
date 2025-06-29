package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.osmand.PlatformUtil;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.fragments.BaseSettingsFragment;
import net.osmand.plus.utils.AndroidUtils;

import org.apache.commons.logging.Log;

import java.io.Serializable;

public class ChangeGeneralProfilesPrefBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = ChangeGeneralProfilesPrefBottomSheet.class.getSimpleName();

	private static final Log LOG = PlatformUtil.getLog(ChangeGeneralProfilesPrefBottomSheet.class);

	private static final String NEW_VALUE_KEY = "new_value_key";

	private static final String CANCEL_TITLE_RES_KEY = "cancel_title_res_key";

	@Nullable
	private Serializable newValue;

	@Nullable
	private OnChangeSettingListener listener;

	public void setListener(@Nullable OnChangeSettingListener listener) {
		this.listener = listener;
	}

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if (args == null) return;

		int cancelTitleRes = args.getInt(CANCEL_TITLE_RES_KEY);
		String prefId = args.getString(PREFERENCE_ID);
		newValue = AndroidUtils.getSerializable(args, NEW_VALUE_KEY, Serializable.class);
		if (newValue == null || prefId == null) {
			return;
		}

		items.add(new TitleItem(getString(R.string.change_default_settings)));
		items.add(new LongDescriptionItem(getString(R.string.apply_preference_to_all_profiles)));

		BaseBottomSheetItem applyToAllProfiles = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.apply_to_all_profiles))
				.setIcon(getActiveIcon(R.drawable.ic_action_copy))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					settings.setPreferenceForAllModes(prefId, newValue);
					updateTargetSettings(false, true);
					if (listener != null) {
						listener.onPreferenceApplied(false);
					}
					dismiss();
				})
				.create();
		items.add(applyToAllProfiles);

		ApplicationMode selectedAppMode = getAppMode();

		BaseBottomSheetItem applyToCurrentProfile = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.apply_to_current_profile, selectedAppMode.toHumanString()))
				.setIcon(getActiveIcon(selectedAppMode.getIconRes()))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					settings.setPreference(prefId, newValue, getAppMode());
					updateTargetSettings(false, false);
					if (listener != null) {
						listener.onPreferenceApplied(true);
					}
					dismiss();
				})
				.create();
		items.add(applyToCurrentProfile);

		BaseBottomSheetItem discardChanges = new SimpleBottomSheetItem.Builder()
				.setTitle(cancelTitleRes == 0 ? getString(R.string.discard_changes) : getString(cancelTitleRes))
				.setIcon(getActiveIcon(R.drawable.ic_action_undo_dark))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(v -> {
					updateTargetSettings(true, false);
					if (listener != null) {
						listener.onDiscard();
					}
					dismiss();
				})
				.create();
		items.add(discardChanges);
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	@Override
	public void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(NEW_VALUE_KEY, newValue);
	}

	private void updateTargetSettings(boolean discard, boolean applyToAllProfiles) {
		BaseSettingsFragment target = (BaseSettingsFragment) getTargetFragment();
		if (target != null) {
			if (!discard) {
				target.onApplyPreferenceChange(getPrefId(), applyToAllProfiles, newValue);
			}
			target.updateSetting(getPrefId());
			if (!discard) {
				if (target.shouldDismissOnChange()) {
					target.dismiss();
				}
				FragmentManager manager = getFragmentManager();
				if (manager != null) {
					BasePreferenceBottomSheet preferenceBottomSheet =
							BasePreferenceBottomSheet.findPreferenceBottomSheet(manager, getPrefId());
					if (preferenceBottomSheet != null && preferenceBottomSheet.shouldDismissOnChange()) {
						preferenceBottomSheet.dismiss();
					}
				}
			}
		}
	}

	public static void showInstance(@NonNull FragmentManager fm,
									@Nullable String prefId,
									@Nullable Serializable newValue,
									Fragment target,
									boolean usedOnMap,
									@Nullable ApplicationMode appMode) {
		showInstance(fm, prefId, newValue, target, usedOnMap, 0, appMode, null);
	}

	public static void showInstance(@NonNull FragmentManager fm,
	                                 @Nullable String prefId,
	                                 @Nullable Serializable newValue,
	                                 Fragment target,
	                                 boolean usedOnMap,
	                                 @StringRes int cancelTitleRes,
	                                 @Nullable ApplicationMode appMode,
	                                 @Nullable OnChangeSettingListener listener) {
		if (AndroidUtils.isFragmentCanBeAdded(fm, TAG, true)) {
			Bundle args = new Bundle();
			args.putString(PREFERENCE_ID, prefId);
			args.putSerializable(NEW_VALUE_KEY, newValue);
			args.putInt(CANCEL_TITLE_RES_KEY, cancelTitleRes);

			ChangeGeneralProfilesPrefBottomSheet fragment = new ChangeGeneralProfilesPrefBottomSheet();
			fragment.setArguments(args);
			fragment.setUsedOnMap(usedOnMap);
			fragment.setAppMode(appMode);
			fragment.setTargetFragment(target, 0);
			fragment.setListener(listener);
			fragment.show(fm, TAG);
		}
	}

	public interface OnChangeSettingListener {
		default void onPreferenceApplied(boolean profileOnly) {
		}

		default void onDiscard() {
		}
	}
}