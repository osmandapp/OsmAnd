package net.osmand.telegram.helpers

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.data.LatLon
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.utils.OsmandLocationUtils
import net.osmand.util.MapUtils
import org.drinkless.tdlib.TdApi
import java.util.concurrent.ConcurrentLinkedQueue

class LocationMessages(val app: TelegramApplication) {

	private val log = PlatformUtil.getLog(LocationMessages::class.java)

	private var bufferedMessages = emptyList<BufferMessage>()

	private var lastLocationPoints = ConcurrentLinkedQueue<LocationMessage>()

	private val dbHelper: SQLiteHelper

	private var lastRemoveTime: Long? = null

	init {
		dbHelper = SQLiteHelper(app)
		readBufferedMessages()
		readLastMessages()
	}

	fun getBufferedMessages(): List<BufferMessage> {
		removeOldBufferedMessages()
		return bufferedMessages.sortedBy { it.time }
	}

	fun getBufferedMessagesCount(): Int {
		removeOldBufferedMessages()
		return bufferedMessages.size
	}

	fun getBufferedMessagesCountForChat(chatId: Long, type: Int): Int {
		removeOldBufferedMessages()
		return bufferedMessages.count { it.chatId == chatId && it.type == type }
	}

	fun getBufferedMessagesCountForChat(chatId: Long): Int {
		removeOldBufferedMessages()
		return bufferedMessages.count { it.chatId == chatId}
	}

	fun getBufferedMessagesForChat(chatId: Long): List<BufferMessage> {
		removeOldBufferedMessages()
		return bufferedMessages.filter { it.chatId == chatId }.sortedBy { it.time }
	}

	fun getBufferedTextMessagesForChat(chatId: Long): List<BufferMessage> {
		removeOldBufferedMessages()
		return bufferedMessages.filter { it.chatId == chatId && it.type == TYPE_TEXT }.sortedBy { it.time }
	}

	fun getBufferedMapMessagesForChat(chatId: Long): List<BufferMessage> {
		removeOldBufferedMessages()
		return bufferedMessages.filter { it.chatId == chatId && it.type == TYPE_MAP }.sortedBy { it.time }
	}

	fun getIngoingMessages(currentUserId: Long, start: Long, end: Long): List<LocationMessage> {
		return dbHelper.getIngoingMessages(currentUserId, start, end)
	}

	fun getIngoingUserLocations(start: Long, end: Long): List<UserLocations> {
		return dbHelper.getIngoingUserLocations(start, end)
	}

	fun getIngoingUserLocationsInChat(userId: Long, chatId: Long, deviceName: String, start: Long, end: Long): UserLocations? {
		return dbHelper.getIngoingUserLocationsInChat(userId, chatId, deviceName, start, end)
	}

	fun getMessagesForUserInChat(userId: Long, chatId: Long, deviceName: String, start: Long, end: Long): List<LocationMessage> {
		return dbHelper.getMessagesForUserInChat(userId, chatId, deviceName, start, end)
	}

	fun getMessagesForUser(userId: Long, start: Long, end: Long): List<LocationMessage> {
		return dbHelper.getMessagesForUser(userId, start, end)
	}

	fun getLastLocationInfoForUserInChat(userId: Long, chatId: Long, deviceName: String) =
		lastLocationPoints.sortedByDescending { it.time }.firstOrNull { it.userId == userId && it.chatId == chatId && it.deviceName == deviceName }

	fun getLastLocationMessagesSinceTime(time: Long) = lastLocationPoints.filter { it.time > time }

	fun addBufferedMessage(message: BufferMessage) {
		log.debug("addBufferedMessage $message")
		val messages = this.bufferedMessages.toMutableList()
		messages.add(message)
		this.bufferedMessages = messages
		dbHelper.addBufferedMessage(message)
	}

