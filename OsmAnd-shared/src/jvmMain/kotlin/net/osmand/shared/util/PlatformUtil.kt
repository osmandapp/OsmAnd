package net.osmand.shared.util

import net.osmand.shared.KException
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.api.SQLiteAPIImpl
import net.osmand.shared.gpx.GpxTrackAnalysis.TrackPointsAnalyser

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

	actual fun getTrackPointsAnalyser(): TrackPointsAnalyser? = osmAndContext.getTrackPointsAnalyser()

	fun getKotlinException(e: java.lang.Exception): KException = KException(e.message, e)

	fun getJavaException(e: KException): java.lang.Exception = Exception(e.message, e)
}