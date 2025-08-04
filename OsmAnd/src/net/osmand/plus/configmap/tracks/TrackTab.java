package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.tracks.TrackTabType.FOLDER;
import static net.osmand.plus.configmap.tracks.TrackTabType.SMART_FOLDER;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.gpx.data.ComparableTracksGroup;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.shared.gpx.data.TracksGroup;
import net.osmand.shared.gpx.filters.TrackFolderAnalysis;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackTab implements TracksGroup, ComparableTracksGroup {

	@NonNull
	public final TrackTabType type;
	public final List<Object> items = new ArrayList<>();

	@Nullable
	public final File directory;
	@Nullable
	public final SmartFolder smartFolder;
	public final String initialName;

	private TracksSortMode sortMode = TracksSortMode.getDefaultSortMode(null);
	private TrackFolderAnalysis analysis = null;

	public TrackTab(@NonNull Context context, @NonNull File directory) {
		this(context, directory, null, FOLDER);
		sortMode = TracksSortMode.getDefaultSortMode(getId());
	}

	public TrackTab(@NonNull Context context, @NonNull SmartFolder smartFolder) {
		this(context, null, smartFolder, SMART_FOLDER);
	}

	public TrackTab(@NonNull Context context, @NonNull TrackTabType type) {
		this(context, null, null, type);
	}

	private TrackTab(@NonNull Context context, @Nullable File directory,
	                 @Nullable SmartFolder smartFolder, @NonNull TrackTabType type) {
		this.directory = directory;
		this.smartFolder = smartFolder;
		this.type = type;
		this.initialName = createInitialName(context);
	}

	@NonNull
	private String createInitialName(@NonNull Context context) {
		if (type.titleId != -1) {
			return context.getString(type.titleId);
		}
		if (directory != null) {
			return GpxUiHelper.getFolderName(context, directory);
		}
		if (smartFolder != null) {
			return smartFolder.getFolderName();
		}
		return "";
	}

	@NonNull
	@Override
	public String getId() {
		return switch (type) {
			case FOLDER -> directory != null ? TrackSortModesHelper.getFolderId(directory.getAbsolutePath()) : "";
			case SMART_FOLDER -> smartFolder != null ? smartFolder.getId() : "";
			default -> type.name();
		};
	}

	@NonNull
	@Override
	public TrackFolderAnalysis getFolderAnalysis() {
		// Note: To avoid excessive calculations that could slow down the UI,
		// analysis is not recalculated when folder or track parameters change.
		// For example after file deletion or moving to another directory.
		if (analysis == null) {
			analysis = smartFolder != null ? smartFolder.getFolderAnalysis() : new TrackFolderAnalysis(this);
		}
		return analysis;
	}

	@NonNull
	public TracksSortMode getSortMode() {
		return sortMode;
	}

	public void setSortMode(@NonNull TracksSortMode sortMode) {
		this.sortMode = sortMode;
	}

	@NonNull
	@Override
	public String getName() {
		return getDirName(false);
	}

	@NonNull
	@Override
	public String getDirName(boolean includingSubdirs) {
		if (directory != null) {
			return GpxUiHelper.getRelativeFolderPath(directory, initialName, includingSubdirs);
		}
		return initialName;
	}

	@Override
	public int getDefaultOrder() {
		return type.ordinal();
	}

	public boolean isBaseFolder() {
		String id = getId();
		return id.isEmpty();
	}

	@NonNull
	public List<TrackItem> getTrackItems() {
		List<TrackItem> trackItems = new ArrayList<>();
		for (Object object : items) {
			if (object instanceof TrackItem) {
				trackItems.add((TrackItem) object);
			}
		}
		return trackItems;
	}

	@NonNull
	public List<TrackFolder> getTrackFolders() {
		List<TrackFolder> trackFolders = new ArrayList<>();
		for (Object object : items) {
			if (object instanceof TrackFolder) {
				trackFolders.add((TrackFolder) object);
			}
		}
		return trackFolders;
	}

	@Override
	public long lastModified() {
		if (directory != null) {
			return directory.lastModified();
		}
		if (smartFolder != null) {
			return smartFolder.lastModified();
		}
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		TrackTab that = (TrackTab) o;
		return Algorithms.stringsEqual(getId(), that.getId());
	}

	@Override
	public int hashCode() {
		return Algorithms.hash(getId());
	}

	@NonNull
	@Override
	public String toString() {
		return "TrackTab{name=" + getId() + "}";
	}
}
