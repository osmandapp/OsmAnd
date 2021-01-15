package net.osmand.plus.onlinerouting.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.util.Algorithms;

public enum EngineType {
	GRAPHHOPPER("Graphhopper"),
	OSRM("OSRM"),
	ORS("Openroute Service");

	private final String title;

	EngineType(String title) {
		this.title = title;
	}

	public String getTitle() {
		return title;
	}

	@NonNull
	public static EngineType getTypeByName(@Nullable String name) {
		if (!Algorithms.isEmpty(name)) {
			for (EngineType type : values()) {
				if (type.name().equals(name)) {
					return type;
				}
			}
		}
		return values()[0];
	}
}
