package net.osmand.plus.activities

import net.osmand.PlatformUtil
import net.osmand.apkdelta.ApkArchive
import net.osmand.apkdelta.ApkDeltaApplier
import net.osmand.osm.io.NetworkUtils
import net.osmand.plus.OsmandApplication
import net.osmand.plus.utils.FileUtils
import net.osmand.util.Algorithms
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLEncoder

/**
 * The part of a build download that knows about the difference to the installed APK: the sizes
 * and the request to the server, and building the new APK byte for byte from the delta.
 */
class BuildDeltaDownloader(private val app: OsmandApplication) {

	/** [exact] is false while the server still has to build the delta and [size] is its estimate. */
	class DeltaSize(val size: Long, val exact: Boolean)

	val deltaFile: File get() = File(FileUtils.getTempDir(app), DELTA_NAME)

	/** What the builds would take instead of the whole APK. Only decorates the list, so it never fails. */
	fun fetchSizes(paths: List<String>): Map<String, DeltaSize> {
		if (installedApk() == null || paths.isEmpty()) {
			return emptyMap()
		}
		try {
			val query = paths.joinToString("&") { "path=" + URLEncoder.encode(it, "UTF-8") }
			val connection = NetworkUtils.getHttpURLConnection("$URL_DELTA/sizes?$query")
			try {
				connection.connectTimeout = BuildsManager.CONNECT_TIMEOUT_MS
				connection.readTimeout = BuildsManager.READ_TIMEOUT_MS
				sendManifest(connection)
				if (connection.responseCode != HttpURLConnection.HTTP_OK) {
					return emptyMap()
				}
				val sizes = JSONArray(Algorithms.readFromInputStream(connection.inputStream).toString())
				return (0 until sizes.length()).map { sizes.getJSONObject(it) }.associate {
					it.getString("path") to DeltaSize(it.getLong("size"), it.getBoolean("exact"))
				}
			} finally {
				connection.disconnect()
			}
		} catch (e: Exception) {
			LOG.warn("No delta sizes: ${e.message}")
			return emptyMap()
		}
	}

	/** Nothing is sent yet, so the caller can stop the connection during [sendManifest] as well. */
	fun openDeltaConnection(build: OsmAndBuild): HttpURLConnection {
		val path = URLEncoder.encode(build.path, "UTF-8")
		val connection = NetworkUtils.getHttpURLConnection("$URL_DELTA?path=$path")
		connection.connectTimeout = BuildsManager.CONNECT_TIMEOUT_MS
		// the first phone after a new build waits for the server to make the patches
		connection.readTimeout = BUILD_TIMEOUT_MS
		// Content-Length must describe the file itself, not a gzip of it
		connection.setRequestProperty("Accept-Encoding", "identity")
		return connection
	}

	fun sendManifest(connection: HttpURLConnection) {
		val manifest = ApkArchive.writeManifest(ApkArchive.readEntries(requireInstalledApk()))
		connection.requestMethod = "POST"
		connection.doOutput = true
		connection.setFixedLengthStreamingMode(manifest.size)
		connection.setRequestProperty("Content-Type", "application/octet-stream")
		connection.outputStream.use { it.write(manifest) }
	}

	fun buildApk(delta: File, target: File, cancellation: ApkDeltaApplier.Cancellation) {
		ApkDeltaApplier.apply(requireInstalledApk(), delta, target, FileUtils.getTempDir(app), cancellation)
	}

	private fun requireInstalledApk(): File = installedApk() ?: throw IOException("No installed APK to start from")

	/** A Play install is split into several APKs, there the build would not be a delta of one. */
	private fun installedApk(): File? {
		val info = app.applicationInfo
		return if (info.splitSourceDirs.isNullOrEmpty()) File(info.sourceDir).takeIf { it.canRead() } else null
	}

	companion object {
		private val LOG = PlatformUtil.getLog(BuildDeltaDownloader::class.java)

		private const val URL_DELTA = "https://osmand.net/api/builds/delta"
		private const val DELTA_NAME = "osmandToInstall.delta"
		private const val BUILD_TIMEOUT_MS = 120_000
	}
}
