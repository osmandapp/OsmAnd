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

object OBDDataComputer {

	private val log = LoggerFactory.getLogger("OBDDataComputer")

	var locations = listOf<OBDLocation>()
	var widgets: List<OBDComputerWidget> = ArrayList()
		private set
	var timeoutForInstantValuesSeconds = 0

	class OBDLocation(val time: Long, val latLon: KLatLon)

	fun acceptValue(value: Map<OBDCommand, OBDDataField?>) {
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
			widget.type.requiredCommands.forEach { OBDDispatcher.addCommand(it) }
		}
	}

	enum class OBDTypeWidget(
		val locationNeeded: Boolean,
		val requiredCommands: List<OBDCommand>,
		val valueCreator: (Map<OBDCommand, OBDDataField?>) -> OBDValue) {
		SPEED(false,
			listOf(OBD_SPEED_COMMAND),
			{ data -> OBDIntValue(OBD_SPEED_COMMAND, data) }),
		RPM(false,
			listOf(OBD_RPM_COMMAND),
			{ data -> OBDIntValue(OBD_RPM_COMMAND, data) }),
		FUEL_LEFT_DISTANCE(true,
			listOf(OBD_FUEL_LEVEL_COMMAND),
			{ data -> OBDValue(OBD_FUEL_LEVEL_COMMAND, data) }),
		FUEL_LEFT_LITERS(false,
			listOf(OBD_FUEL_LEVEL_COMMAND),
			{ data -> OBDValue(OBD_FUEL_LEVEL_COMMAND, data) }),
		FUEL_LEFT_PERCENT(false,
			listOf(OBD_FUEL_LEVEL_COMMAND),
			{ data -> OBDValue(OBD_FUEL_LEVEL_COMMAND, data) }),
		FUEL_CONSUMPTION_RATE(false,
			listOf(OBD_FUEL_LEVEL_COMMAND),
			{ data -> OBDValue(OBD_FUEL_LEVEL_COMMAND, data) }),
		TEMPERATURE_INTAKE(false,
			listOf(OBD_AIR_INTAKE_TEMP_COMMAND),
			{ data -> OBDIntValue(OBD_AIR_INTAKE_TEMP_COMMAND, data) }),
		TEMPERATURE_AMBIENT(false,
			listOf(OBD_AMBIENT_AIR_TEMPERATURE_COMMAND),
			{ data -> OBDIntValue(OBD_AMBIENT_AIR_TEMPERATURE_COMMAND, data) }),
		BATTERY_VOLTAGE(false,
			listOf(OBD_BATTERY_VOLTAGE_COMMAND),
			{ data -> OBDValue(OBD_BATTERY_VOLTAGE_COMMAND, data) }),
		FUEL_TYPE(false,
			listOf(OBD_FUEL_TYPE_COMMAND),
			{ data -> OBDIntValue(OBD_FUEL_TYPE_COMMAND, data) }),
		TEMPERATURE_COOLANT(false,
			listOf(OBD_ENGINE_COOLANT_TEMP_COMMAND),
			{ data -> OBDIntValue(OBD_ENGINE_COOLANT_TEMP_COMMAND, data) });
	}

	private fun averageDouble(values: List<OBDValue>): Double? =
		if (values.isNotEmpty()) values.sumOf { it.doubleValue } / values.size else null

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
		private var values: MutableList<OBDValue> = ArrayList()
		private var value: Any? = null
		private var cachedVersion = 0
		private var version = 0

		fun computeValue(): Any? {
			if (cachedVersion != version) {
				val v = version
				value = formatter.format(compute())
				cachedVersion = v
			}
			return value
		}

		private fun compute(): Any? {
			val locValues = ArrayList(values)
			return when (type) {
				TEMPERATURE_AMBIENT,
				TEMPERATURE_COOLANT,
				TEMPERATURE_INTAKE,
				SPEED,
				BATTERY_VOLTAGE,
				RPM -> {
					if (averageTimeSeconds == 0 && locValues.size > 0) {
						locValues[locValues.size - 1].doubleValue
					} else {
						averageDouble(locValues)
					}
				}

				FUEL_CONSUMPTION_RATE -> {
					if (locValues.size >= 2) {
						val first = locValues[0]
						val last = locValues[locValues.size - 1]
						val diffPerc = first.doubleValue - last.doubleValue
						val diffTime = last.timestamp - first.timestamp
						println("diftime $diffTime; diffPerc $diffPerc")
						diffPerc / diffTime * 1000 * 3600
					} else {
						null
					}
				}

				FUEL_LEFT_DISTANCE -> {
					// works for window > 15 min
					if (locValues.size >= 2) {
						val first = locValues[0]
						val last = locValues[locValues.size - 1]
						val diffPerc = last.doubleValue - first.doubleValue
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
								val lastPerc = last.doubleValue
								lastPerc / diffPerc * dist
							}
						}
					}
					null
				}

				FUEL_LEFT_LITERS,
				FUEL_LEFT_PERCENT -> {
					if (locValues.size > 0) {
						locValues[locValues.size - 1].doubleValue
					} else {
						null
					}
				}

				FUEL_TYPE -> {
					if (locValues.size > 0) {
						locValues[locValues.size - 1].doubleValue
					} else {
						null
					}
				}
			}
		}

		fun acceptValue(value: Map<OBDCommand, OBDDataField?>) {
			val obdValue = type.valueCreator(value)
			if (obdValue.isAccepted) {
				version++
				values.add(obdValue)
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

	open class OBDValue() {
		var timestamp = currentTimeMillis()
		var isAccepted: Boolean = false
			protected set
		var doubleValue: Double = 0.0

		constructor(cmd: OBDCommand, data: Map<OBDCommand, OBDDataField?>) : this() {
			val dataField = data[cmd]
			dataField?.let {
				doubleValue = it.getValue().toDouble()
			}
			isAccepted = acceptData(dataField)
		}

		protected open fun acceptData(dataField: OBDDataField?): Boolean {
			var accepted = false
			dataField?.let {
				try {
					doubleValue = it.getValue().toDouble()
					accepted = true
				} catch (error: NumberFormatException) {
					log.error("Can\'t parse ${it.getValue()} to Double")
				}
			}
			return accepted
		}
	}

	class OBDIntValue(cmd: OBDCommand, data: Map<OBDCommand, OBDDataField?>) : OBDValue(cmd, data) {
		var intValue = 0
		override fun acceptData(dataField: OBDDataField?): Boolean {
			var accepted = false
			dataField?.let {
				try {
					intValue = it.getValue().toInt()
					accepted = true
				} catch (error: NumberFormatException) {
					log.error("Can\'t parse ${it.getValue()} to Int")
				}
			}
			return accepted
		}
	}
}