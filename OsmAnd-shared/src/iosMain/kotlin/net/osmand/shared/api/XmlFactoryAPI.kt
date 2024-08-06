package net.osmand.shared.api

interface XmlFactoryAPI {

	fun createXmlPullParserApi(): XmlPullParserAPI

	fun createXmlSerializerApi(): XmlSerializerAPI
}