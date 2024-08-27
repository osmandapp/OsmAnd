package net.osmand.shared.util

import kotlinx.datetime.Instant
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.io.KFile

expect object PlatformUtil {
	fun currentTimeMillis(): Long

	fun getAppDir(): KFile

	fun getGpxDir(): KFile

	fun getSQLiteAPI(): SQLiteAPI

	fun getStringResource(stringId: String): String

	fun formatDate(date: Instant, pattern: String):String

	fun getOsmAndContext(): OsmAndContext

	fun getFileSeparator(): String

	fun getFileLength(file: KFile): Long

	fun renameFile(src: String, dest: String): Boolean

	fun createFile(path: String): Boolean
}