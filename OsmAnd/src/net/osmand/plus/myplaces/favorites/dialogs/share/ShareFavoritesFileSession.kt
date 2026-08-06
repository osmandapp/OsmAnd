package net.osmand.plus.myplaces.favorites.dialogs.share

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

	private fun getOrCreateDirectory(directory: File): File {
		if ((!directory.exists() && !directory.mkdirs()) || !directory.isDirectory) {
			throw IllegalStateException("Could not create share directory: ${directory.absolutePath}")
		}
		return directory
	}
}
