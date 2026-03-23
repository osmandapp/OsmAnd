package net.osmand.shared.util

import net.osmand.shared.api.NetworkAPI
import net.osmand.shared.api.NetworkAPIImpl
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.api.SQLiteAPIImpl
import net.osmand.shared.api.XmlFactoryAPI
import net.osmand.shared.api.XmlPullParserAPI
import net.osmand.shared.api.XmlSerializerAPI
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.gpx.GpxTrackAnalysis.TrackPointsAnalyser
import net.osmand.shared.gpx.PointAttributes
import net.osmand.shared.gpx.primitives.WptPt

actual object PlatformUtil {

	private lateinit var osmAndContext: OsmAndContext
	private lateinit var sqliteApi: SQLiteAPI
	private lateinit var networkAPI: NetworkAPI
	private lateinit var xmlFactoryApi: XmlFactoryAPI

	fun initialize(osmAndContext: OsmAndContext, xmlFactoryApi: XmlFactoryAPI, networkAPI: NetworkAPI) {
		this.osmAndContext = osmAndContext
		this.sqliteApi = SQLiteAPIImpl()
		this.networkAPI = networkAPI
		this.xmlFactoryApi = xmlFactoryApi
	}

	actual fun getOsmAndContext(): OsmAndContext = osmAndContext

	actual fun getSQLiteAPI(): SQLiteAPI = sqliteApi

	actual fun getNetworkAPI(): NetworkAPI = networkAPI

	actual fun getTrackPointsAnalyser(): TrackPointsAnalyser? {
		val contextTrackPointsAnalyser = osmAndContext.getTrackPointsAnalyser()
		return object : TrackPointsAnalyser {
			override fun onAnalysePoint(
				analysis: GpxTrackAnalysis, point: WptPt, attribute: PointAttributes
			) {
				contextTrackPointsAnalyser?.onAnalysePoint(analysis, point, attribute)
				SensorPointAnalyser.onAnalysePoint(analysis, point, attribute)
			}
		}
	}

	fun getXmlPullParserApi(): XmlPullParserAPI = xmlFactoryApi.createXmlPullParserApi()

	fun getXmlSerializerApi(): XmlSerializerAPI = xmlFactoryApi.createXmlSerializerApi()
}