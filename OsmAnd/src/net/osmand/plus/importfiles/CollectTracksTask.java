package net.osmand.plus.importfiles;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.importfiles.ui.TrackItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class CollectTracksTask extends AsyncTask<Void, Void, List<TrackItem>> {

	private final OsmandApplication app;
	private final GPXFile gpxFile;
	private final String fileName;
	private final CollectTracksListener listener;

	public CollectTracksTask(@NonNull OsmandApplication app, @NonNull GPXFile gpxFile,
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
				trackFile.tracks.add(track);
				trackFile.setColor(gpxFile.getColor(0));
				trackFile.setWidth(gpxFile.getWidth(null));
				trackFile.setShowArrows(gpxFile.isShowArrows());
				trackFile.setShowStartFinish(gpxFile.isShowStartFinish());
				trackFile.setSplitInterval(gpxFile.getSplitInterval());
				trackFile.setSplitType(gpxFile.getSplitType());
				trackFile.setColoringType(gpxFile.getColoringType());

				SelectedGpxFile selectedGpxFile = new SelectedGpxFile();
				selectedGpxFile.setGpxFile(trackFile, app);

				String trackName = track.name;
				if (Algorithms.isEmpty(trackName)) {
					name = app.getString(R.string.ltr_or_rtl_combine_via_dash, name, String.valueOf(i));
				}
				items.add(new TrackItem(selectedGpxFile, trackName, i));
			}
		}
		for (WptPt point : gpxFile.getPoints()) {
			TrackItem item = findNearestTrack(point, items);
			if (item != null) {
				item.selectedPoints.add(point);
				item.suggestedPoints.add(point);
			}
		}
		return items;
	}

	private TrackItem findNearestTrack(@NonNull WptPt point, @NonNull List<TrackItem> items) {
		TrackItem trackItem = null;
		double minDistance = Double.MAX_VALUE;
		for (TrackItem item : items) {
			GPXFile gpxFile = item.selectedGpxFile.getGpxFile();
			for (WptPt wptPt : gpxFile.getAllSegmentsPoints()) {
				double distance = MapUtils.getDistance(point.lat, point.lon, wptPt.lat, wptPt.lon);
				if (distance < minDistance) {
					minDistance = distance;
					trackItem = item;
				}
			}
		}
		return trackItem;
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
