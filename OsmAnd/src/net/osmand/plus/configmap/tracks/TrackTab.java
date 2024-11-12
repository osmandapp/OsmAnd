package net.osmand.plus.configmap.tracks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.shared.gpx.data.ComparableTracksGroup;
import net.osmand.shared.gpx.data.SmartFolder;
import net.osmand.shared.gpx.data.TrackFolder;
import net.osmand.plus.track.helpers.GpxUiHelper;
import net.osmand.shared.gpx.TrackItem;
import net.osmand.shared.gpx.filters.TrackFolderAnalysis;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackTab implements ComparableTracksGroup {

	public static final String SMART_FOLDER_TAB_NAME_PREFIX = "SMART_FOLDER___";

	@NonNull
	public final TrackTabType type;
	public final List<Object> items = new ArrayList<>();

	@Nullable
	public final File directory;
	@Nullable
	public final SmartFolder smartFolder;

	private TracksSortMode sortMode = TracksSortMode.getDefaultSortMode();

	public TrackTab(@NonNull File directory) {
		this.directory = directory;
		this.smartFolder = null;
		this.type = TrackTabType.FOLDER;
	}

	public TrackTab(@NonNull SmartFolder smartFolder) {
		this.directory = null;
		this.smartFolder = smartFolder;
		this.type = TrackTabType.SMART_FOLDER;
	}

	public TrackTab(@NonNull TrackTabType type) {
		this.directory = null;
		this.smartFolder = null;
		this.type = type;
	}

	@Nullable
	@Override
	public TrackFolderAnalysis getFolderAnalysis() {
		// Analysis should be prepared and accessible here.
		// It's needed for proper sorting order on UI.
		return null;
	}

	@NonNull
	public TracksSortMode getSortMode() {
		return sortMode;
	}

	public void setSortMode(@NonNull TracksSortMode sortMode) {
		this.sortMode = sortMode;
	}

	@NonNull
	public String getName(@NonNull Context context) {
		return getName(context, false);
	}

	@NonNull
	public String getName(@NonNull Context context, boolean includeParentDir) {
		return type.titleId != -1 ? context.getString(type.titleId) : getDirName(includeParentDir);
	}

	@NonNull
	@Override
	public String getDirName() {
		return getDirName(false);
	}

	@NonNull
	public String getDirName(boolean includeParentName) {
		if (directory != null) {
			return GpxUiHelper.getFolderName(directory, includeParentName);
		}
		if (smartFolder != null) {
			return smartFolder.getDirName();
		}
		return "";
	}

	@NonNull
	public String getTypeName() {
		return switch (type) {
			case FOLDER -> directory != null ? directory.getName() : "";
			case SMART_FOLDER -> smartFolder != null ? SMART_FOLDER_TAB_NAME_PREFIX + smartFolder.getDirName() : "";
			default -> type.name();
		};
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

	@NonNull
	@Override
	public String toString() {
		return "TrackTab{name=" + getTypeName() + "}";
	}
}
