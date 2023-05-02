package net.osmand.plus.track.data;

import androidx.annotation.NonNull;

import net.osmand.plus.configmap.tracks.TrackItem;

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

	@NonNull
	public List<TrackItem> getFlattenedTrackItems() {
		List<TrackItem> items = new ArrayList<>(trackItems);
		for (TrackFolder folder : folders) {
			items.addAll(folder.trackItems);
		}
		return items;
	}
}
