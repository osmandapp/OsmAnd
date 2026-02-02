package net.osmand.shared.palette.domain.category

import net.osmand.shared.gpx.filters.MeasureUnitType
import net.osmand.shared.palette.domain.GradientRangeType
import net.osmand.shared.palette.domain.filetype.GradientFileType
import net.osmand.shared.units.LengthUnits
import net.osmand.shared.units.MeasurementUnit
import net.osmand.shared.units.NoUnit
import net.osmand.shared.units.SpeedUnits

enum class GradientPaletteCategory(
	override val id: String,
	override val nameResId: String,
	val measureUnitType: MeasureUnitType,     // Logic for formatting values in UI
	val measurementUnit: MeasurementUnit<*>,  // The unit in which raw data/predefined values are stored
	val predefinedValues: List<Float>,        // Suggested values for new Fixed Range palettes (in baseUnit)
	override val editable: Boolean = false
): PaletteCategory {

	// --- Track / Route Data (Prefix: route_*) ---

	SPEED(
		id = "speed",
		nameResId = "shared_string_speed",
		measureUnitType = MeasureUnitType.SPEED,
		measurementUnit = SpeedUnits.KILOMETERS_PER_HOUR,
		predefinedValues = listOf(0f, 13.88f /*50kmh*/, 25.0f /*90kmh*/),
		editable = true
	),

	MAX_SPEED(
		id = "maxspeed",
		nameResId = "shared_string_max_speed",
		measureUnitType = MeasureUnitType.SPEED,
		measurementUnit = SpeedUnits.KILOMETERS_PER_HOUR,
		predefinedValues = listOf(0f, 13.88f, 25.0f)
	),

	ALTITUDE(
		id = "elevation", // Legacy key for routes,
		nameResId = "altitude",
		measureUnitType = MeasureUnitType.ALTITUDE,
		measurementUnit = LengthUnits.METERS,
		predefinedValues = listOf(0f, 500f, 1000f, 2000f),
		editable = true
	),

	SLOPE(
		id = "slope", // Legacy key for routes
		nameResId = "shared_string_slope",
		measureUnitType = MeasureUnitType.NONE,
		measurementUnit = NoUnit,
		predefinedValues = listOf(0f, 10f, 20f)
	),

	// --- Terrain / Map Data ---

	TERRAIN_ALTITUDE(
		id = "HEIGHT", // Legacy key for TerrainMode
		nameResId = "shared_string_height",
		measureUnitType = MeasureUnitType.ALTITUDE,
		measurementUnit = LengthUnits.METERS,
		predefinedValues = listOf(0f, 1000f, 2000f, 4000f)
	),

	TERRAIN_SLOPE(
		id = "SLOPE", // Legacy key for TerrainMode
		nameResId = "shared_string_slope",
		measureUnitType = MeasureUnitType.NONE,
		measurementUnit = NoUnit,
		predefinedValues = listOf(0f, 15f, 30f, 45f)
	),

	TERRAIN_HILLSHADE(
		id = "HILLSHADE",
		nameResId = "shared_string_hillshade",
		measureUnitType = MeasureUnitType.NONE,
		measurementUnit = NoUnit,
		predefinedValues = listOf(0f, 255f)
	),

	// --- Weather (Prefix: weather_*) ---

	WEATHER(
		id = "weather",
		nameResId = "shared_string_weather",
		measureUnitType = MeasureUnitType.NONE,
		measurementUnit = NoUnit,
		predefinedValues = listOf(0f, 100f)
	);

	fun isTerrainRelated() = this in setOf(TERRAIN_ALTITUDE, TERRAIN_SLOPE, TERRAIN_HILLSHADE)

	fun isSupportDifferentRangeTypes(): Boolean {
		return GradientFileType.valuesOf(this)
			.map { it.rangeType }
			.distinct()
			.size > 1
	}

	fun getFileType(): GradientFileType {
		return GradientFileType.valuesOf(this)[0]
	}

	fun getFileType(rangeType: GradientRangeType): GradientFileType? {
		return GradientFileType.valuesOf(this)
			.find { it.rangeType == rangeType }
	}

	fun getSupportedRangeTypes(): List<GradientRangeType> {
		return GradientFileType.valuesOf(this)
			.map { it.rangeType }
			.distinct()
			.sortedBy { it.ordinal }
	}

	companion object {
		private val map = entries.associateBy(GradientPaletteCategory::id)

		fun fromKey(key: String) = map[key]
	}
}