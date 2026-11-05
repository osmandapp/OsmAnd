package net.osmand.plus.gallery.data

import net.osmand.plus.gallery.model.MediaHolder

interface MediaLoadDelegate {

	fun load(
		key: GalleryKey,
		onStarted: () -> Unit,
		onResult: (MediaHolder) -> Unit,
		onError: () -> Unit
	)

	/** Optional incremental results; existing delegates can keep returning one final result. */
	fun loadWithUpdates(
		key: GalleryKey,
		onStarted: () -> Unit,
		onUpdate: (MediaHolder) -> Unit,
		onResult: (MediaHolder) -> Unit,
		onError: () -> Unit
	) = load(key, onStarted, onResult, onError)

	fun cancel(key: GalleryKey) = Unit
	fun canRefresh(): Boolean = true
}