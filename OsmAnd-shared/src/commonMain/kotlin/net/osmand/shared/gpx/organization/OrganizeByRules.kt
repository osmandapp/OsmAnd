package net.osmand.shared.gpx.organization

import kotlinx.serialization.Serializable
import net.osmand.shared.gpx.organization.enums.OrganizeByType

/**
 * Rules for track organization.
 * [stepSize] is non-null only for range-based filters (e.g., Distance, Altitude).
 * For named list filters, [stepSize] is null.
 */
@Serializable
data class OrganizeByRules(
	val type: OrganizeByType,
	val stepSize: Double? = null
) {

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || this::class != other::class) return false

		other as OrganizeByRules

		if (type != other.type) return false
		if (stepSize != other.stepSize) return false

		return true
	}

	override fun hashCode(): Int {
		var result = type.hashCode()
		result = 31 * result + (stepSize?.hashCode() ?: 0)
		return result
	}
}