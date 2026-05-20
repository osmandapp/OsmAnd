package net.osmand.plus.plugins.astronomy

import io.github.cosinekitty.astronomy.Body
import net.osmand.util.Algorithms

/**
 * Unified data class for any object rendered on the sky.
 */
open class SkyObject(
	open val id: String,
	open val hip: Int, // Hipparcos catalog ID

	open var catalogs: List<Catalog> = ArrayList(),
	open val wid: String, // Wikipedia ID
	open val centerWId: String? = null, // Wikipedia ID of parent object like Sun for Earth
	open val type: Type,
	open val body: Body?, // Null for custom stars not in Body enum
	open val name: String,
	open var ra: Double, // Right Ascension
	open var dec: Double, // Declination
	open val magnitude: Float,
	open val color: Int,

	open var radius: Double? = null,
	open var distance: Double? = null,
	open var mass: Double? = null,

	// Localized name from DB
	open var localizedName: String? = null,

	// Real-time calculated coordinates
	open var azimuth: Double = 0.0,
	open var altitude: Double = 0.0,
	open var distAu: Double = 0.0,

	open var isFavorite: Boolean = false,
	open var showDirection: Boolean = false,
	open var showCelestialPath: Boolean = false,
	open var colorIndex: Int = 0,

	// Animation state helpers
	open var startAzimuth: Double = 0.0,
	open var startAltitude: Double = 0.0,
	open var targetAzimuth: Double = 0.0,
	open var targetAltitude: Double = 0.0,

	// Cache helper
	open var lastUpdateTime: Double = -1.0
) {
	companion object {
		private const val HD_CATALOG_WID = "Q111130"
		private const val HIC_CATALOG_WID = "Q28914996"
		private const val HIP_CATALOG_WID = "Q537199"
	}

	enum class Type(val titleKey: String) {
		STAR("astro_type_star"),
		GALAXY("astro_type_galaxy"),
		BLACK_HOLE("astro_type_black_hole"),
		PLANET("astro_type_planet"),
		SUN("astro_type_star"),
		MOON("astro_type_satellite"),

		NEBULA("astro_type_nebula"),
		OPEN_CLUSTER("astro_type_open_cluster"),
		GLOBULAR_CLUSTER("astro_type_globular_cluster"),
		GALAXY_CLUSTER("astro_type_galaxy_cluster"),
		CONSTELLATION("astro_type_constellation");

		fun isSunSystem(): Boolean = this == SUN || this == MOON || this == PLANET
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other !is SkyObject) return false
		return id == other.id
	}

	override fun hashCode(): Int {
		return id.hashCode()
	}

	fun niceName() = getDisplayName()

	fun hasMissingPrimaryName(): Boolean = getPrimaryDisplayName() == null

	fun getDisplayName(): String {
		val primaryName = getPrimaryDisplayName()
		if (primaryName != null) {
			return primaryName
		}

		val catalogName = getCatalogFallbackName()
		if (catalogName != null) {
			return catalogName
		}

		val hipName = getHipFallbackName()
		if (hipName != null) {
			return hipName
		}

		if (!Algorithms.isEmpty(wid)) {
			return wid
		}

		return name
	}

	private fun getPrimaryDisplayName(): String? {
		val localized = localizedName
		if (!Algorithms.isEmpty(localized)) {
			return localized
		}

		if (!Algorithms.isEmpty(name) && !name.equals(wid, ignoreCase = true)) {
			return name
		}

		return null
	}

	private fun getCatalogFallbackName(): String? {
		var catalogId = catalogs.firstOrNull { it.wid == HD_CATALOG_WID }?.catalogId
		if (!Algorithms.isEmpty(catalogId)) {
			return catalogId
		}

		catalogId = catalogs.firstOrNull { it.wid == HIC_CATALOG_WID }?.catalogId
		if (!Algorithms.isEmpty(catalogId)) {
			return catalogId
		}

		catalogId = catalogs.firstOrNull { it.wid == HIP_CATALOG_WID }?.catalogId
		if (!Algorithms.isEmpty(catalogId)) {
			return catalogId
		}

		return null
	}

	private fun getHipFallbackName(): String? {
		if (hip > 0) {
			return "HIP $hip"
		}
		return null
	}

	override fun toString(): String {
		return "SkyObject(id='$id', name='$name', type=$type)"
	}
}
