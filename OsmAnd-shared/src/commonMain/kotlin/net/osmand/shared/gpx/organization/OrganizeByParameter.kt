package net.osmand.shared.gpx.organization

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.osmand.shared.gpx.organization.enums.OrganizeByType

/**
 * Rules for track organization.
 * [stepSize] is non-null only for range-based filters (e.g., Distance, Altitude).
 * For named list filters, [stepSize] is null.
 */
@Serializable
@Polymorphic
open class OrganizeByParameter protected constructor() {
	@SerialName("type")
	lateinit var type: OrganizeByType

	constructor(type: OrganizeByType) : this() {
		this.type = type
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || this::class != other::class) return false

		other as OrganizeByParameter

		return type == other.type
	}

	override fun hashCode(): Int {
		return type.hashCode()
	}
}