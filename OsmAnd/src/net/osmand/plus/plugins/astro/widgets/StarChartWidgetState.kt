package net.osmand.plus.plugins.astro.widgets

import androidx.annotation.StringRes
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState
import net.osmand.util.Algorithms

class StarChartWidgetState(app: OsmandApplication, customId: String?) : WidgetState(app) {
	private val typePreference: OsmandPreference<StarChartType> = registerTypePreference(customId)

	enum class StarChartType(@field:StringRes @param:StringRes val titleId: Int) {
		STAR_VISIBLITY(R.string.star_visibility_name),
		STAR_ALTITUDE(R.string.star_altitude_name),
		CELESTIAL_PATH(R.string.celestial_paths_name);

		fun next(): StarChartType {
			val nextItemIndex: Int = (ordinal + 1) % entries.size
			return entries[nextItemIndex]
		}
	}

	override fun getTitle(): String {
		return app.getString(getStarChartType().titleId)
	}

	override fun getSettingsIconId(nightMode: Boolean): Int {
		return WidgetType.DEV_ZOOM_LEVEL.getIconId(nightMode)
	}

	override fun changeToNextState() {
		typePreference.set(getStarChartType().next())
	}

	override fun copyPrefs(appMode: ApplicationMode, customId: String?) {
		registerTypePreference(customId).setModeValue(appMode, getStarChartType())
	}

	override fun copyPrefsFromMode(
		sourceAppMode: ApplicationMode, appMode: ApplicationMode, customId: String?
	) {
		registerTypePreference(customId).setModeValue(
			appMode, typePreference.getModeValue(sourceAppMode))
	}

	fun getStarChartTypePref() = typePreference

	fun getStarChartType(): StarChartType = typePreference.get()

	private fun registerTypePreference(customId: String?): OsmandPreference<StarChartType> {
		var prefId: String? = "star_chart_type"
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId
		}
		return settings.registerEnumStringPreference(
			prefId,
			StarChartType.STAR_VISIBLITY,
			StarChartType.entries.toTypedArray(),
			StarChartType::class.java
		).makeProfile()
	}
}