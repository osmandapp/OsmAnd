package net.osmand.plus.settings.enums;

/**
 * Defines the UI context in which a theme is applied.
 */
public enum ThemeUsageContext {
	MAP,        // Used for the map itself and its direct UI components (e.g., widgets, map buttons)
	OVER_MAP,   // Used for UI elements displayed over the visible map (e.g., dialogs, bottom sheets opened on top of map)
	APP;        // Used for screens, dialogs, and bottom sheets opened in contexts where the map is not visible

	public static ThemeUsageContext valueOf(boolean usedOnMap) {
		return usedOnMap ? OVER_MAP : APP;
	}
}
