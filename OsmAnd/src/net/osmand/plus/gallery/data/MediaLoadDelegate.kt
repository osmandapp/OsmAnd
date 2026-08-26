package net.osmand.plus.gallery.data

import net.osmand.plus.gallery.model.MediaHolder

interface MediaLoadDelegate {

	fun load(
		key: GalleryKey,
		onStarted: () -> Unit,
		onResult: (MediaHolder) -> Unit,
		onError: () -> Unit
	)

	fun cancel(key: GalleryKey) = Unit
}