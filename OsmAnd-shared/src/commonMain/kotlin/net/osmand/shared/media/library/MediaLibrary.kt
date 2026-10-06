package net.osmand.shared.media.library

import net.osmand.shared.data.KLatLon
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.util.KMapUtils

interface SortableMedia {
	val id: String
	val title: String
	val type: MediaType
	val dateMs: Long?
	val lastModifiedMs: Long?
	val sizeBytes: Long?
	val durationMs: Long?
	val lat: Double?
	val lon: Double?
}

enum class MediaLibrarySortMode(val group: Group) {
	NEAREST(Group.LOCATION), LAST_MODIFIED(Group.LOCATION),
	NAME_A_Z(Group.NAME), NAME_Z_A(Group.NAME),
	NEWEST_FIRST(Group.DATE), OLDEST_FIRST(Group.DATE),
	SIZE_LARGE_SMALL(Group.SIZE), SIZE_SMALL_LARGE(Group.SIZE),
	DURATION_LONG_SHORT(Group.DURATION), DURATION_SHORT_LONG(Group.DURATION);

	enum class Group { LOCATION, NAME, DATE, SIZE, DURATION }
}

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

object MediaLibraryGrouping {
	data class Group<T>(val type: MediaType, val items: List<T>) {
		val count: Int get() = items.size
	}

	fun <T : SortableMedia> group(items: List<T>): List<Group<T>> =
		listOf(MediaType.PHOTO, MediaType.VIDEO, MediaType.AUDIO).mapNotNull { type ->
			items.filter { it.type == type }.takeIf { it.isNotEmpty() }?.let { Group(type, it) }
		}

	fun <T> lastAttachedName(attachments: List<T>, name: (T) -> String): String? =
		attachments.lastOrNull()?.let(name)
}

data class MediaLibraryStats(val photos: Int, val videos: Int, val audios: Int, val bytes: Long) {
	companion object {
		fun compute(items: List<SortableMedia>) = MediaLibraryStats(
			items.count { it.type == MediaType.PHOTO },
			items.count { it.type == MediaType.VIDEO },
			items.count { it.type == MediaType.AUDIO },
			items.sumOf { (it.sizeBytes ?: 0L).coerceAtLeast(0L) }
		)
	}
}
