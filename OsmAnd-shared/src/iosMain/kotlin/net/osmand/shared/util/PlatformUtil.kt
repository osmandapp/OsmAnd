package net.osmand.shared.util

import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.api.SQLiteAPIImpl
import net.osmand.shared.api.XmlFactoryAPI
import net.osmand.shared.api.XmlPullParserAPI
import net.osmand.shared.api.XmlSerializerAPI

actual object PlatformUtil {

	private lateinit var osmAndContext: OsmAndContext
	private lateinit var sqliteApi: SQLiteAPI
	private lateinit var xmlFactoryApi: XmlFactoryAPI

	fun initialize(osmAndContext: OsmAndContext, xmlFactoryApi: XmlFactoryAPI) {
		this.osmAndContext = osmAndContext
		this.sqliteApi = SQLiteAPIImpl()
		this.xmlFactoryApi = xmlFactoryApi
	}

	actual fun getOsmAndContext(): OsmAndContext = osmAndContext

	actual fun getSQLiteAPI(): SQLiteAPI = sqliteApi

	fun getXmlPullParserApi(): XmlPullParserAPI = xmlFactoryApi.createXmlPullParserApi()

	fun getXmlSerializerApi(): XmlSerializerAPI = xmlFactoryApi.createXmlSerializerApi()
}