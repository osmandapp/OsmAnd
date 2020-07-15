package net.osmand.plus.myplaces;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;

import java.io.File;

public class SaveGpxAsyncTask extends AsyncTask<Void, Void, Void> {

	private final GPXFile gpx;
	private final SaveGpxListener saveGpxListener;

	public SaveGpxAsyncTask(@NonNull GPXFile gpx,
	                        @Nullable SaveGpxListener saveGpxListener) {
		this.gpx = gpx;
		this.saveGpxListener = saveGpxListener;
	}

	@Override
	protected void onPreExecute() {
		if (saveGpxListener != null) {
			saveGpxListener.gpxSavingStarted();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		GPXUtilities.writeGpxFile(new File(gpx.path), gpx);
		return null;
	}

	@Override
	protected void onPostExecute(Void aVoid) {
		if (saveGpxListener != null) {
			saveGpxListener.gpxSavingFinished();
		}
	}

	public interface SaveGpxListener {

		void gpxSavingStarted();

		void gpxSavingFinished();
	}
}