package net.osmand.plus.plugins.astro

import io.github.cosinekitty.astronomy.Body

/**
 * Unified data class for any object rendered on the sky.
 */
data class SkyObject(
	val id: String,
	val hip: Int, // Hipparcos catalog ID
	val wid: String, // Wikipedia ID
	val type: Type,
	val body: Body?, // Null for custom stars not in Body enum
	val name: String,
	val ra: Double, // Right Ascension
	val dec: Double, // Declination
	val magnitude: Float,
	val color: Int,

	// Real-time calculated coordinates
	var azimuth: Double = 0.0,
	var altitude: Double = 0.0,
	var distAu: Double = 0.0,

	// Visibility flag
	var isVisible: Boolean = false,

	// Animation state helpers
	var startAzimuth: Double = 0.0,
	var startAltitude: Double = 0.0,
	var targetAzimuth: Double = 0.0,
	var targetAltitude: Double = 0.0
) {
	enum class Type {
		STAR, GALAXY, BLACK_HOLE, PLANET, SUN, MOON,
		NEBULA, OPEN_CLUSTER, GLOBULAR_CLUSTER, GALAXY_CLUSTER, CONSTELLATION;

		fun isSunSystem(): Boolean {
			return this == SUN || this == MOON || this == PLANET
		}
	}
}