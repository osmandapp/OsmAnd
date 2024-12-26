package net.osmand.plus.myplaces.tracks.controller;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.myplaces.tracks.DialogClosedListener;
import net.osmand.shared.gpx.data.SmartFolder;

public interface SmartFolderOptionsListener {
	default void showSmartFolderDetails(@NonNull SmartFolder folder) {
	}

	default void showSmartFolderTracksOnMap(@NonNull SmartFolder folder) {
	}

	default void showExportDialog(@NonNull SmartFolder folder) {
	}

	default void showEditFiltersDialog(@NonNull SmartFolder trackFolder, @Nullable DialogClosedListener dialogClosedListener) {
	}
}
