package net.osmand.shared.palette.domain

enum class PaletteFileType(
	val filePrefix: String,                // Exact file prefix on disk
	val category: PaletteCategory,         // Related category
	val useFixedValues: Boolean = false,   // TODO: indicates if this type values represents as fixed (relative otherwise)
) {
	// --- Track / Route Data (Prefix: route_*) ---

	SPEED_FIXED(
		filePrefix = "route_speed_fixed",
		category = PaletteCategory.SPEED,
		useFixedValues = true
	),

	SPEED_RELATIVE(
		filePrefix = "route_speed",
		category = PaletteCategory.SPEED
	),

	MAX_SPEED_FIXED(
		filePrefix = "route_fixed_maxspeed_",
		category = PaletteCategory.MAX_SPEED,
		useFixedValues = true
	),

	MAX_SPEED_RELATIVE(
		filePrefix = "route_maxspeed_",
		category = PaletteCategory.MAX_SPEED
	),

	ELEVATION_FIXED(
		filePrefix = "route_fixed_elevation_",
		category = PaletteCategory.ALTITUDE,
		useFixedValues = true
	),

	ELEVATION_RELATIVE(
		filePrefix = "route_elevation_",
		category = PaletteCategory.ALTITUDE
	),

	SLOPE_FIXED(
		filePrefix = "route_fixed_slope_",
		category = PaletteCategory.SLOPE,
		useFixedValues = true
	),

	SLOPE_RELATIVE(
		filePrefix = "route_slope_",
		category = PaletteCategory.SLOPE
	),

	// --- Terrain / Map Data ---

	TERRAIN_ALTITUDE(
		filePrefix = "height_altitude_",
		category = PaletteCategory.TERRAIN_ALTITUDE,
		useFixedValues = true
	),

	TERRAIN_SLOPE(
		filePrefix = "slope_",
		category = PaletteCategory.TERRAIN_SLOPE,
		useFixedValues = true
	),

	/**
	 * For hillshade we have pair of files, started with "hillshade_main_" and "hillshade_color_",
	 * but collect and show only "hillshade_main_" files,
	 * "hillshade_color_" file is secondary and comes with the main one
	 */
	TERRAIN_HILLSHADE_MAIN(
		filePrefix = "hillshade_main_",
		category = PaletteCategory.TERRAIN_HILLSHADE,
		useFixedValues = true
	),

//	TERRAIN_HILLSHADE_SECONDARY(
//		filePrefix = "hillshade_color_",
//		category = PaletteCategory.TERRAIN_HILLSHADE,
//		useFixedValues = true
//	),

	// --- User palettes (Prefix: user_palette_*) ---

	USER_PALETTE(
		filePrefix = "user_palette_",
		category = PaletteCategory.SOLID_PALETTE,
		useFixedValues = true // TODO: it is not relevant information for user palette
	),

	// --- Weather (Prefix: weather_*) ---

	WEATHER(
		filePrefix = "weather_",
		category = PaletteCategory.WEATHER,
		useFixedValues = true // TODO: may be it is not relevant information for weather?
	);

	companion object {

		fun valuesOf(category: PaletteCategory) = entries.filter { category == it.category }

		fun fromFileName(fileName: String) = entries.find { fileName.startsWith(it.filePrefix) }
	}
}