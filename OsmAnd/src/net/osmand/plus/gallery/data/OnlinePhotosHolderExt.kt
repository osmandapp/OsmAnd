package net.osmand.plus.gallery.data

import net.osmand.plus.gallery.model.GalleryItem
import net.osmand.plus.gallery.online.OnlinePhotosHolder
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.util.Algorithms

fun OnlinePhotosHolder.getOnlinePhotoItems(): List<GalleryItem.Media> {
	return getOrderedGalleryItems()
		.filterIsInstance<GalleryItem.Media>()
		.filter { it.mediaItem.type == MediaType.PHOTO }
}

fun OnlinePhotosHolder.getPhotoItemIndexById(id: String): Int {
	val items = getOnlinePhotoItems()
	return items.indexOfFirst { Algorithms.stringsEqual(it.mediaItem.id, id) }
		.takeIf { it >= 0 } ?: 0
}