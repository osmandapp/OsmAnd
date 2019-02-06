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
import org.drinkless.td.libcore.telegram.TdApi

class LocationMessages(val app: TelegramApplication) {

	private val log = PlatformUtil.getLog(LocationMessages::class.java)

	private var bufferedMessages = emptyList<BufferMessage>()

	private var lastLocationPoints = mutableMapOf<LocationHistoryPoint, LatLon>()

	private val dbHelper: SQLiteHelper

	init {
		dbHelper = SQLiteHelper(app)
		readBufferedMessages()
	}

	fun getBufferedMessages(): List<BufferMessage> {
		return bufferedMessages.sortedBy { it.time }
	}

	fun getBufferedMessagesForChat(chatId: Long): List<BufferMessage> {
		return bufferedMessages.filter { it.chatId==chatId }.sortedBy { it.time }
	}

	fun getIngoingMessages(currentUserId: Int, start: Long, end: Long): List<LocationMessage> {
		return dbHelper.getIngoingMessages(currentUserId, start, end)
	}

	fun getIngoingUserLocations(start: Long, end: Long): List<UserLocations> {
		return dbHelper.getIngoingUserLocations(start, end)
	}

	fun getMessagesForUserInChat(userId: Int, chatId: Long, start: Long, end: Long): List<LocationMessage> {
		return dbHelper.getMessagesForUserInChat(userId, chatId, start, end)
	}

	fun getMessagesForUser(userId: Int, start: Long, end: Long): List<LocationMessage> {
		return dbHelper.getMessagesForUser(userId, start, end)
	}

	fun addBufferedMessage(message: BufferMessage) {
		log.debug("addBufferedMessage $message")
		val messages = mutableListOf(*this.bufferedMessages.toTypedArray())
		messages.add(message)
		this.bufferedMessages = messages
		dbHelper.addBufferedMessage(message)
	}

	fun addNewLocationMessage(message: TdApi.Message) {
		log.debug("addNewLocationMessage ${message.id}")
		val type = OsmandLocationUtils.getMessageType(message, app.telegramHelper)

		val newItem = LocationHistoryPoint(message.senderUserId, message.chatId, type)
		val previousMessageLatLon = lastLocationPoints[newItem]
		val locationMessage = OsmandLocationUtils.parseMessage(message, app.telegramHelper, previousMessageLatLon)
		if (locationMessage != null) {
			dbHelper.addLocationMessage(locationMessage)
			lastLocationPoints[newItem] = LatLon(locationMessage.lat, locationMessage.lon)
		}
	}

	fun addMyLocationMessage(loc: Location) {
		log.debug("addMyLocationMessage")
		val currentUserId = app.telegramHelper.getCurrentUserId()
		val newItem = LocationHistoryPoint(currentUserId, 0, -1)
		val previousMessageLatLon = lastLocationPoints[newItem]
		val distance = if (previousMessageLatLon != null) { MapUtils.getDistance(previousMessageLatLon, loc.latitude, loc.longitude) } else 0.0
		val message = LocationMessages.LocationMessage(currentUserId, 0, loc.latitude, loc.longitude, loc.altitude,
			loc.speed.toDouble(), loc.accuracy.toDouble(), loc.bearing.toDouble(), loc.time, TYPE_MY_LOCATION, 0, distance)

		dbHelper.addLocationMessage(message)
		lastLocationPoints[newItem] = LatLon(message.lat, message.lon)
	}

	fun clearBufferedMessages() {
		log.debug("clearBufferedMessages")
		dbHelper.clearBufferedMessages()
		bufferedMessages = emptyList()
	}

	fun removeBufferedMessage(message: BufferMessage) {
		log.debug("removeBufferedMessage $message")
		val messages = mutableListOf(*this.bufferedMessages.toTypedArray())
		messages.remove(message)
		this.bufferedMessages = messages
		dbHelper.removeBufferedMessage(message)
	}

	fun getBufferedMessagesCount(): Int {
		return bufferedMessages.size
	}

	fun getBufferedMessagesCountForChat(chatId: Long): Int {
		return bufferedMessages.count { it.chatId == chatId }
	}

	private fun readBufferedMessages() {
		this.bufferedMessages = dbHelper.getBufferedMessages()
	}

	private fun readLastMessages() {
		this.lastLocationPoints = dbHelper.getLastMessages()
	}

