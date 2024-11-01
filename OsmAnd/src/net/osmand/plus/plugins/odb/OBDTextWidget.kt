package net.osmand.plus.plugins.odb

import android.view.View
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.activities.MapActivity
import net.osmand.plus.plugins.PluginsHelper
import net.osmand.plus.settings.backend.preferences.CommonPreference
import net.osmand.plus.utils.ColorUtilities
import net.osmand.plus.views.layers.base.OsmandMapLayer.DrawSettings
import net.osmand.plus.views.mapwidgets.WidgetType
import net.osmand.plus.views.mapwidgets.WidgetsPanel
import net.osmand.plus.views.mapwidgets.widgets.SimpleWidget
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget
import net.osmand.util.Algorithms

class OBDTextWidget(
	mapActivity: MapActivity,
	widgetType: WidgetType,
	fieldType: OBDTypeWidget,
	customId: String?,
	widgetsPanel: WidgetsPanel?) :
	SimpleWidget(mapActivity, widgetType, customId, widgetsPanel) {
	private val plugin = PluginsHelper.getPlugin(VehicleMetricsPlugin::class.java)
	private val widgetComputer: OBDComputerWidget
	private var cacheTextData: String? = null
	private var cacheSubTextData: String? = null

	var measuredIntervalPref: CommonPreference<Long>? = null
	var averageModePref: CommonPreference<Boolean>? = null

	companion object {
		private const val MEASURED_INTERVAL_PREF_ID = "average_obd_measured_interval_millis"
		private const val AVERAGE_MODE_PREF_ID = "average_obd_mode"
		const val DEFAULT_INTERVAL_MILLIS: Long = 30 * 60 * 1000L
		fun formatIntervals(app: OsmandApplication, interval: Long): String {
			val seconds = interval < 60 * 1000
			val timeInterval = if (seconds
			) (interval / 1000).toString() else (interval / 1000 / 60).toString()
			val timeUnit = if (interval < 60 * 1000
			) app.getString(R.string.shared_string_sec)
			else app.getString(R.string.shared_string_minute_lowercase)
			return app.getString(R.string.ltr_or_rtl_combine_via_space, timeInterval, timeUnit)
		}
	}

	init {
		// 0 - for instant
		var averageTimeSeconds = 0

		if (supportsAverageMode()) {
			measuredIntervalPref = registerMeasuredIntervalPref(customId)
			averageModePref = registerAverageModePref(customId)
			if (averageModePref!!.get()) {
				averageTimeSeconds = (measuredIntervalPref!!.get() / 1000).toInt()
			}
		} else if (fieldType == OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR ||
			fieldType == OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR ||
			fieldType == OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM
		) {
			averageTimeSeconds = 5 * 60
		}

		widgetComputer =
			OBDDataComputer.registerWidget(fieldType, averageTimeSeconds)
	}

	fun updatePrefs() {
		if (supportsAverageMode()) {
			if (averageModePref!!.get()) {
				widgetComputer.averageTimeSeconds = (measuredIntervalPref!!.get() / 1000).toInt()
			} else{
				widgetComputer.averageTimeSeconds = 0
			}
			updateWidgetName()
		}
	}

	override fun getOnClickListener(): View.OnClickListener {
		return if(supportsAverageMode()){
			View.OnClickListener { v: View? ->
				averageModePref!!.set(!averageModePref!!.get())
				updatePrefs()
			}
		} else{
			super.getOnClickListener()
		}
	}

	override fun getWidgetActions(): List<PopUpMenuItem> {
		val actions: MutableList<PopUpMenuItem> = ArrayList()
		val uiUtilities = app.uiUtilities
		val iconColor = ColorUtilities.getDefaultIconColor(app, nightMode)

		actions.add(PopUpMenuItem.Builder(app)
			.setIcon(
				uiUtilities.getPaintedIcon(
					R.drawable.ic_action_reset_to_default_dark,
					iconColor
				)
			)
			.setTitleId(R.string.reset_average_value)
			.setOnClickListener { item: PopUpMenuItem? -> resetAverageValue() }
			.showTopDivider(true)
			.create())
		return actions
	}

	private fun resetAverageValue() {
		widgetComputer.resetLocations()
		setText(NO_VALUE, null)
	}

	override fun getWidgetName(): String? {
		val widgetName = if (widgetType != null) getString(widgetType.titleId) else null

		if (supportsAverageMode() && !Algorithms.isEmpty(widgetName) && averageModePref?.get() == true) {
			val formattedInterval = formatIntervals(app, measuredIntervalPref!!.get())
			return app.getString(
				R.string.ltr_or_rtl_combine_via_colon,
				widgetName,
				formattedInterval
			)
		}
		return widgetName
	}

	override fun updateSimpleWidgetInfo(drawSettings: DrawSettings?) {
		val subtext: String? = plugin?.getWidgetUnit(widgetComputer)
		val textData: String = plugin?.getWidgetValue(widgetComputer) ?: NO_VALUE
		if (!Algorithms.objectEquals(textData, cacheTextData) ||
			!Algorithms.objectEquals(subtext, cacheSubTextData)) {
			setText(textData, subtext)
			cacheTextData = textData
			cacheSubTextData = subtext
		}
	}

	override fun isMetricSystemDepended(): Boolean {
		return true
	}

	init {
		updateInfo(null)
		setIcons(widgetType)
	}

	private fun registerAverageModePref(customId: String?): CommonPreference<Boolean> {
		val prefId = if (Algorithms.isEmpty(customId))
			AVERAGE_MODE_PREF_ID
		else AVERAGE_MODE_PREF_ID + customId
		return settings.registerBooleanPreference(prefId, false)
			.makeProfile()
			.cache()
	}

	private fun registerMeasuredIntervalPref(customId: String?): CommonPreference<Long> {
		val prefId = if (Algorithms.isEmpty(customId))
			MEASURED_INTERVAL_PREF_ID
		else MEASURED_INTERVAL_PREF_ID + customId
		return settings.registerLongPreference(prefId, DEFAULT_INTERVAL_MILLIS)
			.makeProfile()
			.cache()
	}

	fun isTemperatureWidget(): Boolean {
		return when (widgetType) {
			WidgetType.OBD_AIR_INTAKE_TEMP,
			WidgetType.ENGINE_OIL_TEMPERATURE,
			WidgetType.OBD_AMBIENT_AIR_TEMP,
			WidgetType.OBD_ENGINE_COOLANT_TEMP -> return true

			else -> false
		}
	}

	fun supportsAverageMode(): Boolean {
		return when (widgetType) {
			WidgetType.OBD_SPEED,
			WidgetType.OBD_CALCULATED_ENGINE_LOAD,
			WidgetType.OBD_FUEL_PRESSURE,
			WidgetType.OBD_THROTTLE_POSITION,
			WidgetType.OBD_BATTERY_VOLTAGE,
			WidgetType.OBD_AIR_INTAKE_TEMP,
			WidgetType.ENGINE_OIL_TEMPERATURE,
			WidgetType.OBD_AMBIENT_AIR_TEMP,
			WidgetType.OBD_ENGINE_COOLANT_TEMP -> return true

			else -> false
		}
	}
}