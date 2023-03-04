package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.tracks.TracksSortMode.LAST_MODIFIED;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TrackTab {

	public final String name;
	public final TrackTabType type;
	public final List<Object> items = new ArrayList<>();

	private TracksSortMode sortMode = LAST_MODIFIED;

	public TrackTab(@NonNull String name, @NonNull TrackTabType type) {
		this.name = name;
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
	public String getTypeName() {
		if (type == TrackTabType.ON_MAP) {
			return TrackTabType.ON_MAP.name();
		} else if (type == TrackTabType.ALL) {
			return TrackTabType.ALL.name();
		}
		return name;
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
}
