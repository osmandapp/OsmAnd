package net.osmand.shared.obd

import kotlinx.datetime.Clock
import net.osmand.shared.data.KLatLon
import net.osmand.shared.extensions.format
import net.osmand.shared.obd.OBDDataComputer.OBDTypeWidget.*
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import kotlin.math.max

object OBDDataComputer {

	private val log = LoggerFactory.getLogger("OBDDataComputer")

	var locations: MutableList<OBDLocation> = ArrayList()
	var widgets: MutableList<OBDComputerWidget> = ArrayList()
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
		if (widgets.size > 0) {
			// concurrency - change collection in one thread. Other places only read
			locations.add(l)
			cleanupLocations()
		}
	}

	fun compute() {
		val now: Long = Clock.System.now().toEpochMilliseconds()
		for (widget in widgets) {
			widget.cleanup(now)
			widget.computeValue()
		}
	}

	private fun cleanupLocations() {
		val now: Long = Clock.System.now().toEpochMilliseconds()
		// calculate maximum window to clean up
		var window = timeoutForInstantValuesSeconds
		for (widget in widgets) {
			if (widget.type.locationNeeded) {
				window = max(window, widget.averageSeconds)
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
		averageSeconds: Int,
		formatter: OBDComputerWidgetFormatter = OBDComputerWidgetFormatter()
	): OBDComputerWidget {
		val widget = OBDComputerWidget(formatter, type, averageSeconds)
		widgets.add(widget)
		return widget
	}

	fun removeWidget(w: OBDComputerWidget) {
		widgets.remove(w)
	}

	enum class OBDTypeWidget(
		val locationNeeded: Boolean,
		val valueCreator: (Map<OBDCommand, OBDDataField?>) -> OBDValue) {
		SPEED(false,
			{ data -> OBDIntValue(OBDCommand.OBD_SPEED_COMMAND, data) }),
		RPM(false,
			{ data -> OBDIntValue(OBDCommand.OBD_RPM_COMMAND, data) }),
		FUEL_LEFT_DISTANCE(true,
			{ data -> OBDValue(OBDCommand.OBD_FUEL_LEVEL_COMMAND, data) }),
		FUEL_LEFT_LITERS(false,
			{ data -> OBDValue(OBDCommand.OBD_FUEL_LEVEL_COMMAND, data) }),
		FUEL_LEFT_PERCENT(false,
			{ data -> OBDValue(OBDCommand.OBD_FUEL_LEVEL_COMMAND, data) }),
		FUEL_CONSUMPTION_RATE(false,
			{ data -> OBDValue(OBDCommand.OBD_FUEL_LEVEL_COMMAND, data) }),
		TEMPERATURE_INTAKE(
			false,
			{ data -> OBDIntValue(OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND, data) }),
		TEMPERATURE_AMBIENT(
			false,
			{ data -> OBDIntValue(OBDCommand.OBD_AMBIENT_AIR_TEMPERATURE_COMMAND, data) }),
		BATTERY_VOLTAGE(
			false,
			{ data -> OBDValue(OBDCommand.OBD_BATTERY_VOLTAGE_COMMAND, data) }),
		FUEL_TYPE(
			false,
			{ data -> OBDIntValue(OBDCommand.OBD_FUEL_TYPE_COMMAND, data) }),
		TEMPERATURE_COOLANT(
			false,
			{ data -> OBDIntValue(OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND, data) });

	}

	private fun averageDouble(values: List<OBDValue>): Any? {
		if (values.isNotEmpty()) {
			var sum = 0.0
			var cnt = 0
			for (o in values) {
				sum += o.doubleValue
				cnt++
			}
			return sum / cnt
		}
		return null
	}

	open class OBDComputerWidgetFormatter {
		open fun format(v: Any?): String {
			return if (v == null) {
				"-"
			} else {
				"%s".format(v)
			}
		}
	}

	class OBDComputerWidget(
		val formatter: OBDComputerWidgetFormatter,
		val type: OBDTypeWidget,
		var averageSeconds: Int) {
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
			return when (type) {
				TEMPERATURE_AMBIENT,
				TEMPERATURE_COOLANT,
				TEMPERATURE_INTAKE,
				SPEED,
				BATTERY_VOLTAGE,
				RPM -> {
					if (averageSeconds == 0 && values.size > 0) {
						values[values.size - 1].doubleValue
					} else {
						averageDouble(values)
					}
				}

				FUEL_CONSUMPTION_RATE -> {
					if (values.size >= 2) {
						val first = values[0]
						val last = values[values.size - 1]
						val diffPerc = last.doubleValue - first.doubleValue
						val diffTime = last.timestamp - first.timestamp
						diffPerc / diffTime / 1000
					}
					null
				}

				FUEL_LEFT_DISTANCE -> {
					// works for window > 15 min
					if (values.size >= 2) {
						val first = values[0]
						val last = values[values.size - 1]
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
					if (values.size > 0) {
						values[values.size - 1].doubleValue
					} else {
						null
					}
				}

				FUEL_TYPE -> {
					if (values.size > 0) {
						values[values.size - 1].doubleValue
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
			val timeout = if (averageSeconds > 0) averageSeconds else timeoutForInstantValuesSeconds
			var inWindow = 0
			while (inWindow < values.size) {
				if (values[inWindow].timestamp >= now - timeout * 1000) {
					break
				}
				inWindow++
			}
			if (inWindow > 0) {
				values = values.subList(inWindow, values.size)
			}
		}
	}

	open class OBDValue() {
		var timestamp = Clock.System.now().toEpochMilliseconds()
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