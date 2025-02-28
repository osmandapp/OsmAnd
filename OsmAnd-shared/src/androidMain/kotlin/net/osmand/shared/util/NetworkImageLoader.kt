package net.osmand.shared.util

import android.content.Context
import coil3.Bitmap
import coil3.ImageLoader
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap

class NetworkImageLoader(private val context: Context, useDiskCache: Boolean = false) {

    companion object {
        private const val DISK_CACHE_SIZE = 1024 * 1024 * 100L // 100MB
        private const val DISK_IMAGES_CACHE_DIR = "net_images_cache"
    }

    private var imageLoader: ImageLoader = ImageLoader.Builder(context)
        .allowHardware(false)
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .apply {
            if (useDiskCache) {
                diskCache {
                    DiskCache.Builder()
                        .directory(context.cacheDir.resolve(DISK_IMAGES_CACHE_DIR))
                        .maxSizeBytes(DISK_CACHE_SIZE)
                        .build()
                }
            }
        }
        .build()

    fun loadImage(
        url: String, callback: ImageLoaderCallback, handlePlaceholder: Boolean = false
    ): LoadingImage {
        val request = ImageRequest.Builder(context)
            .data(url)
            .target(
                onStart = { placeholder ->
                    callback.onStart(placeholder?.takeIf { handlePlaceholder }?.toBitmap())
                },
                onSuccess = { result ->
                    callback.onSuccess(result.toBitmap())
                },
                onError = { _ ->
                    callback.onError()
                })
            .build()

        return LoadingImage(url, imageLoader.enqueue(request))
    }
}

interface ImageLoaderCallback {

    fun onStart(bitmap: Bitmap?)

    fun onSuccess(bitmap: Bitmap)

    fun onError()
}

class LoadingImage(val url: String, private val disposable: Disposable) {

    fun cancel(): Boolean {
        disposable.dispose()
        return true
    }
}