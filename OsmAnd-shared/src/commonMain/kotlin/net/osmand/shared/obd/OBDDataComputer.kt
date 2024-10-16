package net.osmand.shared.obd

import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.extensions.format
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.*
import net.osmand.shared.util.KCollectionUtils
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import kotlin.math.max
import net.osmand.shared.obd.OBDCommand.*
import net.osmand.shared.util.Localization

object OBDDataComputer {

	private val log = LoggerFactory.getLogger("OBDDataComputer")

	var locations = listOf<OBDLocation>()
	var widgets: List<OBDComputerWidget> = ArrayList()
		private set
	var timeoutForInstantValuesSeconds = 0
	var fuelTank = 52f

	class OBDLocation(val time: Long, val latLon: KLatLon)

	fun acceptValue(value: Map<OBDCommand, OBDDataField<Any>?>) {
		for (widget in widgets) {
			widget.acceptValue(value)
		}
		compute()
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
		averageTimeSeconds: Int,
		formatter: OBDComputerWidgetFormatter = OBDComputerWidgetFormatter()
	): OBDComputerWidget {
		val widget = OBDComputerWidget(formatter, type, averageTimeSeconds)
		widgets = KCollectionUtils.addToList(widgets, widget)
		updateRequiredCommands()
		return widget
	}

	fun removeWidget(w: OBDComputerWidget) {
		widgets = KCollectionUtils.removeFromList(widgets, w)
		updateRequiredCommands()
	}

	private fun updateRequiredCommands() {
		OBDDispatcher.clearCommands()
		widgets.forEach { widget ->
			OBDDispatcher.addCommand(widget.type.requiredCommand)
		}
	}

