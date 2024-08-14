package net.osmand.plus.importfiles.tasks;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXFile;
import net.osmand.gpx.GPXUtilities.Track;
import net.osmand.gpx.GPXUtilities.WptPt;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.importfiles.ui.ImportTrackItem;
import net.osmand.plus.track.helpers.SelectedGpxFile;
import net.osmand.util.Algorithms;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.List;

public class CollectTracksTask extends AsyncTask<Void, Void, List<ImportTrackItem>> {

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
	protected List<ImportTrackItem> doInBackground(Void... params) {
		List<ImportTrackItem> items = new ArrayList<>();
		String name = Algorithms.getFileNameWithoutExtension(fileName);
		for (int i = 0; i < gpxFile.tracks.size(); i++) {
			Track track = gpxFile.tracks.get(i);
			if (!track.generalTrack) {
				GPXFile trackFile = new GPXFile(Version.getFullVersion(app));
				trackFile.tracks.add(track);
				trackFile.setColor(track.getColor(gpxFile.getColor(0)));
				trackFile.setWidth(gpxFile.getWidth(null));
				trackFile.setShowArrows(gpxFile.isShowArrows());
				trackFile.setShowStartFinish(gpxFile.isShowStartFinish());
				trackFile.setSplitInterval(gpxFile.getSplitInterval());
				trackFile.setSplitType(gpxFile.getSplitType());
				trackFile.setColoringType(gpxFile.getColoringType());
				trackFile.setGradientColorPalette(gpxFile.getGradientColorPalette());
				trackFile.set3DVisualizationType(gpxFile.get3DVisualizationType());
				trackFile.set3DWallColoringType(gpxFile.get3DWallColoringType());
				trackFile.set3DLinePositionType(gpxFile.get3DLinePositionType());
				trackFile.setAdditionalExaggeration(gpxFile.getAdditionalExaggeration());
				trackFile.setElevationMeters(gpxFile.getElevationMeters());

				SelectedGpxFile selectedGpxFile = new SelectedGpxFile();
				selectedGpxFile.setGpxFile(trackFile, app);

				String trackName = track.name;
				if (Algorithms.isEmpty(trackName)) {
					trackName = app.getString(R.string.ltr_or_rtl_combine_via_dash, name, String.valueOf(i));
				}
				items.add(new ImportTrackItem(selectedGpxFile, trackName, i));
			}
		}
		for (WptPt point : gpxFile.getPoints()) {
			ImportTrackItem item = findNearestTrack(point, items);
			if (item != null) {
				item.selectedPoints.add(point);
				item.suggestedPoints.add(point);
			}
		}
		return items;
	}

	private ImportTrackItem findNearestTrack(@NonNull WptPt point, @NonNull List<ImportTrackItem> items) {
		ImportTrackItem trackItem = null;
		double minDistance = Double.MAX_VALUE;
		for (ImportTrackItem item : items) {
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
	protected void onPostExecute(List<ImportTrackItem> items) {
		if (listener != null) {
			listener.tracksCollectionFinished(items);
		}
	}

	public interface CollectTracksListener {

		void tracksCollectionStarted();

		void tracksCollectionFinished(List<ImportTrackItem> items);
	}
}
