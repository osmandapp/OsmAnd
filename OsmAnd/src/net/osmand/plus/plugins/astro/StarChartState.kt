package net.osmand.plus.plugins.astro

import androidx.annotation.StringRes
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.backend.preferences.OsmandPreference

class StarChartState(private val app: OsmandApplication) {
	private val typePreference: OsmandPreference<StarChartType> = registerTypePreference()

	enum class StarChartType(@field:StringRes @param:StringRes val titleId: Int) {
		STAR_VISIBLITY(R.string.star_visibility_name),
		STAR_ALTITUDE(R.string.star_altitude_name),
		CELESTIAL_PATH(R.string.celestial_paths_name);

		fun next(): StarChartType {
			val nextItemIndex: Int = (ordinal + 1) % entries.size
			return entries[nextItemIndex]
		}
	}

	fun changeToNextState() = typePreference.set(getStarChartType().next())

	fun getStarChartType(): StarChartType = typePreference.get()

	private fun registerTypePreference(): OsmandPreference<StarChartType> {
		return app.settings.registerEnumStringPreference(
			"star_chart_type",
			StarChartType.STAR_VISIBLITY,
			StarChartType.entries.toTypedArray(),
			StarChartType::class.java
		).makeProfile()
	}
}