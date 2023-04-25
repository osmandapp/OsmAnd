package net.osmand.plus.track.data;

import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;

import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackFolder {

	private final List<TrackFolder> folders = new ArrayList<>();
	private final List<TrackItem> trackItems = new ArrayList<>();

	private final File dirFile;

	public TrackFolder(@NonNull File dirFile) {
		this.dirFile = dirFile;
	}

	@NonNull
	public File getDirFile() {
		return dirFile;
	}

	@NonNull
	public List<TrackFolder> getSubFolders() {
		return folders;
	}

	@NonNull
	public List<TrackItem> getTrackItems() {
		return trackItems;
	}

	public void addFolder(@NonNull TrackFolder folder) {
		folders.add(folder);
	}

	public void addTrackItem(@NonNull TrackItem trackItem) {
		trackItems.add(trackItem);
	}

	@ColorInt
	public int getColor() {
		return Color.GREEN; // todo use real folder color
	}

	@NonNull
	public List<TrackItem> getFlattenedTrackItems() {
		List<TrackItem> trackItems = new ArrayList<>();
		for (TrackFolder folder : folders) {
			trackItems.addAll(folder.getTrackItems());
		}
		return trackItems;
	}

	public int getTotalTracksCount() {
		int total = trackItems.size();
		for (TrackFolder folder : folders) {
			total += folder.getTotalTracksCount();
		}
		return total;
	}

	public long getLastModified() {
		long lastUpdateTime = 0;
		for (TrackFolder folder : folders) {
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
	public String getName() {
		return Algorithms.capitalizeFirstLetter(dirFile.getName());
	}
}
