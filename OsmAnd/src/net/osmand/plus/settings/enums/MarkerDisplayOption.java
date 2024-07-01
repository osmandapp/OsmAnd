package net.osmand.plus.settings.enums;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.views.layers.PointLocationLayer.MarkerState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum MarkerDisplayOption {
	OFF(R.string.shared_string_off),
	RESTING(R.string.resting_position, MarkerState.Stay),
	NAVIGATION(R.string.navigation_position, MarkerState.Move),
	RESTING_NAVIGATION(R.string.resting_navigation_position, MarkerState.Move, MarkerState.Stay);

	private final List<MarkerState> markerStates;

	@StringRes
	private final int nameRes;

	MarkerDisplayOption(@StringRes int nameRes, MarkerState... states) {
		this.nameRes = nameRes;
		markerStates = new ArrayList<>(Arrays.asList(states));
	}

	public boolean isVisible(@NonNull MarkerState markerState) {
		return markerStates.contains(markerState);
	}

	@StringRes
	public int getNameRes() {
		return nameRes;
	}
}
