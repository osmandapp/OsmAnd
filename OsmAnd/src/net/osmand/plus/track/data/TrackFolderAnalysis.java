package net.osmand.plus.track.data;

import androidx.annotation.NonNull;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.track.helpers.GpxDataItem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackFolderAnalysis {

	public int tracksCount;
	public float totalDistance;
	public int timeSpan;
	public long fileSize;
	public double diffElevationUp;
	public double diffElevationDown;

	public TrackFolderAnalysis(@NonNull TracksGroup folder) {
		prepareInformation(folder);
	}

	private void prepareInformation(@NonNull TracksGroup folder) {
		List<TrackItem> items = new ArrayList<>();
		if (folder instanceof TrackFolder) {
			items.addAll(((TrackFolder) folder).getFlattenedTrackItems());
		} else {
			items.addAll(folder.getTrackItems());
		}
		for (TrackItem trackItem : items) {
			GpxDataItem dataItem = trackItem.getDataItem();
			GPXTrackAnalysis analysis = dataItem != null ? dataItem.getAnalysis() : null;
			if (analysis != null) {
				totalDistance += analysis.getTotalDistance();
				diffElevationUp += analysis.getDiffElevationUp();
				diffElevationDown += analysis.getDiffElevationDown();

				File file = trackItem.getFile();
				if (file != null && file.exists()) {
					fileSize += file.length();
				}
				if (analysis.isTimeSpecified()) {
					timeSpan += analysis.getDurationInMs() / 1000.0f;
				}
			}
		}
		tracksCount = items.size();
	}
}
