package net.osmand.plus.track.helpers.folder;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.track.data.TrackFolder;
import net.osmand.plus.utils.FileUtils;
import net.osmand.util.Algorithms;

import java.io.File;

public class TrackFolderHelper {

	private final OsmandApplication app;
	private TrackFolderOptionsListener listener;

	public TrackFolderHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void setListener(@NonNull TrackFolderOptionsListener listener) {
		this.listener = listener;
	}

	public void deleteTrackFolder(@NonNull TrackFolder folder) {
		// Remove all individual GPX files
		for (TrackItem trackItem : folder.getFlattenedTrackItems()) {
			File file = trackItem.getFile();
			if (file != null) {
				FileUtils.removeGpxFile(app, file);
			}
		}
		// Remove root directory and all subdirectories
		Algorithms.removeAllFiles(folder.getDirFile());
		if (listener != null) {
			listener.onFolderDeleted();
		}
	}

	public void renameTrackFolder(@NonNull TrackFolder trackFolder, @NonNull String newName) {
		File oldDir = trackFolder.getDirFile();
		File newDir = new File(oldDir.getParentFile(), newName);
		if (oldDir.renameTo(newDir)) {
			if (listener != null) {
				listener.onFolderRenamed(oldDir, newDir);
			}
		}
	}
}