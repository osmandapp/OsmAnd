package net.osmand.shared

import net.osmand.shared.api.CityNameCallback
import net.osmand.shared.api.KStateChangedListener
import net.osmand.shared.api.KStringMatcherMode
import net.osmand.shared.api.OsmAndContext
import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.data.KLatLon
import net.osmand.shared.gpx.GpxDataItem
import net.osmand.shared.gpx.GpxFile
import net.osmand.shared.gpx.GpxParameter
import net.osmand.shared.gpx.GpxTrackAnalysis.TrackPointsAnalyser
import net.osmand.shared.gpx.GpxUtilities
import net.osmand.shared.gpx.GpxUtilities.PointsGroup
import net.osmand.shared.gpx.SmartFolderHelper
import net.osmand.shared.gpx.primitives.WptPt
import net.osmand.shared.io.KFile
import net.osmand.shared.settings.enums.AltitudeMetrics
import net.osmand.shared.settings.enums.AngularConstants
import net.osmand.shared.settings.enums.MetricsConstants
import net.osmand.shared.settings.enums.SpeedConstants
import net.osmand.shared.units.TemperatureUnits
import net.osmand.shared.util.KStringMatcher
import net.osmand.shared.util.PlatformUtil
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GpxDataItemTest {

	@Test
	fun readGpxParamsStoresPointsGroupsParameter() {
		PlatformUtil.initialize(TestOsmAndContext())

		val gpxFile = GpxFile(null)
		val point = WptPt(10.0, 20.0).apply {
			name = "Cafe"
			category = "Food"
		}
		val group = PointsGroup(
			"Food",
			"restaurant",
			"circle",
			GpxUtilities.parseColor("#ff0000", 0) ?: 0,
			hidden = true,
			pinned = false
		)
		group.points.add(point)
		gpxFile.addPointsGroup(group)

		val item = GpxDataItem(KFile("/tmp/osmand-test/gpx/imported.gpx"))
		item.readGpxParams(gpxFile)

		val storedPointsGroups = assertNotNull(item.getParameter<String>(GpxParameter.POINTS_GROUPS))
		val storedGroup = assertNotNull(GpxUtilities.parsePointsGroups(storedPointsGroups)["Food"])
		assertEquals("restaurant", storedGroup.iconName)
		assertEquals("circle", storedGroup.backgroundType)
		assertTrue(storedGroup.isHidden())
		assertEquals(false, storedGroup.isPinned())
	}

	private class TestOsmAndContext : OsmAndContext {
		private val root = KFile("/tmp/osmand-test")
		private val gpxDir = KFile("/tmp/osmand-test/gpx")
		private val smartFoldHelper by lazy { SmartFolderHelper() }

		override fun getAppDir(): KFile = root
		override fun getCacheDir(): KFile = KFile("/tmp/osmand-test/cache")
		override fun getGpxDir(): KFile = gpxDir
		override fun getGpxImportDir(): KFile = KFile("/tmp/osmand-test/gpx/import")
		override fun getGpxRecordedDir(): KFile = KFile("/tmp/osmand-test/gpx/rec")
		override fun getColorPaletteDir(): KFile = KFile("/tmp/osmand-test/color-palettes")
		override fun getSettings(): SettingsAPI = TestSettingsApi
		override fun getSpeedSystem(): SpeedConstants? = null
		override fun getMetricSystem(): MetricsConstants? = null
		override fun getAltitudeMetric(): AltitudeMetrics? = null
		override fun getAngularSystem(): AngularConstants? = null
		override fun getTemperatureUnits(): TemperatureUnits? = null
		override fun isGpxFileVisible(path: String): Boolean = false
		override fun getSelectedFileByPath(path: String): GpxFile? = null
		override fun getNameStringMatcher(name: String, mode: KStringMatcherMode): KStringMatcher =
			object : KStringMatcher {
				override fun matches(name: String): Boolean = false
			}

		override fun getTrackPointsAnalyser(): TrackPointsAnalyser? = null
		override fun getAssetAsString(name: String): String? = null
		override fun searchNearestCityName(latLon: KLatLon, callback: CityNameCallback) = Unit
		override fun getSmartFolderHelper(): SmartFolderHelper = smartFoldHelper
	}

	private object TestSettingsApi : SettingsAPI {
		override fun registerPreference(name: String, defValue: String, global: Boolean, shared: Boolean) = Unit
		override fun addStringPreferenceListener(name: String, listener: KStateChangedListener<String>) = Unit
		override fun getStringPreference(name: String): String? = null
		override fun setStringPreference(name: String, value: String) = Unit
		override fun registerPreference(name: String, defValue: Float, global: Boolean, shared: Boolean) = Unit
		override fun addFloatPreferenceListener(name: String, listener: KStateChangedListener<Float>) = Unit
		override fun getFloatPreference(name: String): Float? = null
		override fun setFloatPreference(name: String, value: Float) = Unit
		override fun <T : Enum<T>> addEnumPreferenceListener(name: String, listener: KStateChangedListener<T>) = Unit
	}
}
