package net.osmand.plus.plugins.astro.widgets

import androidx.annotation.StringRes
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState
import net.osmand.util.Algorithms

class SkyChartWidgetState(app: OsmandApplication, customId: String?) : WidgetState(app) {
	private val typePreference: OsmandPreference<SkyChartType> = registerTypePreference(customId)

	enum class SkyChartType(@field:StringRes @param:StringRes val titleId: Int) {
		PLANETS_VISIBLITY(R.string.planets_visibility_name),
		PLANETS_ALTITUDE(R.string.planets_altitude_name),
		CELESTIAL_PATH(R.string.celestial_paths_name);

		fun next(): SkyChartType {
			val nextItemIndex: Int = (ordinal + 1) % entries.size
			return entries[nextItemIndex]
		}
	}

	override fun getTitle(): String {
		return app.getString(getSkyChartType().titleId)
	}

	override fun getSettingsIconId(nightMode: Boolean): Int {
		return WidgetType.DEV_ZOOM_LEVEL.getIconId(nightMode)
	}

	override fun changeToNextState() {
		typePreference.set(getSkyChartType().next())
	}

	override fun copyPrefs(appMode: ApplicationMode, customId: String?) {
		registerTypePreference(customId).setModeValue(appMode, getSkyChartType())
	}

	override fun copyPrefsFromMode(
		sourceAppMode: ApplicationMode, appMode: ApplicationMode, customId: String?
	) {
		registerTypePreference(customId).setModeValue(
			appMode, typePreference.getModeValue(sourceAppMode))
	}

	fun getSkyChartTypePref() = typePreference

	fun getSkyChartType(): SkyChartType = typePreference.get()

	private fun registerTypePreference(customId: String?): OsmandPreference<SkyChartType> {
		var prefId: String? = "sky_chart_type"
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId
		}
		return settings.registerEnumStringPreference(
			prefId,
			SkyChartType.PLANETS_VISIBLITY,
			SkyChartType.entries.toTypedArray(),
			SkyChartType::class.java
		).makeProfile()
	}
}