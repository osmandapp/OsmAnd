package net.osmand.plus.track.helpers.folder;

import androidx.annotation.NonNull;

import java.io.File;

public interface TrackFolderOptionsListener {
	void onFolderRenamed(@NonNull File newDir);
	void onFolderDeleted();
}
