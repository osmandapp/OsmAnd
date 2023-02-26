package net.osmand.plus.configmap.tracks;

import static net.osmand.plus.configmap.tracks.TracksSortMode.NAME_ASCENDING;

import androidx.annotation.NonNull;

import net.osmand.plus.track.helpers.GPXInfo;

import java.util.ArrayList;
import java.util.List;

public class TrackTab {

	public final String name;
	public final TrackTabType type;
	public final List<Object> items = new ArrayList<>();

	private TracksSortMode sortMode = NAME_ASCENDING;

	public TrackTab(@NonNull String name, @NonNull TrackTabType type) {
		this.name = name;
		this.type = type;
	}

	@NonNull
	public List<GPXInfo> getGPXInfos() {
		List<GPXInfo> gpxInfos = new ArrayList<>();
		for (Object object : items) {
			if (object instanceof GPXInfo)
				gpxInfos.add((GPXInfo) object);
		}
		return gpxInfos;
	}

	@NonNull
	public TracksSortMode getSortMode() {
		return sortMode;
	}

	public void setSortMode(@NonNull TracksSortMode sortMode) {
		this.sortMode = sortMode;
	}
}
