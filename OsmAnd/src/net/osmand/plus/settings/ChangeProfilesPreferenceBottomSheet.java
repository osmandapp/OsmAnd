package net.osmand.plus.settings;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.view.View;

import net.osmand.PlatformUtil;
import net.osmand.plus.ApplicationMode;
import net.osmand.plus.R;
import net.osmand.plus.base.MenuBottomSheetDialogFragment;
import net.osmand.plus.base.bottomsheetmenu.BaseBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.SimpleBottomSheetItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

import org.apache.commons.logging.Log;

public class ChangeProfilesPreferenceBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String TAG = "ChangeProfilesPreferenceBottomSheet";

	private static final String PREFERENCE_ID = "preference_id";

	private static final Log LOG = PlatformUtil.getLog(ChangeProfilesPreferenceBottomSheet.class);

	private Object newValue;

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context context = getContext();
		if (context == null) {
			return;
		}
		items.add(new TitleItem(getString(R.string.change_default_settings)));

		BaseBottomSheetItem applyToAllProfiles = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.apply_to_all_profiles))
				.setIcon(getActiveIcon(R.drawable.ic_action_copy))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						getMyApplication().showShortToastMessage("applyToAllProfiles");
					}
				})
				.create();
		items.add(applyToAllProfiles);

		ApplicationMode selectedAppMode = getMyApplication().getSettings().APPLICATION_MODE.get();

		BaseBottomSheetItem applyToCurrentProfile = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.apply_to_current_profile, selectedAppMode.toHumanString(context)))
				.setIcon(getActiveIcon(selectedAppMode.getIconRes()))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						getMyApplication().showShortToastMessage("applyToCurrentProfile");
					}
				})
				.create();
		items.add(applyToCurrentProfile);

		BaseBottomSheetItem discardChanges = new SimpleBottomSheetItem.Builder()
				.setTitle(getString(R.string.discard_changes))
				.setIcon(getActiveIcon(R.drawable.ic_action_copy))
				.setLayoutId(R.layout.bottom_sheet_item_simple)
				.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
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

	public static void showInstance(@NonNull FragmentManager fm, String prefId, Object newValue) {
		try {
			if (fm.findFragmentByTag(ChangeProfilesPreferenceBottomSheet.TAG) == null) {
				Bundle args = new Bundle();
				args.putString(PREFERENCE_ID, prefId);

				ChangeProfilesPreferenceBottomSheet fragment = new ChangeProfilesPreferenceBottomSheet();
				fragment.setArguments(args);
				fragment.newValue = newValue;
				fragment.show(fm, ChangeProfilesPreferenceBottomSheet.TAG);
			}
		} catch (RuntimeException e) {
			LOG.error("showInstance", e);
		}
	}
}