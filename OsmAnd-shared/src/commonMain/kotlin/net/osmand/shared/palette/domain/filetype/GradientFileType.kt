package net.osmand.shared.palette.domain.filetype

import net.osmand.shared.palette.domain.GradientRangeType
import net.osmand.shared.palette.domain.category.GradientPaletteCategory

enum class GradientFileType(
	override val category: GradientPaletteCategory,
	override val filePrefix: String,
	val rangeType: GradientRangeType
): PaletteFileType {

	// --- Track / Route Data (Prefix: route_*) ---

	SPEED_FIXED(
		filePrefix = "route_speed_fixed_",
		category = GradientPaletteCategory.SPEED,
		rangeType = GradientRangeType.FIXED_VALUES,
	),

	SPEED_RELATIVE(
		filePrefix = "route_speed_",
		category = GradientPaletteCategory.SPEED,
		rangeType = GradientRangeType.RELATIVE
	),

	MAX_SPEED_FIXED(
		filePrefix = "route_fixed_maxspeed_",
		category = GradientPaletteCategory.MAX_SPEED,
		rangeType = GradientRangeType.FIXED_VALUES,
	),

	MAX_SPEED_RELATIVE(
		filePrefix = "route_maxspeed_",
		category = GradientPaletteCategory.MAX_SPEED,
		rangeType = GradientRangeType.RELATIVE,
	),

	ELEVATION_FIXED(
		filePrefix = "route_fixed_elevation_",
		category = GradientPaletteCategory.ALTITUDE,
		rangeType = GradientRangeType.FIXED_VALUES,
	),

	ELEVATION_RELATIVE(
		filePrefix = "route_elevation_",
		category = GradientPaletteCategory.ALTITUDE,
		rangeType = GradientRangeType.RELATIVE,
	),

	SLOPE_FIXED(
		filePrefix = "route_fixed_slope_",
		category = GradientPaletteCategory.SLOPE,
		rangeType = GradientRangeType.FIXED_VALUES,
	),

	SLOPE_RELATIVE(
		filePrefix = "route_slope_",
		category = GradientPaletteCategory.SLOPE,
		rangeType = GradientRangeType.RELATIVE,
	),

	// --- Terrain / Map Data ---

	TERRAIN_ALTITUDE(
		filePrefix = "height_",
		category = GradientPaletteCategory.TERRAIN_ALTITUDE,
		rangeType = GradientRangeType.FIXED_VALUES,
	),

	TERRAIN_SLOPE(
		filePrefix = "slope_",
		category = GradientPaletteCategory.TERRAIN_SLOPE,
		rangeType = GradientRangeType.FIXED_VALUES,
	),

	/**
	 * For hillshade we have pair of files, started with "hillshade_main_" and "hillshade_color_",
	 * but collect and show only "hillshade_main_" files,
	 * "hillshade_color_" file is secondary and comes with the main one
	 */
	TERRAIN_HILLSHADE_MAIN(
		filePrefix = "hillshade_main_",
		category = GradientPaletteCategory.TERRAIN_HILLSHADE,
		rangeType = GradientRangeType.FIXED_VALUES,
	),

//	TERRAIN_HILLSHADE_SECONDARY(
//		filePrefix = "hillshade_color_",
//		category = PaletteCategory.TERRAIN_HILLSHADE,
//		useFixedValues = true
//	),

	// --- Weather (Prefix: weather_*) ---

	WEATHER(
		filePrefix = "weather_",
		category = GradientPaletteCategory.WEATHER,
		rangeType = GradientRangeType.FIXED_VALUES,
	);

	companion object {

		fun valuesOf(category: GradientPaletteCategory) = entries.filter { category == it.category }

		fun fromFileName(fileName: String): GradientFileType? {
			return entries
				.sortedByDescending { it.filePrefix.length }
				.find { fileName.startsWith(it.filePrefix) }
		}
	}
}