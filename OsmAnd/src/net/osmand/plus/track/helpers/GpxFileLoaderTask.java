package net.osmand.plus.track.helpers;

import static net.osmand.GPXUtilities.loadGPXFile;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.CallbackWithObject;
import net.osmand.GPXUtilities.GPXFile;

import java.io.File;

public class GpxFileLoaderTask extends AsyncTask<Void, Void, GPXFile> {

	private final File file;
	private final CallbackWithObject<GPXFile> callback;

	public GpxFileLoaderTask(@NonNull File file, @NonNull CallbackWithObject<GPXFile> callback) {
		this.file = file;
		this.callback = callback;
	}

	@Override
	protected GPXFile doInBackground(Void... voids) {
		return loadGPXFile(file);
	}

	@Override
	protected void onPostExecute(GPXFile gpxFile) {
		if (callback != null) {
			callback.processResult(gpxFile);
		}
	}
}