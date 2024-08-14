package net.osmand.shared.db

import co.touchlab.sqliter.DatabaseConfiguration
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.drivers.native.NativeSqliteDriver

actual class DatabaseDriverFactory(private val dbName: String,
                                   private val dbPath: String,
                                   private val version: Int) {
	actual fun createDriver(): SqlDriver {
		return NativeSqliteDriver(
			DatabaseConfiguration(
				name = dbName,
				version = version,
				create = {},
				extendedConfig = DatabaseConfiguration.Extended(basePath = dbPath))
		)
	}
}