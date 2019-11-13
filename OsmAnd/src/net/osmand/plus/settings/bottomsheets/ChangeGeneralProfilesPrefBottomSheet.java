package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.view.View;

import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;
import net.osmand.plus.settings.BaseSettingsFragment;

import org.apache.commons.logging.Log;

import java.io.Serializable;

public class ChangeGeneralProfilesPrefBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = ChangeGeneralProfilesPrefBottomSheet.class.getSimpleName();

	private static final Log LOG = PlatformUtil.getLog(ChangeGeneralProfilesPrefBottomSheet.class);

	private static final String NEW_VALUE_KEY = "new_value_key";

	private Serializable newValue;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		final OsmandApplication app = getMyApplication();
		Bundle args = getArguments();
		if (app == null || args == null) {
			return;
		}
		final String prefId = args.getString(PREFERENCE_ID);
		newValue = args.getSerializable(NEW_VALUE_KEY);
		if (newValue == null || prefId == null) {
			return;
		}

		items.add(new TitleItem(getString(R.string.change_default_settings)));
		items.add(new LongDescriptionItem(getString(R.string.apply_preference_to_all_profiles)));

		BaseBottomSheetItem applyToAllProfiles = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.apply_to_all_profiles))
				.setIcon(getActiveIcon(R.drawable.ic_action_copy))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						app.getSettings().setSharedGeneralPreference(prefId, newValue);
						updateTargetSettings(false);
						dismiss();
					}
				})
				.create();
		items.add(applyToAllProfiles);

		ApplicationMode selectedAppMode = getAppMode();

		BaseBottomSheetItem applyToCurrentProfile = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.apply_to_current_profile, selectedAppMode.toHumanString(app)))
				.setIcon(getActiveIcon(selectedAppMode.getIconRes()))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						app.getSettings().setPreference(prefId, newValue, getAppMode());
						updateTargetSettings(false);
						dismiss();
					}
				})
				.create();
		items.add(applyToCurrentProfile);

		BaseBottomSheetItem discardChanges = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.discard_changes))
				.setIcon(getActiveIcon(R.drawable.ic_action_undo_dark))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						updateTargetSettings(true);
						dismiss();
					}
				})
				.create();
		items.add(discardChanges);
	}

	@Override
	protected boolean hideButtonsContainer() {
		return true;
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putSerializable(NEW_VALUE_KEY, newValue);
	}

	private void updateTargetSettings(boolean discard) {
		BaseSettingsFragment target = (BaseSettingsFragment) getTargetFragment();
		if (target != null) {
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

	public static void showInstance(@NonNull FragmentManager fm, String prefId, Serializable newValue, Fragment target,
									boolean usedOnMap, @Nullable ApplicationMode appMode) {
		try {
			if (fm.findFragmentByTag(ChangeGeneralProfilesPrefBottomSheet.TAG) == null) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefId);
				args.putSerializable(NEW_VALUE_KEY, newValue);

				ChangeGeneralProfilesPrefBottomSheet fragment = new ChangeGeneralProfilesPrefBottomSheet();
				fragment.setArguments(args);
				fragment.setUsedOnMap(usedOnMap);
				fragment.setAppMode(appMode);
				fragment.setTargetFragment(target, 0);
				fragment.show(fm, ChangeGeneralProfilesPrefBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}