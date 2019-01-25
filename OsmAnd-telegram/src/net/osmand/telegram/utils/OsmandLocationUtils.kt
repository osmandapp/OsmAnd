package net.osmand.telegram.utils

import android.os.AsyncTask
import net.osmand.Location
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.LocationMessages
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.util.GeoPointParserUtil
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.*


object OsmandLocationUtils {

	const val DEVICE_PREFIX = "Device: "
	const val LOCATION_PREFIX = "Location: "
	const val LAST_LOCATION_PREFIX = "Last location: "
	const val UPDATED_PREFIX = "Updated: "
	const val USER_TEXT_LOCATION_TITLE = "\uD83D\uDDFA OsmAnd sharing:"

	const val SHARING_LINK = "https://play.google.com/store/apps/details?id=net.osmand.telegram"

	const val ALTITUDE_PREFIX = "Altitude: "
	const val SPEED_PREFIX = "Speed: "
	const val HDOP_PREFIX = "Horizontal precision: "

	const val NOW = "now"
	const val FEW_SECONDS_AGO = "few seconds ago"
	const val SECONDS_AGO_SUFFIX = " seconds ago"
	const val MINUTES_AGO_SUFFIX = " minutes ago"
	const val HOURS_AGO_SUFFIX = " hours ago"
	const val UTC_FORMAT_SUFFIX = " UTC"

