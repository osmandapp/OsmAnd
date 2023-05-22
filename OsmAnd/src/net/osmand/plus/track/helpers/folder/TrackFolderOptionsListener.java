package net.osmand.plus.track.helpers.folder;

import androidx.annotation.NonNull;

import net.osmand.plus.track.data.TrackFolder;

import java.io.File;

public interface TrackFolderOptionsListener {
	void onFolderRenamed(@NonNull File oldDir, @NonNull File newDir);

	void onFolderDeleted();

	void showFolderTracksOnMap(@NonNull TrackFolder folder);
}
