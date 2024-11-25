package net.osmand.shared.gpx.primitives

data class RouteActivity(
	val id: String,
	val label: String,
	val iconName: String,
	val group: RouteActivityGroup,
	val tags: Set<String>? = null
) {
	override fun toString(): String {
		return id
	}
}