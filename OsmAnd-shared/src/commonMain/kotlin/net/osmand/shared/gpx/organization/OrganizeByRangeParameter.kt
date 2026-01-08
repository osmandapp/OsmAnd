package net.osmand.shared.gpx.organization

import kotlinx.serialization.Serializable
import net.osmand.shared.gpx.organization.enums.OrganizeByType

@Serializable
class OrganizeByRangeParameter private constructor() : OrganizeByParameter() {
	var stepSize: Double = 0.0

	constructor(
		type: OrganizeByType,
		stepSize: Double
	) : this() {
		this.type = type
		this.stepSize = stepSize
	}

	override fun equals(other: Any?): Boolean {
		return other is OrganizeByRangeParameter && super.equals(other) && stepSize == other.stepSize
	}

	override fun hashCode(): Int {
		var result = type.hashCode()
		result = 31 * result + (stepSize.hashCode() ?: 0)
		return result
	}
}