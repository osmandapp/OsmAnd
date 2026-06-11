package net.osmand.plus.gallery.data

import net.osmand.data.LatLon

sealed class GalleryKey {

	data class Location(
		val latLon: LatLon,
		val params: Map<String, String>
	) : GalleryKey()

	data class Astronomy(
		val wikidataId: String
	) : GalleryKey()

	data class Favorite(
		val key: String
	) : GalleryKey()

	data class Waypoint(
		val gpxPath: String,
		val key: String
	) : GalleryKey()
}