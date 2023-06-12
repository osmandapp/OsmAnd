package net.osmand.plus.myplaces.tracks.controller;

import androidx.annotation.NonNull;

import net.osmand.plus.track.data.TrackFolder;

import java.io.File;

public interface TrackFolderOptionsListener {
	void onFolderRenamed(@NonNull File oldDir, @NonNull File newDir);

	void onFolderDeleted();

	void showFolderTracksOnMap(@NonNull TrackFolder folder);

	void showExportDialog(@NonNull TrackFolder folder);

	void showChangeAppearanceDialog(@NonNull TrackFolder trackFolder);
}
