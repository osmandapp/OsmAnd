package net.osmand.plus.settings.bottomsheets;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;

import net.osmand.plus.ApplicationMode;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.LongDescriptionItem;
import net.osmand.plus.base.bottomsheetmenu.simpleitems.TitleItem;

public class ResetProfilePrefsBottomSheet extends BasePreferenceBottomSheet {

	public static final String TAG = ResetProfilePrefsBottomSheet.class.getSimpleName();

	@Override
	public void createMenuItems(Bundle savedInstanceState) {
		Context ctx = getContext();
		if (ctx == null) {
			return;
		}

		items.add(new TitleItem(getString(R.string.reset_all_profile_settings)));
		items.add(new TitleItem(getAppMode().toHumanString(ctx)));
		items.add(new LongDescriptionItem(getString(R.string.reset_all_profile_settings_descr)));
		items.add(new LongDescriptionItem(getString(R.string.reset_confirmation_descr, getString(R.string.shared_string_reset))));
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