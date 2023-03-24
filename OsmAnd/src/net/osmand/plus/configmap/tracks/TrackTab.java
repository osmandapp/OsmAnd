package net.osmand.plus.configmap.tracks;

import static net.osmand.IndexConstants.GPX_IMPORT_DIR;
import static net.osmand.IndexConstants.GPX_INDEX_DIR;
import static net.osmand.IndexConstants.GPX_RECORDED_INDEX_DIR;
import static net.osmand.plus.configmap.tracks.TracksSortMode.LAST_MODIFIED;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.R;
import net.osmand.util.Algorithms;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackTab {

	public final TrackTabType type;
	public final List<Object> items = new ArrayList<>();

	@Nullable
	public final File directory;

	private TracksSortMode sortMode = LAST_MODIFIED;

	public TrackTab(@NonNull File directory) {
		this.directory = directory;
		this.type = TrackTabType.FOLDER;
	}

	public TrackTab(@NonNull TrackTabType type) {
		this.directory = null;
		this.type = type;
	}

	@NonNull
	public TracksSortMode getSortMode() {
		return sortMode;
	}

	public void setSortMode(@NonNull TracksSortMode sortMode) {
		this.sortMode = sortMode;
	}

	@NonNull
	public String getName(@NonNull Context context, boolean includeParentDir) {
		if (type.titleId != -1) {
			return context.getString(type.titleId);
		}
		if (directory != null) {
			String name = directory.getName();
			if (GPX_INDEX_DIR.equals(name + File.separator)) {
				return context.getString(R.string.shared_string_tracks);
			}
			String dirPath = directory.getPath() + File.separator;
			if (dirPath.endsWith(GPX_IMPORT_DIR) || dirPath.endsWith(GPX_RECORDED_INDEX_DIR)) {
				return Algorithms.capitalizeFirstLetter(name);
			}
			if (includeParentDir) {
				File parent = directory.getParentFile();
				String parentName = parent != null ? parent.getName() : "";
				if (!Algorithms.isEmpty(parentName) && !GPX_INDEX_DIR.equals(parentName + File.separator)) {
					name = parentName + File.separator + name;
				}
				return Algorithms.capitalizeFirstLetter(name.toLowerCase());
			}
			return name;
		}
		return "";
	}

	@NonNull
	public String getTypeName() {
		if (type != TrackTabType.FOLDER) {
			return type.name();
		}
		return directory != null ? directory.getName() : "";
	}

	@NonNull
	public List<TrackItem> getTrackItems() {
		List<TrackItem> trackItems = new ArrayList<>();
		for (Object object : items) {
			if (object instanceof TrackItem)
				trackItems.add((TrackItem) object);
		}
		return trackItems;
	}

	@NonNull
	@Override
	public String toString() {
		return "TrackTab{name=" + getTypeName() + "}";
	}
}
