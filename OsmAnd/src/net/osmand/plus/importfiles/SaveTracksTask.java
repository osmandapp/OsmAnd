package net.osmand.plus.importfiles;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.IndexConstants;
import net.osmand.plus.importfiles.ui.TrackItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SaveTracksTask extends AsyncTask<Void, Void, List<String>> {

	private final File importDir;
	private final List<TrackItem> items;
	private final SaveGpxListener listener;

	public SaveTracksTask(@NonNull List<TrackItem> items, @NonNull File importDir, @Nullable SaveGpxListener listener) {
		this.items = items;
		this.importDir = importDir;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.gpxSavingStarted();
		}
	}

	@Override
	protected List<String> doInBackground(Void... params) {
		//noinspection ResultOfMethodCallIgnored
		importDir.mkdirs();
		List<String> warnings = new ArrayList<>();
		if (importDir.exists() && importDir.isDirectory() && importDir.canWrite()) {
			for (TrackItem trackItem : items) {
				GPXFile gpxFile = trackItem.selectedGpxFile.getGpxFile();
				gpxFile.addPoints(trackItem.selectedPoints);

				File file = new File(importDir, trackItem.name + IndexConstants.GPX_FILE_EXT);
				Exception warn = GPXUtilities.writeGpxFile(file, gpxFile);
				if (warn != null) {
					warnings.add(warn.getMessage());
				}
			}
		}
		return warnings;
	}

	@Override
	protected void onPostExecute(List<String> warnings) {
		if (listener != null) {
			listener.gpxSavingFinished(warnings);
		}
	}

	public interface SaveGpxListener {

		void gpxSavingStarted();

		void gpxSavingFinished(List<String> warnings);
	}
}
