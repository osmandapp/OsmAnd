package net.osmand.shared.api

import net.osmand.shared.io.KFile
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.shared.settings.enums.SpeedConstants
import net.osmand.shared.util.KStringMatcher

interface OsmAndContext {
	fun getAppDir(): KFile
	fun getGpxDir(): KFile
	fun getGpxImportDir(): KFile
	fun getGpxRecordedDir(): KFile

	fun getSettings(): SettingsAPI
	fun getSpeedSystem(): SpeedConstants?
	fun getMetricSystem(): MetricsConstants?

	fun isGpxFileVisible(path: String): Boolean

	fun getNameStringMatcher(name: String, mode: KStringMatcherMode): KStringMatcher
}