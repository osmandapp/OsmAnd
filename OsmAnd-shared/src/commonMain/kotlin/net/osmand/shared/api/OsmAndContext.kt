package net.osmand.shared.api

import net.osmand.shared.data.SpeedConstants
import net.osmand.shared.filters.KMetricsConstants

interface OsmAndContext {
	fun isGpxFileVisible(path: String): Boolean
	fun getSpeedSystem(): SpeedConstants?
	fun getMetricSystem(): KMetricsConstants?
}