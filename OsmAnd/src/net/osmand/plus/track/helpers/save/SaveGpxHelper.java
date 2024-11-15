package net.osmand.plus.track.helpers.save;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.shared.gpx.GpxFile;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.myplaces.tracks.tasks.SaveCurrentTrackTask;

import java.io.File;

public class SaveGpxHelper {

	public static void saveGpx(@NonNull GpxFile gpx) {
		saveGpx(gpx, null);
	}

	public static void saveGpx(@NonNull GpxFile gpx, @Nullable SaveGpxListener listener) {
		saveGpx(new File(gpx.getPath()), gpx, listener);
	}

	public static void saveGpx(@NonNull File file, @NonNull GpxFile gpx) {
		saveGpx(file, gpx, null);
	}

	public static void saveGpx(@NonNull File file, @NonNull GpxFile gpx,
	                           @Nullable SaveGpxListener listener) {
		new SaveGpxAsyncTask(file, gpx, listener)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

	public static void saveCurrentTrack(@NonNull OsmandApplication app, @NonNull GpxFile gpx,
	                                    @NonNull SaveGpxListener listener) {
		new SaveCurrentTrackTask(app, gpx, listener)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

}