	val UTC_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
		timeZone = TimeZone.getTimeZone("UTC")
	}

	val UTC_TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
		timeZone = TimeZone.getTimeZone("UTC")
	}

	fun getLastUpdatedTime(message: TdApi.Message): Int {
		val content = message.content
		return when (content) {
			is MessageOsmAndBotLocation -> content.lastUpdated
			is MessageUserTextLocation -> content.lastUpdated
			else -> Math.max(message.editDate, message.date)
		}
	}

	fun getOsmAndBotDeviceName(message: TdApi.Message): String {
		var deviceName = ""
		if (message.replyMarkup is TdApi.ReplyMarkupInlineKeyboard) {
			val replyMarkup = message.replyMarkup as TdApi.ReplyMarkupInlineKeyboard
			try {
				deviceName = replyMarkup.rows[0][1].text.split("\\s".toRegex())[1]
			} catch (e: Exception) {

			}
		}
		return deviceName
	}

	fun parseOsmAndBotLocation(message: TdApi.Message): MessageOsmAndBotLocation {
		val messageLocation = message.content as TdApi.MessageLocation
		return MessageOsmAndBotLocation().apply {
			name = getOsmAndBotDeviceName(message)
			lat = messageLocation.location.latitude
			lon = messageLocation.location.longitude
			lastUpdated = getLastUpdatedTime(message)
		}
	}

	fun parseMessage(message: TdApi.Message, helper: TelegramHelper, status: Int): LocationMessages.LocationMessage? {
		var locationMessage: LocationMessages.LocationMessage? = null
		val oldContent = message.content

		val fromBot = helper.isOsmAndBot(message.senderUserId)
		val viaBot = helper.isOsmAndBot(message.viaBotUserId)

		var messageType: Int = -1
		val parsedMessageContent =
			if (oldContent is TdApi.MessageText) {
				when {
					oldContent.text.text.startsWith(DEVICE_PREFIX) -> {
						messageType = 3
						parseTextLocation(oldContent.text)
					}
					oldContent.text.text.startsWith(USER_TEXT_LOCATION_TITLE) -> {
						messageType = 1
						parseTextLocation(oldContent.text, false)
					}
					else -> null
				}
			} else if (oldContent is TdApi.MessageLocation && (fromBot || viaBot)) {
				messageType = 2
				parseOsmAndBotLocation(message)
			} else if (oldContent is MessageLocation) {
				messageType = 0
				oldContent
			} else {
				null
			}

		if (parsedMessageContent != null) {
			locationMessage = LocationMessages.LocationMessage(message.senderUserId, message.chatId, parsedMessageContent.lat,
				parsedMessageContent.lon, parsedMessageContent.altitude, parsedMessageContent.speed, parsedMessageContent.hdop,
				parsedMessageContent.bearing, parsedMessageContent.lastUpdated * 1000L, messageType, status, message.id)
		}
		return locationMessage
	}

	fun formatLocation(sig: Location): String {
		return String.format(Locale.US, "%.5f, %.5f", sig.latitude, sig.longitude)
	}

	fun formatLocation(sig: LocationMessages.LocationMessage): String {
		return String.format(Locale.US, "%.5f, %.5f", sig.lat, sig.lon)
	}

	fun formatFullTime(ti: Long): String {
		val dt = Date(ti)
		return UTC_DATE_FORMAT.format(dt) + " " + UTC_TIME_FORMAT.format(dt) + " UTC"
	}

	fun parseOsmAndBotLocationContent(oldContent: MessageOsmAndBotLocation, content: TdApi.MessageContent): MessageOsmAndBotLocation {
		val messageLocation = content as TdApi.MessageLocation
		return MessageOsmAndBotLocation().apply {
			name = oldContent.name
			lat = messageLocation.location.latitude
			lon = messageLocation.location.longitude
			lastUpdated = (System.currentTimeMillis() / 1000).toInt()
		}
	}

	fun parseTextLocation(text: TdApi.FormattedText, botLocation: Boolean = true): MessageLocation {
		val res = if (botLocation) MessageOsmAndBotLocation() else MessageUserTextLocation()

		var locationNA = false
		for (s in text.text.lines()) {
			when {
				s.startsWith(DEVICE_PREFIX) -> {
					if (res is MessageOsmAndBotLocation) {
						res.name = s.removePrefix(DEVICE_PREFIX)
					}
				}
				s.startsWith(LOCATION_PREFIX) || s.startsWith(LAST_LOCATION_PREFIX) -> {
					var locStr: String
					var parse = true
					if (s.startsWith(LAST_LOCATION_PREFIX)) {
						locStr = s.removePrefix(LAST_LOCATION_PREFIX)
						if (!locationNA) {
							parse = false
						}
					} else {
						locStr = s.removePrefix(LOCATION_PREFIX)
						if (locStr.trim() == "n/a") {
							locationNA = true
							parse = false
						}
					}
					if (parse) {
						try {
							val urlTextEntity =
								text.entities.firstOrNull { it.type is TdApi.TextEntityTypeTextUrl }
							if (urlTextEntity != null && urlTextEntity.offset == text.text.indexOf(
									locStr
								)
							) {
								val url = (urlTextEntity.type as TdApi.TextEntityTypeTextUrl).url
								val point: GeoPointParserUtil.GeoParsedPoint? =
									GeoPointParserUtil.parse(url)
								if (point != null) {
									res.lat = point.latitude
									res.lon = point.longitude
								}
							} else {
								val (latS, lonS) = locStr.split(" ")
								res.lat = latS.dropLast(1).toDouble()
								res.lon = lonS.toDouble()

								val timeIndex = locStr.indexOf("(")
								if (timeIndex != -1) {
									val updatedS = locStr.substring(timeIndex, locStr.length)
									res.lastUpdated =
											(parseTime(updatedS.removePrefix("(").removeSuffix(")")) / 1000).toInt()
								}
							}
						} catch (e: Exception) {
							e.printStackTrace()
						}
					}
				}
				s.startsWith(ALTITUDE_PREFIX) -> {
					val altStr = s.removePrefix(ALTITUDE_PREFIX)
					try {
						val alt = altStr.split(" ").first()
						res.altitude = alt.toDouble()
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
				s.startsWith(SPEED_PREFIX) -> {
					val altStr = s.removePrefix(SPEED_PREFIX)
					try {
						val alt = altStr.split(" ").first()
						res.speed = alt.toDouble()
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
				s.startsWith(HDOP_PREFIX) -> {
					val altStr = s.removePrefix(HDOP_PREFIX)
					try {
						val alt = altStr.split(" ").first()
						res.hdop = alt.toDouble()
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
				s.startsWith(UPDATED_PREFIX) -> {
					if (res.lastUpdated == 0) {
						val updatedStr = s.removePrefix(UPDATED_PREFIX)
						val endIndex = updatedStr.indexOf("(")
						val updatedS = updatedStr.substring(
							0,
							if (endIndex != -1) endIndex else updatedStr.length
						)
						val parsedTime = (parseTime(updatedS.trim()) / 1000).toInt()
						val currentTime = (System.currentTimeMillis() / 1000) - 1
						res.lastUpdated =
								if (parsedTime < currentTime) parsedTime else currentTime.toInt()
					}
				}
			}
		}
		return res
	}

	fun parseTime(timeS: String): Long {
		try {
			when {
				timeS.endsWith(FEW_SECONDS_AGO) -> return System.currentTimeMillis() - 5000

				timeS.endsWith(SECONDS_AGO_SUFFIX) -> {
					val locStr = timeS.removeSuffix(SECONDS_AGO_SUFFIX)
					return System.currentTimeMillis() - locStr.toLong() * 1000
				}
				timeS.endsWith(MINUTES_AGO_SUFFIX) -> {
					val locStr = timeS.removeSuffix(MINUTES_AGO_SUFFIX)
					val minutes = locStr.toLong()
					return System.currentTimeMillis() - minutes * 60 * 1000
				}
				timeS.endsWith(HOURS_AGO_SUFFIX) -> {
					val locStr = timeS.removeSuffix(HOURS_AGO_SUFFIX)
					val hours = locStr.toLong()
					return (System.currentTimeMillis() - hours * 60 * 60 * 1000)
				}
				timeS.endsWith(UTC_FORMAT_SUFFIX) -> {
					val locStr = timeS.removeSuffix(UTC_FORMAT_SUFFIX)
					val (latS, lonS) = locStr.split(" ")
					val date = UTC_DATE_FORMAT.parse(latS)
					val time = UTC_TIME_FORMAT.parse(lonS)
					val res = date.time + time.time
					return res
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return 0
	}

	fun getTextMessageContent(updateId: Int, location: LocationMessages.LocationMessage): TdApi.InputMessageText {
		val entities = mutableListOf<TdApi.TextEntity>()
		val builder = StringBuilder()
		val locationMessage = formatLocation(location)

		val firstSpace = USER_TEXT_LOCATION_TITLE.indexOf(' ')
		val secondSpace = USER_TEXT_LOCATION_TITLE.indexOf(' ', firstSpace + 1)
		entities.add(TdApi.TextEntity(builder.length + firstSpace + 1, secondSpace - firstSpace, TdApi.TextEntityTypeTextUrl(SHARING_LINK)))
		builder.append("$USER_TEXT_LOCATION_TITLE\n")

		entities.add(TdApi.TextEntity(builder.lastIndex, LOCATION_PREFIX.length, TdApi.TextEntityTypeBold()))
		builder.append(LOCATION_PREFIX)

		entities.add(TdApi.TextEntity(builder.length, locationMessage.length,
			TdApi.TextEntityTypeTextUrl("$BASE_SHARING_URL?lat=${location.lat}&lon=${location.lon}")))
		builder.append("$locationMessage\n")

		if (location.altitude != 0.0) {
			entities.add(TdApi.TextEntity(builder.lastIndex, ALTITUDE_PREFIX.length, TdApi.TextEntityTypeBold()))
			builder.append(String.format(Locale.US, "$ALTITUDE_PREFIX%.1f m\n", location.altitude))
		}
		if (location.speed > 0) {
			entities.add(TdApi.TextEntity(builder.lastIndex, SPEED_PREFIX.length, TdApi.TextEntityTypeBold()))
			builder.append(String.format(Locale.US, "$SPEED_PREFIX%.1f m/s\n", location.speed))
		}
		if (location.hdop != 0.0 && location.speed == 0.0) {
			entities.add(TdApi.TextEntity(builder.lastIndex, HDOP_PREFIX.length, TdApi.TextEntityTypeBold()))
			builder.append(String.format(Locale.US, "$HDOP_PREFIX%d m\n", location.hdop.toInt()))
		}
		if (updateId == 0) {
			builder.append(String.format("$UPDATED_PREFIX%s\n", formatFullTime(location.date)))
		} else {
			builder.append(String.format("$UPDATED_PREFIX%s (%d)\n", formatFullTime(location.date), updateId))
		}
		val textMessage = builder.toString().trim()

		return TdApi.InputMessageText(TdApi.FormattedText(textMessage, entities.toTypedArray()), true, true)
	}

	fun convertLocationMessagesToGpxFiles(items: List<LocationMessages.LocationMessage>, newGpxPerChat: Boolean = true): List<GPXUtilities.GPXFile> {
		val dataTracks = ArrayList<GPXUtilities.GPXFile>()

		var previousTime: Long = -1
		var previousChatId: Long = -1
		var previousUserId = -1
		var segment: GPXUtilities.TrkSegment? = null
		var track: GPXUtilities.Track? = null
		var gpx: GPXUtilities.GPXFile? = null

		items.forEach {
			val userId = it.userId
			val chatId = it.chatId
			val time = it.date
			if (previousUserId != userId || (newGpxPerChat && previousChatId != chatId)) {
				gpx = GPXUtilities.GPXFile()
				gpx!!.chatId = chatId
				gpx!!.userId = userId
				previousTime = 0
				track = null
				segment = null
				dataTracks.add(gpx!!)
			}
			val pt = GPXUtilities.WptPt()
			pt.userId = userId
			pt.chatId = chatId
			pt.lat = it.lat
			pt.lon = it.lon
			pt.ele = it.altitude
			pt.speed = it.speed
			pt.hdop = it.hdop
			pt.time = time
			val currentInterval = Math.abs(time - previousTime)
			if (track != null) {
				if (currentInterval < 30 * 60 * 1000) {
					// 30 minute - same segment
					segment!!.points.add(pt)
				} else {
					segment = GPXUtilities.TrkSegment()
					segment!!.points.add(pt)
					track!!.segments.add(segment)
				}
			} else {
				track = GPXUtilities.Track()
				segment = GPXUtilities.TrkSegment()
				track!!.segments.add(segment)
				segment!!.points.add(pt)

				gpx!!.tracks.add(track)
			}
			previousTime = time
			previousUserId = userId
			previousChatId = chatId
		}

		return dataTracks
	}

	fun saveGpx(app: TelegramApplication, listener: SaveGpxListener, dir: File, gpxFile: GPXUtilities.GPXFile) {
		if (!gpxFile.isEmpty) {
			val task = SaveGPXTrackToFileTask(app, listener, gpxFile, dir, 0)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
		}
	}

	abstract class MessageLocation : TdApi.MessageContent() {

		var lat: Double = Double.NaN
			internal set
		var lon: Double = Double.NaN
			internal set
		var lastUpdated: Int = 0
			internal set
		var speed: Double = 0.0
			internal set
		var altitude: Double = 0.0
			internal set
		var hdop: Double = 0.0
			internal set
		var bearing: Double = 0.0
			internal set

		override fun getConstructor() = -1

		abstract fun isValid(): Boolean
	}

	class MessageOsmAndBotLocation : MessageLocation() {

		var name: String = ""
			internal set

		override fun isValid() = name != "" && lat != Double.NaN && lon != Double.NaN
	}

	class MessageUserTextLocation : MessageLocation() {

		override fun isValid() = lat != Double.NaN && lon != Double.NaN

	}

	private class SaveGPXTrackToFileTask internal constructor(
		private val app: TelegramApplication, private val listener: SaveGpxListener?,
		private val gpxFile: GPXUtilities.GPXFile, private val dir: File, private val userId: Int
	) :
		AsyncTask<Void, Void, List<String>>() {

		override fun doInBackground(vararg params: Void): List<String> {
			val warnings = ArrayList<String>()
			dir.mkdirs()
			if (dir.parentFile.canWrite()) {
				if (dir.exists()) {
					// save file
					var fout = File(dir, "$userId.gpx")
					if (!gpxFile.isEmpty) {
						val pt = gpxFile.findPointToShow()

						val user = app.telegramHelper.getUser(pt!!.userId)
						val fileName: String
						fileName = if (user != null) {
							(TelegramUiHelper.getUserName(user) + "_" + SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(Date(pt.time)))
						} else {
							userId.toString() + "_" + SimpleDateFormat("yyyy-MM-dd_HH-mm_EEE", Locale.US).format(Date(pt.time))
						}
						fout = File(dir, "$fileName.gpx")
						var ind = 1
						while (fout.exists()) {
							fout = File(dir, "${fileName}_${++ind}.gpx")
						}
					}
					val warn = GPXUtilities.writeGpxFile(fout, gpxFile, app)
					if (warn != null) {
						warnings.add(warn)
						return warnings
					}
				}
			}

			return warnings
		}

		override fun onPostExecute(warnings: List<String>?) {
			if (listener != null) {
				if (warnings != null && warnings.isEmpty()) {
					listener.onSavingGpxFinish(gpxFile.path)
				} else {
					listener.onSavingGpxError(warnings)
				}
			}
		}
	}

	interface SaveGpxListener {

		fun onSavingGpxFinish(path: String)

		fun onSavingGpxError(warnings: List<String>?)
	}
}