package net.osmand.shared.obd

import net.osmand.shared.api.SettingsAPI
import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.extensions.format
import net.osmand.shared.obd.OBDCommand.*
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.*
import net.osmand.shared.util.KCollectionUtils
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import kotlin.math.max
import net.osmand.shared.util.Localization
import net.osmand.shared.util.PlatformUtil


private const val FUEL_CONSUMPTION_DEFAULT_AVERAGE_TIME = 5 * 60

object OBDDataComputer {

	private const val LITER_KM_CONSUMPTION_LIMIT = 100
	private const val M_LITER_CONSUMPTION_LIMIT = 1000

	private const val LITER_HOUR_CONSUMPTION_LIMIT = 100

	private val log = LoggerFactory.getLogger("OBDDataComputer")

	private val osmAndSettings: SettingsAPI = PlatformUtil.getOsmAndContext().getSettings()
	const val DEFAULT_FUEL_TANK_CAPACITY = 52f
	private const val SAME_FUEL_LVL_SEQUENCE_LENGTH = 5
	private const val FUEL_TANK_CAPACITY_SETTING_ID = "fuel_tank_capacity"
	var locations = listOf<OBDLocation>()
	var widgets: List<OBDComputerWidget> = ArrayList()
		private set
	var timeoutForInstantValuesSeconds = 0
	var obdDispatcher: OBDDispatcher? = null
		set(value) {
			field = value
			updateRequiredCommands()
		}

	class OBDLocation(val time: Long, val latLon: KLatLon)

	fun acceptValue(value: Map<OBDCommand, OBDDataField<Any>?>) {
		for (widget in widgets) {
			widget.acceptValue(value)
		}
		compute()
	}

	fun clearCache() {
		for (widget in widgets) {
			widget.clearData()
		}
	}

	fun registerLocation(l: OBDLocation) {
		if (widgets.isNotEmpty()) {
			// concurrency - change collection in one thread. Other places only read
			locations = KCollectionUtils.addToList(locations, l)
			cleanupLocations()
		}
	}

	fun compute() {
		val now: Long = currentTimeMillis()
		for (widget in widgets) {
			widget.cleanup(now)
			widget.computeValue()
		}
	}

	private fun cleanupLocations() {
		val now: Long = currentTimeMillis()
		// calculate maximum window to clean up
		var window = timeoutForInstantValuesSeconds
		for (widget in widgets) {
			if (widget.type.locationNeeded) {
				window = max(window, widget.averageTimeSeconds)
			}
		}
		var inWindow = 0
		while (inWindow < locations.size) {
			if (locations[inWindow].time >= now - window * 1000) {
				break
			}
			inWindow++
		}
		if (inWindow - 1 > 0) {
			locations = locations.subList(inWindow - 1, locations.size)
		}
	}

	fun registerWidget(
		type: OBDTypeWidget,
		averageTimeSeconds: Int
	): OBDComputerWidget {
		for (widget in widgets) {
			if (widget.type == type && widget.averageTimeSeconds == averageTimeSeconds) {
				return widget
			}
		}
		val widget = OBDComputerWidget(type, averageTimeSeconds)
		widgets = KCollectionUtils.addToList(widgets, widget)
		updateRequiredCommands()
		return widget
	}

	fun removeWidget(w: OBDComputerWidget) {
		widgets = KCollectionUtils.removeFromList(widgets, w)
		updateRequiredCommands()
	}

	private fun updateRequiredCommands() {
		obdDispatcher?.apply {
			clearCommands()
			widgets.forEach { widget ->
				addCommand(widget.type.requiredCommand)
			}
		}
	}

