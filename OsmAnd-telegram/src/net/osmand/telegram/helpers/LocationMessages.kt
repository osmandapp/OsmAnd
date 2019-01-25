package net.osmand.telegram.helpers

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.osmand.telegram.TelegramApplication
import kotlin.collections.ArrayList

class LocationMessages(val app: TelegramApplication) {

	private val locationMessages = ArrayList<LocationMessage>()

	private val sqliteHelper: SQLiteHelper

	init {
		sqliteHelper = SQLiteHelper(app)
		readMessages()
	}

	fun getLocationMessages(): List<LocationMessage> {
		return this.locationMessages
	}

	fun getPreparedToShareMessages(): List<LocationMessage> {
		val currentUserId = app.telegramHelper.getCurrentUserId()
		return locationMessages.filter { it.userId == currentUserId && it.status == LocationMessage.STATUS_PREPARED }.sortedBy { it.date }
	}

	fun getOutgoingMessagesToChat(chatId: Long): List<LocationMessage> {
		val currentUserId = app.telegramHelper.getCurrentUserId()
		return locationMessages.filter { it.userId == currentUserId && it.chatId == chatId }.sortedBy { it.date }
	}

	fun getOutgoingMessagesToChatFromDate(chatId: Long, date:Long): List<LocationMessage> {
		val currentUserId = app.telegramHelper.getCurrentUserId()
		return locationMessages.filter { it.userId == currentUserId && it.chatId == chatId && it.date > date }.sortedBy { it.date }
	}

	fun getSentOutgoingMessagesToChat(chatId: Long): List<LocationMessage> {
		val currentUserId = app.telegramHelper.getCurrentUserId()
		return locationMessages.filter { it.userId == currentUserId && it.chatId == chatId && it.status == LocationMessages.LocationMessage.STATUS_SENT }.sortedBy { it.date }
	}

	fun getIncomingMessages(): List<LocationMessage> {
		return locationMessages.filter { it.status != LocationMessage.STATUS_INCOMING }.sortedBy { it.date }
	}

	fun addLocationMessage(locationMessage: LocationMessage) {
		locationMessages.add(locationMessage)
		sqliteHelper.addLocationMessage(locationMessage)
	}

	fun clearMessages() {
		sqliteHelper.clearLocationMessages()
	}

	fun collectRecordedDataForUser(userId: Int, chatId: Long, start: Long, end: Long): List<LocationMessages.LocationMessage> {
		return if (chatId == 0L) {
			locationMessages.sortedWith(compareBy({ it.userId }, { it.chatId })).filter { it.userId == userId&&it.date in (start + 1)..(end - 1) }
		} else {
			locationMessages.sortedWith(compareBy({ it.userId }, { it.chatId })).filter {
				it.chatId == chatId && it.userId == userId && it.status == LocationMessage.STATUS_SENT && it.date in (start + 1)..(end - 1) }
		}
	}

	fun collectRecordedDataForUsers(start: Long, end: Long, ignoredUsersIds: ArrayList<Int>): List<LocationMessages.LocationMessage> {
		return locationMessages.sortedWith(compareBy({ it.userId }, { it.chatId })).filter {
			it.date in (start + 1)..(end - 1) && !ignoredUsersIds.contains(it.userId)
		}
	}

	private fun readMessages() {
		val messages = sqliteHelper.getLocationMessages()
		locationMessages.addAll(messages)
	}

	private class SQLiteHelper(context: Context) :
		SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

		override fun onCreate(db: SQLiteDatabase) {
			db.execSQL(TRACKS_TABLE_CREATE)
			db.execSQL("CREATE INDEX $TRACK_DATE_INDEX ON $TRACK_TABLE_NAME (\"$TRACK_COL_DATE\" DESC);")
		}

