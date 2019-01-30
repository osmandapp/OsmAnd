package net.osmand.telegram.helpers

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.osmand.PlatformUtil
import net.osmand.telegram.TelegramApplication

class LocationMessages(val app: TelegramApplication) {

	private val log = PlatformUtil.getLog(LocationMessages::class.java)

	// todo - bufferedMessages is for prepared/pending messages only. On app start we read prepared/pending messages to bufferedMessages. After status changed to sent/error - remove message from buffered.
	private var bufferedMessages = emptyList<BufferMessage>()

	private val dbHelper: SQLiteHelper

	init {
		dbHelper = SQLiteHelper(app)
		readBufferedMessages()
	}

	fun getPreparedMessages(): List<BufferMessage> {
		return bufferedMessages.sortedBy { it.time }
	}

	fun getPreparedMessagesForChat(chatId: Long): List<BufferMessage> {
		log.debug("getPreparedMessagesForChat chatId")
		return bufferedMessages.filter { it.chatId==chatId }.sortedBy { it.time }
	}

	// todo - read from db by date (Victor's suggestion - filter by one day only. Need to be changed in UI also.
	fun getIngoingMessages(currentUserId: Int, start: Long, end: Long): List<LocationMessage> {
		return dbHelper.getIngoingMessages(currentUserId, start, end)
	}

	fun getIngoingMessagesForUser(userId: Int, chatId: Long, start: Long, end: Long): List<LocationMessage> {
		return dbHelper.getIngoingMessagesForUser(userId, chatId, start, end)
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

	fun addLocationMessage(message: LocationMessage) {
		log.debug("addIngoingMessage $message")
		dbHelper.addLocationMessage(message)
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

	private fun readBufferedMessages() {
		this.bufferedMessages = dbHelper.getBufferedMessages()
	}

	private class SQLiteHelper(context: Context) :
		SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

		override fun onCreate(db: SQLiteDatabase) {
			db.execSQL(TIMELINE_TABLE_CREATE)
			db.execSQL("CREATE INDEX $DATE_INDEX ON $TIMELINE_TABLE_NAME (\"$COL_TIME\" DESC);")
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
					message.hdop, message.bearing, message.time, message.type))
		}

		internal fun getMessagesForUser(userId: Int, start: Long, end: Long): List<LocationMessage> {
			val res = arrayListOf<LocationMessage>()
			readableDatabase?.rawQuery(
				"$TIMELINE_TABLE_SELECT WHERE $COL_USER_ID = ? AND $COL_TIME BETWEEN $start AND $end ORDER BY $COL_TIME ASC ",
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

		internal fun getIngoingMessages(currentUserId: Int, start: Long, end: Long): List<LocationMessage> {
			val res = arrayListOf<LocationMessage>()
			readableDatabase?.rawQuery(
				"$TIMELINE_TABLE_SELECT WHERE $COL_USER_ID != ? AND $COL_CHAT_ID = ? AND $COL_TIME BETWEEN $start AND $end ORDER BY $COL_TIME ASC ",
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

		internal fun getIngoingMessagesForUser(userId: Int, chatId: Long, start: Long, end: Long): List<LocationMessage> {
			val res = arrayListOf<LocationMessage>()
			readableDatabase?.rawQuery(
				"$TIMELINE_TABLE_SELECT WHERE $COL_USER_ID = ? AND $COL_CHAT_ID = ? AND $COL_TIME BETWEEN $start AND $end ORDER BY $COL_TIME ASC ",
				arrayOf(userId.toString(), chatId.toString(), start.toString(), end.toString()))?.apply {
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

			return LocationMessage(userId, chatId, lat, lon, altitude, speed, hdop, bearing, date, type)
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
			private const val DATABASE_VERSION = 4

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

			private const val DATE_INDEX = "date_index"

			// Timeline messages table
			private const val TIMELINE_TABLE_INSERT =
				("INSERT INTO $TIMELINE_TABLE_NAME ($COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE,  $COL_MESSAGE_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")

			private const val TIMELINE_TABLE_CREATE =
				("CREATE TABLE IF NOT EXISTS $TIMELINE_TABLE_NAME ($COL_USER_ID long, $COL_CHAT_ID long,$COL_LAT double, $COL_LON double, $COL_ALTITUDE double, $COL_SPEED float, $COL_HDOP double, $COL_BEARING double, $COL_TIME long, $COL_TYPE int, $COL_MESSAGE_ID long )")

			private const val TIMELINE_TABLE_SELECT =
				"SELECT $COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE, $COL_MESSAGE_ID FROM $TIMELINE_TABLE_NAME"

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
		val type: Int)

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

	companion object {

		const val TYPE_USER_MAP = 0
		const val TYPE_USER_TEXT = 1
		const val TYPE_USER_BOTH = 2
		const val TYPE_BOT_MAP = 3
		const val TYPE_BOT_TEXT = 4
		const val TYPE_BOT_BOTH = 5
	}
}