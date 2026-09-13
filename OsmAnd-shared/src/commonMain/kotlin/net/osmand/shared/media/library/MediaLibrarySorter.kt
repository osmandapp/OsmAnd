package net.osmand.shared.media.library

import net.osmand.shared.data.KLatLon
import net.osmand.shared.util.KMapUtils

object MediaLibrarySorter {
	fun comparator(mode: MediaLibrarySortMode, referenceLatLon: KLatLon? = null): Comparator<SortableMedia> =
		Comparator { a, b ->
			val result = when (mode) {
				MediaLibrarySortMode.NAME_A_Z -> compareTitles(a, b)
				MediaLibrarySortMode.NAME_Z_A -> compareTitles(b, a)
				MediaLibrarySortMode.LAST_MODIFIED -> compareNullable(a.lastModifiedMs, b.lastModifiedMs, true)
				MediaLibrarySortMode.NEWEST_FIRST -> compareNullable(a.dateMs ?: a.lastModifiedMs, b.dateMs ?: b.lastModifiedMs, true)
				MediaLibrarySortMode.OLDEST_FIRST -> compareNullable(a.dateMs ?: a.lastModifiedMs, b.dateMs ?: b.lastModifiedMs)
				MediaLibrarySortMode.SIZE_LARGE_SMALL -> compareNullable(a.sizeBytes, b.sizeBytes, true)
				MediaLibrarySortMode.SIZE_SMALL_LARGE -> compareNullable(a.sizeBytes, b.sizeBytes)
				MediaLibrarySortMode.DURATION_LONG_SHORT -> compareNullable(a.durationMs, b.durationMs, true)
				MediaLibrarySortMode.DURATION_SHORT_LONG -> compareNullable(a.durationMs, b.durationMs)
				MediaLibrarySortMode.NEAREST -> compareNullable(distance(a, referenceLatLon), distance(b, referenceLatLon))
			}
			if (result != 0) result else compareTitles(a, b)
		}

	private fun compareTitles(a: SortableMedia, b: SortableMedia) = a.title.compareTo(b.title, ignoreCase = true)

	private fun <T : Comparable<T>> compareNullable(a: T?, b: T?, descending: Boolean = false): Int = when {
		a == null && b == null -> 0
		a == null -> 1
		b == null -> -1
		descending -> b.compareTo(a)
		else -> a.compareTo(b)
	}

	private fun distance(item: SortableMedia, reference: KLatLon?): Double? {
		val lat = item.lat ?: return null
		val lon = item.lon ?: return null
		if (reference == null || !lat.isFinite() || !lon.isFinite()) return null
		return KMapUtils.getDistance(reference.latitude, reference.longitude, lat, lon)
	}
}