		override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
			db.execSQL(TRACKS_TABLE_DELETE)
			onCreate(db)
		}

		internal fun addLocationMessages(locationMessages: List<LocationMessage>) {
			locationMessages.forEach {
				writableDatabase?.execSQL(TRACKS_TABLE_INSERT,
					arrayOf(it.userId, it.chatId, it.lat, it.lon, it.altitude, it.speed, it.hdop, it.bearing, it.date, it.type, it.status, it.messageId))
			}
		}

		internal fun addLocationMessage(locationMessage: LocationMessage) {
				writableDatabase?.execSQL(TRACKS_TABLE_INSERT,
					arrayOf(locationMessage.userId, locationMessage.chatId, locationMessage.lat, locationMessage.lon, locationMessage.altitude, locationMessage.speed,
						locationMessage.hdop, locationMessage.bearing, locationMessage.date, locationMessage.type, locationMessage.status, locationMessage.messageId))
		}

		internal fun getLocationMessages(): List<LocationMessage> {
			val res = ArrayList<LocationMessage>()
			readableDatabase?.rawQuery(TRACKS_TABLE_SELECT, null)?.apply {
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
			val date = cursor.getLong(7)
			val bearing = cursor.getDouble(8)
			val textInfo = cursor.getInt(9)
			val status = cursor.getInt(10)
			val messageId = cursor.getLong(11)

			return LocationMessage(userId, chatId, lat, lon, altitude, speed, hdop, bearing, date, textInfo, status, messageId)
		}

		internal fun clearLocationMessages() {
			writableDatabase?.execSQL(TRACKS_TABLE_CLEAR)
		}

		companion object {

			private const val DATABASE_NAME = "location_messages"
			private const val DATABASE_VERSION = 2

			private const val TRACK_TABLE_NAME = "track"
			private const val TRACK_COL_USER_ID = "user_id"
			private const val TRACK_COL_CHAT_ID = "chat_id"
			private const val TRACK_COL_DATE = "date"
			private const val TRACK_COL_LAT = "lat"
			private const val TRACK_COL_LON = "lon"
			private const val TRACK_COL_ALTITUDE = "altitude"
			private const val TRACK_COL_SPEED = "speed"
			private const val TRACK_COL_HDOP = "hdop"
			private const val TRACK_COL_BEARING = "bearing"
			private const val TRACK_COL_TYPE = "type" // 0 = user map message, 1 = user text message, 2 = bot map message, 3 = bot text message
			private const val TRACK_COL_MESSAGE_STATUS = "status" // 0 = preparing , 1 = pending, 2 = sent, 3 = error
			private const val TRACK_COL_MESSAGE_ID = "message_id"

			private const val TRACK_DATE_INDEX = "date_index"

			private const val TRACKS_TABLE_INSERT =
				("INSERT INTO $TRACK_TABLE_NAME ($TRACK_COL_USER_ID, $TRACK_COL_CHAT_ID, $TRACK_COL_LAT, $TRACK_COL_LON, $TRACK_COL_ALTITUDE, $TRACK_COL_SPEED, $TRACK_COL_HDOP, $TRACK_COL_BEARING, $TRACK_COL_DATE, $TRACK_COL_TYPE, $TRACK_COL_MESSAGE_STATUS, $TRACK_COL_MESSAGE_ID) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")

			private const val TRACKS_TABLE_CREATE =
				("CREATE TABLE IF NOT EXISTS $TRACK_TABLE_NAME ($TRACK_COL_USER_ID long, $TRACK_COL_CHAT_ID long,$TRACK_COL_LAT double, $TRACK_COL_LON double, $TRACK_COL_ALTITUDE double, $TRACK_COL_SPEED float, $TRACK_COL_HDOP double, $TRACK_COL_BEARING double, $TRACK_COL_DATE long, $TRACK_COL_TYPE int, $TRACK_COL_MESSAGE_STATUS int, $TRACK_COL_MESSAGE_ID long )")

			private const val TRACKS_TABLE_SELECT =
				"SELECT $TRACK_COL_USER_ID, $TRACK_COL_CHAT_ID, $TRACK_COL_LAT, $TRACK_COL_LON, $TRACK_COL_ALTITUDE, $TRACK_COL_SPEED, $TRACK_COL_HDOP, $TRACK_COL_BEARING, $TRACK_COL_DATE, $TRACK_COL_TYPE, $TRACK_COL_MESSAGE_STATUS, $TRACK_COL_MESSAGE_ID FROM $TRACK_TABLE_NAME"

			private const val TRACKS_TABLE_CLEAR = "DELETE FROM $TRACK_TABLE_NAME"

			private const val TRACKS_TABLE_DELETE = "DROP TABLE IF EXISTS $TRACK_TABLE_NAME"
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
		var status: Int,
		var messageId: Long
	) {

		companion object {

			const val STATUS_PREPARED = 0
			const val STATUS_PENDING = 1
			const val STATUS_SENT = 2
			const val STATUS_ERROR = 3
			const val STATUS_INCOMING = 4

			const val TYPE_USER_MAP = 0
			const val TYPE_USER_TEXT = 1
			const val TYPE_BOT_MAP = 2
			const val TYPE_BOT_TEXT = 3
		}
	}
}