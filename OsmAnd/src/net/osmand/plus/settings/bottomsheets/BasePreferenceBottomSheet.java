package net.osmand.plus.settings.bottomsheets;

import android.os.Bundle;
import android.support.v7.preference.DialogPreference.TargetFragment;
import android.support.v7.preference.Preference;

import net.osmand.plus.base.MenuBottomSheetDialogFragment;

public abstract class BasePreferenceBottomSheet extends MenuBottomSheetDialogFragment {

	public static final String PREFERENCE_ID = "preference_id";

	private Preference preference;

	public Preference getPreference() {
		if (preference == null) {
			Bundle args = getArguments();
			if (args != null && args.containsKey(PREFERENCE_ID)) {
				String prefId = args.getString(PREFERENCE_ID);
				TargetFragment targetFragment = (TargetFragment) getTargetFragment();

				if (targetFragment != null) {
					preference = targetFragment.findPreference(prefId);
				}
			}
		}
		return preference;
	}
}
