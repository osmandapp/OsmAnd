package net.osmand.plus.settings.enums;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import net.osmand.plus.R;
import net.osmand.plus.views.layers.PointLocationLayer.MarkerState;

import java.util.Arrays;
import java.util.List;

public enum MarkerDisplayOption {

	OFF(R.string.shared_string_off, MarkerState.NONE),
	RESTING(R.string.resting_position, MarkerState.STAY),
	NAVIGATION(R.string.navigation_position, MarkerState.MOVE),
	RESTING_NAVIGATION(R.string.resting_navigation_position, MarkerState.MOVE, MarkerState.STAY);

	@StringRes
	private final int nameId;
	private final List<MarkerState> markerStates;


	MarkerDisplayOption(@StringRes int nameId, @NonNull MarkerState... states) {
		this.nameId = nameId;
		this.markerStates = Arrays.asList(states);
	}

	@StringRes
	public int getNameId() {
		return nameId;
	}

	public boolean isVisible(@NonNull MarkerState state) {
		return this != OFF && markerStates.contains(state);
	}
}
