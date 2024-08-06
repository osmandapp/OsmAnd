package net.osmand.shared.gpx.primitives

class Route : GpxExtensions() {
	var name: String? = null
	var desc: String? = null
	var points = mutableListOf<WptPt>()
}