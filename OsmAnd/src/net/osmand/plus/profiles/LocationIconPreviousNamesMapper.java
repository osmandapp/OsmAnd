package net.osmand.plus.profiles;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Map;

public class LocationIconPreviousNamesMapper {

	private final static Map<String, LocationIcon> navigationIconNames = new HashMap<>();

	static {
		navigationIconNames.put("DEFAULT", LocationIcon.MOVEMENT_DEFAULT);
		navigationIconNames.put("NAUTICAL", LocationIcon.MOVEMENT_NAUTICAL);
		navigationIconNames.put("CAR", LocationIcon.MOVEMENT_CAR);
	}

	public static LocationIcon findIconByPreviousName(@NonNull String name,
	                                                  boolean forStaticLocation) {
		return !forStaticLocation ? navigationIconNames.get(name) : null;
	}

}
