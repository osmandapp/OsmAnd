package net.osmand.shared.palette.domain

import net.osmand.shared.gpx.filters.MeasureUnitType
import net.osmand.shared.units.LengthUnits
import net.osmand.shared.units.MeasurementUnit
import net.osmand.shared.units.NoUnit
import net.osmand.shared.units.SpeedUnits

enum class PaletteCategory(
	val key: String,                      // Internal ID & legacy key support
	val displayName: String,              // TODO: don't use predefined
	val measureUnitType: MeasureUnitType, // Logic for formatting values in UI
	val baseUnit: MeasurementUnit<*>,     // The unit in which raw data/predefined values are stored
	val predefinedValues: List<Float>,    // Suggested values for new Fixed Range palettes (in baseUnit)
	val isEditable: Boolean = false
) {
	// --- Track / Route Data (Prefix: route_*) ---

	SPEED(
		key = "speed",
		displayName = "Speed",
		measureUnitType = MeasureUnitType.SPEED,
		baseUnit = SpeedUnits.METERS_PER_SECOND,
		predefinedValues = listOf(0f, 13.88f /*50kmh*/, 25.0f /*90kmh*/),
		isEditable = true
	),

	MAX_SPEED(
		key = "maxspeed",
		displayName = "Max Speed",
		measureUnitType = MeasureUnitType.SPEED,
		baseUnit = SpeedUnits.METERS_PER_SECOND,
		predefinedValues = listOf(0f, 13.88f, 25.0f)
	),

	ALTITUDE(
		key = "elevation", // Legacy key for routes,
		displayName = "Altitude",
		measureUnitType = MeasureUnitType.ALTITUDE,
		baseUnit = LengthUnits.METERS,
		predefinedValues = listOf(0f, 500f, 1000f, 2000f),
		isEditable = true
	),

	SLOPE(
		key = "slope", // Legacy key for routes
		displayName = "Slope",
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 10f, 20f)
	),

	// --- Terrain / Map Data ---

	TERRAIN_ALTITUDE(
		key = "HEIGHT", // Legacy key for TerrainMode
		displayName = "Terrain height",
		measureUnitType = MeasureUnitType.ALTITUDE,
		baseUnit = LengthUnits.METERS,
		predefinedValues = listOf(0f, 1000f, 2000f, 4000f)
	),

	TERRAIN_SLOPE(
		key = "SLOPE", // Legacy key for TerrainMode
		displayName = "Terrain slope",
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 15f, 30f, 45f)
	),

	TERRAIN_HILLSHADE(
		key = "HILLSHADE",
		displayName = "Terrain hillshade",
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 255f)
	),

	// --- User solid color palettes ---

	SOLID_PALETTE(
		key = "user_palette",
		displayName = "User palette",
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 100f),
		isEditable = true
	),

	// --- Weather (Prefix: weather_*) ---

	WEATHER(
		key = "weather",
		displayName = "Weather",
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 100f)
	);

	fun isTerrainRelated() = this in setOf(TERRAIN_ALTITUDE, TERRAIN_SLOPE, TERRAIN_HILLSHADE)

	companion object {
		private val map = entries.associateBy(PaletteCategory::key)

		fun fromKey(key: String) = map[key]
	}
}