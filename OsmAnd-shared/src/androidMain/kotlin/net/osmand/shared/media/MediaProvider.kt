package net.osmand.shared.media

import android.content.Context
import android.net.Uri
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.util.ImageLoaderCallback
import net.osmand.shared.util.ImageRequestListener
import net.osmand.shared.util.LoadingImage
import net.osmand.shared.util.NetworkImageLoader
import net.osmand.shared.util.PlatformUtil
import java.io.File

/**
 * Provides UI-facing operations for MediaItem.
 *
 * MediaItem stores media metadata and resolved URIs, while MediaProvider decides
 * how to load, open, share or otherwise consume each item depending on its type
 * and source.
 *
 * Currently, it supports photo loading through NetworkImageLoader.
 */
class MediaProvider(context: Context) {

	private val appDir = PlatformUtil.getOsmAndContext().getAppDir()
	private val imageLoader = NetworkImageLoader(context, useDiskCache = true)

	@JvmOverloads
	fun loadThumbnail(
		item: MediaItem,
		callback: ImageLoaderCallback? = null,
		requestListener: ImageRequestListener? = null
	): LoadingImage? {
		return load(resolvePhotoUri(item, item.previewUris.thumbnailUri), callback, requestListener, null)
	}

	@JvmOverloads
	fun loadStandardSizeImage(
		item: MediaItem,
		callback: ImageLoaderCallback? = null,
		requestListener: ImageRequestListener? = null,
		targetSizePx: Int? = null
	): LoadingImage? {
		return load(
			resolvePhotoUri(item, item.previewUris.standardSizeUri),
			callback,
			requestListener,
			targetSizePx
		)
	}

	@JvmOverloads
	fun loadFullSizeImage(
		item: MediaItem,
		callback: ImageLoaderCallback? = null,
		requestListener: ImageRequestListener? = null,
		targetSizePx: Int? = null
	): LoadingImage? {
		return load(resolvePhotoUri(item, item.previewUris.fullSizeUri), callback, requestListener, targetSizePx)
	}

	private fun load(
		uri: String?,
		callback: ImageLoaderCallback?,
		requestListener: ImageRequestListener?,
		targetSizePx: Int?
	): LoadingImage? {
		if (uri.isNullOrBlank()) {
			callback?.onError()
			return null
		}
		return imageLoader.loadImage(uri, callback, requestListener, handlePlaceholder = false, targetSizePx = targetSizePx)
	}

	private fun resolvePhotoUri(item: MediaItem, uri: String?): String? {
		if (item.type != MediaType.PHOTO || uri.isNullOrEmpty()) {
			return null
		}
		return if (item is MediaItem.Internal) {
			Uri.fromFile(resolveInternalMediaFile(appDir.absolutePath(), item.relativePath)).toString()
		} else {
			uri
		}
	}

	companion object {

		private const val INTERNAL_MEDIA_DIR = "avnotes"

		@JvmStatic
		fun getInternalMediaDir(appBaseDir: String): File {
			return File(appBaseDir, INTERNAL_MEDIA_DIR)
		}

		@JvmStatic
		fun getInternalMediaRelativePath(internalPath: String?): String? {
			val fileName = LinkMediaFactory.getInternalMediaFileName(internalPath) ?: return null
			return "$INTERNAL_MEDIA_DIR/$fileName"
		}

		/**
		 * Resolves an internal media link path (`osmand://media/<name>`) to a physical file: the
		 * canonical media folder first, with a literal fallback so legacy links still resolve.
		 */
		@JvmStatic
		fun resolveInternalMediaFile(appBaseDir: String, internalPath: String): File {
			val relativePath = getInternalMediaRelativePath(internalPath)
			if (relativePath != null) {
				val file = File(appBaseDir, relativePath)
				if (file.exists()) {
					return file
				}
			}
			return File(appBaseDir, internalPath)
		}
	}
}
