package net.osmand.plus.profiles;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;

public class NavigationIconsPreviousNamesMapper {

	private final static Map<String, LocationIcon> navigationIconNames = new HashMap<>();

	static {
		navigationIconNames.put("DEFAULT", LocationIcon.MOVEMENT_DEFAULT);
		navigationIconNames.put("NAUTICAL", LocationIcon.MOVEMENT_NAUTICAL);
		navigationIconNames.put("CAR", LocationIcon.MOVEMENT_CAR);
	}

	@NonNull
	public static String getActualNavigationIconName(@NonNull String name) {
		LocationIcon newIcon = findNavigationIconByPreviousName(name);
		return newIcon != null ? newIcon.name() : name;
	}

	@Nullable
	public static LocationIcon findNavigationIconByPreviousName(@NonNull String name) {
		return navigationIconNames.get(name);
	}

}
