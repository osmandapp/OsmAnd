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
import net.osmand.shared.obd.OBDCommand
import net.osmand.plus.widgets.popup.PopUpMenuItem
import net.osmand.shared.obd.OBDDataComputer
import net.osmand.shared.obd.OBDDataComputer.OBDComputerWidget
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget
import net.osmand.util.Algorithms

open class OBDTextWidget(
	mapActivity: MapActivity,
	widgetType: WidgetType,
	private val fieldType: OBDTypeWidget,
	customId: String?,
	widgetsPanel: WidgetsPanel?
) :
	SimpleWidget(mapActivity, widgetType, customId, widgetsPanel) {
	private val plugin = PluginsHelper.requirePlugin(VehicleMetricsPlugin::class.java)
	protected var widgetComputer: OBDComputerWidget
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
			averageTimeSeconds = fieldType.defaultAverageTime
		}

		widgetComputer =
			OBDDataComputer.registerWidget(fieldType, averageTimeSeconds)
	}

	open fun updatePrefs(prefsChanged: Boolean) {
		if (supportsAverageMode()) {
			val newTimeSeconds: Int =
				if (averageModePref?.get() == true) ((measuredIntervalPref?.get()
					?: 0) / 1000).toInt() else 0
			if (prefsChanged) {
				widgetComputer = OBDDataComputer.registerWidget(widgetComputer.type, newTimeSeconds)
			} else {
				widgetComputer.averageTimeSeconds = newTimeSeconds
			}
			updateWidgetName()
		}
	}

	override fun getOnClickListener(): View.OnClickListener? {
		return if (supportsAverageMode() && averageModePref != null) {
			View.OnClickListener { v: View? ->
				averageModePref?.let {
					it.set(!it.get())
					updatePrefs(true)
				}
			}
		} else {
			null
		}
	}

	override fun getWidgetActions(): MutableList<PopUpMenuItem>? {
		if (supportsAverageMode() && averageModePref?.get() == true) {
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
		return null
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
		val visible = widgetType.isPurchased(app)
		if (visible) {
			updateSimpleWidgetInfoImpl()
		}
	}

	private fun updateSimpleWidgetInfoImpl() {
		val subtext: String? = plugin.getWidgetUnit(widgetComputer)
		val textData: String = plugin.getWidgetValue(widgetComputer)
		if (!Algorithms.objectEquals(textData, cacheTextData) ||
			!Algorithms.objectEquals(subtext, cacheSubTextData)
		) {
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

	fun getWidgetOBDCommand(): OBDCommand {
		return  fieldType.requiredCommand
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
				// TODO: add OBD alt battery widget
//			WidgetType.OBD_ALT_BATTERY_VOLTAGE,
			WidgetType.OBD_BATTERY_VOLTAGE,
			WidgetType.OBD_AIR_INTAKE_TEMP,
			WidgetType.ENGINE_OIL_TEMPERATURE,
			WidgetType.OBD_AMBIENT_AIR_TEMP,
			WidgetType.OBD_ENGINE_COOLANT_TEMP -> return true

			else -> false
		}
	}
}