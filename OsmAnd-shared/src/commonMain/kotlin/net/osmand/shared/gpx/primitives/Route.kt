package net.osmand.shared.gpx.primitives

class Route : GpxExtensions {
	var name: String? = null
	var desc: String? = null
	var points = mutableListOf<WptPt>()

	constructor()

	constructor(other: Route) {
		this.name = other.name
		this.desc = other.desc
		this.points = other.points.map { WptPt(it) }.toMutableList()
		copyExtensions(other)
	}
}