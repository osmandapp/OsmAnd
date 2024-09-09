package net.osmand.shared.util

import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SQLiteAPI

expect object PlatformUtil {

	fun getSQLiteAPI(): SQLiteAPI

	fun getOsmAndContext(): OsmAndContext
}