package net.osmand.plus.myplaces.favorites;

public enum SaveOption {
	APPLY_TO_EXISTING,
	APPLY_TO_NEW,
	APPLY_TO_ALL;

	public boolean shouldUpdatePoints() {
		return this == APPLY_TO_EXISTING || this == APPLY_TO_ALL;
	}

	public boolean shouldUpdateGroup() {
		return this == APPLY_TO_NEW || this == APPLY_TO_ALL;
	}
}