	fun addNewLocationMessage(message: TdApi.Message) {
		log.debug("try addNewLocationMessage ${message.id}")
		val type = OsmandLocationUtils.getMessageType(message)
		val senderId = OsmandLocationUtils.getSenderMessageId(message)
		val content = OsmandLocationUtils.parseMessageContent(message, app.telegramHelper)
		if (content != null) {
			val deviceName = if (content is OsmandLocationUtils.MessageOsmAndBotLocation) content.deviceName else ""
			val previousLocationMessage = lastLocationPoints.sortedByDescending { it.time }.firstOrNull {
				it.userId == senderId && it.chatId == message.chatId && it.deviceName == deviceName && it.type == type
			}
			if (previousLocationMessage == null || content.lastUpdated * 1000L > previousLocationMessage.time) {
				log.debug("addNewLocationMessage passed ${message.id}")
				val previousMessageLatLon = if (previousLocationMessage != null) LatLon(previousLocationMessage.lat, previousLocationMessage.lon) else null
				val locationMessage = OsmandLocationUtils.createLocationMessage(message, content, previousMessageLatLon)
				if (locationMessage != null) {
					dbHelper.addLocationMessage(locationMessage)
					lastLocationPoints.remove(previousLocationMessage)
					lastLocationPoints.add(locationMessage)
				}
			}
		}
	}

	fun addMyLocationMessage(loc: Location) {
		log.debug("addMyLocationMessage")
		val currentUserId = app.telegramHelper.getCurrentUserId()
		val previousLocationMessage = lastLocationPoints.sortedBy { it.time }.firstOrNull { it.userId == currentUserId && it.type == TYPE_MY_LOCATION }
		val distance = if (previousLocationMessage != null) MapUtils.getDistance(previousLocationMessage.lat, previousLocationMessage.lon, loc.latitude, loc.longitude)  else 0.0
		val message = LocationMessages.LocationMessage(currentUserId, 0, loc.latitude, loc.longitude, loc.altitude,
			loc.speed.toDouble(), loc.accuracy.toDouble(), loc.bearing.toDouble(), loc.time, TYPE_MY_LOCATION, 0, distance, "")

		dbHelper.addLocationMessage(message)
		lastLocationPoints.remove(previousLocationMessage)
		lastLocationPoints.add(message)
	}

	fun clearBufferedMessages() {
		log.debug("clearBufferedMessages")
		dbHelper.clearBufferedMessages()
		bufferedMessages = emptyList()
	}

	fun removeBufferedMessage(message: BufferMessage) {
		log.debug("removeBufferedMessage $message")
		val messages = this.bufferedMessages.toMutableList()
		messages.remove(message)
		this.bufferedMessages = messages
		dbHelper.removeBufferedMessage(message)
	}

	private fun removeOldBufferedMessages() {
		val currentTime = System.currentTimeMillis()
		if (this.bufferedMessages.isNotEmpty() && isTimeToDelete(currentTime)) {
			val bufferExpirationTime = app.settings.bufferTime * 1000
			val messages = this.bufferedMessages.toMutableList()
			val expiredList = messages.filter {
				currentTime - it.time > bufferExpirationTime
			}
			expiredList.forEach { message ->
				dbHelper.removeBufferedMessage(message)
			}
			messages.removeAll(expiredList)
			this.bufferedMessages = messages.toList()
			lastRemoveTime = currentTime
		}
	}

	private fun isTimeToDelete(currentTime: Long) = if (lastRemoveTime != null) {
		currentTime - lastRemoveTime!! > 60000L
	} else {
		true
	}

	private fun readBufferedMessages() {
		this.bufferedMessages = dbHelper.getBufferedMessages()
		removeOldBufferedMessages()
	}

	private fun readLastMessages() {
		this.lastLocationPoints.addAll(dbHelper.getLastMessages())
	}

	private class SQLiteHelper(context: Context) :
		SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

		private val log = PlatformUtil.getLog(SQLiteHelper::class.java)

		override fun onCreate(db: SQLiteDatabase) {
			db.execSQL(TIMELINE_TABLE_CREATE)
			db.execSQL("CREATE INDEX IF NOT EXISTS $DATE_INDEX ON $TIMELINE_TABLE_NAME (\"$COL_TIME\" DESC);")
			db.execSQL(BUFFER_TABLE_CREATE)
		}

