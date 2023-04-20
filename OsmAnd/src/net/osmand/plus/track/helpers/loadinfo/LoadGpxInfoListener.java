package net.osmand.plus.track.helpers.loadinfo;

import androidx.annotation.NonNull;

import net.osmand.plus.track.data.GPXFolderInfo;
import net.osmand.plus.track.data.GPXInfo;

public interface LoadGpxInfoListener {

	default void onGpxInfoLoadStarted() { }

	default void onUpdateGpxInfoLoadProgress(GPXInfo[] lastLoaded) { }

	void onGpxInfoLoadFinished(@NonNull GPXFolderInfo result);

}
