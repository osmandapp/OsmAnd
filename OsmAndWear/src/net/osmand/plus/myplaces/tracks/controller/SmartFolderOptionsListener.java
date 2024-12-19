package net.osmand.plus.myplaces.tracks.controller;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.data.SmartFolder;

public interface SmartFolderOptionsListener {
	default void showSmartFolderDetails(@NonNull SmartFolder folder) {
	}

	default void showSmartFolderTracksOnMap(@NonNull SmartFolder folder) {
	}

	default void showExportDialog(@NonNull SmartFolder folder) {
	}

	default void showEditFiltersDialog(@NonNull SmartFolder trackFolder) {
	}
}
