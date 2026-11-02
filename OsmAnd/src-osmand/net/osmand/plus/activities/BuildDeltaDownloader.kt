package net.osmand.plus.activities

import net.osmand.IndexConstants
import net.osmand.PlatformUtil
import net.osmand.apkdelta.ApkArchive
import net.osmand.apkdelta.ApkDeltaApplier
import net.osmand.osm.io.NetworkUtils
import net.osmand.plus.OsmandApplication
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.net.HttpURLConnection
import java.net.URLEncoder

/**
 * Gets a build as a difference to the installed APK: sends the list of what is inside that APK,
 * downloads the delta and builds the new APK here, byte for byte. Any failure means false and the
 * caller downloads the whole build.
 */
class BuildDeltaDownloader(private val app: OsmandApplication) {

	/** [exact] is false while the server still has to build the delta and [size] is its estimate. */
	class DeltaSize(val size: Long, val exact: Boolean)

	interface Callback {
		fun onConnection(connection: HttpURLConnection?)
		fun onProgress(done: Long, total: Long)
		fun isCancelled(): Boolean
	}

	fun download(build: OsmAndBuild, target: File, callback: Callback): Boolean {
		val installedApk = installedApk() ?: return false
		val tempDir = app.getAppPath(IndexConstants.TEMP_DIR)
		tempDir.mkdirs()
		val delta = File(tempDir, DELTA_NAME)
		val builtApk = File(tempDir, BUILT_APK_NAME)
		try {
			val key = requestDelta(build, installedApk, callback) ?: return false
			downloadDelta(key, delta, callback)
			ApkDeltaApplier.apply(installedApk, delta, builtApk, tempDir, object : ApkDeltaApplier.Progress {
				override fun onProgress(written: Long, total: Long) = callback.onProgress(written, total)
				override fun isCancelled() = callback.isCancelled()
			})
			target.delete()
			return builtApk.renameTo(target)
		} catch (e: IOException) {
			LOG.warn("No delta for ${build.path}: ${e.message}")
		} catch (e: JSONException) {
			LOG.warn("No delta for ${build.path}: ${e.message}")
		} finally {
			callback.onConnection(null)
			delta.delete()
			builtApk.delete()
		}
		return false
	}

	/** What the newest builds would take instead of the whole APK, by the path of the build. */
	fun fetchSizes(): Map<String, DeltaSize> {
		val installedApk = installedApk() ?: return emptyMap()
		val connection = NetworkUtils.getHttpURLConnection("$URL_DELTA/sizes")
		try {
			connection.connectTimeout = CONNECT_TIMEOUT_MS
			connection.readTimeout = READ_TIMEOUT_MS
			if (!post(connection, manifest(installedApk))) {
				return emptyMap()
			}
			val sizes = JSONArray(readBody(connection))
			val result = HashMap<String, DeltaSize>()
			for (i in 0 until sizes.length()) {
				val size = sizes.getJSONObject(i)
				result[size.getString("path")] = DeltaSize(size.getLong("size"), size.getBoolean("exact"))
			}
			return result
		} catch (e: IOException) {
			LOG.warn("No delta sizes: ${e.message}")
		} catch (e: JSONException) {
			LOG.warn("No delta sizes: ${e.message}")
		} finally {
			connection.disconnect()
		}
		return emptyMap()
	}

	/** A Play install is split into several APKs, there the build would not be a delta of one. */
	private fun installedApk(): File? {
		val info = app.applicationInfo
		if (!info.splitSourceDirs.isNullOrEmpty()) {
			return null
		}
		return File(info.sourceDir).takeIf { it.canRead() }
	}

	private fun requestDelta(build: OsmAndBuild, installedApk: File, callback: Callback): String? {
		val url = "$URL_DELTA?path=" + URLEncoder.encode(build.path, "UTF-8")
		val connection = NetworkUtils.getHttpURLConnection(url)
		callback.onConnection(connection)
		try {
			connection.connectTimeout = CONNECT_TIMEOUT_MS
			// the first phone after a new build waits for the server to make the patches
			connection.readTimeout = BUILD_TIMEOUT_MS
			return if (post(connection, manifest(installedApk))) {
				JSONObject(readBody(connection)).getString("key")
			} else {
				null
			}
		} finally {
			connection.disconnect()
		}
	}

	private fun manifest(installedApk: File): ByteArray =
		RandomAccessFile(installedApk, "r").use { ApkArchive.writeManifest(ApkArchive.readEntries(it)) }

	private fun post(connection: HttpURLConnection, body: ByteArray): Boolean {
		connection.requestMethod = "POST"
		connection.doOutput = true
		connection.setFixedLengthStreamingMode(body.size)
		connection.setRequestProperty("Content-Type", "application/octet-stream")
		connection.outputStream.use { it.write(body) }
		return connection.responseCode == HttpURLConnection.HTTP_OK
	}

	private fun readBody(connection: HttpURLConnection): String =
		connection.inputStream.use { it.readBytes() }.toString(Charsets.UTF_8)

	private fun downloadDelta(key: String, delta: File, callback: Callback) {
		val connection = NetworkUtils.getHttpURLConnection("$URL_DELTA/$key")
		callback.onConnection(connection)
		try {
			connection.connectTimeout = CONNECT_TIMEOUT_MS
			connection.readTimeout = READ_TIMEOUT_MS
			// Content-Length must describe the file itself, not a gzip of it
			connection.setRequestProperty("Accept-Encoding", "identity")
			if (connection.responseCode != HttpURLConnection.HTTP_OK) {
				throw IOException("HTTP ${connection.responseCode}")
			}
			val total = connection.contentLengthLong
			var downloaded = 0L
			connection.inputStream.use { input ->
				FileOutputStream(delta).use { output ->
					val buffer = ByteArray(BUFFER_SIZE)
					while (true) {
						if (callback.isCancelled()) {
							throw IOException("Cancelled")
						}
						val read = input.read(buffer)
						if (read == -1) {
							break
						}
						output.write(buffer, 0, read)
						downloaded += read
						callback.onProgress(downloaded, total)
					}
				}
			}
			if (total > 0 && downloaded != total) {
				throw IOException("Incomplete delta: $downloaded of $total bytes")
			}
		} finally {
			connection.disconnect()
		}
	}

	companion object {
		private val LOG = PlatformUtil.getLog(BuildDeltaDownloader::class.java)

		private const val URL_DELTA = "https://osmand.net/api/builds/delta"
		private const val DELTA_NAME = "osmandToInstall.delta"
		private const val BUILT_APK_NAME = "osmandToInstall.built.apk"
		private const val CONNECT_TIMEOUT_MS = 15_000
		private const val READ_TIMEOUT_MS = 30_000
		private const val BUILD_TIMEOUT_MS = 300_000
		private const val BUFFER_SIZE = 64 * 1024
	}
}
