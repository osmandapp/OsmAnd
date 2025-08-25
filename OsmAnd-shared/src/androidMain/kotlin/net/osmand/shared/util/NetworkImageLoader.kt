package net.osmand.shared.util

import android.content.Context
import coil3.Bitmap
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.CachePolicy
import coil3.request.Disposable
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import kotlin.random.Random

class NetworkImageLoader(private val context: Context, useDiskCache: Boolean = false) {
    val log = LoggerFactory.getLogger("NetworkImageLoader")

    companion object {
        private const val DISK_CACHE_SIZE = 1024 * 1024 * 300L // 300MB
        private const val DISK_IMAGES_CACHE_DIR = "net_images_cache"

        private const val MAX_THREADS = 8
        private const val MAX_REQUESTS_PER_HOST = 4
        private const val USER_AGENT = "Mozilla/5.0 (OsmAnd; Android)"

        private const val MAX_ATTEMPTS = 5
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private const val RETRY_SLEEP = MAX_REQUESTS_PER_HOST * 1000L
    }

    private val okDispatcher = okhttp3.Dispatcher().apply {
        maxRequestsPerHost = MAX_REQUESTS_PER_HOST
        maxRequests = MAX_THREADS
    }

    private val okHttp = okhttp3.OkHttpClient.Builder()
        .dispatcher(okDispatcher)
        .addInterceptor { chain ->
            android.net.TrafficStats.setThreadStatsTag(android.os.Process.myTid() and 0xFFFFF)
            try {
                chain.proceed(chain.request())
            } finally {
                android.net.TrafficStats.clearThreadStatsTag()
            }
        }
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .header("User-Agent", USER_AGENT)
                .build())
        }
        .addInterceptor { chain ->
            var attempt = 0
            var lastCode = 0
            val req = chain.request()
            var lastIoError: java.io.IOException? = null
            while (attempt++ < MAX_ATTEMPTS) {
                try {
                    val resp = chain.proceed(req)
                    if (resp.code != HTTP_TOO_MANY_REQUESTS) {
                        return@addInterceptor resp
                    }
                    lastCode = resp.code
                    resp.close()
                } catch (e: java.io.IOException) {
                    lastIoError = e
                }
                val backoff = RETRY_SLEEP * attempt
                Thread.sleep(Random.nextLong(backoff, backoff * 2))
            }
            throw lastIoError ?: java.io.IOException("MAX_ATTEMPTS (HTTP $lastCode)")
        }
        .build()

    private var imageLoader: ImageLoader = ImageLoader.Builder(context)
        .allowHardware(false)
        .components {
            add(OkHttpNetworkFetcherFactory(okHttp))
        }
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
            } else {
                diskCachePolicy(CachePolicy.DISABLED)
            }
        }
        .build()

    fun loadImage(
        url: String,
        callback: ImageLoaderCallback? = null,
        imageRequestListener: ImageRequestListener? = null,
        handlePlaceholder: Boolean = false
    ): LoadingImage {
        val requestBuilder = ImageRequest.Builder(context)
            .data(url)

        callback?.let {
            requestBuilder.target(
                onStart = { placeholder ->
                    it.onStart(placeholder?.takeIf { handlePlaceholder }?.toBitmap())
                },
                onSuccess = { result ->
                    it.onSuccess(result.toBitmap())
                },
                onError = { _ ->
                    it.onError()
                }
            )
        }

        imageRequestListener?.let {
            requestBuilder.listener(
                onSuccess = { _, result ->
                    val source = when (result.dataSource) {
                        DataSource.MEMORY_CACHE -> ImageLoadSource.MEMORY_CACHE
                        DataSource.MEMORY -> ImageLoadSource.MEMORY
                        DataSource.DISK -> ImageLoadSource.DISK
                        DataSource.NETWORK -> ImageLoadSource.NETWORK
                        else -> ImageLoadSource.MEMORY
                    }
                    it.onSuccess(source)
                },
                onError = { _, errorResult ->
                    val throwable = errorResult.throwable
                    val info =
                        (throwable as? coil3.network.HttpException)?.response?.code?.toString()
                            ?: (throwable.message ?: "$throwable")
                    log.error("NetworkImageLoader error $url ($info)")
                }
            )
        }

        val request = requestBuilder.build()
        return LoadingImage(url, imageLoader.enqueue(request))
    }

    fun loadImage(
        url: String,
        callback: ImageLoaderCallback,
        handlePlaceholder: Boolean = false
    ): LoadingImage {
        return loadImage(url, callback, null, handlePlaceholder)
    }

    fun loadImage(url: String): LoadingImage {
        return loadImage(url, null, null, false)
    }
}

interface ImageLoaderCallback {

    fun onStart(bitmap: Bitmap?)

    fun onSuccess(bitmap: Bitmap)

    fun onError()
}

interface ImageRequestListener {
    fun onSuccess(source: ImageLoadSource)
}

enum class ImageLoadSource {
    MEMORY_CACHE,
    MEMORY,
    DISK,
    NETWORK
}

class LoadingImage(val url: String, private val disposable: Disposable) {

    fun cancel() {
        disposable.dispose()
    }
}