package net.osmand.plus.track.helpers;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.plus.track.SaveGpxAsyncTask;
import net.osmand.plus.track.SaveGpxAsyncTask.SaveGpxListener;

import java.io.File;

public class SaveGpxHelper {

	public static void saveGpx(@NonNull File file, @NonNull GPXFile gpx) {
		saveGpx(file, gpx, null);
	}

	public static void saveGpx(@NonNull File file, @NonNull GPXFile gpx,
	                           @Nullable SaveGpxListener listener) {
		new SaveGpxAsyncTask(file, gpx, listener)
				.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
	}

}
