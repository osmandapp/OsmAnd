package net.osmand.plus.track.helpers.savetrack;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXUtilities;
import net.osmand.gpx.GPXFile;

import java.io.File;

public class SaveGpxTask extends AsyncTask<Void, Void, Exception> {

	private final File file;
	private final GPXFile gpx;
	private final SaveGpxListener saveGpxListener;

	public SaveGpxTask(@NonNull File file, @NonNull GPXFile gpx,
	                   @Nullable SaveGpxListener saveGpxListener) {
		this.gpx = gpx;
		this.file = file;
		this.saveGpxListener = saveGpxListener;
	}

	@Override
	protected void onPreExecute() {
		if (saveGpxListener != null) {
			saveGpxListener.onSaveGpxStarted();
		}
	}

	@Override
	protected Exception doInBackground(Void... params) {
		return GPXUtilities.writeGpxFile(file, gpx);
	}

	@Override
	protected void onPostExecute(Exception errorMessage) {
		if (saveGpxListener != null) {
			saveGpxListener.onSaveGpxFinished(errorMessage);
		}
	}
}