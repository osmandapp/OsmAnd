package net.osmand.plus.myplaces.favorites.dialogs.share

import net.osmand.PlatformUtil
import net.osmand.plus.OsmandApplication
import net.osmand.plus.utils.FileUtils
import net.osmand.util.Algorithms
import java.io.File
import java.util.UUID

class ShareFavoritesFileSession(app: OsmandApplication) {

	private val sessionId = UUID.randomUUID().toString()
	private val gpxDirectory = File(File(app.cacheDir, "share"), sessionId)
	private val osfDirectory = File(File(FileUtils.getTempDir(app), "share_favorites"), sessionId)

	fun createGpxDestination(fileName: String): File {
		return File(getOrCreateDirectory(gpxDirectory), fileName)
	}

	fun getOrCreateOsfDirectory(): File {
		return getOrCreateDirectory(osfDirectory)
	}

	fun cleanup() {
		Algorithms.removeAllFiles(gpxDirectory)
		Algorithms.removeAllFiles(osfDirectory)
	}

	fun cleanupOsf() {
		Algorithms.removeAllFiles(osfDirectory)
	}

	fun cleanupExpiredGpxSessions() {
		val expirationTime = System.currentTimeMillis() - GPX_SESSION_MAX_AGE_MS
		gpxDirectory.parentFile?.listFiles()?.forEach { directory ->
			if (directory.isDirectory && directory.lastModified() < expirationTime && isGpxSessionDirectory(directory)) {
				if (!Algorithms.removeAllFiles(directory)) {
					LOG.warn("Failed to remove expired Favorites share session: ${directory.absolutePath}")
				}
			}
		}
	}

	private fun getOrCreateDirectory(directory: File): File {
		if ((!directory.exists() && !directory.mkdirs()) || !directory.isDirectory) {
			throw IllegalStateException("Could not create share directory: ${directory.absolutePath}")
		}
		return directory
	}

	private fun isGpxSessionDirectory(directory: File): Boolean {
		return try {
			UUID.fromString(directory.name)
			true
		} catch (e: IllegalArgumentException) {
			false
		}
	}

	companion object {
		private const val GPX_SESSION_MAX_AGE_MS = 24 * 60 * 60 * 1000L // 24 hours

		private val LOG = PlatformUtil.getLog(ShareFavoritesFileSession::class.java)
	}
}