	enum class OBDTypeWidget(
		val locationNeeded: Boolean,
		val requiredCommand: OBDCommand,
		val nameId: String,
		val formatter: OBDComputerWidgetFormatter,
		val defaultAverageTime: Int = 0
	) {
		SPEED(
			false,
			OBD_SPEED_COMMAND,
			"obd_widget_vehicle_speed",
			OBDComputerWidgetFormatter("%.0f")),
		RPM(
			false,
			OBD_RPM_COMMAND,
			"obd_widget_engine_speed",
			OBDComputerWidgetFormatter("%d")),
		ENGINE_RUNTIME(
			false,
			OBD_ENGINE_RUNTIME_COMMAND,
			"obd_engine_runtime",
			OBDComputerWidgetFormatter("%s")),
		FUEL_PRESSURE(
			false,
			OBD_FUEL_PRESSURE_COMMAND,
			"obd_fuel_pressure",
			OBDComputerWidgetFormatter("%d")),
		FUEL_LEFT_KM(
			true,
			OBD_FUEL_LEVEL_COMMAND,
			"remaining_fuel",
			OBDComputerWidgetFormatter("%.0f")),
		CALCULATED_ENGINE_LOAD(
			false,
			OBD_CALCULATED_ENGINE_LOAD_COMMAND,
			"obd_calculated_engine_load",
			OBDComputerWidgetFormatter("%.0f")),
		THROTTLE_POSITION(
			false,
			OBD_THROTTLE_POSITION_COMMAND,
			"obd_throttle_position",
			OBDComputerWidgetFormatter("%.0f")),
		FUEL_LEFT_PERCENT(
			false,
			OBD_FUEL_LEVEL_COMMAND,
			"remaining_fuel",
			OBDComputerWidgetFormatter("%.1f")),
		FUEL_LEFT_LITER(
			false,
			OBD_FUEL_LEVEL_COMMAND,
			"remaining_fuel",
			OBDComputerWidgetFormatter("%.1f")),
		FUEL_CONSUMPTION_RATE_PERCENT_HOUR(
			false,
			OBD_FUEL_LEVEL_COMMAND,
			"obd_fuel_consumption_rate_percent_hour", OBDComputerWidgetFormatter("%.1f"),
			FUEL_CONSUMPTION_DEFAULT_AVERAGE_TIME),
		FUEL_CONSUMPTION_RATE_LITER_KM(
			true,
			OBD_FUEL_LEVEL_COMMAND,
			"obd_fuel_consumption_rate", OBDComputerWidgetFormatter("%.1f"),
			FUEL_CONSUMPTION_DEFAULT_AVERAGE_TIME),
		FUEL_CONSUMPTION_RATE_M_PER_LITER(
			true,
			OBD_FUEL_LEVEL_COMMAND,
			"obd_fuel_consumption_rate", OBDComputerWidgetFormatter("%.1f"),
			FUEL_CONSUMPTION_DEFAULT_AVERAGE_TIME),
		FUEL_CONSUMPTION_RATE_LITER_HOUR(
			false,
			OBD_FUEL_LEVEL_COMMAND,
			"obd_fuel_consumption_rate_liter_hour", OBDComputerWidgetFormatter("%.1f"),
			FUEL_CONSUMPTION_DEFAULT_AVERAGE_TIME),
		FUEL_CONSUMPTION_RATE_SENSOR(
			false,
			OBD_FUEL_CONSUMPTION_RATE_COMMAND,
			"obd_fuel_consumption_rate_scanner", OBDComputerWidgetFormatter("%.2f")),
		TEMPERATURE_INTAKE(
			false,
			OBD_AIR_INTAKE_TEMP_COMMAND,
			"obd_air_intake_temp",
			OBDComputerWidgetFormatter("%.0f")),
		ENGINE_OIL_TEMPERATURE(
			false,
			OBD_ENGINE_OIL_TEMPERATURE_COMMAND,
			"obd_engine_oil_temperature",
			OBDComputerWidgetFormatter("%.0f")),
		TEMPERATURE_AMBIENT(
			false,
			OBD_AMBIENT_AIR_TEMPERATURE_COMMAND,
			"obd_ambient_air_temp",
			OBDComputerWidgetFormatter("%.0f")),
		BATTERY_VOLTAGE(
			false,
			OBD_BATTERY_VOLTAGE_COMMAND,
			"obd_battery_voltage",
			OBDComputerWidgetFormatter("%.2f")),
		ADAPTER_BATTERY_VOLTAGE(
			false,
			OBD_ALT_BATTERY_VOLTAGE_COMMAND,
			"obd_alt_battery_voltage",
			OBDComputerWidgetFormatter("%.1f")),
		FUEL_TYPE(
			false,
			OBD_FUEL_TYPE_COMMAND,
			"obd_fuel_type",
			OBDFuelTypeFormatter()),
		VIN(false, OBD_VIN_COMMAND, "obd_vin", OBDComputerWidgetFormatter("%s")),
		TEMPERATURE_COOLANT(
			false,
			OBD_ENGINE_COOLANT_TEMP_COMMAND,
			"obd_engine_coolant_temp",
			OBDComputerWidgetFormatter("%.0f"));

		fun getTitle(): String {
			return Localization.getString(nameId)
		}
	}

	private fun averageNumber(values: List<OBDDataField<Any>>): Double? =
		if (values.isNotEmpty()) values
			.filter { it.value is Number }
			.map { (it.value as Number).toDouble() }
			.sumOf { it } / values.size
		else
			null

