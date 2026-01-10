package net.osmand.shared.gpx.enums

/**
 * Defines the context where a specific sort mode can be used.
 */
enum class TracksSortScope {
	TRACKS,             // Regular tracks and folders list
	ORGANIZED_BY_NAME,  // Display groups organized by text (e.g., city, activity)
	ORGANIZED_BY_VALUE  // Display groups organized by numeric values (e.g., distance, speed)
}