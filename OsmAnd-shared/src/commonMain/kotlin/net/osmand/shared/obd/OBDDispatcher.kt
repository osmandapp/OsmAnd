package net.osmand.shared.obd

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import net.osmand.shared.extensions.format
import net.osmand.shared.util.KCollectionUtils
import net.osmand.shared.util.LoggerFactory
import okio.Buffer
import okio.IOException
import okio.Sink
import okio.Source


object OBDDispatcher {

	private var commandQueue = listOf<OBDCommand>()
	private val staleCommandsCache: MutableMap<OBDCommand, String> = HashMap()
	private var inputStream: Source? = null
	private var outputStream: Sink? = null
	private val log = LoggerFactory.getLogger("OBDDispatcher")
	private var job: Job? = null
	private var scope: CoroutineScope? = null
	private var readStatusListener: OBDReadStatusListener? = null
	private val sensorDataCache = HashMap<OBDCommand, OBDDataField<Any>?>()
	private var obd2Connection: Obd2Connection? = null

	interface OBDReadStatusListener {
		fun onIOError()
	}

	private fun startReadObdLooper() {
		job = Job()
		scope = CoroutineScope(Dispatchers.IO + job!!)
		scope!!.launch {
			try {
				log.debug("Start reading obd with $inputStream and $outputStream")
				obd2Connection = Obd2Connection(object : UnderlyingTransport {
					override fun write(bytes: ByteArray) {
						val buffer = Buffer()
						buffer.write(bytes)
						outputStream?.write(buffer, buffer.size)
					}

					override fun readByte(): Byte? {
						val readBuffer = Buffer()
						return if (inputStream?.read(readBuffer, 1) == 1L) {
							readBuffer.readByte()
						} else {
							null
						}
					}
				})
				while (true) {
					try {
						for (command in commandQueue) {
							if (command.isStale) {
								val cachedCommandResponse = staleCommandsCache[command]
								if (cachedCommandResponse != null && cachedCommandResponse != OBDUtils.INVALID_RESPONSE_CODE) {
									continue
								}
							}
							val hexGroupCode = "%02X".format(command.commandGroup)
							val hexCode = "%02X".format(command.command)
							val fullCommand = "$hexGroupCode$hexCode"
							val commandResult =
								obd2Connection!!.run(fullCommand, command.command, command.commandType)
							if(commandResult.isValid()) {
								sensorDataCache[command] = command.parseResponse(commandResult.result)
							}
						}
					} catch (error: IOException) {
						log.error("Run OBD looper error. $error")
						readStatusListener?.onIOError()
					}
					OBDDataComputer.acceptValue(sensorDataCache)
				}
			} catch (cancelError: CancellationException) {
				log.debug("OBD reading canceled")
			}
		}
	}

	fun addCommand(commandToRead: OBDCommand) {
		if (commandQueue.indexOf(commandToRead) == -1) {
			commandQueue = KCollectionUtils.addToList(commandQueue, commandToRead)
		}
	}

	fun clearCommands() {
		commandQueue = listOf()
	}

	fun removeCommand(commandToStopReading: OBDCommand) {
		commandQueue = KCollectionUtils.removeFromList(commandQueue, commandToStopReading)
	}

	fun setReadStatusListener(listener: OBDReadStatusListener?) {
		readStatusListener = listener
	}

	fun setReadWriteStreams(readStream: Source?, writeStream: Sink?) {
		scope?.cancel()
		inputStream = readStream
		outputStream = writeStream
		if (readStream != null && writeStream != null) {
			startReadObdLooper()
		}
	}

	fun stopReading() {
		log.debug("stop reading")
		setReadWriteStreams(null, null)
		log.debug("after stop reading")
	}
}