	open class OBDComputerWidgetFormatter(val pattern: String = "%s") {
		open fun format(v: Any?): String {
			return if (v == null) {
				"-"
			} else {
				var ret = ""
				try {
					ret = pattern.format(v)
				} catch (error: Throwable) {
					log.error(error.message)
				}
				ret
			}
		}
	}

	class OBDComputerWidget(
		val type: OBDTypeWidget,
		var averageTimeSeconds: Int) {
		private var values: List<OBDDataField<Any>> = ArrayList()
		private var tmpValues: List<OBDDataField<Any>> = ArrayList()
		private var value: Any? = null
		private var cachedVersion = 0
		private var version = 0

		fun clearData() {
			values = ArrayList()
			value = null
			cachedVersion = 0
			version = 0
		}

		fun computeValue(): Any? {
			if (cachedVersion != version) {
				val v = version
				value = compute()
				cachedVersion = v
			}
			return value
		}

		private fun compute(): Any? {
			val locValues = ArrayList(values)
			if (locValues.size > 0 && locValues[locValues.size - 1] == OBDDataField.NO_DATA) {
				return "N/A"
			}
			return when (type) {
				TEMPERATURE_AMBIENT,
				TEMPERATURE_COOLANT,
				TEMPERATURE_INTAKE,
				ENGINE_OIL_TEMPERATURE,
				SPEED,
				BATTERY_VOLTAGE,
				ADAPTER_BATTERY_VOLTAGE,
				FUEL_CONSUMPTION_RATE_SENSOR,
				FUEL_PRESSURE,
				THROTTLE_POSITION,
				CALCULATED_ENGINE_LOAD,

				RPM -> {
					if (averageTimeSeconds == 0 && locValues.size > 0) {
						locValues[locValues.size - 1].value
					} else {
						averageNumber(locValues)
					}
				}

				FUEL_CONSUMPTION_RATE_PERCENT_HOUR -> {
					if (locValues.size >= 2) {
						calculateFuelConsumption(locValues)
					} else if (locValues.size == 1) {
						Float.NaN
					} else {
						null
					}
				}

				FUEL_CONSUMPTION_RATE_LITER_HOUR -> {
					if (locValues.size >= 2) {
						val result = getFuelTank() * calculateFuelConsumption(locValues) / 100
						if (result > LITER_HOUR_CONSUMPTION_LIMIT) {
							Float.NaN
						} else {
							result
						}
					} else if (locValues.size == 1) {
						Float.NaN
					} else {
						null
					}
				}

				FUEL_CONSUMPTION_RATE_LITER_KM -> {
					if (locValues.size >= 2) {
						val first = locValues[locValues.size - 2]
						val last = locValues[locValues.size - 1]
						val diffPerc =
							(first.value as Number).toFloat() - (last.value as Number).toFloat()
						if (diffPerc > 0) {
							val difLiter = getFuelTank() * diffPerc / 100
							val distance = getDistanceForTimePeriod(first.timestamp, last.timestamp)
							if (distance > 0) {
								log.debug("l/100km. distance $distance; difLiter $difLiter; result ${100 * difLiter / (distance / 1000)}")
								val result = 100 * difLiter / (distance / 1000)
								return if (result > LITER_KM_CONSUMPTION_LIMIT) {
									Float.NaN
								} else {
									result
								}
							}
						}
						null
					} else if (locValues.size == 1) {
						Float.NaN
					} else {
						null
					}
				}

				FUEL_LEFT_KM -> {
					if (locValues.size >= 2) {
						val first = locValues[locValues.size - 2]
						val last = locValues[locValues.size - 1]
						val diffPerc =
							(first.value as Number).toFloat() - (last.value as Number).toFloat()
						if (diffPerc > 0) {
							val dist = getDistanceForTimePeriod(first.timestamp, last.timestamp)
							if (dist > 0) {
								val lastPerc = last.value.toFloat()
								log.debug("left km. fuelLvl $lastPerc; distance $dist; difPercent $diffPerc; result ${lastPerc * dist / diffPerc}")
								return lastPerc * dist / diffPerc
							}
						}
						null
					} else if (locValues.size == 1) {
						Float.NaN
					} else {
						null
					}
				}

				FUEL_LEFT_PERCENT -> {
					if (locValues.size > 0) {
						locValues[locValues.size - 1].value as Float
					} else {
						null
					}
				}

				FUEL_LEFT_LITER -> {
					if (locValues.size > 0) {
						getFuelTank() * (locValues[locValues.size - 1].value as Float) / 100
					} else {
						null
					}
				}

				FUEL_TYPE,
				ENGINE_RUNTIME,
				VIN -> if (locValues.size > 0) {
					(locValues[locValues.size - 1]).value
				} else {
					null
				}

				FUEL_CONSUMPTION_RATE_M_PER_LITER -> {
					if (locValues.size >= 2) {
						val first = locValues[locValues.size - 2]
						val last = locValues[locValues.size - 1]
						val diffPerc =
							(first.value as Number).toFloat() - (last.value as Number).toFloat()
						if (diffPerc > 0) {
							val difLiter = getFuelTank() * diffPerc / 100
							val distance = getDistanceForTimePeriod(first.timestamp, last.timestamp)
							if (distance > 0 && difLiter > 0) {
								val result = distance / difLiter
								return if (result > M_LITER_CONSUMPTION_LIMIT) {
									Float.NaN
								} else {
									result
								}
							}
						}
						null
					} else if (locValues.size == 1) {
						Float.NaN
					} else {
						null
					}
				}
			}
		}

		private fun getDistanceForTimePeriod(startTime: Long, endTime: Long): Double {
			val localLocations = locations
			var start = 0
			var end = localLocations.size - 1
			while (start < localLocations.size) {
				if (localLocations[start].time > startTime) {
					break
				}
				start++
			}
			while (end >= 0) {
				if (localLocations[end].time < endTime) {
					break
				}
				end--
			}
			var dist = 0.0
			if (start < end) {
				for (k in start until end) {
					dist += KMapUtils.getDistance(
						localLocations[k].latLon,
						localLocations[k + 1].latLon)
				}
			}
			return dist
		}

		private fun calculateFuelConsumption(locValues: ArrayList<OBDDataField<Any>>): Float {
			val first = locValues[locValues.size - 2]
			val last = locValues[locValues.size - 1]
			val diffPerc = (first.value as Number).toFloat() - (last.value as Number).toFloat()
			val diffTime = last.timestamp - first.timestamp
			return diffPerc / diffTime * 1000 * 3600
		}

		fun acceptValue(value: Map<OBDCommand, OBDDataField<Any>?>) {
			value[type.requiredCommand]?.let {
				if (it == OBDDataField.NO_DATA) {
					if (values.isEmpty()) {
						version++
					}
					values = mutableListOf(it)
				} else {
					when (type) {
						FUEL_LEFT_KM,
						FUEL_CONSUMPTION_RATE_LITER_KM,
						FUEL_CONSUMPTION_RATE_PERCENT_HOUR,
						FUEL_CONSUMPTION_RATE_LITER_HOUR -> {
							val lastLvl =
								if (values.isNotEmpty()) (values[values.size - 1].value as Number).toFloat() else 0f
							val newlvl = (it.value as Number).toFloat()
							if (values.isEmpty() || lastLvl != newlvl) {
								var valueToAdd: OBDDataField<Any>? = it
								if (tmpValues.isEmpty() || tmpValues.last().value == newlvl) {
									log.debug("Fuel level increase found. last $lastLvl; new $newlvl tmpValues.size=${tmpValues.size}")
									tmpValues = KCollectionUtils.addToList(
										tmpValues,
										OBDDataField(newlvl))
									if (tmpValues.size >= SAME_FUEL_LVL_SEQUENCE_LENGTH) {
										log.debug("New fuel level accepted")
										valueToAdd = tmpValues[0]
										tmpValues = emptyList()
									} else {
										valueToAdd = null
									}
								} else if (tmpValues.isNotEmpty() && tmpValues.last().value != newlvl) {
									log.debug("Last fuel level increase changed. last $lastLvl; last change ${tmpValues.last().value} new $newlvl")
									tmpValues = arrayListOf(OBDDataField(newlvl))
									valueToAdd = null
								}
								valueToAdd?.let { newData ->
									version++
									values = KCollectionUtils.addToList(values, newData)
								}
							}
						}

						else -> {
							version++
							values = KCollectionUtils.addToList(values, it)
						}
					}
				}
			}
		}

		fun resetLocations() {
			values = ArrayList()
		}

		fun cleanup(now: Long) {
			val timeout =
				if (averageTimeSeconds > 0) averageTimeSeconds else timeoutForInstantValuesSeconds
			var inWindow = 0
			while (inWindow < values.size) {
				if (values[inWindow].timestamp >= now - timeout * 1000) {
					break
				}
				inWindow++
			}
			if (inWindow > 0 && inWindow < values.size - 1) {
				values = values.subList(inWindow, values.size)
			}
		}
	}

	private fun getFuelTank(): Float {
		val fuelTank = osmAndSettings.getFloatPreference(FUEL_TANK_CAPACITY_SETTING_ID)
		return fuelTank ?: DEFAULT_FUEL_TANK_CAPACITY
	}
}