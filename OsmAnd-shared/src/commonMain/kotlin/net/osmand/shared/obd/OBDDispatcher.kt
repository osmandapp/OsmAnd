package net.osmand.shared.obd

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import net.osmand.shared.util.KCollectionUtils
import net.osmand.shared.util.LoggerFactory
import okio.Buffer
import okio.IOException
import okio.Sink
import okio.Source


object OBDDispatcher {

	private var commandQueue: List<OBDCommand> = ArrayList()
	private val staleCommandsCache: MutableMap<OBDCommand, String> = HashMap()
	private var inputStream: Source? = null
	private var outputStream: Sink? = null
	private val log = LoggerFactory.getLogger("OBDDispatcher")
	private const val TERMINATE_SYMBOL = "\r\r>"
	private const val RESPONSE_LINE_TERMINATOR = "\r"
	private const val READ_DATA_COMMAND_CODE = "01"
	private var responseListeners: List<OBDResponseListener> = ArrayList()
	private var job: Job? = null
	private var scope: CoroutineScope? = null
	private var readStatusListener: OBDReadStatusListener? = null

	interface OBDReadStatusListener {
		fun onIOError()
		fun onBatchReadCompleted()
	}

	private fun startReadObdLooper() {
		job = Job()
		scope = CoroutineScope(Dispatchers.IO + job!!)
		scope!!.launch {
			try {
				log.debug("Start reading obd with $inputStream and $outputStream")
				while (inputStream != null && outputStream != null) {
					val inStream = inputStream!!
					val outStream = outputStream!!
					try {
						for (command in commandQueue) {
							if (command.isStale) {
								val cachedCommandResponse = staleCommandsCache[command]
								if (cachedCommandResponse != null) {
									dispatchResult(command, cachedCommandResponse)
									continue
								}
							}
							val fullCommand = "$READ_DATA_COMMAND_CODE${command.command}\r"
							val bufferToWrite = Buffer()
							bufferToWrite.write(fullCommand.encodeToByteArray())
							outStream.write(bufferToWrite, bufferToWrite.size)
							outStream.flush()
							log.debug("sent $fullCommand command")
							val readBuffer = Buffer()
							var resultRaw = StringBuilder()
							var readResponseFailed = false
							try {
								val startReadTime = Clock.System.now().toEpochMilliseconds()
								while (true) {
									if (Clock.System.now()
											.toEpochMilliseconds() - startReadTime > 3000) {
										readResponseFailed = true
										log.error("Read command ${command.name} timeout")
										break
									}
									val bytesRead = inStream.read(readBuffer, 1024)
									log.debug("read $bytesRead bytes")
									if (bytesRead == -1L) {
										log.debug("end of stream")
										break
									}
									val receivedData = readBuffer.readByteArray()
									resultRaw.append(receivedData.decodeToString())

									log.debug("response so far ${resultRaw}")
									if (resultRaw.contains(TERMINATE_SYMBOL)) {
										log.debug("found terminator")
										break
									} else {
										log.debug("no terminator found")
										log.debug("${resultRaw.lines().size}")

									}
								}
							} catch (e: IOException) {
								e.printStackTrace()
								log.error("Error reading data: ${e.message}")
							}
							if (readResponseFailed) {
								continue
							}
							var response = resultRaw.toString()
							response = response.replace(TERMINATE_SYMBOL, "")
							val listResponses = response.split(RESPONSE_LINE_TERMINATOR)
							for (responseIndex in 1 until listResponses.size) {
								val result = command.parseResponse(listResponses[responseIndex])
								log.debug("raw_response_$responseIndex: $result")
								dispatchResult(command, result)
								if (command.isStale) {
									staleCommandsCache[command] = result
									break
								}
							}
							log.info("response. ${command.name} **${response.replace('\\', '\\')}")
						}
					} catch (error: IOException) {
						log.error("Run OBD looper error. $error")
						readStatusListener?.onIOError()
					}
					readStatusListener?.onBatchReadCompleted()
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

	fun removeCommand(commandToStopReading: OBDCommand) {
		commandQueue = KCollectionUtils.removeFromList(commandQueue, commandToStopReading)
	}

	fun addResponseListener(responseListener: OBDResponseListener) {
		responseListeners = KCollectionUtils.addToList(responseListeners, responseListener)
	}

	fun removeResponseListener(responseListener: OBDResponseListener) {
		responseListeners = KCollectionUtils.removeFromList(responseListeners, responseListener)
	}

	fun setReadStatusListener(listener: OBDReadStatusListener?) {
		readStatusListener = listener
	}

	fun setReadWriteStreams(readStream: Source, writeStream: Sink) {
		scope?.cancel()
		inputStream = readStream
		outputStream = writeStream
		startReadObdLooper()
	}

	fun getCommandQueue(): List<OBDCommand> {
		return ArrayList(commandQueue)
	}

	private fun dispatchResult(command: OBDCommand, result: String) {
		for (listener in responseListeners) {
			listener.onCommandResponse(command, result)
		}
	}
}