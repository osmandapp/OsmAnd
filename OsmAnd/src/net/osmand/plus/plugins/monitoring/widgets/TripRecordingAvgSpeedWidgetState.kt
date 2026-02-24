package net.osmand.plus.plugins.monitoring.widgets

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState

class TripRecordingAvgSpeedWidgetState(
	app: OsmandApplication,
	customId: String?,
	val widgetType: WidgetType
) : WidgetState(app) {

	companion object {
		const val AVG_SPEED_WIDGET_MODE = "avg_speed_widget_mode"
	}

	val avgSpeedModePreference: OsmandPreference<AvgSpeedMode> =
		registerAvgSpeedModePreference(customId)

	override fun getTitle(): String {
		return app.getString(widgetType.titleId)
	}

	override fun getSettingsIconId(nightMode: Boolean): Int {
		return widgetType.getIconId(nightMode)
	}

	override fun changeToNextState() {
		val currentMode = avgSpeedModePreference.get()
		avgSpeedModePreference.set(currentMode.next())
	}

	override fun copyPrefs(appMode: ApplicationMode, customId: String?) {
		copyPrefsFromMode(appMode, appMode, customId)
	}

	override fun copyPrefsFromMode(sourceAppMode: ApplicationMode, appMode: ApplicationMode, customId: String?) {
		registerAvgSpeedModePreference(customId).setModeValue(appMode, avgSpeedModePreference.getModeValue(sourceAppMode))
	}

	private fun registerAvgSpeedModePreference(customId: String?): OsmandPreference<AvgSpeedMode> {
		var prefId = AVG_SPEED_WIDGET_MODE
		if (!customId.isNullOrEmpty()) {
			prefId += customId
		}
		return settings.registerEnumStringPreference(
			prefId, AvgSpeedMode.TRIP_AVERAGE,
			AvgSpeedMode.entries.toTypedArray(), AvgSpeedMode::class.java
		).makeProfile()
	}

	enum class AvgSpeedMode(
		@StringRes val titleId: Int,
		@DrawableRes private val dayIcon: Int,
		@DrawableRes private val nightIcon: Int
	) {
		TRIP_AVERAGE(
			R.string.shared_string_trip_average,
			R.drawable.widget_track_recording_average_speed_day,
			R.drawable.widget_track_recording_average_speed_night
		),
		LAST_DOWNHILL(
			R.string.shared_string_last_downhill,
			R.drawable.widget_track_recording_average_speed_last_downhill_day,
			R.drawable.widget_track_recording_average_speed_last_downhill_night
		),
		LAST_UPHILL(
			R.string.shared_string_last_uphill,
			R.drawable.widget_track_recording_average_speed_last_uphill_day,
			R.drawable.widget_track_recording_average_speed_last_uphill_night
		);

		fun getIcon(nightMode: Boolean): Int {
			return if (nightMode) nightIcon else dayIcon
		}

		fun next(): AvgSpeedMode {
			val nextItemIndex = (ordinal + 1) % entries.size
			return entries[nextItemIndex]
		}
	}
}