package net.osmand.plus.myplaces.tracks.controller;

import androidx.annotation.NonNull;

import net.osmand.plus.track.data.SmartFolder;

public interface SmartFolderOptionsListener {
	default void showSmartFolderDetails(@NonNull SmartFolder folder) {

	}

	default void showSmartFolderTracksOnMap(@NonNull SmartFolder folder) {
	}

	default void showExportDialog(@NonNull SmartFolder folder) {
	}

	default void showChangeAppearanceDialog(@NonNull SmartFolder trackFolder) {
	}
}
