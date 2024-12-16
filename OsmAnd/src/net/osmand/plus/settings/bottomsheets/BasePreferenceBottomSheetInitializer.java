package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet.PREFERENCE_ID;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import net.osmand.plus.settings.backend.ApplicationMode;

class BasePreferenceBottomSheetInitializer<T extends BasePreferenceBottomSheet> {

	private final T basePreferenceBottomSheet;

	private BasePreferenceBottomSheetInitializer(final T basePreferenceBottomSheet) {
		this.basePreferenceBottomSheet = basePreferenceBottomSheet;
	}

	public static <T extends BasePreferenceBottomSheet> BasePreferenceBottomSheetInitializer<T> initialize(final T basePreferenceBottomSheet) {
		return new BasePreferenceBottomSheetInitializer<>(basePreferenceBottomSheet);
	}

	public T with(final Preference preference,
				  final @Nullable ApplicationMode appMode,
				  final boolean usedOnMap,
				  final @Nullable Fragment target) {
		final Bundle args = new Bundle();
		args.putString(PREFERENCE_ID, preference.getKey());
		basePreferenceBottomSheet.setArguments(args);
		basePreferenceBottomSheet.setUsedOnMap(usedOnMap);
		basePreferenceBottomSheet.setAppMode(appMode);
		basePreferenceBottomSheet.setTargetFragment(target, 0);
		// FK-TODO: find a clearer way to handle this (everywhere):
		if (target == null) {
			basePreferenceBottomSheet.setPreference(preference);
		}
		return basePreferenceBottomSheet;
	}
}
