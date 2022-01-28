package net.osmand.plus.importfiles;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.importfiles.ui.TrackItem;
import net.osmand.plus.track.helpers.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.util.Algorithms;

import java.util.ArrayList;
import java.util.List;

public class CollectTracksAsyncTask extends AsyncTask<Void, Void, List<TrackItem>> {

	private final OsmandApplication app;
	private final GPXFile gpxFile;
	private final String fileName;
	private final CollectTracksListener listener;

	public CollectTracksAsyncTask(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
	                              @NonNull String fileName, @Nullable CollectTracksListener listener) {
		this.app = app;
		this.gpxFile = gpxFile;
		this.fileName = fileName;
		this.listener = listener;
	}

	@Override
	protected void onPreExecute() {
		if (listener != null) {
			listener.tracksCollectionStarted();
		}
	}

	@Override
	protected List<TrackItem> doInBackground(Void... params) {
		List<TrackItem> items = new ArrayList<>();
		String name = Algorithms.getFileNameWithoutExtension(fileName);
		for (int i = 0; i < gpxFile.tracks.size(); i++) {
			Track track = gpxFile.tracks.get(i);
			if (!track.generalTrack) {
				GPXFile trackFile = new GPXFile(Version.getFullVersion(app));
				trackFile.modifiedTime = -1;
				trackFile.tracks.add(track);
				trackFile.addPoints(gpxFile.getPoints());

				SelectedGpxFile selectedGpxFile = new SelectedGpxFile();
				selectedGpxFile.setGpxFile(trackFile, app);

				TrackItem trackItem = new TrackItem(selectedGpxFile, i);
				trackItem.suggestedPoints = gpxFile.getPoints();
				if (Algorithms.isEmpty(track.name)) {
					trackItem.name = app.getString(R.string.ltr_or_rtl_combine_via_dash, name, String.valueOf(i));
				} else {
					trackItem.name = track.name;
				}
				items.add(trackItem);
			}
		}
		return items;
	}

	@Override
	protected void onPostExecute(List<TrackItem> items) {
		if (listener != null) {
			listener.tracksCollectionFinished(items);
		}
	}

	public interface CollectTracksListener {

		void tracksCollectionStarted();

		void tracksCollectionFinished(List<TrackItem> items);
	}
}
