package net.osmand.plus.gallery.data

import net.osmand.plus.gallery.model.MediaHolder

interface MediaLoadListener {
	fun onLoadingStarted(key: GalleryKey) {}
	fun onLoaded(key: GalleryKey, holder: MediaHolder)
	fun onLoadFailed(key: GalleryKey) {}
}