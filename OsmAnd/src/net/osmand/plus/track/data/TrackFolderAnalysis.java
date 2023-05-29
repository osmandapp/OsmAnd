package net.osmand.plus.track.data;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;

import java.io.File;
import java.util.List;

public class TrackFolderAnalysis {

	public int tracksCount;
	public float totalDistance;
	public int duration;
	public long fileSize;
	public double diffElevationUp;
	public double diffElevationDown;

	@NonNull
	public static TrackFolderAnalysis getFolderAnalysis(@NonNull TrackFolder trackFolder) {
		TrackFolderAnalysis folderAnalysis = new TrackFolderAnalysis();

		List<TrackItem> items = trackFolder.getFlattenedTrackItems();
		for (TrackItem trackItem : items) {
			GPXTrackAnalysis analysis = getTrackAnalysis(trackItem);
			if (analysis != null) {
				folderAnalysis.totalDistance += analysis.totalDistance;
				folderAnalysis.diffElevationUp += analysis.diffElevationUp;
				folderAnalysis.diffElevationDown += analysis.diffElevationDown;

				File file = trackItem.getFile();
				if (file != null && file.exists()) {
					folderAnalysis.fileSize += file.length();
				}
				if (analysis.isTimeSpecified()) {
					folderAnalysis.duration += analysis.timeSpan / 1000.0f;
				}
			}
		}
		folderAnalysis.tracksCount = items.size();
		return folderAnalysis;
	}

	@Nullable
	private static GPXTrackAnalysis getTrackAnalysis(@NonNull TrackItem trackItem) {
		GpxDataItem gpxDataItem = trackItem.getDataItem();
		if (gpxDataItem != null) {
			return gpxDataItem.getAnalysis();
		}
		return null;
	}
}
