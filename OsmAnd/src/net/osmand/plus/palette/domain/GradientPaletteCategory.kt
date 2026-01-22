package net.osmand.plus.palette.domain

import net.osmand.shared.gpx.filters.MeasureUnitType
import net.osmand.shared.units.LengthUnits
import net.osmand.shared.units.MeasurementUnit
import net.osmand.shared.units.NoUnit
import net.osmand.shared.units.SpeedUnits

/**
 * Defines the context for a gradient palette.
 * Acts as a single source of truth for units, file naming, and default values.
 * Mapped to file prefixes defined in OsmAnd documentation and file system.
 */
// TODO: improve
enum class GradientPaletteCategory(
	val key: String,                      // Internal ID & legacy key support
	val filePrefix: String,               // Exact file prefix on disk
	val measureUnitType: MeasureUnitType, // Logic for formatting values in UI
	val baseUnit: MeasurementUnit<*>,     // The unit in which raw data/predefined values are stored
	val predefinedValues: List<Float>     // Suggested values for new Fixed Range palettes (in baseUnit)
) {
	// --- Track / Route Data (Prefix: route_*) ---

	SPEED(
		key = "speed",
		filePrefix = "route_speed_",
		measureUnitType = MeasureUnitType.SPEED,
		baseUnit = SpeedUnits.METERS_PER_SECOND,
		predefinedValues = listOf(0f, 13.88f /*50kmh*/, 25.0f /*90kmh*/)
	),

	MAX_SPEED(
		key = "maxspeed",
		filePrefix = "route_maxspeed_",
		measureUnitType = MeasureUnitType.SPEED,
		baseUnit = SpeedUnits.METERS_PER_SECOND,
		predefinedValues = listOf(0f, 13.88f, 25.0f)
	),

	ALTITUDE(
		key = "elevation", // Legacy key for routes
		filePrefix = "route_elevation_",
		measureUnitType = MeasureUnitType.ALTITUDE,
		baseUnit = LengthUnits.METERS,
		predefinedValues = listOf(0f, 500f, 1000f, 2000f)
	),

	SLOPE(
		key = "slope", // Legacy key for routes
		filePrefix = "route_slope_",
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 10f, 20f)
	),

	// --- Terrain / Map Data (Prefix: Specific per type) ---

	TERRAIN_ALTITUDE(
		key = "HEIGHT", // Legacy key for TerrainMode
		filePrefix = "height_altitude_", // Verified by screenshot
		measureUnitType = MeasureUnitType.ALTITUDE,
		baseUnit = LengthUnits.METERS,
		predefinedValues = listOf(0f, 1000f, 2000f, 4000f)
	),

	TERRAIN_SLOPE(
		key = "SLOPE", // Legacy key for TerrainMode
		filePrefix = "slope_", // Verified by screenshot (slope_avalanche.txt)
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 15f, 30f, 45f)
	),

	HILLSHADE(
		key = "HILLSHADE",
		filePrefix = "hillshade_color_", // Verified by screenshot
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 255f)
	),

	HILLSHADE_MAIN(
		key = "HILLSHADE_MAIN",
		filePrefix = "hillshade_main_", // Verified by screenshot
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 255f)
	),

	// --- Weather (Prefix: weather_*) ---

	WEATHER(
		key = "weather",
		filePrefix = "weather_", // Covers weather_cloud.txt, weather_precip.txt, etc.
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 100f)
	),

	// --- Fallback ---

	UNKNOWN(
		key = "unknown",
		filePrefix = "unknown_",
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 100f)
	);

	companion object {
		private val map = entries.associateBy(GradientPaletteCategory::key)

		/**
		 * Returns the category by its legacy key.
		 * Strict matching (case-sensitive) as defined in legacy code.
		 */
		fun fromKey(key: String): GradientPaletteCategory {
			return map[key] ?: UNKNOWN
		}

		/**
		 * Determines category by filename prefix.
		 * Vital for parsing files from the directory (Reverse engineering).
		 */
		fun fromFileName(fileName: String): GradientPaletteCategory {
			// Find the longest matching prefix to avoid conflicts (e.g. slope_ vs route_slope_)
			return entries
				.filter { it != UNKNOWN && fileName.startsWith(it.filePrefix) }
				.maxByOrNull { it.filePrefix.length }
				?: UNKNOWN
		}
	}
}