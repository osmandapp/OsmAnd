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
	private var bufferedMessages = emptyList<LocationMessage>()

	private val dbHelper: SQLiteHelper

	init {
		dbHelper = SQLiteHelper(app)
		readBufferedMessages()
	}

	fun getPreparedMessages(): List<LocationMessage> {
		val currentUserId = app.telegramHelper.getCurrentUserId()
		return bufferedMessages.filter { it.userId == currentUserId && it.status == LocationMessage.STATUS_PREPARED }.sortedBy { it.date }
	}

	// todo - drop method. add collected / sent messages count to ShareChatInfo
	fun getOutgoingMessages(chatId: Long): List<LocationMessage> {
		val currentUserId = app.telegramHelper.getCurrentUserId()
		return bufferedMessages.filter { it.userId == currentUserId && it.chatId == chatId }.sortedBy { it.date }
	}

	// todo - drop it or read from db
	fun getSentMessages(chatId: Long, date:Long): List<LocationMessage> {
		val currentUserId = app.telegramHelper.getCurrentUserId()
		return bufferedMessages.filter { it.userId == currentUserId && it.chatId == chatId && it.date > date }.sortedBy { it.date }
	}

	// todo - drop it or read from db
	fun getSentMessages(chatId: Long): List<LocationMessage> {
		val currentUserId = app.telegramHelper.getCurrentUserId()
		return bufferedMessages.filter { it.userId == currentUserId && it.chatId == chatId && it.status == LocationMessage.STATUS_SENT }.sortedBy { it.date }
	}

	// todo - read from db by date (Victor's suggestion - filter by one day only. Need to be changed in UI also.
	fun getIngoingMessages(userId: Int, date: Date): List<LocationMessage> {
		return emptyList()
	}

	fun addOutgoingMessage(message: LocationMessage) {
		val messages = mutableListOf(*this.bufferedMessages.toTypedArray())
		messages.add(message)
		this.bufferedMessages = messages
		dbHelper.addOutgoingMessage(message)
	}

	fun addIngoingMessage(message: LocationMessage) {
		dbHelper.addIngoingMessage(message)
	}

	fun clearOutgoingMessages() {
		dbHelper.clearOutgoingMessages()
		bufferedMessages = emptyList()
	}

	// todo - both methods should be refactored since we have two tables now for outgoing/ingoing messages. Data should be read from db.
	fun collectRecordedDataForUser(userId: Int, chatId: Long, start: Long, end: Long): List<LocationMessage> {
		return if (chatId == 0L) {
			bufferedMessages.sortedWith(compareBy({ it.userId }, { it.chatId })).filter { it.userId == userId && it.date in (start + 1)..(end - 1) }
		} else {
			bufferedMessages.sortedWith(compareBy({ it.userId }, { it.chatId })).filter {
				it.chatId == chatId && it.userId == userId && it.status == LocationMessage.STATUS_SENT && it.date in (start + 1)..(end - 1) }
		}
	}

	fun collectRecordedDataForUsers(start: Long, end: Long, ignoredUsersIds: ArrayList<Int>): List<LocationMessage> {
		return bufferedMessages.sortedWith(compareBy({ it.userId }, { it.chatId })).filter {
			it.date in (start + 1)..(end - 1) && !ignoredUsersIds.contains(it.userId)
		}
	}

	private fun readBufferedMessages() {
		this.bufferedMessages = dbHelper.getOutgoingMessages();
	}

	private class SQLiteHelper(context: Context) :
		SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

		override fun onCreate(db: SQLiteDatabase) {
			db.execSQL(OUTGOING_TABLE_CREATE)
			db.execSQL("CREATE INDEX $DATE_INDEX ON $OUTGOING_TABLE_NAME (\"$COL_DATE\" DESC);")
			db.execSQL(INGOING_TABLE_CREATE)
			db.execSQL("CREATE INDEX $DATE_INDEX ON $INGOING_TABLE_NAME (\"$COL_DATE\" DESC);")
		}

		override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
			db.execSQL(OUTGOING_TABLE_DELETE)
			db.execSQL(INGOING_TABLE_DELETE)
			onCreate(db)
		}

		internal fun addOutgoingMessage(message: LocationMessage) {
			writableDatabase?.execSQL(OUTGOING_TABLE_INSERT,
					arrayOf(message.userId, message.chatId, message.lat, message.lon, message.altitude, message.speed,
							message.hdop, message.bearing, message.date, message.type, message.status, message.messageId))
		}

		internal fun addIngoingMessage(message: LocationMessage) {
			writableDatabase?.execSQL(INGOING_TABLE_INSERT,
					arrayOf(message.userId, message.chatId, message.lat, message.lon, message.altitude, message.speed,
							message.hdop, message.bearing, message.date, message.type, message.status, message.messageId))
		}

		internal fun getOutgoingMessages(): List<LocationMessage> {
			val res = arrayListOf<LocationMessage>()
			readableDatabase?.rawQuery(OUTGOING_TABLE_SELECT, null)?.apply {
				if (moveToFirst()) {
					do {
						res.add(readLocationMessage(this@apply))
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
			val status = cursor.getInt(10)
			val messageId = cursor.getLong(11)

			return LocationMessage(userId, chatId, lat, lon, altitude, speed, hdop, bearing, date, type, status, messageId)
		}

		internal fun clearOutgoingMessages() {
			writableDatabase?.execSQL(OUTGOING_TABLE_CLEAR)
		}

		companion object {

			private const val DATABASE_NAME = "location_messages"
			private const val DATABASE_VERSION = 3

			private const val OUTGOING_TABLE_NAME = "outgoing"
			private const val INGOING_TABLE_NAME = "ingoing"

			private const val COL_USER_ID = "user_id"
			private const val COL_CHAT_ID = "chat_id"
			private const val COL_DATE = "date"
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

			// Outgoing messages table
			private const val OUTGOING_TABLE_INSERT =
				("INSERT INTO $OUTGOING_TABLE_NAME ($COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_DATE, $COL_TYPE, $COL_MESSAGE_STATUS, $COL_MESSAGE_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")

			private const val OUTGOING_TABLE_CREATE =
				("CREATE TABLE IF NOT EXISTS $OUTGOING_TABLE_NAME ($COL_USER_ID long, $COL_CHAT_ID long,$COL_LAT double, $COL_LON double, $COL_ALTITUDE double, $COL_SPEED float, $COL_HDOP double, $COL_BEARING double, $COL_DATE long, $COL_TYPE int, $COL_MESSAGE_STATUS int, $COL_MESSAGE_ID long )")

			private const val OUTGOING_TABLE_SELECT =
				"SELECT $COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_DATE, $COL_TYPE, $COL_MESSAGE_STATUS, $COL_MESSAGE_ID FROM $OUTGOING_TABLE_NAME"

			private const val OUTGOING_TABLE_CLEAR = "DELETE FROM $OUTGOING_TABLE_NAME"

			private const val OUTGOING_TABLE_DELETE = "DROP TABLE IF EXISTS $OUTGOING_TABLE_NAME"

			// Ingoing messages table
			private const val INGOING_TABLE_INSERT =
					("INSERT INTO $INGOING_TABLE_NAME ($COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_DATE, $COL_TYPE, $COL_MESSAGE_STATUS, $COL_MESSAGE_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")

			private const val INGOING_TABLE_CREATE =
					("CREATE TABLE IF NOT EXISTS $INGOING_TABLE_NAME ($COL_USER_ID long, $COL_CHAT_ID long,$COL_LAT double, $COL_LON double, $COL_ALTITUDE double, $COL_SPEED float, $COL_HDOP double, $COL_BEARING double, $COL_DATE long, $COL_TYPE int, $COL_MESSAGE_STATUS int, $COL_MESSAGE_ID long )")

			private const val INGOING_TABLE_SELECT =
					"SELECT $COL_USER_ID, $COL_CHAT_ID, $COL_LAT, $COL_LON, $COL_ALTITUDE, $COL_SPEED, $COL_HDOP, $COL_BEARING, $COL_DATE, $COL_TYPE, $COL_MESSAGE_STATUS, $COL_MESSAGE_ID FROM $INGOING_TABLE_NAME"

			private const val INGOING_TABLE_CLEAR = "DELETE FROM $INGOING_TABLE_NAME"

			private const val INGOING_TABLE_DELETE = "DROP TABLE IF EXISTS $INGOING_TABLE_NAME"
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
		val type: Int,
		// todo - status and messageId should be updated in db right away. Make them val instead of var.
		var status: Int,
		var messageId: Long) {

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
}