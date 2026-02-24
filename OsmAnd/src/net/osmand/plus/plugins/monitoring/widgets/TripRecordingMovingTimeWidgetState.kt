package net.osmand.plus.plugins.monitoring.widgets

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.settings.backend.ApplicationMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.widgetstates.WidgetState

class TripRecordingMovingTimeWidgetState(
	app: OsmandApplication,
	customId: String?,
	val widgetType: WidgetType
) : WidgetState(app) {

	companion object {
		const val MOVING_TIME_WIDGET_MODE = "moving_time_widget_mode"
	}

	val movingTimeModePreference: OsmandPreference<MovingTimeMode> = registerMovingTimeModePreference(customId)

	override fun getTitle(): String {
		return app.getString(widgetType.titleId)
	}

	override fun getSettingsIconId(nightMode: Boolean): Int {
		return widgetType.getIconId(nightMode)
	}

	override fun changeToNextState() {
		val currentMode = movingTimeModePreference.get()
		movingTimeModePreference.set(currentMode.next())
	}

	override fun copyPrefs(appMode: ApplicationMode, customId: String?) {
		copyPrefsFromMode(appMode, appMode, customId)
	}

	override fun copyPrefsFromMode(sourceAppMode: ApplicationMode, appMode: ApplicationMode, customId: String?) {
		registerMovingTimeModePreference(customId).setModeValue(appMode, movingTimeModePreference.getModeValue(sourceAppMode))
	}

	private fun registerMovingTimeModePreference(customId: String?): OsmandPreference<MovingTimeMode> {
		var prefId = MOVING_TIME_WIDGET_MODE
		if (!customId.isNullOrEmpty()) {
			prefId += customId
		}
		// Registering preference to keep the selected mode in user profile
		return settings.registerEnumStringPreference(
			prefId, MovingTimeMode.TOTAL,
			MovingTimeMode.entries.toTypedArray(), MovingTimeMode::class.java
		).makeProfile()
	}

	enum class MovingTimeMode(
		@StringRes val titleId: Int,
		@DrawableRes private val dayIcon: Int,
		@DrawableRes private val nightIcon: Int
	) {
		TOTAL(
			R.string.shared_string_total,
			R.drawable.widget_track_recording_moving_time_day,
			R.drawable.widget_track_recording_moving_time_night
		),
		LAST_DOWNHILL(
			R.string.shared_string_last_downhill,
			R.drawable.widget_track_recording_moving_time_downhill_day,
			R.drawable.widget_track_recording_moving_time_downhill_night
		),
		LAST_UPHILL(
			R.string.shared_string_last_uphill,
			R.drawable.widget_track_recording_moving_time_uphill_day,
			R.drawable.widget_track_recording_moving_time_uphill_night
		);

		fun getIcon(nightMode: Boolean): Int {
			return if (nightMode) nightIcon else dayIcon
		}

		fun next(): MovingTimeMode {
			val nextItemIndex = (ordinal + 1) % entries.size
			return entries[nextItemIndex]
		}
	}
}