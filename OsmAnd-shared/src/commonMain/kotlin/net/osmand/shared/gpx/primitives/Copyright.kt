package net.osmand.shared.gpx.primitives

class Copyright : GpxExtensions {
	var author: String? = null
	var year: String? = null
	var license: String? = null

	constructor()

	constructor(copyright: Copyright) {
		author = copyright.author
		year = copyright.year
		license = copyright.license
		copyExtensions(copyright)
	}
}