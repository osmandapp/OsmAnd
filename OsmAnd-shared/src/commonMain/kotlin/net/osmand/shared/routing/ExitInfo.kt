package net.osmand.shared.routing

/** Motorway exit reference and street name attached to a turn. */
class ExitInfo {

	var ref: String? = null

	var exitStreetName: String? = null

	fun isEmpty(): Boolean = ref == null && exitStreetName == null
}
