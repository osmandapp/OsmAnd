package net.osmand.plus.myplaces.tracks.filters;

import net.osmand.plus.track.data.SmartFolder;

public interface SmartFolderUpdateListener {
	default void onSmartFoldersUpdated() {
	}

	default void onSmartFolderUpdated(SmartFolder smartFolder) {
	}

	default void onSmartFolderRenamed(SmartFolder smartFolder) {
	}

	default void onSmartFolderSaved(SmartFolder smartFolder) {
	}

	default void onSmartFolderCreated(SmartFolder smartFolder) {
	}
}
