package net.osmand.plus.profiles.migration;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.plus.profiles.LocationIcon;
import net.osmand.plus.settings.backend.ApplicationMode;

import java.util.Objects;

public class MergeLocationAndNavigationIconsAlgorithm {

	private static final String MOVEMENT_PREFIX = "MOVEMENT_";

	private void execute() {
		for (ApplicationMode appMode : ApplicationMode.allPossibleValues()) {
			String navIconName = appMode.getNavigationIcon();
			String newNavIconName = getActualNavigationIconName(navIconName);
			if (!Objects.equals(navIconName, newNavIconName)) {
				appMode.setNavigationIcon(newNavIconName);
			}
		}
	}

	@NonNull
	private String getActualNavigationIconName(@NonNull String name) {
		LocationIcon newIcon = findNavigationIconByPreviousName(name);
		return newIcon != null ? newIcon.name() : name;
	}

	@Nullable
	private LocationIcon findNavigationIconByPreviousName(@NonNull String name) {
		try {
			return LocationIcon.valueOf(MOVEMENT_PREFIX + name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	public static void doMigration() {
		new MergeLocationAndNavigationIconsAlgorithm().execute();
	}
}
