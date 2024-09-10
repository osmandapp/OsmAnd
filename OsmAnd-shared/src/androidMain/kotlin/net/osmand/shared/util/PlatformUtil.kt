package net.osmand.shared.util

import android.content.Context
import net.osmand.shared.KException
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.api.SQLiteAPIImpl
import java.lang.ref.WeakReference

actual object PlatformUtil {

	private lateinit var context: WeakReference<Context>
	private lateinit var osmAndContext: OsmAndContext
	private lateinit var sqliteApi: SQLiteAPI

	fun initialize(context: Context, osmAndContext: OsmAndContext) {
		this.context = WeakReference(context)
		this.osmAndContext = osmAndContext

		sqliteApi = SQLiteAPIImpl(context)
		Localization.initialize(context)
	}

	actual fun getOsmAndContext(): OsmAndContext = osmAndContext

	actual fun getSQLiteAPI(): SQLiteAPI = sqliteApi

	fun getKotlinException(e: java.lang.Exception): KException = KException(e.message, e)

	fun getJavaException(e: KException): java.lang.Exception = Exception(e.message, e)
}