package net.osmand.plus.track.data;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.track.helpers.GPXDatabase.GpxDataItem;

import java.io.File;
import java.util.List;

public class TrackFolderAnalysis {

	public int tracksCount;
	public float totalDistance;
	public int timeSpan;
	public long fileSize;
	public double diffElevationUp;
	public double diffElevationDown;

	public TrackFolderAnalysis(@NonNull TrackFolder folder) {
		prepareInformation(folder);
	}

	private void prepareInformation(@NonNull TrackFolder folder) {
		List<TrackItem> items = folder.getFlattenedTrackItems();
		for (TrackItem trackItem : items) {
			GpxDataItem dataItem = trackItem.getDataItem();
			GPXTrackAnalysis analysis = dataItem != null ? dataItem.getAnalysis() : null;
			if (analysis != null) {
				totalDistance += analysis.totalDistance;
				diffElevationUp += analysis.diffElevationUp;
				diffElevationDown += analysis.diffElevationDown;

				File file = trackItem.getFile();
				if (file != null && file.exists()) {
					fileSize += file.length();
				}
				if (analysis.isTimeSpecified()) {
					timeSpan += analysis.timeSpan / 1000.0f;
				}
			}
		}
		tracksCount = items.size();
	}
}
