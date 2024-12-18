package net.osmand.plus.search;

public enum ShowQuickSearchMode {
	NEW,
	NEW_IF_EXPIRED,
	CURRENT,
	START_POINT_SELECTION,
	DESTINATION_SELECTION,
	DESTINATION_SELECTION_AND_START,
	INTERMEDIATE_SELECTION,
	HOME_POINT_SELECTION,
	WORK_POINT_SELECTION;

	public boolean isPointSelection() {
		return this != NEW && this != NEW_IF_EXPIRED && this != CURRENT;
	}
}