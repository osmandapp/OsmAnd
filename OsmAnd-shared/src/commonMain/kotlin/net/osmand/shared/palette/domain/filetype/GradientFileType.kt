package net.osmand.shared.palette.domain.filetype

import net.osmand.shared.ColorPalette
import net.osmand.shared.palette.domain.GradientPoint
import net.osmand.shared.palette.domain.GradientRangeType
import net.osmand.shared.palette.domain.category.GradientPaletteCategory
import net.osmand.shared.units.LengthUnits
import net.osmand.shared.units.MeasurementUnit
import net.osmand.shared.units.NoUnit
import net.osmand.shared.units.PercentUnits
import net.osmand.shared.units.SpeedUnits

enum class GradientFileType(
	override val category: GradientPaletteCategory,
	override val filePrefix: String,
	val rangeType: GradientRangeType,
	val baseUnits: MeasurementUnit<*>,          // Unit used for storage (e.g., meters, fraction)
	val displayUnits: MeasurementUnit<*> = baseUnits, // Unit used for UI display (e.g., percent)
	val defaultValues: List<Float> = emptyList(),     // Default steps for new palettes
	val minLimit: Float? = null,                // Min allowed value for this type of data
	val maxLimit: Float? = null                 // Max allowed value for this type of data
) : PaletteFileType {

	// --- Track / Route Data (Prefix: route_*) ---

	SPEED_FIXED(
		filePrefix = "route_speed_fixed_",
		category = GradientPaletteCategory.SPEED,
		rangeType = GradientRangeType.FIXED_VALUES,
		baseUnits = SpeedUnits.KILOMETERS_PER_HOUR,
		defaultValues = listOf(0f, 50f, 100f),
		minLimit = 0f,
		maxLimit = null
	),

	SPEED_RELATIVE(
		filePrefix = "route_speed_",
		category = GradientPaletteCategory.SPEED,
		rangeType = GradientRangeType.RELATIVE,
		baseUnits = PercentUnits.FRACTION,
		displayUnits = PercentUnits.PERCENT,
		defaultValues = listOf(0f, 0.5f, 1.0f),
		minLimit = 0f,
		maxLimit = 1f
	),

	MAX_SPEED_FIXED(
		filePrefix = "route_fixed_maxspeed_",
		category = GradientPaletteCategory.MAX_SPEED,
		rangeType = GradientRangeType.FIXED_VALUES,
		baseUnits = SpeedUnits.KILOMETERS_PER_HOUR,
		defaultValues = listOf(0f, 50f, 100f),
		minLimit = 0f,
		maxLimit = null
	),

	MAX_SPEED_RELATIVE(
		filePrefix = "route_maxspeed_",
		category = GradientPaletteCategory.MAX_SPEED,
		rangeType = GradientRangeType.RELATIVE,
		baseUnits = PercentUnits.FRACTION,
		displayUnits = PercentUnits.PERCENT,
		defaultValues = listOf(0f, 0.5f, 1.0f),
		minLimit = 0f,
		maxLimit = 1f
	),

	ELEVATION_FIXED(
		filePrefix = "route_fixed_elevation_",
		category = GradientPaletteCategory.ALTITUDE,
		rangeType = GradientRangeType.FIXED_VALUES,
		baseUnits = LengthUnits.METERS,
		defaultValues = listOf(0f, 500f, 1000f, 2000f),
	),

	ELEVATION_RELATIVE(
		filePrefix = "route_elevation_",
		category = GradientPaletteCategory.ALTITUDE,
		rangeType = GradientRangeType.RELATIVE,
		baseUnits = PercentUnits.FRACTION,
		displayUnits = PercentUnits.PERCENT,
		defaultValues = listOf(0f, 0.5f, 1.0f)
	),

	SLOPE_FIXED(
		filePrefix = "route_fixed_slope_",
		category = GradientPaletteCategory.SLOPE,
		rangeType = GradientRangeType.FIXED_VALUES,
		baseUnits = NoUnit, // Usually degrees
		defaultValues = listOf(0f, 10f, 20f)
	),

	SLOPE_RELATIVE(
		filePrefix = "route_slope_",
		category = GradientPaletteCategory.SLOPE,
		rangeType = GradientRangeType.RELATIVE,
		baseUnits = PercentUnits.FRACTION,
		displayUnits = PercentUnits.PERCENT,
		defaultValues = listOf(0f, 0.5f, 1.0f)
	),

	// --- Terrain / Map Data ---

	TERRAIN_ALTITUDE(
		filePrefix = "height_",
		category = GradientPaletteCategory.TERRAIN_ALTITUDE,
		rangeType = GradientRangeType.FIXED_VALUES,
		baseUnits = LengthUnits.METERS,
		defaultValues = listOf(0f, 1000f, 2000f, 4000f)
	),

	TERRAIN_SLOPE(
		filePrefix = "slope_",
		category = GradientPaletteCategory.TERRAIN_SLOPE,
		rangeType = GradientRangeType.FIXED_VALUES,
		baseUnits = NoUnit, // Degrees
		defaultValues = listOf(0f, 15f, 30f, 45f)
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
		baseUnits = NoUnit, // 0-255 byte value
		defaultValues = listOf(0f, 255f)
	),

	// --- Weather (Prefix: weather_*) ---

	WEATHER(
		filePrefix = "weather_",
		category = GradientPaletteCategory.WEATHER,
		rangeType = GradientRangeType.FIXED_VALUES,
		baseUnits = NoUnit,
		defaultValues = listOf(0f, 100f)
	);

	/**
	 * Generates a list of GradientPoints based on the file type's default values.
	 * Colors are assigned sequentially from the default palette.
	 */
	fun getDefaultGradientPoints(): List<GradientPoint> {
		val defaultColors = ColorPalette.COLORS
		val lastColor = defaultColors.last()

		return defaultValues.mapIndexed { index, value ->
			GradientPoint(
				value = value,
				color = defaultColors.getOrElse(index) { lastColor }
			)
		}
	}

	companion object {

		fun valuesOf(category: GradientPaletteCategory) = entries.filter { category == it.category }

		fun fromFileName(fileName: String): GradientFileType? {
			return entries
				.sortedByDescending { it.filePrefix.length }
				.find { fileName.startsWith(it.filePrefix) }
		}
	}
}