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
	val stepSize: Int? = null
)