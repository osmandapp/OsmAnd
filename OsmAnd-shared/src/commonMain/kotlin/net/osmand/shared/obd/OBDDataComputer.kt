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


// Types computed from the difference of two fuel levels. The sensor changes in 1% steps
// (~0.5 L), so they need a window long enough to see two different levels and to keep
// the GPS locations driven in between. Widgets use defaultAverageTime, not a list of types.
private const val FUEL_CONSUMPTION_DEFAULT_AVERAGE_TIME = 5 * 60

object OBDDataComputer {

	// Noise limits: more than 100 L/100 km or less than 1 km/L (1000 m/L) is discarded.
	// The distance-per-volume limit is a lower bound, the volume-per-distance one an upper bound.
	private const val LITER_KM_CONSUMPTION_LIMIT = 100
	private const val M_LITER_CONSUMPTION_LIMIT = 1000

	private const val LITER_HOUR_CONSUMPTION_LIMIT = 100

	private val log = LoggerFactory.getLogger("OBDDataComputer")

	private val osmAndSettings: SettingsAPI = PlatformUtil.getOsmAndContext().getSettings()
	const val DEFAULT_FUEL_TANK_CAPACITY = 52f
	private const val FUEL_LVL_WINDOW_SECONDS = 30
	private const val FUEL_LVL_MIN_SAMPLES = 5
	private const val FUEL_LVL_MAX_GAP_SECONDS = 60
	// Percent of the tank; smaller rises are sensor noise (seen: 3 steps of 100/255 = 1.2 %)
	private const val REFUEL_MIN_LEVEL_INCREASE = 5f
	private const val FUEL_TANK_CAPACITY_SETTING_ID = "fuel_tank_capacity"
	private val FUEL_LEVEL_DIFF_TYPES = setOf(
		OBDTypeWidget.FUEL_LEFT_KM,
		OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_KM,
		OBDTypeWidget.FUEL_CONSUMPTION_RATE_M_PER_LITER,
		OBDTypeWidget.FUEL_CONSUMPTION_RATE_PERCENT_HOUR,
		OBDTypeWidget.FUEL_CONSUMPTION_RATE_LITER_HOUR)
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

