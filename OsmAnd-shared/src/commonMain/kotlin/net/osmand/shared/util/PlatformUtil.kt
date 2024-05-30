package net.osmand.shared.util

import net.osmand.shared.db.SQLiteAPI
import net.osmand.shared.io.CommonFile

expect object PlatformUtil {
	fun currentTimeMillis(): Long

	fun getAppDir(): CommonFile

	fun getGpxDir(): CommonFile

	fun getSQLiteAPI(): SQLiteAPI
}