package net.osmand.plus.gallery.attached

import net.osmand.data.FavouritePoint
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.gpx.primitives.WptPt

class AttachedMediaRegistry {

	private val registry = mutableMapOf<GalleryKey, List<Link>>()

	// --- Favorites ---

	fun registerFavorite(point: FavouritePoint) {
		val key = GalleryKey.Favorite(point.key)
		val links = point.links
		if (!links.isNullOrEmpty()) {
			registry[key] = links
		} else {
			registry.remove(key)
		}
	}

	fun unregisterFavorite(point: FavouritePoint) {
		registry.remove(GalleryKey.Favorite(point.key))
	}

	// --- Waypoints ---

	fun registerWaypoints(gpxFile: GpxFile) {
		for (wpt in gpxFile.getPointsList()) {
			registerWaypoint(wpt, gpxFile.path)
		}
	}

	fun registerWaypoint(wpt: WptPt, gpxPath: String) {
		val name = wpt.name ?: return
		val key = GalleryKey.Waypoint(gpxPath, name)
		val links = wpt.links
		if (!links.isNullOrEmpty()) {
			registry[key] = links
		} else {
			registry.remove(key)
		}
	}

	fun unregisterWaypoints(gpxPath: String) {
		registry.keys.removeAll { it is GalleryKey.Waypoint && it.gpxPath == gpxPath }
	}

	// --- Query ---

	fun getLinks(key: GalleryKey): List<Link>? = registry[key]

	fun hasLinks(key: GalleryKey): Boolean = !registry[key].isNullOrEmpty()

	fun getAllEntries(): Map<GalleryKey, List<Link>> = registry.toMap()

	fun clear() { registry.clear() }
}