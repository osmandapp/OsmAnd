package net.osmand.shared.util

import net.osmand.shared.KException
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.api.SQLiteAPIImpl

actual object PlatformUtil {

	private lateinit var osmAndContext: OsmAndContext
	private lateinit var sqliteApi: SQLiteAPI

	fun initialize(osmAndContext: OsmAndContext) {
		this.osmAndContext = osmAndContext
		this.sqliteApi = SQLiteAPIImpl()

		Localization.initialize()
	}

	actual fun getOsmAndContext(): OsmAndContext = osmAndContext

	actual fun getSQLiteAPI(): SQLiteAPI = sqliteApi

	fun getKotlinException(e: java.lang.Exception): KException = KException(e.message, e)

	fun getJavaException(e: KException): java.lang.Exception = Exception(e.message, e)
}