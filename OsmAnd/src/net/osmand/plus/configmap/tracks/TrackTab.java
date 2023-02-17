package net.osmand.plus.configmap.tracks;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class TrackTab {

	public final String name;
	public final TrackTabType type;
	public final List<Object> items = new ArrayList<>();

	public TrackTab(@NonNull String name, @NonNull TrackTabType type) {
		this.name = name;
		this.type = type;
	}
}
