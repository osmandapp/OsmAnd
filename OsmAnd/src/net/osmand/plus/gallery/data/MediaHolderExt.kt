package net.osmand.plus.gallery.data

import net.osmand.plus.gallery.model.MediaHolder
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.util.KAlgorithms

fun MediaHolder.getPhotoItems(): List<MediaItem> =
	getItems().filter { it.type == MediaType.PHOTO }

fun MediaHolder.getPhotoIndexById(id: String): Int =
	getPhotoItems()
		.indexOfFirst { KAlgorithms.stringsEqual(it.id, id) }
		.takeIf { it >= 0 } ?: 0