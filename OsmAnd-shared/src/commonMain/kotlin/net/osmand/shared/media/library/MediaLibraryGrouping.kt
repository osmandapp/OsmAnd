package net.osmand.shared.media.library

import net.osmand.shared.media.domain.MediaType

object MediaLibraryGrouping {
	data class Group<T>(val type: MediaType, val items: List<T>) {
		val count: Int get() = items.size
	}

	fun <T : SortableMedia> group(items: List<T>): List<Group<T>> =
		listOf(MediaType.PHOTO, MediaType.VIDEO, MediaType.AUDIO).mapNotNull { type ->
			items.filter { it.type == type }.takeIf { it.isNotEmpty() }?.let { Group(type, it) }
		}

	/** Links have no attachment timestamp; callers supply favorites order, then selected tracks order. */
	fun <T> lastAttachedName(attachments: List<T>, name: (T) -> String): String? =
		attachments.lastOrNull()?.let(name)
}
