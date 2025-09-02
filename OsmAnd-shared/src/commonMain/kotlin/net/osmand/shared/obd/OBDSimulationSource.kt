package net.osmand.shared.obd

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import net.osmand.shared.extensions.currentTimeMillis
import net.osmand.shared.util.LoggerFactory
import okio.Buffer
import okio.Sink
import okio.Source
import okio.Timeout
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class OBDSimulationSource {

	private var bufferToRead: String? = null
	private var fuelLeftLvl = 255
	private var lastFuelChangedTime = 0L
	private val NEW_DATA_PACK_DELAY = 2000L
	private val CHANGE_FUEL_LV_TIMEOUT = 100000
	private val log = LoggerFactory.getLogger("ODBSimulationSource")
	private var showFuelPeak = true

	val writer: Sink = object : Sink {
		override fun close() {
		}

		override fun flush() {
		}

		override fun timeout(): Timeout {
			return Timeout.NONE
		}

		override fun write(source: Buffer, byteCount: Long) = runBlocking {
			val fullCommand = source.readUtf8()
			val splitCommand = fullCommand.chunked(2)
			val commandCode = splitCommand[0]
			if (Obd2Connection.isInitCommand(fullCommand.replace("\r", "").trim())) {
				bufferToRead = ">"
				return@runBlocking
			}
			val commandTypeCode = when (commandCode) {
				"01" -> "41"
				"09" -> "49"
				else -> {
					throw IllegalArgumentException("Not supported command group $commandCode")
				}
			}
			val command = splitCommand[1]
			val obdCommand = OBDCommand.getByCode(commandCode.toInt(16), command.toInt(16))
			if (obdCommand?.ordinal == OBDCommand.entries.size - 1) {
				delay(NEW_DATA_PACK_DELAY)
			}
			val response = when (obdCommand) {
				OBDCommand.OBD_VIN_COMMAND -> ""
				OBDCommand.OBD_CALCULATED_ENGINE_LOAD_COMMAND -> toNormalizedHex(102)
				OBDCommand.OBD_THROTTLE_POSITION_COMMAND -> toNormalizedHex(66)
				OBDCommand.OBD_ENGINE_OIL_TEMPERATURE_COMMAND -> toNormalizedHex(130)
				OBDCommand.OBD_FUEL_PRESSURE_COMMAND -> toNormalizedHex(Random.nextInt(24500, 35000))
				OBDCommand.OBD_BATTERY_VOLTAGE_COMMAND -> toNormalizedHex(12700)
				OBDCommand.OBD_AMBIENT_AIR_TEMPERATURE_COMMAND -> toNormalizedHex(45)
				OBDCommand.OBD_RPM_COMMAND -> toNormalizedHex(8000)
				OBDCommand.OBD_ENGINE_RUNTIME_COMMAND -> toNormalizedHex(2000)
				OBDCommand.OBD_SPEED_COMMAND -> toNormalizedHex(99)
				OBDCommand.OBD_AIR_INTAKE_TEMP_COMMAND -> toNormalizedHex(100)
				OBDCommand.OBD_ENGINE_COOLANT_TEMP_COMMAND -> toNormalizedHex(80)
				OBDCommand.OBD_FUEL_CONSUMPTION_RATE_COMMAND -> {
					bufferToRead = "NODATA>"
					return@runBlocking
				}

				OBDCommand.OBD_FUEL_TYPE_COMMAND -> "01"
				OBDCommand.OBD_FUEL_LEVEL_COMMAND -> {
					val curTime = currentTimeMillis()
					if (curTime - lastFuelChangedTime > CHANGE_FUEL_LV_TIMEOUT) {
						lastFuelChangedTime = curTime
						fuelLeftLvl = max(0, --fuelLeftLvl)
						if (fuelLeftLvl < 255 * 80 / 100) {
							fuelLeftLvl = 250
							showFuelPeak = true
						}
					}
					log.debug("fuelLeftLvl $fuelLeftLvl; curTime $curTime; lastFuelChangedTime $lastFuelChangedTime")
					if (fuelLeftLvl < 255 * 90 / 100 && showFuelPeak) {
						showFuelPeak = false
						toNormalizedHex(250)
					} else {
						toNormalizedHex(fuelLeftLvl)
					}
				}

				null -> ""
				OBDCommand.OBD_ALT_BATTERY_VOLTAGE_COMMAND -> {
					bufferToRead = "NODATA>"
					return@runBlocking
				}
			}
			bufferToRead = "$commandTypeCode$command$response>"
		}
	}

	private fun toNormalizedHex(data: Int): String {
		val hexString = data.toUInt().toString(16).uppercase()
		return if (hexString.length % 2 != 0) {
			"0$hexString"
		} else {
			hexString
		}
	}

	val reader: Source = object : Source {
		override fun close() {
		}

		override fun read(sink: Buffer, byteCount: Long): Long {
			bufferToRead?.let {
				val readCount = min(byteCount, it.length.toLong())
				if (readCount > 0) {
					val data = it.substring(0, readCount.toInt())
					bufferToRead = it.substring(readCount.toInt())
					sink.writeUtf8(data)
					return readCount
				}
			}
			return 0
		}

		override fun timeout(): Timeout {
			return Timeout.NONE
		}
	}
}