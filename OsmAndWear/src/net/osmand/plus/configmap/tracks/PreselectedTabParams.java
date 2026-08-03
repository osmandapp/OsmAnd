package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.tracks.TrackTabType.SMART_FOLDER;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import java.util.Collections;
import java.util.List;

public class PreselectedTabParams {

	public static final String PRESELECTED_TRACKS_TAB_NAME = "preselected_tab_name";
	public static final String PRESELECTED_TRACKS_TAB_TYPE = "preselected_tab_type";
	public static final String SELECT_ALL_ITEMS_ON_TAB = "select_all_items_on_tab";
	public static final String CALLING_FRAGMENT_TAG = "calling_fragment_tag";

	@NonNull
	private final String name;
	@NonNull
	private final TrackTabType type;
	private final boolean selectAll;

	public PreselectedTabParams(@NonNull String name, @NonNull TrackTabType type, boolean selectAll) {
		this.name = name;
		this.type = type;
		this.selectAll = selectAll;
	}

	public boolean shouldSelectAll() {
		return selectAll;
	}

	@NonNull
	public String getPreselectedTabName(@NonNull Context context, @NonNull List<TrackTab> trackTabs) {
		if (type == SMART_FOLDER) {
			for (TrackTab tab : trackTabs) {
				String tabName = tab.getName(context);
				if (tab.type == SMART_FOLDER && Algorithms.stringsEqual(tabName, name)) {
					return tab.getTypeName();
				}
			}
			return "";
		}
		return name;
	}
}