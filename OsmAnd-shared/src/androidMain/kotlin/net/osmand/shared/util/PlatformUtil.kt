package net.osmand.shared.util

import android.content.Context
import net.osmand.shared.KException
import net.osmand.shared.api.NetworkAPIImpl
import net.osmand.shared.api.NetworkAPI
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.api.SQLiteAPIImpl
import net.osmand.shared.gpx.GpxTrackAnalysis.TrackPointsAnalyser
import java.lang.ref.WeakReference

actual object PlatformUtil {

	private lateinit var context: WeakReference<Context>
	private lateinit var osmAndContext: OsmAndContext
	private lateinit var sqliteApi: SQLiteAPI
	private lateinit var networkAPI: NetworkAPI

	fun initialize(context: Context, osmAndContext: OsmAndContext) {
		this.context = WeakReference(context)
		this.osmAndContext = osmAndContext
		this.networkAPI = NetworkAPIImpl()

		sqliteApi = SQLiteAPIImpl(context)
		Localization.initialize(context)
	}

	actual fun getOsmAndContext(): OsmAndContext = osmAndContext

	actual fun getSQLiteAPI(): SQLiteAPI = sqliteApi

	actual fun getNetworkAPI(): NetworkAPI = networkAPI

	actual fun getTrackPointsAnalyser(): TrackPointsAnalyser? = osmAndContext.getTrackPointsAnalyser()

	fun getKotlinException(e: java.lang.Exception): KException = KException(e.message, e)

	fun getJavaException(e: KException): java.lang.Exception = Exception(e.message, e)
}