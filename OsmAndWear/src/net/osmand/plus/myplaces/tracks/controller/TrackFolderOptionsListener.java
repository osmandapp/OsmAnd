package net.osmand.plus.myplaces.tracks.controller;

import androidx.annotation.NonNull;

import net.osmand.shared.gpx.data.TrackFolder;

import java.io.File;

public interface TrackFolderOptionsListener {
	default void onFolderRenamed(@NonNull File newDir) {

	}

	default void onFolderDeleted() {

	}

	default void showFolderTracksOnMap(@NonNull TrackFolder folder) {

	}

	default void showExportDialog(@NonNull TrackFolder folder) {

	}

	default void showChangeAppearanceDialog(@NonNull TrackFolder trackFolder) {

	}
}
