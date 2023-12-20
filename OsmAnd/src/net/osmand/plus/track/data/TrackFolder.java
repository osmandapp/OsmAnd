package net.osmand.plus.track.data;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.configmap.tracks.TrackItem;
import net.osmand.plus.track.ComparableTracksGroup;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TrackFolder implements TracksGroup, ComparableTracksGroup {

	private File dirFile;
	private final TrackFolder parentFolder;
	private final List<TrackItem> trackItems = new ArrayList<>();
	private final List<TrackFolder> subFolders = new ArrayList<>();

	private List<TrackItem> flattenedTrackItems;
	private List<TrackFolder> flattenedSubFolders;

	private TrackFolderAnalysis folderAnalysis;
	private long lastModified = -1;

	public TrackFolder(@NonNull File dirFile, @Nullable TrackFolder parentFolder) {
		this.dirFile = dirFile;
		this.parentFolder = parentFolder;
	}

	@NonNull
	@Override
	public String getName(@NonNull Context context) {
		return GpxUiHelper.getFolderName(context, dirFile, false);
	}

	@NonNull
	public File getDirFile() {
		return dirFile;
	}

	public void setDirFile(@NonNull File dirFile) {
		this.dirFile = dirFile;
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
		return Collections.unmodifiableList(subFolders);
	}

	@NonNull
	@Override
	public List<TrackItem> getTrackItems() {
		return Collections.unmodifiableList(trackItems);
	}

	public void addSubFolder(@NonNull TrackFolder folder) {
		subFolders.add(folder);
	}

	public void addTrackItem(@NonNull TrackItem trackItem) {
		trackItems.add(trackItem);
	}

	public boolean isEmpty() {
		return Algorithms.isEmpty(getTrackItems()) && Algorithms.isEmpty(getSubFolders());
	}

	@ColorInt
	public int getColor() {
		return Algorithms.parseColor("#727272");
	}

	public int getTotalTracksCount() {
		return getFlattenedTrackItems().size();
	}

	@NonNull
	public List<TrackItem> getFlattenedTrackItems() {
		if (flattenedTrackItems == null) {
			flattenedTrackItems = new ArrayList<>(getTrackItems());
			for (TrackFolder folder : getSubFolders()) {
				flattenedTrackItems.addAll(folder.getFlattenedTrackItems());
			}
		}
		return flattenedTrackItems;
	}

	@NonNull
	public List<TrackFolder> getFlattenedSubFolders() {
		if (flattenedSubFolders == null) {
			flattenedSubFolders = new ArrayList<>(getSubFolders());
			for (TrackFolder folder : getSubFolders()) {
				flattenedSubFolders.addAll(folder.getFlattenedSubFolders());
			}
		}
		return flattenedSubFolders;
	}

	@NonNull
	public TrackFolderAnalysis getFolderAnalysis() {
		if (folderAnalysis == null) {
			folderAnalysis = new TrackFolderAnalysis(this);
		}
		return folderAnalysis;
	}

	public long getLastModified() {
		if (lastModified < 0) {
			lastModified = dirFile.lastModified();
			for (TrackFolder folder : getSubFolders()) {
				lastModified = Math.max(lastModified, folder.getLastModified());
			}
			for (TrackItem item : getTrackItems()) {
				lastModified = Math.max(lastModified, item.getLastModified());
			}
		}
		return lastModified;
	}

	public void clearData() {
		resetCashedData();
		trackItems.clear();
		subFolders.clear();
	}

	public void resetCashedData() {
		lastModified = -1;
		flattenedTrackItems = null;
		flattenedSubFolders = null;
		folderAnalysis = null;
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

	@Override
	public long lastModified() {
		return getDirFile().lastModified();
	}
}
