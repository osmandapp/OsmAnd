package net.osmand.plus.configmap.tracks;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.settings.enums.TracksSortMode;
import net.osmand.plus.track.helpers.GpxUiHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TrackTab {

	public final TrackTabType type;
	public final List<Object> items = new ArrayList<>();

	@Nullable
	public final File directory;

	private TracksSortMode sortMode = TracksSortMode.getDefaultSortMode();

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
			return GpxUiHelper.getFolderName(context, directory, includeParentDir);
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
			if (object instanceof TrackItem) {
				trackItems.add((TrackItem) object);
			}
		}
		return trackItems;
	}

	@NonNull
	@Override
	public String toString() {
		return "TrackTab{name=" + getTypeName() + "}";
	}
}
