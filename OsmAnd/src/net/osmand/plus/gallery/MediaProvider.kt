package net.osmand.plus.gallery

import android.content.Context
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.util.ImageLoaderCallback
import net.osmand.shared.util.LoadingImage
import net.osmand.shared.util.NetworkImageLoader

// TODO: remove or update docs, implement loading for other types
/**
 * Temporary provider that extracts existing online image loading logic from UI components
 * and adapts it to the new MediaItem model.
 *
 * At this stage it only supports previously existing online photo sources.
 * Other media types are intentionally not implemented yet and will be added
 * in subsequent migration steps.
 */
class MediaProvider(context: Context) {

	private val imageLoader = NetworkImageLoader(context, useDiskCache = true)

	fun loadThumbnail(item: MediaItem, callback: ImageLoaderCallback): LoadingImage? {
		return load(resolveUrl(item, ImageResolution.THUMBNAIL), callback)
	}

	fun loadStandardImage(item: MediaItem, callback: ImageLoaderCallback): LoadingImage? {
		return load(resolveUrl(item, ImageResolution.STANDARD), callback)
	}

	fun loadHiResImage(item: MediaItem, callback: ImageLoaderCallback): LoadingImage? {
		return load(resolveUrl(item, ImageResolution.HI_RES), callback)
	}

	private fun load(url: String?, callback: ImageLoaderCallback): LoadingImage? {
		if (url.isNullOrEmpty()) {
			callback.onError()
			return null
		}
		return imageLoader.loadImage(url, callback, handlePlaceholder = false)
	}

	private enum class ImageResolution { THUMBNAIL, STANDARD, HI_RES }

	private fun resolveUrl(item: MediaItem, resolution: ImageResolution): String? {
		if (item.type == MediaType.AUDIO || item.type == MediaType.VIDEO) {
			throw NotImplementedError("A/V media loading is not implemented yet")
		}

		return when (item) {
			is MediaItem.Remote -> when (resolution) {
				ImageResolution.THUMBNAIL -> item.previewContent?.thumbnailUrl ?: item.sourceUrl
				ImageResolution.STANDARD -> item.previewContent?.standardUrl ?: item.sourceUrl
				ImageResolution.HI_RES -> item.previewContent?.hiResUrl ?: item.sourceUrl
			}
			is MediaItem.Wiki -> when (resolution) {
				ImageResolution.HI_RES -> item.wikiImage.imageHiResUrl
				else -> item.wikiImage.imageStubUrl
			}
			is MediaItem.Internal, is MediaItem.Gallery -> {
				throw NotImplementedError("Local media loading is not implemented yet")
			}
		}
	}
}