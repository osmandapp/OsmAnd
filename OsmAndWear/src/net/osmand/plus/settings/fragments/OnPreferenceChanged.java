package net.osmand.plus.settings.fragments;

import androidx.annotation.NonNull;

public interface OnPreferenceChanged {

	default void onPreferenceChanged(@NonNull String prefId) {

	}
}
