package net.osmand.shared.db

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper.Callback
import androidx.sqlite.db.SupportSQLiteOpenHelper.Configuration
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.squareup.sqldelight.android.AndroidSqliteDriver
import com.squareup.sqldelight.db.SqlDriver

actual class DatabaseDriverFactory(
	private val context: Context,
	private val name: String,
	private val version: Int
) {
	actual fun createDriver(): SqlDriver {
		val callback = object: Callback(version) {
			override fun onCreate(db: SupportSQLiteDatabase) {
			}

			override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {
			}
		}
		return AndroidSqliteDriver(
			FrameworkSQLiteOpenHelperFactory().create(
				Configuration.builder(context)
					.callback(callback)
					.name(name)
					.noBackupDirectory(false)
					.build()
			)
		)
	}
}