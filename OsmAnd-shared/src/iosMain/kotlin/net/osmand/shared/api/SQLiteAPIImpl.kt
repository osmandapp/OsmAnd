package net.osmand.shared.api

import kotlinx.cinterop.ExperimentalForeignApi
import net.osmand.shared.api.SQLiteAPI.SQLiteConnection
import net.osmand.shared.util.LoggerFactory
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask

class SQLiteAPIImpl : SQLiteAPI {

	companion object {
		private val log = LoggerFactory.getLogger("SQLiteAPIImpl")
	}

	override fun getOrCreateDatabase(name: String, readOnly: Boolean): SQLiteConnection? {
		return try {
			SQLiteConnectionImpl.openReadWrite("${databasesDir()}/$name")
		} catch (e: Exception) {
			log.error("Failed to get or create database $name", e)
			null
		}
	}

	override fun openByAbsolutePath(path: String, readOnly: Boolean): SQLiteConnection? {
		return try {
			if (readOnly) SQLiteConnectionImpl.openReadOnly(path) else SQLiteConnectionImpl.openReadWrite(path)
		} catch (e: Exception) {
			log.error("Failed to open database by path: $path readOnly=$readOnly", e)
			null
		}
	}

	// existing installs keep their databases here
	@OptIn(ExperimentalForeignApi::class)
	private fun databasesDir(): String {
		val appSupport = NSSearchPathForDirectoriesInDomains(NSApplicationSupportDirectory, NSUserDomainMask, true)[0] as String
		val dir = "$appSupport/databases"
		val fileManager = NSFileManager.defaultManager
		if (!fileManager.fileExistsAtPath(dir)) {
			fileManager.createDirectoryAtPath(dir, true, null, null)
		}
		return dir
	}
}
