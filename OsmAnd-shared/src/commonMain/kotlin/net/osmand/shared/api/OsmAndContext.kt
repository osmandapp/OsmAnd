package net.osmand.shared.api

import net.osmand.shared.data.SpeedConstants
import net.osmand.shared.filters.KMetricsConstants
import net.osmand.shared.util.KStringMatcher

interface OsmAndContext {
	fun isGpxFileVisible(path: String): Boolean
	fun getSpeedSystem(): SpeedConstants?
	fun getMetricSystem(): KMetricsConstants?
	fun getNameStringMatcher(name: String): KStringMatcher
	fun getSettings(): KOsmAndSettings
}