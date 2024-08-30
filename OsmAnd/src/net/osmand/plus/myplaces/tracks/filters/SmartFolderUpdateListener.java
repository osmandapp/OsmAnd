package net.osmand.plus.myplaces.tracks.filters;

import androidx.annotation.WorkerThread;

import net.osmand.plus.track.data.SmartFolder;

public interface SmartFolderUpdateListener {
	default void onSmartFoldersUpdated() {
	}

	@WorkerThread
	default void onSmartFolderUpdated(SmartFolder smartFolder) {
	}

	default void onSmartFolderRenamed(SmartFolder smartFolder) {
	}

	default void onSmartFolderSaved(SmartFolder smartFolder) {
	}

	default void onSmartFolderCreated(SmartFolder smartFolder) {
	}
}
