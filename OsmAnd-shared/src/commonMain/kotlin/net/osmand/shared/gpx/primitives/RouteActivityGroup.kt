package net.osmand.shared.gpx.primitives

data class RouteActivityGroup(
	val id: String,
	val label: String,
	val activities: List<RouteActivity>
)