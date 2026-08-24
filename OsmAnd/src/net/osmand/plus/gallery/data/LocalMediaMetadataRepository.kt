package net.osmand.plus.gallery.data

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_LOCATION
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.LruCache
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.plus.OsmandApplication
import net.osmand.plus.media.MediaMetadataUtils
import net.osmand.plus.utils.AndroidUtils
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.media.MediaProvider
import net.osmand.shared.media.domain.MediaItem
import net.osmand.shared.media.domain.MediaType
import java.io.File
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import androidx.core.graphics.scale
import androidx.core.net.toUri

/**
 * TEMPORARY implementation of [MediaMetadataRepository] / [MediaPosterLoader]
 * until the media backend provides metadata natively.
 *
 * All the "dirty" knowledge lives here and only here: how a [MediaItem] maps
 * to a local [File] or content:// uri, that size/date come from the file
 * system, provider columns, or embedded metadata, and that duration/posters
 * are decoded with [MediaMetadataRetriever]. When the backend lands, this class
 * is replaced by a backend-backed implementation; nothing above the interfaces
 * changes.
 */
class LocalMediaMetadataRepository(
	private val app: OsmandApplication
) : MediaMetadataRepository, MediaPosterLoader, MediaSourceResolver {

	private val executor = Executors.newSingleThreadExecutor()
	private val mainHandler = Handler(Looper.getMainLooper())
	private val creationTimeFormats by lazy {
		listOf(
			"yyyyMMdd'T'HHmmss.SSSX",
			"yyyyMMdd'T'HHmmssX",
			"yyyyMMdd'T'HHmmss",
			"yyyyMMdd",
			"yyyy-MM-dd",
			"yyyy MM dd"
		).map { pattern ->
			SimpleDateFormat(pattern, Locale.US).apply {
				isLenient = false
				timeZone = TimeZone.getTimeZone("UTC")
			}
		}
	}

	private val metadata = ConcurrentHashMap<String, GalleryMediaMetadata>()
	private val noPosterIds = ConcurrentHashMap.newKeySet<String>()
	private val posters = object : LruCache<String, Bitmap>(POSTER_CACHE_BYTES) {
		override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
	}

	// --- MediaMetadataRepository ---

	override fun getCached(item: MediaItem): GalleryMediaMetadata? = metadata[item.id]

	override fun request(items: List<MediaItem>, listener: MediaMetadataListener): Cancellable {
		val cancelled = AtomicBoolean(false)
		val toExtract = items.filter { metadata[it.id] == null }
		if (toExtract.isEmpty()) {
			mainHandler.post { if (!cancelled.get()) listener.onBatchFinished() }
			return Cancellable { cancelled.set(true) }
		}
		executor.execute {
			for (item in toExtract) {
				if (cancelled.get()) return@execute
				if (metadata[item.id] != null) continue
				val extracted = extract(item)
				metadata[item.id] = extracted
				mainHandler.post {
					if (!cancelled.get()) listener.onMetadataLoaded(item, extracted)
				}
			}
			mainHandler.post { if (!cancelled.get()) listener.onBatchFinished() }
		}
		return Cancellable { cancelled.set(true) }
	}

	// --- MediaPosterLoader ---

	override fun loadPoster(item: MediaItem, callback: (Bitmap?) -> Unit) {
		if (item.type != MediaType.VIDEO) {
			callback(null)
			return
		}
		posters.get(item.id)?.let {
			callback(it)
			return
		}
		if (noPosterIds.contains(item.id)) {
			callback(null)
			return
		}
		executor.execute {
			val poster = posters.get(item.id) ?: extractPoster(item)?.also {
				posters.put(item.id, it)
			}
			if (poster == null) {
				noPosterIds.add(item.id)
			}
			mainHandler.post { callback(poster) }
		}
	}

	// --- MediaSourceResolver ---

	override fun getPlaybackUri(item: MediaItem): Uri? {
		resolveFile(item)?.let { return Uri.fromFile(it) }
		return resolveContentUri(item)
	}

	override fun getShareableUri(item: MediaItem): Uri? {
		resolveContentUri(item)?.let { return it }
		val file = resolveFile(item) ?: return null
		return try {
			AndroidUtils.getUriForFile(app, file)
		} catch (_: Exception) {
			null
		}
	}

	// --- Extraction (temporary local knowledge) ---

	private fun extract(item: MediaItem): GalleryMediaMetadata {
		val file = resolveFile(item)
		val contentUri = if (file == null) resolveContentUri(item) else null

		val sizeBytes = file?.length()?.takeIf { it > 0 }
			?: contentUri?.let { queryContentLong(it, OpenableColumns.SIZE) }
		val lastModifiedTimeMs = file?.lastModified()?.takeIf { it > 0 }
			?: contentUri?.let {
				queryContentLong(it, DocumentsContract.Document.COLUMN_LAST_MODIFIED)
					?: queryContentLong(it, MediaStore.MediaColumns.DATE_MODIFIED)?.times(1000L)
			}
		val durationMs = if (item.type == MediaType.VIDEO || item.type == MediaType.AUDIO) {
			extractDuration(file, contentUri)
		} else {
			null
		}
		val creationTimeMs = contentUri?.let {
			queryContentLong(it, MediaStore.MediaColumns.DATE_TAKEN)
		}
			?: extractCreationTime(item, file, contentUri)
		val latLon = extractLocation(item, file, contentUri)
		return GalleryMediaMetadata(
			sizeBytes = sizeBytes,
			lastModifiedTimeMs = lastModifiedTimeMs,
			durationMs = durationMs,
			latLon = latLon,
			creationTimeMs = creationTimeMs
		)
	}

	private fun extractCreationTime(item: MediaItem, file: File?, contentUri: Uri?): Long? =
		when (item.type) {
			MediaType.PHOTO -> runCatching {
				file?.let { MediaMetadataUtils.getPhotoCreationTime(it) }
					?: contentUri?.let { uri ->
						app.contentResolver.openInputStream(uri)?.use { stream ->
							MediaMetadataUtils.getPhotoCreationTime(stream)
						}
					}
			}.getOrNull()
			MediaType.VIDEO, MediaType.AUDIO -> withRetriever(file, contentUri) {
				parseCreationTime(it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE))
			}
			else -> null
		}

	private fun extractLocation(item: MediaItem, file: File?, contentUri: Uri?): LatLon? {
		val location = when {
			file != null -> MediaMetadataUtils.getLocation(file)
			contentUri != null -> extractContentLocation(item, contentUri)
			else -> null
		}
		return location?.let { LatLon(it.latitude, it.longitude) }
	}

	private fun extractContentLocation(item: MediaItem, uri: Uri): Location? {
		val location = if (item.type == MediaType.PHOTO) {
			runCatching {
				app.contentResolver.openInputStream(uri)?.use {
					MediaMetadataUtils.getPhotoInformation(it)
				}
			}.getOrNull()
		} else {
			withRetriever(null, uri) {
				MediaMetadataUtils.parseMediaLocation(it.extractMetadata(METADATA_KEY_LOCATION))
			}
		}
		return location ?: MediaMetadataUtils.getLocationFromFileName(item.title)
	}

	private fun resolveFile(item: MediaItem): File? {
		val file = when (item) {
			is MediaItem.Internal -> MediaProvider.resolveInternalMediaFile(app.getAppPath().absolutePath, item.relativePath)
			is MediaItem.Gallery -> fileFromUriString(item.uri)
			else -> null
		}
		return file?.takeIf { it.exists() && it.isFile }
	}

	private fun fileFromUriString(value: String): File? = try {
		val uri = value.toUri()
		when (uri.scheme) {
			"file" -> uri.path?.let { File(it) }
			null -> File(value)
			else -> null
		}
	} catch (_: Exception) {
		null
	}

	private fun resolveContentUri(item: MediaItem): Uri? {
		val value = (item as? MediaItem.Gallery)?.uri ?: return null
		return try {
			value.toUri().takeIf { it.scheme == "content" }
		} catch (_: Exception) {
			null
		}
	}

	private fun queryContentLong(uri: Uri, column: String): Long? = try {
		app.contentResolver.query(uri, arrayOf(column), null, null, null)?.use { cursor ->
			val index = cursor.getColumnIndex(column)
			if (cursor.moveToFirst() && index >= 0 && !cursor.isNull(index)) {
				cursor.getLong(index).takeIf { it > 0 }
			} else {
				null
			}
		}
	} catch (_: Exception) {
		null
	}

	private fun parseCreationTime(value: String?): Long? {
		val date = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
		val position = ParsePosition(0)
		for (format in creationTimeFormats) {
			position.index = 0
			position.errorIndex = -1
			val time = format.parse(date, position)?.time
			if (position.index == date.length && time != null && time > 0) {
				return time
			}
		}
		return GpxUtilities.parseTime(date).takeIf { it > 0 }
	}

	private fun extractDuration(file: File?, contentUri: Uri?): Long? =
		withRetriever(file, contentUri) { retriever ->
			retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
		}

	private fun extractPoster(item: MediaItem): Bitmap? {
		val file = resolveFile(item)
		val contentUri = if (file == null) resolveContentUri(item) else null
		return withRetriever(file, contentUri) { retriever ->
			retriever.frameAtTime?.let { scaleDown(it) }
		}
	}

	private fun <T> withRetriever(
		file: File?,
		contentUri: Uri?,
		block: (MediaMetadataRetriever) -> T?
	): T? {
		if (file == null && contentUri == null) return null
		val retriever = MediaMetadataRetriever()
		return try {
			if (file != null) {
				retriever.setDataSource(file.absolutePath)
			} else {
				retriever.setDataSource(app, contentUri)
			}
			block(retriever)
		} catch (_: Exception) {
			null
		} finally {
			try {
				retriever.release()
			} catch (_: Exception) {
				// ignore
			}
		}
	}

	private fun scaleDown(frame: Bitmap): Bitmap {
		val minSide = minOf(frame.width, frame.height)
		if (minSide <= MAX_POSTER_MIN_SIDE_PX) return frame
		val scale = MAX_POSTER_MIN_SIDE_PX.toFloat() / minSide
		val scaled = frame.scale(
			(frame.width * scale).toInt().coerceAtLeast(1),
			(frame.height * scale).toInt().coerceAtLeast(1)
		)
		if (scaled != frame) frame.recycle()
		return scaled
	}

	companion object {
		private const val POSTER_CACHE_BYTES = 8 * 1024 * 1024
		private const val MAX_POSTER_MIN_SIDE_PX = 512
	}
}
