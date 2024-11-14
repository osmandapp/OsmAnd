package net.osmand.shared.obd

import kotlinx.coroutines.*
import net.osmand.shared.extensions.format
import net.osmand.shared.util.KCollectionUtils
import net.osmand.shared.util.LoggerFactory
import okio.Buffer
import okio.Sink
import okio.Source
import kotlin.coroutines.CoroutineContext

class OBDDispatcher(val debug: Boolean = false) {

	private var commandQueue = listOf<OBDCommand>()
	private var inputStream: Source? = null
	private var outputStream: Sink? = null
	private val log = LoggerFactory.getLogger("OBDDispatcher")
	private var readStatusListener: OBDReadStatusListener? = null
	private var sensorDataCache = HashMap<OBDCommand, OBDDataField<Any>?>()
	private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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
					startReadObdLooper(coroutineContext)
				}
			} catch (cancelError: CancellationException) {
				log("OBD reading canceled")
			} catch (e: Exception) {
				log("Unexpected error in connect: ${e.message}")
				readStatusListener?.onIOError()
			} finally {
				connector.disconnect()
				cleanupResources()
			}
		}
	}

	private fun startReadObdLooper(context: CoroutineContext) {
		log("Start reading obd with $inputStream and $outputStream")
		val connection = Obd2Connection(createTransport(), this)
		try {
			while (isConnected(connection)) {
				commandQueue.forEach { command ->
					context.ensureActive()
					handleCommand(command, connection)
				}
				context.ensureActive()
				OBDDataComputer.acceptValue(sensorDataCache)
			}
		} finally {
			connection.finish()
		}
	}

	private fun createTransport(): UnderlyingTransport = object : UnderlyingTransport {
		override fun write(bytes: ByteArray) {
			val buffer = Buffer().apply { write(bytes) }
			outputStream?.write(buffer, buffer.size)
		}

		override fun readByte(): Byte? {
			val readBuffer = Buffer()
			return if (inputStream?.read(readBuffer, 1) == 1L) readBuffer.readByte() else null
		}
	}

	private fun isConnected(connection: Obd2Connection): Boolean =
		inputStream != null && outputStream != null && !connection.isFinished()

	private fun handleCommand(command: OBDCommand, connection: Obd2Connection) {
		if (command.isStale && sensorDataCache[command] != null) {
			return
		}

		val fullCommand = "%02X%02X".format(command.commandGroup, command.command)
		val commandResult = connection.run(fullCommand, command.command, command.commandType)
		commandResult.let {
			when {
				it.isValid() && it.result.size >= command.responseLength -> {
					sensorDataCache[command] = command.parseResponse(it.result)
				}

				it == OBDResponse.NO_DATA -> sensorDataCache[command] = OBDDataField.NO_DATA
				it == OBDResponse.ERROR -> readStatusListener?.onIOError()
				else -> log("Incorrect response length or unknown error for command $command")
			}
		}
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