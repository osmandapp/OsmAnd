package net.osmand.plus.myplaces.favorites

import net.osmand.PlatformUtil
import net.osmand.data.FavouritePoint
import net.osmand.plus.OsmandApplication
import net.osmand.plus.myplaces.favorites.FavouritesFileHelper.FAV_FILE_PREFIX
import net.osmand.shared.IndexConstants.TMP_FILE_EXT
import java.io.BufferedWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

/**
 * Lightweight journal for favorite deletions.
 *
 * Favorites are saved asynchronously, so the app may be killed before deleted items are
 * removed from all GPX files. This journal stores pending point/group deletions and applies
 * them on the next load, preventing deleted favorites from being restored from stale files.
 *
 * The journal is cleared only after a successful full save if it was not changed meanwhile.
 */
object FavoriteDeletionsJournal {

	private val log = PlatformUtil.getLog(FavoriteDeletionsJournal::class.java)

	private const val PENDING_DELETIONS_SUFFIX = "_deletions"
	private const val PREFIX_POINT = "point:"
	private const val PREFIX_GROUP = "group:"

	private val lock = Any()

	private fun getFile(app: OsmandApplication): File {
		return app.getFileStreamPath(FAV_FILE_PREFIX + PENDING_DELETIONS_SUFFIX + TMP_FILE_EXT)
	}

	@JvmStatic
	fun addPoint(
		app: OsmandApplication,
		point: FavouritePoint?
	) {
		if (point != null) {
			addAll(app, listOf(point), null)
		}
	}

	@JvmStatic
	fun addGroup(
		app: OsmandApplication,
		group: FavoriteGroup?
	) {
		if (group != null) {
			addAll(app, null, listOf(group))
		}
	}

	@JvmStatic
	fun addAll(
		app: OsmandApplication,
		points: Collection<FavouritePoint>?,
		groups: Collection<FavoriteGroup>?
	) {
		if (!points.isNullOrEmpty() || !groups.isNullOrEmpty()) {
			appendPendingDeletionLines(app, points, groups)
		}
	}

	@JvmStatic
	fun read(app: OsmandApplication): ReadResult {
		synchronized(lock) {
			val file = getFile(app)
			val deletions = FavoritePendingDeletions()

			if (file.exists()) {
				try {
					file.bufferedReader(Charsets.UTF_8, 8192).useLines { lines ->
						lines.forEach { line ->
							if (line.isNotEmpty()) {
								deserializeLine(line, deletions)
							}
						}
					}
				} catch (e: IOException) {
					log.error("Failed to read favorite deletions journal", e)
					return ReadResult(deletions, null, readFailed = true)
				}
			}

			return ReadResult(deletions, getState(file), readFailed = false)
		}
	}

	@JvmStatic
	fun clearIfUnchanged(app: OsmandApplication, expectedState: JournalState): Boolean {
		synchronized(lock) {
			val file = getFile(app)
			val currentState = getState(file)

			if (currentState != expectedState) {
				return false
			}

			if (file.exists() && !file.delete()) {
				log.warn("Failed to clear favorite deletions journal: ${file.absolutePath}")
				return false
			}
			return true
		}
	}

	private fun appendPendingDeletionLines(app: OsmandApplication, points: Collection<FavouritePoint>?, groups: Collection<FavoriteGroup>?) {
		synchronized(lock) {
			val file = getFile(app)

			try {
				FileOutputStream(file, true).use { fos ->
					BufferedWriter(OutputStreamWriter(fos, Charsets.UTF_8), 8192).use { writer ->
						points?.forEach {
							writer.write(serializePoint(it.key))
							writer.newLine()
						}
						groups?.forEach {
							writer.write(serializeGroup(it.name))
							writer.newLine()
						}
						writer.flush()
					}
				}
			} catch (e: IOException) {
				log.error("appendPendingDeletionLines failed", e)
			}
		}
	}

	private fun serializePoint(pointKey: String): String {
		return "$PREFIX_POINT$pointKey"
	}

	private fun serializeGroup(groupName: String): String {
		return "$PREFIX_GROUP$groupName"
	}

	private fun deserializeLine(line: String, deletions: FavoritePendingDeletions) {
		when {
			line.startsWith(PREFIX_POINT) -> {
				deletions.addPoint(line.removePrefix(PREFIX_POINT))
			}

			line.startsWith(PREFIX_GROUP) -> {
				deletions.addGroup(line.removePrefix(PREFIX_GROUP))
			}
		}
	}

	private fun getState(file: File): JournalState = if (file.exists()) JournalState(file.length(), file.lastModified()) else JournalState(0L, 0L)

	data class ReadResult(
		val deletions: FavoritePendingDeletions,
		val state: JournalState?,
		val readFailed: Boolean
	)

	data class JournalState(
		val length: Long,
		val timestamp: Long
	)
}