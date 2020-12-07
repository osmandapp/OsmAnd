package net.osmand.plus.track;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;

import java.io.File;

public class SaveGpxAsyncTask extends AsyncTask<Void, Void, Exception> {

	private final File file;
	private final GPXFile gpx;
	private final SaveGpxListener saveGpxListener;

	public SaveGpxAsyncTask(@NonNull File file, @NonNull GPXFile gpx,
							@Nullable SaveGpxListener saveGpxListener) {
		this.gpx = gpx;
		this.file = file;
		this.saveGpxListener = saveGpxListener;
	}

	@Override
	protected void onPreExecute() {
		if (saveGpxListener != null) {
			saveGpxListener.gpxSavingStarted();
		}
	}

	@Override
	protected Exception doInBackground(Void... params) {
		return GPXUtilities.writeGpxFile(file, gpx);
	}

	@Override
	protected void onPostExecute(Exception errorMessage) {
		if (saveGpxListener != null) {
			saveGpxListener.gpxSavingFinished(errorMessage);
		}
	}

	public interface SaveGpxListener {

		void gpxSavingStarted();

		void gpxSavingFinished(Exception errorMessage);
	}
}