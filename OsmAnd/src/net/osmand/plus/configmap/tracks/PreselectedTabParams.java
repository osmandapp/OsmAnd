package net.osmand.plus.configmap.tracks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class PreselectedTabParams {

	@NonNull
	private final String folderName;
	private final boolean isSmartFolder;
	private final boolean shouldSelectAllItems;

	public PreselectedTabParams(@NonNull String folderName,
	                            boolean isSmartFolder,
	                            boolean shouldSelectAllItems) {
		this.folderName = folderName;
		this.isSmartFolder = isSmartFolder;
		this.shouldSelectAllItems = shouldSelectAllItems;
	}

	@NonNull
	public String getPreselectedTabName(@NonNull Context context,
	                                    @NonNull List<TrackTab> tabList) {
		if (isSmartFolder) {
			for (TrackTab tab : tabList) {
				String tabName = tab.getName(context);
				if (tab.type == TrackTabType.SMART_FOLDER && Algorithms.stringsEqual(tabName, folderName)) {
					return tab.getTypeName();
				}
			}
			return "";
		}
		return folderName;
	}

	@NonNull
	public List<TrackItem> getPreselectedTrackItems(@Nullable TrackTab trackTab) {
		if (shouldSelectAllItems && trackTab != null) {
			return trackTab.getTrackItems();
		}
		return new ArrayList<>();
	}
}
