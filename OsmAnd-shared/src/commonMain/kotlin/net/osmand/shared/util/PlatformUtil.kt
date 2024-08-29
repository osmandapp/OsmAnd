package net.osmand.shared.util

import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.io.KFile

expect object PlatformUtil {
	fun currentTimeMillis(): Long

	fun getAppDir(): KFile

	fun getGpxDir(): KFile

	fun getSQLiteAPI(): SQLiteAPI
}