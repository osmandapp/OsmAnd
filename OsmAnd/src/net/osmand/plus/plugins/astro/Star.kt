package net.osmand.plus.plugins.astro

data class Star(
	val name: String,
	val ra: Double, // Right Ascension in hours
	val dec: Double, // Declination in degrees
	val magnitude: Float,
	val color: Int,
	var azimuth: Double = 0.0,
	var altitude: Double = 0.0
)