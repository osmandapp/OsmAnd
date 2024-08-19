package net.osmand.shared.gpx.primitives

class Bounds : GpxExtensions {
	var minlat: Double = 0.0
	var minlon: Double = 0.0
	var maxlat: Double = 0.0
	var maxlon: Double = 0.0

	constructor()

	constructor(source: Bounds) {
		minlat = source.minlat
		minlon = source.minlon
		maxlat = source.maxlat
		maxlon = source.maxlon
		copyExtensions(source)
	}
}