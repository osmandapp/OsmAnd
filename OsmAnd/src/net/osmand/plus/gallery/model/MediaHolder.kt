package net.osmand.plus.gallery.model

import net.osmand.shared.media.domain.MediaItem

interface MediaHolder {
	fun getItems(): List<MediaItem>
	fun isEmpty(): Boolean = getItems().isEmpty()
}