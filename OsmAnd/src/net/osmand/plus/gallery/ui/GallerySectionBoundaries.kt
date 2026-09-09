package net.osmand.plus.gallery.ui

import net.osmand.plus.gallery.model.GalleryItem

/** Immutable adapter metadata, shared by row binding and section-card drawing. */
data class GallerySectionBoundary(
	val sectionId: String,
	val firstPosition: Int,
	val lastPosition: Int,
	val firstMediaPosition: Int,
	val collapsed: Boolean,
	val isFirst: Boolean,
	val isLast: Boolean
) {
	val roundTopCorners get() = isFirst
	val roundBottomCorners get() = isLast
}

object GallerySectionBoundaries {
	fun build(items: List<GalleryItem>): List<GallerySectionBoundary?> {
		val result = MutableList<GallerySectionBoundary?>(items.size) { null }
		var position = 0
		while (position < items.size) {
			val first = position
			val item = items[first]
			if (item !is GalleryItem.GroupHeader && item !is GalleryItem.Media && item !is GalleryItem.NoMedia) {
				position++
				continue
			}
			val header = item as? GalleryItem.GroupHeader
			val id = header?.let { "type:${it.type.name}" } ?: if (item is GalleryItem.NoMedia) "empty" else "all"
			position++
			if (item !is GalleryItem.NoMedia && header?.collapsed != true) {
				while (position < items.size && items[position] is GalleryItem.Media) position++
			}
			val last = position - 1
			val firstMedia = if (header != null) first + 1 else first
			for (index in first..last) {
				result[index] = GallerySectionBoundary(id, first, last, firstMedia,
					header?.collapsed == true, index == first, index == last)
			}
		}
		return result.toList()
	}
}
