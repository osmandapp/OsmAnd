package net.osmand.plus.plugins.drone.model

data class RawValue(
	val raw: String,
	val knownSemantic: String? = null,
)

data class VerticalLimit(
	val value: String,
	val unit: RawValue,
	val reference: RawValue,
) {
	fun format(): String = listOf(value, unit.raw, reference.raw).filter { it.isNotBlank() }.joinToString(" ")
}

data class DroneZoneLink(
	val text: String,
	val url: String,
)

data class DroneZoneDatasetInfo(
	val id: String?,
	val provider: String?,
	val source: String?,
	val validFrom: String?,
	val validTo: String?,
	val expired: Boolean,
)

data class DroneZone(
	val sourceId: String?,
	val identifier: String?,
	val name: String,
	val restriction: RawValue,
	val variant: RawValue?,
	val typeCode: RawValue?,
	val reasons: List<RawValue>,
	val lowerLimit: VerticalLimit?,
	val upperLimit: VerticalLimit?,
	val legalTexts: List<String>,
	val links: List<DroneZoneLink>,
	val dataset: DroneZoneDatasetInfo,
)