	private class SQLiteHelper(context: Context) :
		SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

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
					message.hdop, message.bearing, message.time, message.type))
		}

		internal fun addLocationMessage(message: LocationMessage) {
			writableDatabase?.execSQL(TIMELINE_TABLE_INSERT,
				arrayOf(message.userId, message.chatId, message.lat, message.lon, message.altitude, message.speed,
					message.hdop, message.bearing, message.time, message.type, message.messageId, message.distanceFromPrev))
		}

		internal fun getMessagesForUser(userId: Int, start: Long, end: Long): List<LocationMessage> {
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

		internal fun getPreviousMessage(userId: Int, chatId: Long): LocationMessage? {
			var res:LocationMessage? = null
			readableDatabase?.rawQuery(
				"$TIMELINE_TABLE_SELECT WHERE $COL_USER_ID = ? AND $COL_CHAT_ID = ? ORDER BY $COL_TIME DESC LIMIT 1",
				arrayOf(userId.toString(), chatId.toString()))?.apply {
				if (moveToFirst()) {
					res = readLocationMessage(this@apply)
				}
				close()
			}
			return res
		}

		internal fun getIngoingMessages(currentUserId: Int, start: Long, end: Long): List<LocationMessage> {
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
			readableDatabase?.rawQuery(
				"$TIMELINE_TABLE_SELECT WHERE $COL_TIME BETWEEN $start AND $end ORDER BY $COL_USER_ID, $COL_CHAT_ID, $COL_TYPE DESC, $COL_TIME ", null
				)?.apply {
				if (moveToFirst()) {
					var userId = -1
					var chatId = -1L
					// TODO query bot name
					var botName = ""
					var userLocations: UserLocations? = null
					var userLocationsMap: MutableMap<Int, MutableList<UserTrkSegment>>? = null
					var userLocationsListByType: MutableList<LocationMessage>? = null
					var segment: UserTrkSegment? = null
					do {
						val locationMessage = readLocationMessage(this@apply)
						userId = locationMessage.userId
						chatId = locationMessage.chatId
						// TODO compare bot name as well
						if(userLocations == null || userLocations.userId != userId ||
							userLocations.chatId != chatId) {
							userLocationsMap = mutableMapOf()
							userLocations = UserLocations(userId, chatId, botName, userLocationsMap)
							res.add(userLocations)
							segment = null
						}
						if(segment == null ||
							segment.type != locationMessage.type || locationMessage.time - segment.maxTime > 30 * 1000 * 60) {
							segment = UserTrkSegment(mutableListOf(), 0.0, locationMessage.type,
								locationMessage.time, locationMessage.time)
							if(userLocationsMap!![segment.type] == null) {
								userLocationsMap[segment.type] = mutableListOf()
							}
							userLocationsMap[segment.type]!!.add(segment)
						}
						if(segment.points.size > 0) {
							segment.distance += MapUtils.getDistance(locationMessage.lat,
								locationMessage.lon, segment.points.last().lat, segment.points.last().lon)
						}
						segment.maxTime = locationMessage.time
						segment.points.add(locationMessage)
					} while (moveToNext())
				}
				close()
			}
			return res
		}

		internal fun getMessagesForUserInChat(userId: Int, chatId: Long, start: Long, end: Long): List<LocationMessage> {
			val res = arrayListOf<LocationMessage>()
			readableDatabase?.rawQuery(
				"$TIMELINE_TABLE_SELECT WHERE $COL_USER_ID = ? AND $COL_CHAT_ID = ? AND $COL_TIME BETWEEN $start AND $end ORDER BY $COL_TYPE DESC, $COL_TIME ",
				arrayOf(userId.toString(), chatId.toString()))?.apply {
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

		internal fun getLastMessages(): MutableMap<LocationHistoryPoint, LatLon> {
			val res = mutableMapOf<LocationHistoryPoint, LatLon>()
			readableDatabase?.rawQuery("$TIMELINE_TABLE_SELECT_HISTORY_POINTS ORDER BY $COL_TIME ASC", null)?.apply {
				if (moveToFirst()) {
					do {
//						res.add(readLocationMessage(this@apply))
					} while (moveToNext())
				}
				close()
			}
			return res
		}

		internal fun readLocationMessage(cursor: Cursor): LocationMessage {
			val userId = cursor.getInt(0)
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

			return LocationMessage(userId, chatId, lat, lon, altitude, speed, hdop, bearing, date, type, messageId, distanceFromPrev)
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

			return BufferMessage(chatId, lat, lon, altitude, speed, hdop, bearing, date, type)
		}

		internal fun readLocationHistoryPoint(cursor: Cursor): Pair<LocationHistoryPoint, LatLon> {
			val userId = cursor.getInt(0)
			val chatId = cursor.getLong(1)
			val lat = cursor.getDouble(2)
			val lon = cursor.getDouble(3)
			val type = cursor.getInt(4)

			return Pair(LocationHistoryPoint(userId, chatId, type), LatLon(lat, lon))
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
					message.type
				)
			)
		}

		companion object {

			private const val DATABASE_NAME = "location_messages"
			private const val DATABASE_VERSION = 5

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

			private const val DATE_INDEX = "date_index"

			// Timeline messages table
			private const val TIMELINE_TABLE_INSERT =
				("INSERT INTO $TIMELINE_TABLE_NAME ($COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE,  $COL_MESSAGE_ID,  $COL_DISTANCE_FROM_PREV) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")

			private const val TIMELINE_TABLE_CREATE =
				("CREATE TABLE IF NOT EXISTS $TIMELINE_TABLE_NAME ($COL_USER_ID long, $COL_CHAT_ID long,$COL_LAT double, $COL_LON double, $COL_ALTITUDE double, $COL_SPEED float, $COL_HDOP double, $COL_BEARING double, $COL_TIME long, $COL_TYPE int, $COL_MESSAGE_ID long, $COL_DISTANCE_FROM_PREV double )")

			private const val TIMELINE_TABLE_SELECT =
				"SELECT $COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE, $COL_MESSAGE_ID, $COL_DISTANCE_FROM_PREV FROM $TIMELINE_TABLE_NAME"

			private const val TIMELINE_TABLE_SELECT_HISTORY_POINTS =
				"SELECT $COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_TIME, $COL_TYPE FROM $TIMELINE_TABLE_NAME"

			private const val TIMELINE_TABLE_CLEAR = "DELETE FROM $TIMELINE_TABLE_NAME"

			private const val TIMELINE_TABLE_DELETE = "DROP TABLE IF EXISTS $TIMELINE_TABLE_NAME"

			// Buffer messages table
			private const val BUFFER_TABLE_INSERT =
					("INSERT INTO $BUFFER_TABLE_NAME ($COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")

			private const val BUFFER_TABLE_CREATE =
					("CREATE TABLE IF NOT EXISTS $BUFFER_TABLE_NAME ($COL_CHAT_ID long, $COL_LAT double, $COL_LON double, $COL_ALTITUDE double, $COL_SPEED float, $COL_HDOP double, $COL_BEARING double, $COL_TIME long, $COL_TYPE int)")

			private const val BUFFER_TABLE_SELECT =
					"SELECT $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE FROM $BUFFER_TABLE_NAME"

			private const val BUFFER_TABLE_CLEAR = "DELETE FROM $BUFFER_TABLE_NAME"

			private const val BUFFER_TABLE_REMOVE = "DELETE FROM $BUFFER_TABLE_NAME  WHERE $COL_CHAT_ID = ? AND $COL_LAT = ? AND $COL_LON = ? AND $COL_ALTITUDE = ? AND $COL_SPEED = ? AND $COL_HDOP = ? AND $COL_BEARING = ? AND $COL_TIME = ? AND $COL_TYPE = ?"

			private const val BUFFER_TABLE_DELETE = "DROP TABLE IF EXISTS $BUFFER_TABLE_NAME"
		}
	}

	data class LocationMessage(
		val userId: Int,
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
		val distanceFromPrev: Double)

	data class BufferMessage (
		val chatId: Long,
		val lat: Double,
		val lon: Double,
		val altitude: Double,
		val speed: Double,
		val hdop: Double,
		val bearing: Double,
		val time: Long,
		val type: Int)

	data class UserLocations(
		var userId: Int,
		var chatId: Long,
		var botName: String,
		var locationsByType: Map<Int, List<UserTrkSegment>>


	){
		fun getUniqueSegments(): List<UserTrkSegment> {
			// TODO TYPE_BOT_MAP. TYPE_BOT_TEXT, TYPE_USER_BOTH, TYPE_BOT_BOTH - delete
			val list = mutableListOf<UserTrkSegment>()
			if(locationsByType.containsKey(TYPE_MY_LOCATION)) {
				return locationsByType.get(TYPE_MY_LOCATION)?: list
			}
			list.addAll(locationsByType.get(TYPE_USER_TEXT)?: emptyList())
			val mapList = locationsByType.get(TYPE_USER_MAP)?: emptyList();
			mapList.forEach {
				var ti = 0;
				while(ti < list.size && list[ti].maxTime < it.minTime) {
					ti++;
				}
				if(ti < list.size  && list[ti].minTime > it.maxTime ) {
					list.add(ti, it)
				} else if(ti == list.size) {
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
			return other.maxTime < maxTime;
		}


		fun overlap(other: UserTrkSegment): Boolean {

			if(other.maxTime < maxTime) {
				return other.maxTime > minTime;
			} else {
				return other.minTime < maxTime;
			}
		}

	}

	data class LocationHistoryPoint(
		val userId: Int,
		val chatId: Long,
		val type: Int
	) {

		override fun equals(other: Any?): Boolean {
			if (other == null) {
				return false
			}
			if (other !is LocationHistoryPoint) {
				return false
			}
			val o = other as LocationHistoryPoint?
			return this.userId == o!!.userId && this.chatId == o.chatId && this.type == o.type
		}

		override fun hashCode(): Int {
			val prime = 31
			var result = 1
			result = prime * result + userId.hashCode()
			result = prime * result + chatId.hashCode()
			result = prime * result + type.hashCode()
			return result
		}
	}


	companion object {

		const val TYPE_USER_MAP = 0
		const val TYPE_USER_TEXT = 1
		const val TYPE_USER_BOTH = 2
		const val TYPE_BOT_MAP = 3
		const val TYPE_BOT_TEXT = 4
		const val TYPE_BOT_BOTH = 5
		const val TYPE_MY_LOCATION = 6
	}
}
