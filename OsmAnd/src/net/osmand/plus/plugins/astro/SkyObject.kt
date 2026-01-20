package net.osmand.plus.plugins.astro

import io.github.cosinekitty.astronomy.Body

/**
 * Unified data class for any object rendered on the sky.
 */
open class SkyObject(
	open val id: String,
	open val hip: Int, // Hipparcos catalog ID
	open val wid: String, // Wikipedia ID
	open val type: Type,
	open val body: Body?, // Null for custom stars not in Body enum
	open val name: String,
	open var ra: Double, // Right Ascension
	open var dec: Double, // Declination
	open val magnitude: Float,
	open val color: Int,

	// Localized name from DB
	open var localizedName: String? = null,

	// Real-time calculated coordinates
	open var azimuth: Double = 0.0,
	open var altitude: Double = 0.0,
	open var distAu: Double = 0.0,

	// Visibility flag
	open var isVisible: Boolean = false,

	// Animation state helpers
	open var startAzimuth: Double = 0.0,
	open var startAltitude: Double = 0.0,
	open var targetAzimuth: Double = 0.0,
	open var targetAltitude: Double = 0.0
) {
	enum class Type {
		STAR, GALAXY, BLACK_HOLE, PLANET, SUN, MOON,
		NEBULA, OPEN_CLUSTER, GLOBULAR_CLUSTER, GALAXY_CLUSTER, CONSTELLATION;

		fun isSunSystem(): Boolean {
			return this == SUN || this == MOON || this == PLANET
		}
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is SkyObject) return false
		return id == other.id
	}

	override fun hashCode(): Int {
		return id.hashCode()
	}

	override fun toString(): String {
		return "SkyObject(id='$id', name='$name', type=$type)"
	}
}
