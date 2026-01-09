package net.osmand.shared.gpx.organization

import kotlinx.serialization.Polymorphic
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.osmand.shared.gpx.organization.enums.OrganizeByType

@Serializable
@Polymorphic
open class OrganizeByParams protected constructor() {
	@SerialName("type")
	lateinit var type: OrganizeByType
		protected set

	constructor(type: OrganizeByType) : this() {
		this.type = type
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) return true
		if (other == null || this::class != other::class) return false

		other as OrganizeByParams

		return type == other.type
	}

	override fun hashCode(): Int {
		return type.hashCode()
	}
}