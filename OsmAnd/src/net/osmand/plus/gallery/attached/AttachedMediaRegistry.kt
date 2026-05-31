package net.osmand.plus.gallery.attached

import net.osmand.data.FavouritePoint
import net.osmand.plus.gallery.data.GalleryKey
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.primitives.Link
import net.osmand.shared.gpx.primitives.WptPt

// TODO: Temporary solution
class AttachedMediaRegistry {
	private val favorites = mutableMapOf<String, FavouritePoint>()
	private val waypoints = mutableMapOf<GalleryKey.Waypoint, WptPt>()

	fun registerFavorite(point: FavouritePoint) {
		if (!point.links.isNullOrEmpty()) {
			favorites[point.key] = point
		} else {
			favorites.remove(point.key)
		}
	}

	fun unregisterFavorite(point: FavouritePoint) {
		favorites.remove(point.key)
	}

	fun registerWaypoints(gpxFile: GpxFile) {
		for (wpt in gpxFile.getPointsList()) {
			registerWaypoint(wpt, gpxFile.path)
		}
	}

	fun registerWaypoint(wpt: WptPt, gpxPath: String) {
		val name = wpt.name ?: return
		val key = GalleryKey.Waypoint(gpxPath, name)
		if (!wpt.links.isNullOrEmpty()) {
			waypoints[key] = wpt
		} else {
			waypoints.remove(key)
		}
	}

	fun unregisterWaypoints(gpxPath: String) {
		waypoints.keys.removeAll { it.gpxPath == gpxPath }
	}

	fun hasAttachedMedia(key: GalleryKey): Boolean = when (key) {
		is GalleryKey.Favorite -> !favorites[key.pointKey]?.links.isNullOrEmpty()
		is GalleryKey.Waypoint -> !waypoints[key]?.links.isNullOrEmpty()
		else -> false
	}

	fun getLinks(key: GalleryKey): List<Link>? = when (key) {
		is GalleryKey.Favorite -> favorites[key.pointKey]?.links
		is GalleryKey.Waypoint -> waypoints[key]?.links
		else -> null
	}

	fun getAllFavorites(): Collection<FavouritePoint> = favorites.values
	fun getAllWaypoints(): Collection<WptPt> = waypoints.values
}