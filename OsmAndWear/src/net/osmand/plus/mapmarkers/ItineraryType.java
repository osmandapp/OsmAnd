package net.osmand.plus.mapmarkers;

import androidx.annotation.NonNull;

public enum ItineraryType {
	MARKERS("markers", -1),
	FAVOURITES("favourites", 0),
	TRACK("track", 1);

	private final int typeId;
	private final String typeName;

	ItineraryType(@NonNull String typeName, int typeId) {
		this.typeName = typeName;
		this.typeId = typeId;
	}

	public int getTypeId() {
		return typeId;
	}

	public String getTypeName() {
		return typeName;
	}

	public static ItineraryType findTypeForId(int typeId) {
		for (ItineraryType type : values()) {
			if (type.getTypeId() == typeId) {
				return type;
			}
		}
		return MARKERS;
	}

	public static ItineraryType findTypeForName(String typeName) {
		for (ItineraryType type : values()) {
			if (type.getTypeName().equalsIgnoreCase(typeName)) {
				return type;
			}
		}
		return MARKERS;
	}
}
