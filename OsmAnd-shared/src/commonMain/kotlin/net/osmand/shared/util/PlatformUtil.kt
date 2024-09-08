package net.osmand.shared.util

import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SQLiteAPI
import net.osmand.shared.io.KFile

expect object PlatformUtil {

	fun getSQLiteAPI(): SQLiteAPI

	fun getOsmAndContext(): OsmAndContext
}