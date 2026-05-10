package net.osmand.plus.gallery

import android.content.Context
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.util.ImageLoaderCallback
import net.osmand.shared.util.ImageRequestListener
import net.osmand.shared.util.LoadingImage
import net.osmand.shared.util.NetworkImageLoader

/**
 * Provides UI-facing operations for MediaItem.
 *
 * MediaItem stores media metadata and resolved URIs, while MediaProvider decides
 * how to load, open, share or otherwise consume each item depending on its type
 * and source.
 *
 * Currently it supports remote photo loading through NetworkImageLoader.
 * Local media and audio/video handling can be added behind the same API later.
 */
class MediaProvider(context: Context) {

	private val imageLoader = NetworkImageLoader(context, useDiskCache = true)

	@JvmOverloads
	fun loadThumbnail(
		item: MediaItem,
		callback: ImageLoaderCallback? = null,
		requestListener: ImageRequestListener? = null
	): LoadingImage? {
		return load(resolveUri(item, ImageResolution.THUMBNAIL), callback, requestListener)
	}

	@JvmOverloads
	fun loadStandardSizeImage(
		item: MediaItem,
		callback: ImageLoaderCallback? = null,
		requestListener: ImageRequestListener? = null
	): LoadingImage? {
		return load(resolveUri(item, ImageResolution.STANDARD), callback, requestListener)
	}

	@JvmOverloads
	fun loadFullSizeImage(
		item: MediaItem,
		callback: ImageLoaderCallback? = null,
		requestListener: ImageRequestListener? = null
	): LoadingImage? {
		return load(resolveUri(item, ImageResolution.FULL), callback, requestListener)
	}

	private fun load(
		uri: String?,
		callback: ImageLoaderCallback?,
		requestListener: ImageRequestListener?
	): LoadingImage? {
		if (uri.isNullOrBlank()) {
			callback?.onError()
			return null
		}
		return imageLoader.loadImage(uri, callback, requestListener, handlePlaceholder = false)
	}

	private enum class ImageResolution { THUMBNAIL, STANDARD, FULL }

	private fun resolveUri(item: MediaItem, resolution: ImageResolution): String? {
		if (item.type != MediaType.PHOTO) {
			return null
		}

		return when (resolution) {
			ImageResolution.THUMBNAIL -> item.previewUris.thumbnailUri
			ImageResolution.STANDARD -> item.previewUris.standardSizeUri
			ImageResolution.FULL -> item.previewUris.fullSizeUri
		}.takeIf { !it.isNullOrEmpty() }
	}
}