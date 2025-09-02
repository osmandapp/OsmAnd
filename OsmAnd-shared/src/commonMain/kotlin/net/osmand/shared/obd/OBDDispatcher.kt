package net.osmand.shared.obd

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.osmand.shared.extensions.format
import net.osmand.shared.obd.Obd2Connection.COMMAND_TYPE.LIVE
import net.osmand.shared.util.KCollectionUtils
import net.osmand.shared.util.LoggerFactory
import okio.Buffer
import okio.Sink
import okio.Source
import kotlin.coroutines.coroutineContext

class OBDDispatcher(val debug: Boolean = false) {

	private var commandQueue = listOf<OBDCommand>()
	private var inputStream: Source? = null
	private var outputStream: Sink? = null
	private val log = LoggerFactory.getLogger("OBDDispatcher")
	private var readStatusListener: OBDReadStatusListener? = null
	private var sensorDataCache = HashMap<OBDCommand, OBDDataField<Any>?>()
	private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

	private val initOBDTimeout = 30000
	private val initOBDRetryOffset = 2000L

	interface OBDReadStatusListener {
		fun onIOError()
	}

	fun connect(connector: OBDConnector) {
		scope.launch {
			try {
				val connectionResult = connector.connect()
				if (connectionResult == null) {
					connector.onConnectionFailed()
				} else {
					connector.onConnectionSuccess()
					inputStream = connectionResult.first
					outputStream = connectionResult.second
					startReadObdLooper()
				}
			} catch (cancelError: CancellationException) {
				log("OBD reading canceled")
			} catch (e: Exception) {
				log.error("Unexpected error in connect: ${e.message}", e)
				readStatusListener?.onIOError()
				connector.onConnectionFailed()
			} finally {
				connector.disconnect()
				cleanupResources()
			}
		}
	}

	private suspend fun startReadObdLooper() {
		log("Start reading obd with $inputStream and $outputStream")
		val connection = Obd2Connection(createTransport(), this)
		val startInitTime = Clock.System.now().toEpochMilliseconds()
		while (!connection.initialize() && coroutineContext.isActive) {
			delay(initOBDRetryOffset)
			if (Clock.System.now().toEpochMilliseconds() - startInitTime > initOBDTimeout) {
				break
			}
		}
		if (!connection.isInitialized()) {
			connection.finish()
			return
		}
		try {
			var cycleIndex = 0 //todo remove after testing
			while (isConnected(connection) && coroutineContext.isActive) {
				if (commandQueue.isEmpty()) {
					delay(500) // Prevent busy-looping when there are no commands
					continue
				}
				log.debug("Start new commandsRound $cycleIndex")
				cycleIndex++
				commandQueue.forEach { command ->
					coroutineContext.ensureActive()
					handleCommand(command, connection)
				}
				coroutineContext.ensureActive()
				OBDDataComputer.acceptValue(sensorDataCache)
			}
		} finally {
			connection.finish()
		}
	}

	private fun createTransport(): UnderlyingTransport = object : UnderlyingTransport {
		override suspend fun write(bytes: ByteArray) {
			val buffer = Buffer().apply { write(bytes) }
			outputStream?.write(buffer, buffer.size)
		}

		override suspend fun read(): String {
			val readBuffer = Buffer()
			val loopDelay = 100L
			var ticks = 0L
			val timeout = 15000L
			val timeoutTicks = timeout / loopDelay
			while (coroutineContext.isActive) {
				val bytesRead = inputStream?.read(readBuffer, 1024)
				if (bytesRead != null && bytesRead > 0) {
					return readBuffer.readUtf8()
				}
				if (bytesRead == -1L) { // End of stream
					return UnderlyingTransport.UNABLETOREAD
				}
				if (ticks > timeoutTicks) {
					return UnderlyingTransport.TIMEOUT
				}
				// Suspend for a short duration to avoid hammering the CPU
				delay(loopDelay)
				ticks++
			}
			return UnderlyingTransport.CONTEXTINACTIVE
		}
	}

	private fun isConnected(connection: Obd2Connection): Boolean =
		inputStream != null && outputStream != null && !connection.isFinished()

	private suspend fun handleCommand(command: OBDCommand, connection: Obd2Connection): OBDResponse {
		if (command.isStale && sensorDataCache[command] != null) {
			return OBDResponse.OK
		}

		val fullCommand = if (command.isTextCommand()) command.textCommand!! else "%02X%02X".format(
			command.commandGroup,
			command.command)
		val commandResult = connection.run(fullCommand, command)
		commandResult.let {
			when {
				it.isValid() && it.result.size >= command.responseLength -> {
					sensorDataCache[command] = command.parseResponse(it.result)
				}

				it == OBDResponse.NO_DATA -> sensorDataCache[command] = OBDDataField.NO_DATA
				it == OBDResponse.CONNECTION_FAILURE -> readStatusListener?.onIOError()
				else -> log("Incorrect response length or unknown error for command $command")
			}
		}
		return commandResult
	}

	fun addCommand(commandToRead: OBDCommand) {
		if (!commandQueue.contains(commandToRead)) {
			commandQueue = KCollectionUtils.addToList(commandQueue, commandToRead)
		}
	}

	fun clearCommands() {
		commandQueue = emptyList()
	}

	fun removeCommand(commandToStopReading: OBDCommand) {
		commandQueue = KCollectionUtils.removeFromList(commandQueue, commandToStopReading)
	}

	fun setReadStatusListener(listener: OBDReadStatusListener?) {
		readStatusListener = listener
	}

	private fun cleanupResources() {
		inputStream = null
		outputStream = null
		OBDDataComputer.clearCache()
		readStatusListener = null
	}

	fun stopReading() {
		log("stop reading")
		scope.cancel()
		log("after stop reading")
	}

	fun getRawData(): HashMap<OBDCommand, OBDDataField<Any>?> = HashMap(sensorDataCache)

	private fun log(msg: String) {
		if (debug) {
			log.debug(msg)
		} else {
			log.info(msg)
		}
	}
}
