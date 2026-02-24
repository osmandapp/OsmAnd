package net.osmand.plus.plugins.monitoring.widgets

import android.view.View
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidget.showOnMap
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingMovingTimeWidgetState.MovingTimeMode
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.util.Algorithms

class TripRecordingMovingTimeWidget(
	mapActivity: MapActivity,
	private val widgetState: TripRecordingMovingTimeWidgetState,
	widgetType: WidgetType,
	customId: String?,
	widgetsPanel: WidgetsPanel?
) : BaseRecordingWidget(mapActivity, widgetType, customId, widgetsPanel) {

	private val minute = getString(R.string.shared_string_minute_lowercase)

	private var cachedMovingTime: Long = -1L
	private var forceUpdate: Boolean = false

	init {
		updateWidgetView()
		updateInfo(null)
	}

	override fun getOnClickListener(): View.OnClickListener {
		return View.OnClickListener {
			forceUpdate = true
			widgetState.changeToNextState()
			updateInfo(null)
			mapActivity.refreshMap()
			updateWidgetName()
			updateIcon()
		}
	}

	override fun updateSimpleWidgetInfo(drawSettings: DrawSettings?) {
		super.updateSimpleWidgetInfo(drawSettings)
		val movingTime = getMovingTime()
		if (forceUpdate || isUpdateNeeded || cachedMovingTime != movingTime) {
			cachedMovingTime = movingTime
			forceUpdate = false
			val formattedTime =
				Algorithms.formatDuration((movingTime / 1000).toInt(), app.accessibilityEnabled())
			setText(formattedTime, minute)
		}
	}

	private fun getMovingTime(): Long {
		val mode = widgetState.movingTimeModePreference.get()
		return if (mode == MovingTimeMode.TOTAL) {
			analysis.timeMoving
		} else {
			getLastSlopeMovingTime(mode)
		}
	}

	private fun getLastSlopeMovingTime(mode: MovingTimeMode): Long {
		val lastSlope = getLastSlope(mode == MovingTimeMode.LAST_UPHILL)
		return lastSlope?.movingTime ?: 0L
	}

	override fun resetCachedValue() {
		super.resetCachedValue()
		cachedMovingTime = -1L
	}

	override fun getWidgetActions(): List<PopUpMenuItem>? {
		val actions = mutableListOf<PopUpMenuItem>()
		val uiUtilities: UiUtilities = app.uiUtilities
		val iconColor = ColorUtilities.getDefaultIconColor(app, nightMode)

		actions.add(
			PopUpMenuItem.Builder(app)
				.setIcon(uiUtilities.getPaintedIcon(R.drawable.ic_action_center_on_track, iconColor))
				.setTitleId(R.string.show_track_on_map)
				.setOnClickListener { showOnMap(mapActivity) }
				.showTopDivider(true)
				.create()
		)
		return actions
	}

	override fun getIconId(nightMode: Boolean): Int {
		return widgetState.movingTimeModePreference.get().getIcon(nightMode)
	}

	override fun getAdditionalWidgetName(): String {
		return getString(widgetState.movingTimeModePreference.get().titleId)
	}

	override fun getAdditionalWidgetNameDivider(): Int {
		return R.string.ltr_or_rtl_combine_via_colon
	}

	fun getMovingTimeModeOsmandPreference(): OsmandPreference<MovingTimeMode> {
		return widgetState.movingTimeModePreference
	}
}