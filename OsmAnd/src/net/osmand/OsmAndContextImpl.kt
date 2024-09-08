package net.osmand

import net.osmand.IndexConstants.GPX_IMPORT_DIR
import net.osmand.IndexConstants.GPX_INDEX_DIR
import net.osmand.IndexConstants.GPX_RECORDED_INDEX_DIR
import net.osmand.plus.OsmandApplication
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.io.KFile
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.shared.settings.enums.SpeedConstants
import net.osmand.shared.util.KStringMatcher

class OsmAndContextImpl(private val app: OsmandApplication) : OsmAndContext {
	private val settings: SettingsAPIImpl = SettingsAPIImpl(app)

	override fun getAppDir(): KFile = app.getAppPathKt(null)

	override fun getGpxDir(): KFile = app.getAppPathKt(GPX_INDEX_DIR)

	override fun getGpxImportDir(): KFile = app.getAppPathKt(GPX_IMPORT_DIR)

	override fun getGpxRecordedDir(): KFile = app.getAppPathKt(GPX_RECORDED_INDEX_DIR)

	override fun getSettings(): SettingsAPI = settings

	override fun getSpeedSystem(): SpeedConstants? = app.settings.SPEED_SYSTEM.get()

	override fun getMetricSystem(): MetricsConstants? = app.settings.METRIC_SYSTEM.get()

	override fun isGpxFileVisible(path: String): Boolean {
		val helper = app.selectedGpxHelper
		helper.getSelectedFileByPath(path) != null
		return false
	}

	override fun getNameStringMatcher(name: String): KStringMatcher {
		return object : KStringMatcher {
			private val sm: CollatorStringMatcher =
				CollatorStringMatcher(name, CollatorStringMatcher.StringMatcherMode.CHECK_CONTAINS)

			override fun matches(name: String): Boolean {
				return sm.matches(name)
			}
		}
	}
}