		override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
			db.execSQL(TIMELINE_TABLE_DELETE)
			db.execSQL(BUFFER_TABLE_DELETE)
			onCreate(db)
		}

		internal fun addBufferedMessage(message: BufferMessage) {
			writableDatabase?.execSQL(BUFFER_TABLE_INSERT,
				arrayOf(message.chatId, message.lat, message.lon, message.altitude, message.speed,
					message.hdop, message.bearing, message.time, message.type, message.deviceName))
		}

		internal fun addLocationMessage(message: LocationMessage) {
			writableDatabase?.execSQL(TIMELINE_TABLE_INSERT,
				arrayOf(message.userId, message.chatId, message.lat, message.lon, message.altitude, message.speed,
					message.hdop, message.bearing, message.time, message.type, message.messageId, message.distanceFromPrev, message.deviceName))
		}

		internal fun getMessagesForUser(userId: Long, start: Long, end: Long): List<LocationMessage> {
			val res = arrayListOf<LocationMessage>()
			readableDatabase?.rawQuery(
				"$TIMELINE_TABLE_SELECT WHERE $COL_USER_ID = ? AND $COL_TIME BETWEEN $start AND $end ORDER BY $COL_CHAT_ID ASC, $COL_TYPE DESC, $COL_TIME ASC ",
				arrayOf(userId.toString()))?.apply {
				if (moveToFirst()) {
					do {
						res.add(readLocationMessage(this@apply))
					} while (moveToNext())
				}
				close()
			}
			return res
		}

		internal fun getIngoingMessages(currentUserId: Long, start: Long, end: Long): List<LocationMessage> {
			val res = arrayListOf<LocationMessage>()
			readableDatabase?.rawQuery(
				"$TIMELINE_TABLE_SELECT WHERE $COL_USER_ID != ? AND $COL_TIME BETWEEN $start AND $end ORDER BY $COL_USER_ID, $COL_CHAT_ID, $COL_TYPE DESC, $COL_TIME ",
				arrayOf(currentUserId.toString()))?.apply {
				if (moveToFirst()) {
					do {
						res.add(readLocationMessage(this@apply))
					} while (moveToNext())
				}
				close()
			}
			return res
		}

		internal fun getIngoingUserLocations(start: Long, end: Long): List<UserLocations> {
			val res = arrayListOf<UserLocations>()
			readableDatabase?.rawQuery("$TIMELINE_TABLE_SELECT WHERE $COL_TIME BETWEEN $start AND $end ORDER BY $COL_USER_ID, $COL_CHAT_ID, $COL_DEVICE_NAME, $COL_TYPE DESC, $COL_TIME ", null)?.apply {
				if (moveToFirst()) {
					var userId: Long
					var chatId: Long
					var deviceName: String
					var userLocations: UserLocations? = null
					var userLocationsMap: MutableMap<Int, MutableList<UserTrkSegment>>? = null
					var segment: UserTrkSegment? = null
					do {
						val locationMessage = readLocationMessage(this@apply)
						userId = locationMessage.userId
						chatId = locationMessage.chatId
						deviceName = locationMessage.deviceName
						if (userLocations == null || userLocations.userId != userId ||
							userLocations.chatId != chatId || userLocations.deviceName != deviceName) {
							userLocationsMap = mutableMapOf()
							userLocations = UserLocations(userId, chatId, deviceName, userLocationsMap)
							res.add(userLocations)
							segment = null
						}
						if (segment == null || segment.type != locationMessage.type || locationMessage.time - segment.maxTime > 30 * 1000 * 60) {
							segment = UserTrkSegment(mutableListOf(), 0.0, locationMessage.type, locationMessage.time, locationMessage.time)
							if (userLocationsMap!![segment.type] == null) {
								userLocationsMap[segment.type] = mutableListOf()
							}
							userLocationsMap[segment.type]!!.add(segment)
						}
						if (segment.points.size > 0) {
							segment.distance += MapUtils.getDistance(locationMessage.lat, locationMessage.lon, segment.points.last().lat, segment.points.last().lon)
						}
						segment.maxTime = locationMessage.time
						segment.points.add(locationMessage)
					} while (moveToNext())
				}
				close()
			}
			return res
		}

		internal fun getIngoingUserLocationsInChat(userId: Long, chatId: Long, deviceName: String,start: Long, end: Long): UserLocations? {
			val userLocationsMap: MutableMap<Int, MutableList<UserTrkSegment>> = mutableMapOf()
			val userLocations = UserLocations(userId,chatId,deviceName,userLocationsMap)
			val whereDeviceQuery = if (deviceName.isNotEmpty()) "AND $COL_DEVICE_NAME = ?" else ""
			val args = if (deviceName.isNotEmpty()) arrayOf(userId.toString(), chatId.toString(), deviceName) else arrayOf(userId.toString(), chatId.toString())
			readableDatabase?.rawQuery("$TIMELINE_TABLE_SELECT WHERE $COL_USER_ID = ? AND $COL_CHAT_ID = ? $whereDeviceQuery AND $COL_TIME BETWEEN $start AND $end ORDER BY $COL_TYPE DESC, $COL_TIME ", args)?.apply {
				if (moveToFirst()) {
					var segment: UserTrkSegment? = null
					do {
						val locationMessage = readLocationMessage(this@apply)
						if (segment == null || segment.type != locationMessage.type || locationMessage.time - segment.maxTime > 30 * 1000 * 60) {
							segment = UserTrkSegment(mutableListOf(), 0.0, locationMessage.type, locationMessage.time, locationMessage.time)
							if (userLocationsMap[segment.type] == null) {
								userLocationsMap[segment.type] = mutableListOf<UserTrkSegment>()
							}
							userLocationsMap[segment.type]?.add(segment)
						}
						if (segment.points.size > 0) {
							segment.distance += MapUtils.getDistance(locationMessage.lat, locationMessage.lon, segment.points.last().lat, segment.points.last().lon)
						}
						segment.maxTime = locationMessage.time
						segment.points.add(locationMessage)
					} while (moveToNext())
				}
				close()
			}
			return userLocations
		}

		internal fun getMessagesForUserInChat(userId: Long, chatId: Long, deviceName: String, start: Long, end: Long): List<LocationMessage> {
			val res = arrayListOf<LocationMessage>()
			val whereDeviceQuery = if (deviceName.isNotEmpty()) "AND $COL_DEVICE_NAME = ?" else ""
			val args = if (deviceName.isNotEmpty()) arrayOf(userId.toString(), chatId.toString(), deviceName) else arrayOf(userId.toString(), chatId.toString())
			readableDatabase?.rawQuery(
				"$TIMELINE_TABLE_SELECT WHERE $COL_USER_ID = ? AND $COL_CHAT_ID = ? $whereDeviceQuery AND $COL_TIME BETWEEN $start AND $end ORDER BY $COL_TYPE DESC, $COL_TIME ", args)?.apply {
				if (moveToFirst()) {
					do {
						res.add(readLocationMessage(this@apply))
					} while (moveToNext())
				}
				close()
			}
			return res
		}

		internal fun getBufferedMessages(): List<BufferMessage> {
			val res = arrayListOf<BufferMessage>()
			readableDatabase?.rawQuery(BUFFER_TABLE_SELECT, null)?.apply {
				if (moveToFirst()) {
					do {
						res.add(readBufferMessage(this@apply))
					} while (moveToNext())
				}
				close()
			}
			return res
		}

		internal fun getLastMessages(): MutableList<LocationMessage> {
			val res = arrayListOf<LocationMessage>()
			readableDatabase?.rawQuery("$TIMELINE_TABLE_SELECT_LAST_LOCATIONS GROUP BY $COL_USER_ID, $COL_CHAT_ID, $COL_DEVICE_NAME, $COL_TYPE", null)?.apply {
				if (moveToFirst()) {
					do {
						val locationMessage = readLocationMessage(this@apply)
						res.add(locationMessage)
						log.debug("add last location message - $locationMessage")
					} while (moveToNext())
				}
				close()
			}
			return res
		}

		internal fun readLocationMessage(cursor: Cursor): LocationMessage {
			val userId = cursor.getLong(0)
			val chatId = cursor.getLong(1)
			val lat = cursor.getDouble(2)
			val lon = cursor.getDouble(3)
			val altitude = cursor.getDouble(4)
			val speed = cursor.getDouble(5)
			val hdop = cursor.getDouble(6)
			val bearing = cursor.getDouble(7)
			val date = cursor.getLong(8)
			val type = cursor.getInt(9)
			val messageId = cursor.getLong(10)
			val distanceFromPrev = cursor.getDouble(11)
			val botName = cursor.getString(12)

			return LocationMessage(userId, chatId, lat, lon, altitude, speed, hdop, bearing, date, type, messageId, distanceFromPrev, botName)
		}

		internal fun readBufferMessage(cursor: Cursor): BufferMessage {
			val chatId = cursor.getLong(0)
			val lat = cursor.getDouble(1)
			val lon = cursor.getDouble(2)
			val altitude = cursor.getDouble(3)
			val speed = cursor.getDouble(4)
			val hdop = cursor.getDouble(5)
			val bearing = cursor.getDouble(6)
			val date = cursor.getLong(7)
			val type = cursor.getInt(8)
			val botName = cursor.getString(9)

			return BufferMessage(chatId, lat, lon, altitude, speed, hdop, bearing, date, type, botName)
		}

		internal fun clearBufferedMessages() {
			writableDatabase?.execSQL(BUFFER_TABLE_CLEAR)
		}

		internal fun removeBufferedMessage(message: BufferMessage) {

			writableDatabase?.execSQL(
				BUFFER_TABLE_REMOVE,
				arrayOf(
					message.chatId,
					message.lat,
					message.lon,
					message.altitude,
					message.speed,
					message.hdop,
					message.bearing,
					message.time,
					message.type,
					message.deviceName
				)
			)
		}

		companion object {

			private const val DATABASE_NAME = "location_messages"
			private const val DATABASE_VERSION = 6

			private const val TIMELINE_TABLE_NAME = "timeline"
			private const val BUFFER_TABLE_NAME = "buffer"

			private const val COL_USER_ID = "user_id"
			private const val COL_CHAT_ID = "chat_id"
			private const val COL_TIME = "time"
			private const val COL_LAT = "lat"
			private const val COL_LON = "lon"
			private const val COL_ALTITUDE = "altitude"
			private const val COL_SPEED = "speed"
			private const val COL_HDOP = "hdop"
			private const val COL_BEARING = "bearing"
			private const val COL_TYPE = "type" // 0 = user map message, 1 = user text message, 2 = bot map message, 3 = bot text message
			private const val COL_MESSAGE_ID = "message_id"
			private const val COL_DISTANCE_FROM_PREV = "distance_from_prev"
			private const val COL_DEVICE_NAME = "device_name"

			private const val DATE_INDEX = "date_index"

			// Timeline messages table
			private const val TIMELINE_TABLE_INSERT =
				("INSERT INTO $TIMELINE_TABLE_NAME ($COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE,  $COL_MESSAGE_ID,  $COL_DISTANCE_FROM_PREV,  $COL_DEVICE_NAME) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")

			private const val TIMELINE_TABLE_CREATE =
				("CREATE TABLE IF NOT EXISTS $TIMELINE_TABLE_NAME ($COL_USER_ID long, $COL_CHAT_ID long,$COL_LAT double, $COL_LON double, $COL_ALTITUDE double, $COL_SPEED float, $COL_HDOP double, $COL_BEARING double, $COL_TIME long, $COL_TYPE int, $COL_MESSAGE_ID long, $COL_DISTANCE_FROM_PREV double, $COL_DEVICE_NAME TEXT NOT NULL DEFAULT '')")

			private const val TIMELINE_TABLE_SELECT =
				"SELECT $COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE, $COL_MESSAGE_ID, $COL_DISTANCE_FROM_PREV, $COL_DEVICE_NAME FROM $TIMELINE_TABLE_NAME"

			private const val TIMELINE_TABLE_SELECT_LAST_LOCATIONS =
					"SELECT $COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE, $COL_MESSAGE_ID, $COL_DISTANCE_FROM_PREV, $COL_DEVICE_NAME, MAX($COL_TIME) FROM $TIMELINE_TABLE_NAME"

			private const val TIMELINE_TABLE_CLEAR = "DELETE FROM $TIMELINE_TABLE_NAME"

			private const val TIMELINE_TABLE_DELETE = "DROP TABLE IF EXISTS $TIMELINE_TABLE_NAME"

			// Buffer messages table
			private const val BUFFER_TABLE_INSERT =
				("INSERT INTO $BUFFER_TABLE_NAME ($COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE,  $COL_DEVICE_NAME) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")

			private const val BUFFER_TABLE_CREATE =
				("CREATE TABLE IF NOT EXISTS $BUFFER_TABLE_NAME ($COL_CHAT_ID long, $COL_LAT double, $COL_LON double, $COL_ALTITUDE double, $COL_SPEED float, $COL_HDOP double, $COL_BEARING double, $COL_TIME long, $COL_TYPE int,  $COL_DEVICE_NAME TEXT NOT NULL DEFAULT '')")

			private const val BUFFER_TABLE_SELECT =
				"SELECT $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE, $COL_DEVICE_NAME FROM $BUFFER_TABLE_NAME"

			private const val BUFFER_TABLE_CLEAR = "DELETE FROM $BUFFER_TABLE_NAME"

			private const val BUFFER_TABLE_REMOVE = "DELETE FROM $BUFFER_TABLE_NAME  WHERE $COL_CHAT_ID = ? AND $COL_LAT = ? AND $COL_LON = ? AND $COL_ALTITUDE = ? AND $COL_SPEED = ? AND $COL_HDOP = ? AND $COL_BEARING = ? AND $COL_TIME = ? AND $COL_TYPE = ? AND $COL_DEVICE_NAME = ?"

			private const val BUFFER_TABLE_DELETE = "DROP TABLE IF EXISTS $BUFFER_TABLE_NAME"
		}
	}

	data class LocationMessage(
		val userId: Long,
		val chatId: Long,
		val lat: Double,
		val lon: Double,
		val altitude: Double,
		val speed: Double,
		val hdop: Double,
		val bearing: Double,
		val time: Long,
		val type: Int,
		val messageId: Long,
		val distanceFromPrev: Double,
		val deviceName: String)

	data class BufferMessage (
		val chatId: Long,
		val lat: Double,
		val lon: Double,
		val altitude: Double,
		val speed: Double,
		val hdop: Double,
		val bearing: Double,
		val time: Long,
		val type: Int,
		val deviceName: String)

	data class UserLocations(
		val userId: Long,
		val chatId: Long,
		val deviceName: String,
		val locationsByType: Map<Int, List<UserTrkSegment>>
	) {
		fun getUniqueSegments(): List<UserTrkSegment> {
			val list = mutableListOf<UserTrkSegment>()
			if (locationsByType.containsKey(TYPE_MY_LOCATION)) {
				return locationsByType[TYPE_MY_LOCATION] ?: list
			}
			list.addAll(locationsByType[TYPE_TEXT] ?: emptyList())
			val mapList = locationsByType[TYPE_MAP] ?: emptyList()
			mapList.forEach {
				var ti = 0
				while (ti < list.size && list[ti].maxTime < it.minTime) {
					ti++
				}
				if (ti < list.size && list[ti].minTime > it.maxTime) {
					list.add(ti, it)
				} else if (ti == list.size) {
					list.add(it)
				}
			}


			return list
		}
	}

	data class UserTrkSegment(
		val points: MutableList<LocationMessage>,
		var distance: Double,
		var type: Int,
		var minTime: Long,
		var maxTime: Long
	) {
		fun newer(other: UserTrkSegment): Boolean {
			return other.maxTime < maxTime
		}

		fun overlap(other: UserTrkSegment): Boolean {
			return if (other.maxTime < maxTime) {
				other.maxTime > minTime
			} else {
				other.minTime < maxTime
			}
		}
	}

	companion object {

		const val TYPE_MAP = 0
		const val TYPE_TEXT = 1
		const val TYPE_MY_LOCATION = 3
	}
}