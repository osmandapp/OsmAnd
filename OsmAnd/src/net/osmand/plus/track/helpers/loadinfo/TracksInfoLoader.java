package net.osmand.plus.track.helpers.loadinfo;

import static net.osmand.IndexConstants.GPX_INDEX_DIR;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.track.helpers.loadinfo.LoadTrackInfoTask.LoadTracksListener;

import java.io.File;

public class TracksInfoLoader {

	public static void loadTracksInfo(
			@NonNull OsmandApplication app,
			@NonNull LoadTracksListener listener
	) {
		loadTracksInfoForDirectory(app, app.getAppPath(GPX_INDEX_DIR), listener);
	}

	public static void loadTracksInfoForDirectory(
			@NonNull OsmandApplication app,
			@NonNull File rootDir, @NonNull LoadTracksListener listener
	) {
		LoadTrackInfoTask task = new LoadTrackInfoTask(app, rootDir, listener);
		task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}
}