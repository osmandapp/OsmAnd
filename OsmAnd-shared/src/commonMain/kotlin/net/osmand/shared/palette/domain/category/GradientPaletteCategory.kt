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
	override val displayName: String,
	val measureUnitType: MeasureUnitType, // Logic for formatting values in UI
	val baseUnit: MeasurementUnit<*>,     // The unit in which raw data/predefined values are stored
	val predefinedValues: List<Float>,   // Suggested values for new Fixed Range palettes (in baseUnit)
	override val editable: Boolean = false
): PaletteCategory {

	// --- Track / Route Data (Prefix: route_*) ---

	SPEED(
		id = "speed",
		displayName = "Speed",
		measureUnitType = MeasureUnitType.SPEED,
		baseUnit = SpeedUnits.METERS_PER_SECOND,
		predefinedValues = listOf(0f, 13.88f /*50kmh*/, 25.0f /*90kmh*/),
		editable = true
	),

	MAX_SPEED(
		id = "maxspeed",
		displayName = "Max Speed",
		measureUnitType = MeasureUnitType.SPEED,
		baseUnit = SpeedUnits.METERS_PER_SECOND,
		predefinedValues = listOf(0f, 13.88f, 25.0f)
	),

	ALTITUDE(
		id = "elevation", // Legacy key for routes,
		displayName = "Altitude",
		measureUnitType = MeasureUnitType.ALTITUDE,
		baseUnit = LengthUnits.METERS,
		predefinedValues = listOf(0f, 500f, 1000f, 2000f),
		editable = true
	),

	SLOPE(
		id = "slope", // Legacy key for routes
		displayName = "Slope",
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 10f, 20f)
	),

	// --- Terrain / Map Data ---

	TERRAIN_ALTITUDE(
		id = "HEIGHT", // Legacy key for TerrainMode
		displayName = "Terrain height",
		measureUnitType = MeasureUnitType.ALTITUDE,
		baseUnit = LengthUnits.METERS,
		predefinedValues = listOf(0f, 1000f, 2000f, 4000f)
	),

	TERRAIN_SLOPE(
		id = "SLOPE", // Legacy key for TerrainMode
		displayName = "Terrain slope",
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 15f, 30f, 45f)
	),

	TERRAIN_HILLSHADE(
		id = "HILLSHADE",
		displayName = "Terrain hillshade",
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
		predefinedValues = listOf(0f, 255f)
	),

	// --- Weather (Prefix: weather_*) ---

	WEATHER(
		id = "weather",
		displayName = "Weather",
		measureUnitType = MeasureUnitType.NONE,
		baseUnit = NoUnit,
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