package net.osmand.shared.media.library

import net.osmand.shared.media.domain.MediaType

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
