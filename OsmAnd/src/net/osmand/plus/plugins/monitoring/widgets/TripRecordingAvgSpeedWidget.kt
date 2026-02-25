package net.osmand.plus.plugins.monitoring.widgets

import android.view.View
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingAvgSpeedWidgetState.AvgSpeedMode
import net.osmand.plus.plugins.monitoring.widgets.TripRecordingElevationWidget.showOnMap
import net.osmand.plus.settings.backend.preferences.OsmandPreference
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.utils.UiUtilities
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.widgets.popup.PopUpMenuItem

class TripRecordingAvgSpeedWidget(
	mapActivity: MapActivity,
	private val widgetState: TripRecordingAvgSpeedWidgetState,
	widgetType: WidgetType,
	customId: String?,
	widgetsPanel: WidgetsPanel?
) : BaseRecordingWidget(mapActivity, widgetType, customId, widgetsPanel) {

	private var cachedAvgSpeed: Float = -1f
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
		val avgSpeed = getAvgSpeed()
		if (forceUpdate || isUpdateNeeded || cachedAvgSpeed != avgSpeed) {
			cachedAvgSpeed = avgSpeed
			forceUpdate = false
			val formattedSpeed = OsmAndFormatter.getFormattedSpeedValue(avgSpeed, app)
			setText(formattedSpeed.value, formattedSpeed.unit)
		}
	}

	private fun getAvgSpeed(): Float {
		val mode = widgetState.avgSpeedModePreference.get()
		return if (mode == AvgSpeedMode.TRIP_AVERAGE) {
			analysis.let { calculateAvgSpeed(it.timeSpan, it.totalDistance) }
		} else {
			getLastSlopeAvgSpeed(mode)
		}
	}

	private fun getLastSlopeAvgSpeed(mode: AvgSpeedMode): Float {
		return getLastSlope(mode == AvgSpeedMode.LAST_UPHILL)?.let {
			calculateAvgSpeed(it.movingTime, it.distance.toFloat())
		} ?: 0f
	}

	private fun calculateAvgSpeed(timeInMilliseconds: Long, distance: Float): Float {
		// Convert movingTime from milliseconds to seconds
		val timeInSeconds = timeInMilliseconds / 1000f

		// Calculate Average Speed for the slope
		return if (timeInSeconds > 0) {
			(distance / timeInSeconds)
		} else {
			0f
		}
	}

	override fun resetCachedValue() {
		super.resetCachedValue()
		cachedAvgSpeed = -1f
	}

	override fun getWidgetActions(): List<PopUpMenuItem> {
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
		return widgetState.avgSpeedModePreference.get().getIcon(nightMode)
	}

	override fun getAdditionalWidgetName(): String {
		return getString(widgetState.avgSpeedModePreference.get().titleId)
	}

	override fun getAdditionalWidgetNameDivider(): Int {
		return R.string.ltr_or_rtl_combine_via_colon
	}

	fun getAvgSpeedModePreference(): OsmandPreference<AvgSpeedMode> {
		return widgetState.avgSpeedModePreference
	}

	override fun isMetricSystemDepended(): Boolean {
		return true
	}
}