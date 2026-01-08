package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;

public class PreselectedTabParams {

	public static final String PRESELECTED_TRACKS_TAB_ID = "preselected_tab_id";
	public static final String SELECT_ALL_ITEMS_ON_TAB = "select_all_items_on_tab";
	public static final String CALLING_FRAGMENT_TAG = "calling_fragment_tag";

	@NonNull
	private final String id;
	private final boolean selectAll;

	public PreselectedTabParams(@NonNull String id, boolean selectAll) {
		this.id = id;
		this.selectAll = selectAll;
	}

	public boolean shouldSelectAll() {
		return selectAll;
	}

	@NonNull
	public String getPreselectedTabId() {
		return id;
	}
}