		companion object {
			fun Companion.findByGpxTag(key: String?): OBDTypeWidget? {
				if (key == null) return null

				return OBDTypeWidget.entries.firstOrNull { widget ->
					widget.requiredCommand.gpxTag == key
				}
			}
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
		private var levelSamples: List<OBDDataField<Any>> = ArrayList()
		private var lastFuelValue: Any? = null
		private var lastFuelReadingTime = 0L
		private var fuelNoData = false
		private var currentLevel: Float? = null
		private var value: Any? = null
		private var cachedVersion = 0
		private var version = 0

		fun clearData() {
			values = ArrayList()
			levelSamples = ArrayList()
			lastFuelValue = null
			lastFuelReadingTime = 0L
			fuelNoData = false
			currentLevel = null
			value = null
			cachedVersion = 0
			version = 0
		}

		fun computeValue(): Any? {
			if (cachedVersion != version) {
				val v = version
				value = if (type in FUEL_LEVEL_DIFF_TYPES) computeFuelLevelDiff() else compute()
				cachedVersion = v
			}
			return value
		}

		// A history restart, a filtered result or a short loss of readings keeps the last value;
		// it goes only when the level has not been read for FUEL_LVL_MAX_GAP_SECONDS
		private fun computeFuelLevelDiff(): Any? {
			val readingLost = lastFuelReadingTime == 0L
					|| currentTimeMillis() - lastFuelReadingTime > FUEL_LVL_MAX_GAP_SECONDS * 1000L
			if (fuelNoData && readingLost) {
				lastFuelValue = null
				return "N/A"
			}
			val result = compute()
			if (result is Number && !result.toDouble().isNaN()) {
				lastFuelValue = result
				return result
			}
			return lastFuelValue ?: result
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
						val result = calculateFuelConsumption(locValues)?.let { getFuelTank() * it / 100 }
						if (result != null && result > LITER_HOUR_CONSUMPTION_LIMIT) {
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
								return if (result < M_LITER_CONSUMPTION_LIMIT) {
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

		private fun calculateFuelConsumption(locValues: ArrayList<OBDDataField<Any>>): Float? {
			val first = locValues[locValues.size - 2]
			val last = locValues[locValues.size - 1]
			val diffPerc = (first.value as Number).toFloat() - (last.value as Number).toFloat()
			val diffTime = last.timestamp - first.timestamp
			return if (diffPerc > 0 && diffTime > 0) diffPerc / diffTime * 1000 * 3600 else null
		}

		fun acceptValue(value: Map<OBDCommand, OBDDataField<Any>?>) {
			value[type.requiredCommand]?.let {
				if (type in FUEL_LEVEL_DIFF_TYPES) {
					acceptFuelLevel(it)
				} else if (it == OBDDataField.NO_DATA) {
					if (values.isEmpty()) {
						version++
					}
					values = mutableListOf(it)
				} else {
					version++
					values = KCollectionUtils.addToList(values, it)
				}
			}
		}

		// The raw level wobbles around a sensor step (sloshing, slopes), so a new level is taken
		// from the median of the readings over FUEL_LVL_WINDOW_SECONDS. The median lags behind
		// by about the same time at every step, so the time between two accepted levels stays right.
		// The level only goes down while driving: a rise is noise unless it is big enough
		// to be a refuel, which restarts the history.
		private fun acceptFuelLevel(data: OBDDataField<Any>) {
			val prevSample = levelSamples.lastOrNull()
			// the dispatcher repeats its cached reading object when a request fails
			if (data === OBDDataField.NO_DATA || data === prevSample) {
				fuelNoData = true
				// recompute to drop the last value once the readings are lost for too long
				version++
				return
			}
			fuelNoData = false
			val now = data.timestamp
			lastFuelReadingTime = now
			if (prevSample != null && now - prevSample.timestamp > FUEL_LVL_MAX_GAP_SECONDS * 1000L) {
				// a stop without readings would count into the interval of the next level
				// while the GPS distance driven before it is already gone
				log.debug("Fuel level history restarted after a gap of ${(now - prevSample.timestamp) / 1000} s")
				levelSamples = emptyList()
				currentLevel = null
				if (values.isNotEmpty()) {
					version++
					values = emptyList()
				}
			}
			val windowStart = now - FUEL_LVL_WINDOW_SECONDS * 1000L
			levelSamples = KCollectionUtils.addToList(levelSamples.filter { it.timestamp >= windowStart }, data)
			val first = levelSamples.first()
			if (levelSamples.size < FUEL_LVL_MIN_SAMPLES || now - first.timestamp < FUEL_LVL_WINDOW_SECONDS * 1000L / 2) {
				return
			}
			val newLvl = levelSamples.map { (it.value as Number).toFloat() }.sorted()[levelSamples.size / 2]
			val lastLvl = currentLevel
			if (lastLvl != null && newLvl < lastLvl) {
				log.debug("New fuel level accepted. last $lastLvl; new $newLvl")
				currentLevel = newLvl
				version++
				values = KCollectionUtils.addToList(values, OBDDataField<Any>(newLvl).also { it.timestamp = now })
			} else if (lastLvl == null || newLvl - lastLvl >= REFUEL_MIN_LEVEL_INCREASE) {
				// when the level was reached is unknown, so the history starts from the next drop
				log.debug("Fuel level history restarted. last $lastLvl; new $newLvl")
				currentLevel = newLvl
				if (values.isNotEmpty()) {
					version++
					values = emptyList()
				}
			}
		}

		fun resetLocations() {
			values = ArrayList()
			lastFuelValue = null
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