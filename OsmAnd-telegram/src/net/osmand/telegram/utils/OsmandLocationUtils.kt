package net.osmand.telegram.utils

import android.os.AsyncTask
import net.osmand.Location
import net.osmand.data.LatLon
import net.osmand.gpx.GPXFile
import net.osmand.gpx.GPXUtilities
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.LocationMessages
import net.osmand.telegram.helpers.LocationMessages.BufferMessage
import net.osmand.telegram.helpers.LocationMessages.LocationMessage
import net.osmand.telegram.helpers.ShowLocationHelper
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.helpers.TelegramUiHelper
import net.osmand.util.GeoParsedPoint
import net.osmand.util.GeoPointParserUtil
import net.osmand.util.MapUtils
import org.drinkless.tdlib.TdApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.abs

const val TRACKS_DIR = "tracker/"

object OsmandLocationUtils {

	const val DEVICE_PREFIX = "Device: "
	const val LOCATION_PREFIX = "Location: "
	const val LAST_LOCATION_PREFIX = "Last location: "
	const val UPDATED_PREFIX = "Updated: "
	const val USER_TEXT_LOCATION_TITLE = "\uD83D\uDDFA OsmAnd sharing:"

	const val SHARING_LINK = "https://play.google.com/store/apps/details?id=net.osmand.telegram"

	const val ALTITUDE_PREFIX = "Altitude: "
	const val BEARING_PREFIX = "Bearing: "
	const val SPEED_PREFIX = "Speed: "
	const val HDOP_PREFIX = "Horizontal precision: "
	const val BEARING_SUFFIX = "°"

	const val NOW = "now"
	const val FEW_SECONDS_AGO = "few seconds ago"
	const val SECONDS_AGO_SUFFIX = " seconds ago"
	const val MINUTES_AGO_SUFFIX = " minutes ago"
	const val HOURS_AGO_SUFFIX = " hours ago"
	const val UTC_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss"

	const val ONE_HOUR_TIME_MS = 60 * 60 * 1000 // 1 hour

