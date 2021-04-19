package net.osmand.plus.itinerary;

public enum ItineraryType {
	MARKERS(-1),
	FAVOURITES(0),
	TRACK(1),
	POINTS(2);

	private int typeId;

	ItineraryType(int typeId) {
		this.typeId = typeId;
	}

	public int getTypeId() {
		return typeId;
	}

	public static ItineraryType findTypeForId(int typeId) {
		for (ItineraryType type : values()) {
			if (type.getTypeId() == typeId) {
				return type;
			}
		}
		return ItineraryType.POINTS;
	}

	public static ItineraryType findTypeForName(String typeName) {
		for (ItineraryType type : values()) {
			if (type.name().equalsIgnoreCase(typeName)) {
				return type;
			}
		}
		return ItineraryType.POINTS;
	}
}
