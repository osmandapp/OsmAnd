package net.osmand.plus.track.data;

import static net.osmand.plus.track.helpers.GPXDatabase.*;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.gpx.GPXTrackAnalysis;
import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackFolder implements TracksGroup {

	private final File dirFile;
	private final TrackFolder parentFolder;
	private final List<TrackItem> trackItems = new ArrayList<>();
	private final List<TrackFolder> subFolders = new ArrayList<>();


	public TrackFolder(@NonNull File dirFile, @Nullable TrackFolder parentFolder) {
		this.dirFile = dirFile;
		this.parentFolder = parentFolder;
	}

	@NonNull
	public File getDirFile() {
		return dirFile;
	}

	@NonNull
	public String getDirName() {
		return dirFile.getName();
	}

	@Nullable
	public TrackFolder getParentFolder() {
		return parentFolder;
	}

	@NonNull
	public List<TrackFolder> getSubFolders() {
		return subFolders;
	}

	@NonNull
	@Override
	public List<TrackItem> getTrackItems() {
		return trackItems;
	}

	public void addFolder(@NonNull TrackFolder folder) {
		subFolders.add(folder);
	}

	public void addTrackItem(@NonNull TrackItem trackItem) {
		trackItems.add(trackItem);
	}

	@ColorInt
	public int getColor() {
		return Algorithms.parseColor("#727272");
	}

	public int getTotalTracksCount() {
		return getFlattenedTrackItems().size();
	}

	@NonNull
	public FolderStats getFolderStats() {
		List<TrackItem> flattenedTrackItems = getFlattenedTrackItems();
		int tracksCount = flattenedTrackItems.size();
		float totalDistance = 0f;
		int duration = 0;
		long fileSize = 0;
		double diffElevationUp = 0.0;
		double diffElevationDown = 0.0;
		for (TrackItem trackItem : flattenedTrackItems) {
			GPXTrackAnalysis analysis = getTrackAnalysis(trackItem);
			if (analysis != null) {
				totalDistance += analysis.totalDistance;
				diffElevationUp += analysis.diffElevationUp;
				diffElevationDown += analysis.diffElevationDown;
				File file = trackItem.getFile();
				if (file != null && file.exists()) {
					fileSize += trackItem.getFile().length();
				}
				if (analysis.isTimeSpecified()) {
					duration += analysis.timeSpan / 1000.0f;
				}
			}
		}
		return new FolderStats(tracksCount, totalDistance, duration, fileSize, diffElevationUp, diffElevationDown);
	}

	@Nullable
	public GPXTrackAnalysis getTrackAnalysis(TrackItem trackItem) {
		GpxDataItem gpxDataItem = trackItem.getDataItem();
		if (gpxDataItem != null) {
			return gpxDataItem.getAnalysis();
		}
		return null;
	}

	@NonNull
	public List<TrackItem> getFlattenedTrackItems() {
		List<TrackItem> items = new ArrayList<>(trackItems);
		for (TrackFolder folder : subFolders) {
			items.addAll(folder.getFlattenedTrackItems());
		}
		return items;
	}

	@NonNull
	public List<TrackFolder> getFlattenedSubFolders() {
		List<TrackFolder> folders = new ArrayList<>(subFolders);
		for (TrackFolder folder : subFolders) {
			folders.addAll(folder.getFlattenedSubFolders());
		}
		return folders;
	}

	public long getLastModified() {
		long lastUpdateTime = 0;
		for (TrackFolder folder : subFolders) {
			long folderLastUpdate = folder.getLastModified();
			lastUpdateTime = Math.max(lastUpdateTime, folderLastUpdate);
		}
		for (TrackItem item : trackItems) {
			long fileLastUpdate = item.getLastModified();
			lastUpdateTime = Math.max(lastUpdateTime, fileLastUpdate);
		}
		return lastUpdateTime;
	}

	@NonNull
	@Override
	public String getName(@NonNull Context context) {
		return GpxUiHelper.getFolderName(context, dirFile, false);
	}

	@Override
	public int hashCode() {
		return dirFile.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		TrackFolder trackFolder = (TrackFolder) obj;
		return Algorithms.objectEquals(trackFolder.dirFile, dirFile);
	}

	@NonNull
	@Override
	public String toString() {
		return dirFile.getAbsolutePath();
	}

	public static class FolderStats {
		public int tracksCount;
		public float totalDistance;
		public int duration;
		public long fileSize;
		public double diffElevationUp;
		public double diffElevationDown;

		public FolderStats(int tracksCount, float totalDistance, int duration, long fileSize, double diffElevationUp, double diffElevationDown) {
			this.tracksCount = tracksCount;
			this.totalDistance = totalDistance;
			this.duration = duration;
			this.fileSize = fileSize;
			this.diffElevationUp = diffElevationUp;
			this.diffElevationDown = diffElevationDown;
		}
	}
}