	enum class OBDTypeWidget(
		val locationNeeded: Boolean,
		val requiredCommand: OBDCommand,
		val nameId: String
	) {
		SPEED(false, OBD_SPEED_COMMAND, "shared_string_speed"),
		RPM(false, OBD_RPM_COMMAND, "obd_rpm"),
		FUEL_LEFT_KM(true, OBD_FUEL_LEVEL_COMMAND, "obd_fuel_left_distance"),
		FUEL_PERCENT(false, OBD_FUEL_LEVEL_COMMAND, "obd_fuel_level"),
		FUEL_LEFT_PERCENT(false, OBD_FUEL_LEVEL_COMMAND, "obd_fuel_left_percent"),
		FUEL_LEFT_LITER(false, OBD_FUEL_LEVEL_COMMAND, "obd_fuel_left_liter"),
		FUEL_CONSUMPTION_RATE_PERCENT_HOUR(
			false,
			OBD_FUEL_LEVEL_COMMAND,
			"obd_fuel_consumption_rate_percent_hour"),
		FUEL_CONSUMPTION_RATE_LITER_HOUR(
			false,
			OBD_FUEL_LEVEL_COMMAND,
			"obd_fuel_consumption_rate_liter_hour"),
		FUEL_CONSUMPTION_RATE_SENSOR(
			false,
			OBD_FUEL_CONSUMPTION_RATE_COMMAND,
			"obd_fuel_consumption_rate_scanner"),
		TEMPERATURE_INTAKE(false, OBD_AIR_INTAKE_TEMP_COMMAND, "obd_air_intake_temp"),
		TEMPERATURE_AMBIENT(false, OBD_AMBIENT_AIR_TEMPERATURE_COMMAND, "obd_ambient_air_temp"),
		BATTERY_VOLTAGE(false, OBD_BATTERY_VOLTAGE_COMMAND, "obd_battery_voltage"),
		FUEL_TYPE(false, OBD_FUEL_TYPE_COMMAND, "obd_fuel_type"),
		VIN(false, OBD_VIN_COMMAND, "obd_vin"),
		TEMPERATURE_COOLANT(false, OBD_ENGINE_COOLANT_TEMP_COMMAND, "obd_engine_coolant_temp");

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
		val formatter: OBDComputerWidgetFormatter,
		val type: OBDTypeWidget,
		var averageTimeSeconds: Int) {
		private var values: MutableList<OBDDataField<Any>> = ArrayList()
		private var value: Any? = null
		private var cachedVersion = 0
		private var version = 0

		fun computeValue(): Any? {
			if (cachedVersion != version) {
				val v = version
				val computedValue = compute()
				value = if ("N/A" == computedValue) {
					"N/A"
				} else {
					formatter.format(compute())
				}
				cachedVersion = v
			}
			return value
		}

		private fun compute(): Any? {
			val locValues = ArrayList(values)
			if (locValues.size > 0 && locValues[0] == OBDDataField.NO_DATA) {
				return "N/A"
			}
			return when (type) {
				TEMPERATURE_AMBIENT,
				TEMPERATURE_COOLANT,
				TEMPERATURE_INTAKE,
				SPEED,
				BATTERY_VOLTAGE,
				FUEL_CONSUMPTION_RATE_SENSOR,
				RPM -> {
					if (averageTimeSeconds == 0 && locValues.size > 0) {
						locValues[locValues.size - 1].value
					} else {
						averageNumber(locValues)
					}
				}

				FUEL_CONSUMPTION_RATE_PERCENT_HOUR -> {
					if (locValues.size >= 2) {
						val first = locValues[0]
						val last = locValues[locValues.size - 1]
						val diffPerc = first.value as Float - last.value as Float
						val diffTime = last.timestamp - first.timestamp
						println("diftime $diffTime; diffPerc $diffPerc")
						diffPerc / diffTime * 1000 * 3600
					} else {
						null
					}
				}

				FUEL_CONSUMPTION_RATE_LITER_HOUR -> {
					if (locValues.size >= 2) {
						val first = locValues[0]
						val last = locValues[locValues.size - 1]
						val diffPerc = first.value as Float - last.value as Float
						val diffTime = last.timestamp - first.timestamp
						println("diftime $diffTime; diffPerc $diffPerc")
						fuelTank * diffPerc / 100 / diffTime * 1000 * 3600
					} else {
						null
					}
				}

				FUEL_LEFT_KM -> {
					// works for window > 15 min
					if (locValues.size >= 2) {
						val first = locValues[0]
						val last = locValues[locValues.size - 1]
						val diffPerc = last.value as Float - first.value as Float
						if (diffPerc > 0) {
							var start = 0
							var end = locations.size - 1
							while (start < locations.size) {
								if (locations[start].time > first.timestamp) {
									break
								}
								start++
							}
							while (end >= 0) {
								if (locations[end].time < last.timestamp) {
									break
								}
								end--
							}
							var dist = 0.0
							if (start < end) {
								for (k in start until end) {
									dist += KMapUtils.getDistance(
										locations[start].latLon,
										locations[end].latLon)
								}
							}
							if (dist > 0) {
								val lastPerc = last.value as Float
								lastPerc / diffPerc * dist
							}
						}
					}
					null
				}

				FUEL_LEFT_PERCENT -> {
					if (locValues.size > 0) {
						100 - locValues[locValues.size - 1].value as Float
					} else {
						null
					}
				}

				FUEL_LEFT_LITER -> {
					if (locValues.size > 0) {
						fuelTank * (100 - locValues[locValues.size - 1].value as Float) / 100
					} else {
						null
					}
				}

				FUEL_PERCENT -> {
					if (locValues.size > 0) {
						locValues[locValues.size - 1].value
					} else {
						null
					}
				}

				FUEL_TYPE -> {
					if (locValues.size > 0) {
						locValues[locValues.size - 1].value
					} else {
						null
					}
				}

				VIN -> if (locValues.size > 0) {
					(locValues[locValues.size - 1]).value
				} else {
					null
				}
			}
		}

		fun acceptValue(value: Map<OBDCommand, OBDDataField<Any>?>) {
			value[type.requiredCommand]?.let {
				if (it == OBDDataField.NO_DATA) {
					if(values.isEmpty()) {
						version++
					}
					values = mutableListOf(it)
				} else {
					version++
					values.add(it)
				}
			}
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
}