package net.osmand.plus.settings.bottomsheets;

import static net.osmand.plus.settings.bottomsheets.BasePreferenceBottomSheet.PREFERENCE_ID;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;

import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.Optional;

public class BasePreferenceBottomSheetInitializer<T extends BasePreferenceBottomSheet> {

	private final T basePreferenceBottomSheet;

	private BasePreferenceBottomSheetInitializer(final T basePreferenceBottomSheet) {
		this.basePreferenceBottomSheet = basePreferenceBottomSheet;
	}

	public static <T extends BasePreferenceBottomSheet> BasePreferenceBottomSheetInitializer<T> initialize(final T basePreferenceBottomSheet) {
		return new BasePreferenceBottomSheetInitializer<>(basePreferenceBottomSheet);
	}

	public T with(final Optional<Preference> preference,
				  final @Nullable ApplicationMode appMode,
				  final boolean usedOnMap,
				  final @Nullable Fragment target) {
		preference.ifPresent(this::setPreference);
		basePreferenceBottomSheet.setUsedOnMap(usedOnMap);
		basePreferenceBottomSheet.setAppMode(appMode);
		basePreferenceBottomSheet.setTargetFragment(target, 0);
		return basePreferenceBottomSheet;
	}

	private void setPreference(final Preference preference) {
		basePreferenceBottomSheet.setPreference(preference);
		getArguments(basePreferenceBottomSheet).putString(PREFERENCE_ID, preference.getKey());
	}

	@NonNull
	private static Bundle getArguments(final Fragment fragment) {
		if (fragment.getArguments() == null) {
			fragment.setArguments(new Bundle());
		}
		return fragment.getArguments();
	}
}
