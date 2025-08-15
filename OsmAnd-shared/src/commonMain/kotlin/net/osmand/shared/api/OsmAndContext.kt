package net.osmand.shared.api

import net.osmand.shared.data.KLatLon
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxTrackAnalysis
import net.osmand.shared.gpx.GpxTrackAnalysis.TrackPointsAnalyser
import net.osmand.shared.io.KFile
import net.osmand.shared.settings.enums.AltitudeMetrics
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.shared.settings.enums.SpeedConstants
import net.osmand.shared.util.KStringMatcher

typealias CityNameCallback = (String) -> Unit

interface OsmAndContext {
	fun getAppDir(): KFile
	fun getGpxDir(): KFile
	fun getGpxImportDir(): KFile
	fun getGpxRecordedDir(): KFile

	fun getSettings(): SettingsAPI
	fun getSpeedSystem(): SpeedConstants?
	fun getMetricSystem(): MetricsConstants?
	fun getAltitudeMetric(): AltitudeMetrics?

	fun isGpxFileVisible(path: String): Boolean
	fun getSelectedFileByPath(path: String): GpxFile?
	fun getNameStringMatcher(name: String, mode: KStringMatcherMode): KStringMatcher
	fun getTrackPointsAnalyser(): TrackPointsAnalyser?
	fun getAssetAsString(name: String): String?
	fun searchNearestCityName(latLon: KLatLon, callback: CityNameCallback)
}