package net.osmand.shared.palette.domain.category

import net.osmand.shared.gpx.filters.MeasureUnitType
import net.osmand.shared.palette.domain.GradientRangeType
import net.osmand.shared.palette.domain.filetype.GradientFileType

enum class GradientPaletteCategory(
	override val id: String,
	override val nameResId: String,
	val measureUnitType: MeasureUnitType, // Logic for formatting values in UI
	override val editable: Boolean = false
) : PaletteCategory {

	// --- Track / Route Data (Prefix: route_*) ---

	SPEED(
		id = "speed",
		nameResId = "shared_string_speed",
		measureUnitType = MeasureUnitType.SPEED,
		editable = true
	),

	MAX_SPEED(
		id = "maxspeed",
		nameResId = "shared_string_max_speed",
		measureUnitType = MeasureUnitType.SPEED
	),

	ALTITUDE(
		id = "elevation", // Legacy key for routes
		nameResId = "altitude",
		measureUnitType = MeasureUnitType.ALTITUDE,
		editable = true
	),

	SLOPE(
		id = "slope", // Legacy key for routes
		nameResId = "shared_string_slope",
		measureUnitType = MeasureUnitType.NONE
	),

	// --- Terrain / Map Data ---

	TERRAIN_ALTITUDE(
		id = "HEIGHT", // Legacy key for TerrainMode
		nameResId = "shared_string_height",
		measureUnitType = MeasureUnitType.ALTITUDE
	),

	TERRAIN_SLOPE(
		id = "SLOPE", // Legacy key for TerrainMode
		nameResId = "shared_string_slope",
		measureUnitType = MeasureUnitType.NONE
	),

	TERRAIN_HILLSHADE(
		id = "HILLSHADE",
		nameResId = "shared_string_hillshade",
		measureUnitType = MeasureUnitType.NONE
	),

	// --- Weather (Prefix: weather_*) ---

	WEATHER(
		id = "weather",
		nameResId = "shared_string_weather",
		measureUnitType = MeasureUnitType.NONE
	);

	fun isTerrainRelated() = this in setOf(TERRAIN_ALTITUDE, TERRAIN_SLOPE, TERRAIN_HILLSHADE)

	fun isSupportDifferentRangeTypes(): Boolean {
		return GradientFileType.valuesOf(this)
			.map { it.rangeType }
			.distinct()
			.size > 1
	}

	/**
	 * Returns the default file type for this category.
	 * Usually maps to the first defined type in GradientFileType (typically FIXED).
	 */
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