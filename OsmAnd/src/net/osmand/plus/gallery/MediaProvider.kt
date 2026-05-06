package net.osmand.plus.gallery

import android.content.Context
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.util.ImageLoaderCallback
import net.osmand.shared.util.ImageRequestListener
import net.osmand.shared.util.LoadingImage
import net.osmand.shared.util.NetworkImageLoader

// TODO: remove or update docs, implement loading for other types
/**
 * Temporary provider that extracts existing online image loading logic from UI components
 * and adapts it to the new MediaItem model.
 *
 * At this stage it supports image loading through the normalized MediaResource contract.
 * Local/video/audio loading can be extended later without changing UI consumers.
 */
class MediaProvider(context: Context) {

	private val imageLoader = NetworkImageLoader(context, useDiskCache = true)

	@JvmOverloads
	fun loadThumbnail(
		item: MediaItem,
		callback: ImageLoaderCallback,
		requestListener: ImageRequestListener? = null
	): LoadingImage? {
		return load(resolveUri(item, ImageResolution.THUMBNAIL), callback, requestListener)
	}

	@JvmOverloads
	fun loadPreview(
		item: MediaItem,
		callback: ImageLoaderCallback,
		requestListener: ImageRequestListener? = null
	): LoadingImage? {
		return load(resolveUri(item, ImageResolution.PREVIEW), callback, requestListener)
	}

	@JvmOverloads
	fun loadFull(
		item: MediaItem,
		callback: ImageLoaderCallback,
		requestListener: ImageRequestListener? = null
	): LoadingImage? {
		return load(resolveUri(item, ImageResolution.FULL), callback, requestListener)
	}

	private fun load(
		uri: String?,
		callback: ImageLoaderCallback,
		requestListener: ImageRequestListener?
	): LoadingImage? {
		if (uri.isNullOrBlank()) {
			callback.onError()
			return null
		}
		return imageLoader.loadImage(uri, callback, requestListener, handlePlaceholder = false)
	}

	private enum class ImageResolution { THUMBNAIL, PREVIEW, FULL }

	private fun resolveUri(item: MediaItem, resolution: ImageResolution): String? {
		if (item.type != MediaType.PHOTO) {
			return null
		}

		return when (resolution) {
			ImageResolution.THUMBNAIL -> item.resource.thumbnailUri
			ImageResolution.PREVIEW -> item.resource.previewUri
			ImageResolution.FULL -> item.resource.fullUri
		}.takeIf { !it.isNullOrEmpty() }
	}
}