	val UTC_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
		timeZone = TimeZone.getTimeZone("UTC")
	}

	val UTC_TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
		timeZone = TimeZone.getTimeZone("UTC")
	}

	fun convertLocation(l: android.location.Location?): Location? {
		if (l == null) {
			return null
		}
		val r = Location(l.provider)
		r.latitude = l.latitude
		r.longitude = l.longitude
		r.time = l.time
		if (l.hasAccuracy()) {
			r.accuracy = l.accuracy
		}
		if (l.hasSpeed()) {
			r.speed = l.speed
		}
		if (l.hasAltitude()) {
			r.altitude = l.altitude
		}
		if (l.hasBearing()) {
			r.bearing = l.bearing
		}
		return r
	}

	fun getLastUpdatedTime(message: TdApi.Message): Int {
		val content = message.content
		return when (content) {
			is MessageOsmAndBotLocation -> content.lastUpdated
			is MessageUserLocation -> content.lastUpdated
			else -> Math.max(message.editDate, message.date)
		}
	}

	fun getOsmAndBotDeviceName(message: TdApi.Message): String {
		var deviceName = ""
		if (message.replyMarkup is TdApi.ReplyMarkupInlineKeyboard) {
			val replyMarkup = message.replyMarkup as TdApi.ReplyMarkupInlineKeyboard
			try {
				val content = message.content
				when {
					content is TdApi.MessageText -> deviceName = content.text.text.lines().firstOrNull()?.removePrefix(DEVICE_PREFIX) ?: ""
					content is TdApi.MessageLocation && replyMarkup.rows[0].size > 1 -> deviceName = replyMarkup.rows[0][0].text.split("\\s".toRegex())[1]
					content is MessageOsmAndBotLocation -> deviceName = content.deviceName
				}
			} catch (e: Exception) {

			}
		}
		return deviceName
	}

	fun parseMapLocation(message: TdApi.Message, botLocation: Boolean): MessageLocation {
		val res = if (botLocation) MessageOsmAndBotLocation() else MessageUserLocation()
		val messageLocation = message.content as TdApi.MessageLocation
		res.apply {
			lat = messageLocation.location.latitude
			lon = messageLocation.location.longitude
			lastUpdated = getLastUpdatedTime(message)
			type = LocationMessages.TYPE_MAP
		}
		if (res is MessageOsmAndBotLocation) {
			res.deviceName = getOsmAndBotDeviceName(message)
		}

		return res
	}

	fun getSenderMessageId(message: TdApi.Message): Long {
		val forwardInfo = message.forwardInfo
		return if (forwardInfo != null && forwardInfo.origin is TdApi.MessageOriginUser) {
			(forwardInfo.origin as TdApi.MessageOriginUser).senderUserId
		} else {
			val sender = message.senderId
			if (sender is TdApi.MessageSenderUser) {
				sender.userId
			} else {
				0
			}
		}
	}

	fun parseMessage(message: TdApi.Message, helper: TelegramHelper, previousMessageLatLon: LatLon?): LocationMessage? {
		val parsedContent = parseMessageContent(message, helper)
		return createLocationMessage(message, parsedContent, previousMessageLatLon)
	}

	fun parseMessageContent(message: TdApi.Message, helper: TelegramHelper): MessageLocation? {
		val senderUserId = getSenderMessageId(message)
		val fromBot = helper.isOsmAndBot(senderUserId)
		val viaBot = helper.isOsmAndBot(message.viaBotUserId)
		return when (message.content) {
			is TdApi.MessageText -> parseTextLocation((message.content as TdApi.MessageText).text, (fromBot || viaBot))
			is TdApi.MessageLocation -> parseMapLocation(message, (fromBot || viaBot))
			is MessageLocation -> message.content as MessageLocation
			else -> null
		}
	}

	fun createLocationMessage(message: TdApi.Message, content:MessageLocation?, previousMessageLatLon: LatLon?):LocationMessage?{
		if (content == null) {
			return null
		}
		val senderUserId = getSenderMessageId(message)
		val messageType = getMessageType(message)
		val distanceFromPrev = if (previousMessageLatLon != null) MapUtils.getDistance(previousMessageLatLon, content.lat, content.lon) else 0.0
		val deviceName = if (content is MessageOsmAndBotLocation) content.deviceName else ""

		return LocationMessage(senderUserId, message.chatId, content.lat, content.lon,
			content.altitude, content.speed, content.hdop, content.bearing,
			content.lastUpdated * 1000L, messageType, message.id, distanceFromPrev, deviceName
		)
	}

	fun getMessageType(message: TdApi.Message): Int {
		val oldContent = message.content
		return when (oldContent) {
			is TdApi.MessageText -> LocationMessages.TYPE_TEXT
			is TdApi.MessageLocation -> LocationMessages.TYPE_MAP
			is MessageLocation -> oldContent.type
			else -> -1
		}
	}

	fun formatLocation(sig: Location): String {
		return String.format(Locale.US, "%.5f, %.5f", sig.latitude, sig.longitude)
	}

	fun formatLocation(sig: LocationMessage): String {
		return String.format(Locale.US, "%.5f, %.5f", sig.lat, sig.lon)
	}

	fun formatLocation(sig: BufferMessage): String {
		return String.format(Locale.US, "%.5f, %.5f", sig.lat, sig.lon)
	}

	fun formatFullTime(ti: Long, app: TelegramApplication): String {
		val dt = Date(ti)
		val offsetKey = app.settings.utcOffset
		val utcOffset = DataConstants.utcOffsets[offsetKey] ?: 0f
		val simpleDateFormat = SimpleDateFormat(UTC_FORMAT_PATTERN, Locale.US)

		simpleDateFormat.timeZone = TimeZone.getTimeZone(DataConstants.UTC_FORMAT).apply {
			rawOffset = (utcOffset * ONE_HOUR_TIME_MS).toInt()
		}

		return "${simpleDateFormat.format(dt)} $offsetKey"
	}

	fun parseOsmAndBotLocationContent(oldContent: MessageOsmAndBotLocation, content: TdApi.MessageContent): MessageOsmAndBotLocation {
		val messageLocation = content as TdApi.MessageLocation
		return MessageOsmAndBotLocation().apply {
			deviceName = oldContent.deviceName
			lat = messageLocation.location.latitude
			lon = messageLocation.location.longitude
			lastUpdated = (System.currentTimeMillis() / 1000).toInt()
			type = LocationMessages.TYPE_MAP
		}
	}

	fun parseTextLocation(text: TdApi.FormattedText, botLocation: Boolean): MessageLocation? {
		if (botLocation && !text.text.startsWith(DEVICE_PREFIX) || !botLocation && !text.text.startsWith(USER_TEXT_LOCATION_TITLE)) {
			return null
		}
		val res = if (botLocation) MessageOsmAndBotLocation() else MessageUserLocation()
		res.type = LocationMessages.TYPE_TEXT
		var locationNA = false
		for (s in text.text.lines()) {
			when {
				s.startsWith(DEVICE_PREFIX) -> {
					if (res is MessageOsmAndBotLocation) {
						res.deviceName = s.removePrefix(DEVICE_PREFIX)
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
								val point: GeoParsedPoint? = GeoPointParserUtil.parse(url)
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
					res.altitude = parseDistance(altStr)
				}
				s.startsWith(SPEED_PREFIX) -> {
					val speedStr = s.removePrefix(SPEED_PREFIX)
					res.speed = parseSpeed(speedStr)
				}
				s.startsWith(BEARING_PREFIX) -> {
					val bearingStr = s.removePrefix(BEARING_PREFIX)
					try {
						val bearing = bearingStr.removeSuffix(BEARING_SUFFIX)
						res.bearing = bearing.toDouble()
					} catch (e: Exception) {
						e.printStackTrace()
					}
				}
				s.startsWith(HDOP_PREFIX) -> {
					val hdopStr = s.removePrefix(HDOP_PREFIX)
					res.hdop = parseDistance(hdopStr)
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
				timeS.contains(DataConstants.UTC_FORMAT) -> {
					val utcIndex = timeS.indexOf(DataConstants.UTC_FORMAT)
					if (utcIndex != -1) {
						val locStr = timeS.substring(0, utcIndex)
						val utcOffset = timeS.substring(utcIndex)
						val utcTimeOffset = DataConstants.utcOffsets[utcOffset] ?: 0f
						val simpleDateFormat = SimpleDateFormat(UTC_FORMAT_PATTERN, Locale.US)
						simpleDateFormat.timeZone = TimeZone.getTimeZone(DataConstants.UTC_FORMAT).apply {
								rawOffset = (utcTimeOffset * ONE_HOUR_TIME_MS).toInt()
							}
						val res = simpleDateFormat.parse(locStr)
						return res.time
					}
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return 0
	}

	fun parseSpeed(speedS: String): Double {
		try {
			if (!speedS.contains(" ")) {
				return 0.0
			}
			val speedSplit = speedS.split(" ")
			val speedVal = speedSplit.first().toDouble()
			val speedFormat = OsmandFormatter.SpeedConstants.values().firstOrNull { it.getDefaultString() == speedSplit.last() }
			return when (speedFormat) {
				OsmandFormatter.SpeedConstants.KILOMETERS_PER_HOUR -> speedVal / 3.6f
				OsmandFormatter.SpeedConstants.MILES_PER_HOUR -> (speedVal / 3.6f) / (OsmandFormatter.METERS_IN_KILOMETER / OsmandFormatter.METERS_IN_ONE_MILE)
				OsmandFormatter.SpeedConstants.NAUTICALMILES_PER_HOUR -> (speedVal / 3.6f) / (OsmandFormatter.METERS_IN_KILOMETER / OsmandFormatter.METERS_IN_ONE_NAUTICALMILE)
				OsmandFormatter.SpeedConstants.MINUTES_PER_KILOMETER -> (OsmandFormatter.METERS_IN_KILOMETER / speedVal) / 60
				OsmandFormatter.SpeedConstants.MINUTES_PER_MILE -> (OsmandFormatter.METERS_IN_ONE_MILE / speedVal) / 60
				else -> speedVal
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return 0.0
	}

	fun parseDistance(distanceS: String): Double {
		try {
			val distanceSplit = distanceS.split(" ")
			val distanceVal = distanceSplit.first().toDouble()
			return when (distanceSplit.last()) {
				OsmandFormatter.FORMAT_METERS_KEY -> return distanceVal
				OsmandFormatter.FORMAT_FEET_KEY -> return distanceVal / OsmandFormatter.FEET_IN_ONE_METER
				OsmandFormatter.FORMAT_YARDS_KEY -> return distanceVal / OsmandFormatter.YARDS_IN_ONE_METER
				OsmandFormatter.FORMAT_KILOMETERS_KEY -> return distanceVal * OsmandFormatter.METERS_IN_KILOMETER
				OsmandFormatter.FORMAT_NAUTICALMILES_KEY -> return distanceVal * OsmandFormatter.METERS_IN_ONE_NAUTICALMILE
				OsmandFormatter.FORMAT_MILES_KEY -> return distanceVal * OsmandFormatter.METERS_IN_ONE_MILE
				else -> distanceVal
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return 0.0
	}

	fun getTextMessageContent(updateId: Int, location: BufferMessage, app: TelegramApplication): TdApi.InputMessageText {
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
			val formattedAltitude = OsmandFormatter.getFormattedAlt(location.altitude, app, false)
			builder.append(String.format(Locale.US, "$ALTITUDE_PREFIX%s\n", formattedAltitude))
		}
		if (location.speed > 0) {
			entities.add(TdApi.TextEntity(builder.lastIndex, SPEED_PREFIX.length, TdApi.TextEntityTypeBold()))
			val formattedSpeed = OsmandFormatter.getFormattedSpeed(location.speed.toFloat(), app, false)
			builder.append(String.format(Locale.US, "$SPEED_PREFIX%s\n", formattedSpeed))
		}
		if (location.bearing > 0) {
			entities.add(TdApi.TextEntity(builder.lastIndex, BEARING_PREFIX.length, TdApi.TextEntityTypeBold()))
			builder.append(String.format(Locale.US, "$BEARING_PREFIX%.1f$BEARING_SUFFIX\n", location.bearing))
		}
		if (location.hdop != 0.0 && location.speed == 0.0) {
			entities.add(TdApi.TextEntity(builder.lastIndex, HDOP_PREFIX.length, TdApi.TextEntityTypeBold()))
			val formattedHdop = OsmandFormatter.getFormattedDistance(location.hdop.toFloat(), app, false, false)
			builder.append(String.format(Locale.US, "$HDOP_PREFIX%s\n", formattedHdop))
		}
		if (updateId == 0) {
			builder.append(String.format("$UPDATED_PREFIX%s\n", formatFullTime(location.time, app)))
		} else {
			builder.append(String.format("$UPDATED_PREFIX%s (%d)\n", formatFullTime(location.time, app), updateId))
		}
		val textMessage = builder.toString().trim()

		return TdApi.InputMessageText(TdApi.FormattedText(textMessage, entities.toTypedArray()), null, true)
	}

	fun convertLocationMessagesToGpxFiles(app: TelegramApplication, items: List<LocationMessage>, newGpxPerChat: Boolean = true): List<GPXFile> {
		val dataTracks = ArrayList<GPXFile>()

		var previousTime: Long = -1
		var previousChatId: Long = -1
		var previousUserId: Long = -1
		var previousDeviceName = ""
		var segment: GPXUtilities.TrkSegment? = null
		var track: GPXUtilities.Track? = null
		var gpx: GPXFile? = null
		var countedLocations = 0

		items.forEach {
			val userId = it.userId
			val chatId = it.chatId
			val deviceName = it.deviceName
			val time = it.time
			if (previousUserId != userId || previousDeviceName != deviceName || (newGpxPerChat && previousChatId != chatId)) {
				gpx = GPXFile(app.packageName).apply {
					metadata = GPXUtilities.Metadata().apply {
						name = getGpxFileNameForUserId(app, userId, chatId, time)
					}
					val colorIndex = app.settings.getLiveTrackInfo(userId, chatId, deviceName)?.colorIndex ?: -1
					if (colorIndex != -1) {
						extensionsToWrite["color"] = ShowLocationHelper.GPX_COLORS[colorIndex]
					}
				}
				previousTime = 0
				track = null
				segment = null
				dataTracks.add(gpx!!)
			}
			if (previousTime >= time) {
				return@forEach
			}
			countedLocations++
			val pt = GPXUtilities.WptPt()
			pt.lat = it.lat
			pt.lon = it.lon
			pt.ele = it.altitude
			pt.speed = it.speed
			pt.hdop = it.hdop
			pt.time = time
			val currentInterval = abs(time - previousTime)
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
			previousDeviceName = deviceName
		}

		return dataTracks
	}

	fun saveGpx(app: TelegramApplication, gpxFile: GPXFile, listener: SaveGpxListener) {
		if (!gpxFile.isEmpty) {
			val dir = File(app.getExternalFilesDir(null), TRACKS_DIR)
			val task = SaveGPXTrackToFileTask(listener, gpxFile, dir)
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
		}
	}

	fun getGpxFileNameForUserId(app: TelegramApplication, userId: Long, chatId: Long, time: Long): String {
		var userName = userId.toString()
		val user = app.telegramHelper.getUser(userId)
		if (user != null) {
			userName = TelegramUiHelper.getUserName(user)
		}
		val chat = app.telegramHelper.getChat(chatId)
		if (chat != null && app.telegramHelper.isGroup(chat)) {
			return "${userName}_${chat.title}_${UTC_DATE_FORMAT.format(Date(time))}"
		}
		return "${userName}_${UTC_DATE_FORMAT.format(Date(time))}"
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
		var type: Int = -1
			internal set

		override fun getConstructor() = TdApi.MessageLocation.CONSTRUCTOR

		abstract fun isValid(): Boolean
	}

	class MessageOsmAndBotLocation : MessageLocation() {

		var deviceName: String = ""
			internal set

		override fun isValid() = deviceName != "" && lat != Double.NaN && lon != Double.NaN
	}

	class MessageUserLocation : MessageLocation() {

		override fun isValid() = lat != Double.NaN && lon != Double.NaN

	}

	private class SaveGPXTrackToFileTask internal constructor(
            private val listener: SaveGpxListener?,
            private val gpxFile: GPXFile,
            private val dir: File
	) :
		AsyncTask<Void, Void, java.lang.Exception>() {

		override fun doInBackground(vararg params: Void): Exception? {
			dir.mkdirs()
			if (dir.parentFile.canWrite()) {
				if (dir.exists()) {
					// save file
					if (!gpxFile.isEmpty) {
						val fileName = gpxFile.metadata.name
						val fout = File(dir, "$fileName.gpx")

						return GPXUtilities.writeGpxFile(fout, gpxFile)
					}
				}
			}
			return null
		}

		override fun onPostExecute(warning: Exception?) {
			if (listener != null) {
				if (warning == null) {
					listener.onSavingGpxFinish(gpxFile.path)
				} else {
					listener.onSavingGpxError(warning)
				}
			}
		}
	}

	interface SaveGpxListener {

		fun onSavingGpxFinish(path: String)

		fun onSavingGpxError(error: Exception)
	}
}