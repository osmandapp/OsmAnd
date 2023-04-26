package net.osmand.plus.track.helpers.loadinfo;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask;
import net.osmand.plus.configmap.tracks.TrackFolderLoaderTask.LoadTracksListener;

import java.io.File;

public class GpxInfoLoader {

	public static void loadAllGpxInfo(@NonNull OsmandApplication app,
	                                  @NonNull LoadTracksListener listener) {
		loadGpxInfoForDirectory(app, app.getAppPath(GPX_INDEX_DIR), listener);
	}

	public static void loadGpxInfoForDirectory(@NonNull OsmandApplication app,
	                                           @NonNull File rootDir,
	                                           @NonNull LoadTracksListener listener) {
		TrackFolderLoaderTask task = new TrackFolderLoaderTask(app, rootDir, listener);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}