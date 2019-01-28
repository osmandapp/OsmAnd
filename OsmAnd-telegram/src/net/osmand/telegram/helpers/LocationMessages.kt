package net.osmand.telegram.helpers

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.osmand.telegram.TelegramApplication
import java.util.*
import kotlin.collections.ArrayList

class LocationMessages(val app: TelegramApplication) {

	// todo - bufferedMessages is for prepared/pending messages only. On app start we read prepared/pending messages to bufferedMessages. After status changed to sent/error - remove message from buffered.
	private var bufferedMessages = emptyList<BufferMessage>()

	private val dbHelper: SQLiteHelper

	init {
		dbHelper = SQLiteHelper(app)
		readBufferedMessages()
	}

	fun getPreparedMessages(): List<BufferMessage> {
		return bufferedMessages.sortedBy { it.date }
	}

//	// todo - drop method. add collected / sent messages count to ShareChatInfo
//	fun getOutgoingMessages(chatId: Long): List<LocationMessage> {
//		val currentUserId = app.telegramHelper.getCurrentUserId()
//		return bufferedMessages.filter { it.userId == currentUserId && it.chatId == chatId }.sortedBy { it.date }
//	}

	fun updateBufferedMessageStatus(oldMessage: BufferMessage, status: Int){
		val messages = mutableListOf(*this.bufferedMessages.toTypedArray())
		messages.remove(oldMessage)
		val newMessage = BufferMessage(oldMessage.chatId,oldMessage.lat,oldMessage.lon,oldMessage.altitude,oldMessage.speed,oldMessage.hdop,oldMessage.bearing,oldMessage.date,oldMessage.type,status)
		messages.add(newMessage)
		this.bufferedMessages = messages
//		dbHelper.addBufferedMessage(newMessage)
	}

	// todo - read from db by date (Victor's suggestion - filter by one day only. Need to be changed in UI also.
	fun getIngoingMessages(userId: Int, date: Date): List<LocationMessage> {
		return emptyList()
	}

	fun addBufferedMessage(message: BufferMessage) {
		val messages = mutableListOf(*this.bufferedMessages.toTypedArray())
		messages.add(message)
		this.bufferedMessages = messages
		dbHelper.addBufferedMessage(message)
	}

	fun addIngoingMessage(message: LocationMessage) {
		dbHelper.addIngoingMessage(message)
	}

	fun clearBufferedMessages() {
		dbHelper.clearBufferedMessages()
		bufferedMessages = emptyList()
	}

	// todo - both methods should be refactored since we have two tables now for outgoing/ingoing messages. Data should be read from db.
//	fun collectRecordedDataForUser(userId: Int, chatId: Long, start: Long, end: Long): List<LocationMessage> {
//		return if (chatId == 0L) {
//			bufferedMessages.sortedWith(compareBy({ it.userId }, { it.chatId })).filter { it.userId == userId && it.date in (start + 1)..(end - 1) }
//		} else {
//			bufferedMessages.sortedWith(compareBy({ it.userId }, { it.chatId })).filter {
//				it.chatId == chatId && it.userId == userId&&it.date in (start + 1)..(end - 1) }
//		}
//	}
//
//	fun collectRecordedDataForUsers(start: Long, end: Long, ignoredUsersIds: ArrayList<Int>): List<LocationMessage> {
//		return bufferedMessages.sortedWith(compareBy({ it.userId }, { it.chatId })).filter {
//			it.date in (start + 1)..(end - 1) && !ignoredUsersIds.contains(it.userId)
//		}
//	}

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
							message.hdop, message.bearing, message.date, message.type))
		}

		internal fun addIngoingMessage(message: LocationMessage) {
			writableDatabase?.execSQL(TIMELINE_TABLE_INSERT,
					arrayOf(message.userId, message.chatId, message.lat, message.lon, message.altitude, message.speed,
							message.hdop, message.bearing, message.date, message.type))
		}

		internal fun addOutgoingMessage(message: LocationMessage) {
			writableDatabase?.execSQL(TIMELINE_TABLE_INSERT,
					arrayOf(message.userId, message.chatId, message.lat, message.lon, message.altitude, message.speed,
							message.hdop, message.bearing, message.date, message.type))
		}

		internal fun getOutgoingMessages(): List<LocationMessage> {
			val res = arrayListOf<LocationMessage>()
			readableDatabase?.rawQuery(TIMELINE_TABLE_SELECT, null)?.apply {
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
			val chatId = cursor.getLong(1)
			val lat = cursor.getDouble(2)
			val lon = cursor.getDouble(3)
			val altitude = cursor.getDouble(4)
			val speed = cursor.getDouble(5)
			val hdop = cursor.getDouble(6)
			val bearing = cursor.getDouble(7)
			val date = cursor.getLong(8)
			val type = cursor.getInt(9)
			val status = cursor.getInt(10)

			return BufferMessage(chatId, lat, lon, altitude, speed, hdop, bearing, date, type,status)
		}

		internal fun clearBufferedMessages() {
			writableDatabase?.execSQL(BUFFER_TABLE_CLEAR)
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
			private const val COL_MESSAGE_STATUS = "status" // 0 = preparing , 1 = pending, 2 = sent, 3 = error
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
					("INSERT INTO $BUFFER_TABLE_NAME ($COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE, $COL_MESSAGE_STATUS) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")

			private const val BUFFER_TABLE_CREATE =
					("CREATE TABLE IF NOT EXISTS $BUFFER_TABLE_NAME ($COL_CHAT_ID long, $COL_LAT double, $COL_LON double, $COL_ALTITUDE double, $COL_SPEED float, $COL_HDOP double, $COL_BEARING double, $COL_TIME long, $COL_TYPE int, $COL_MESSAGE_STATUS int)")

			private const val BUFFER_TABLE_SELECT =
					"SELECT $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_TIME, $COL_TYPE, $COL_MESSAGE_STATUS FROM $BUFFER_TABLE_NAME"

			private const val BUFFER_TABLE_CLEAR = "DELETE FROM $BUFFER_TABLE_NAME"

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
		val date: Long,
		val type: Int)

	data class BufferMessage (
		val chatId: Long,
		val lat: Double,
		val lon: Double,
		val altitude: Double,
		val speed: Double,
		val hdop: Double,
		val bearing: Double,
		val date: Long,
		val type: Int,
		// todo - status and messageId should be updated in db right away. Make them val instead of var.
		val status: Int)

	companion object {

		const val STATUS_PREPARED = 0
		const val STATUS_PENDING = 1
		const val STATUS_SENT = 2
		const val STATUS_ERROR = 3

		const val TYPE_USER_MAP = 0
		const val TYPE_USER_TEXT = 1
		const val TYPE_BOT_MAP = 2
		const val TYPE_BOT_TEXT = 3
	}
}