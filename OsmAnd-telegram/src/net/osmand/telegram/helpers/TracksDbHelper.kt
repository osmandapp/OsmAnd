package net.osmand.telegram.helpers

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.AsyncTask
import net.osmand.PlatformUtil
import net.osmand.telegram.TelegramApplication
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

const val TRACKS_LOG_FILE_NAME = "tracks.txt"

class TracksDbHelper(val app: TelegramApplication) :
	SQLiteOpenHelper(app, DATABASE_NAME, null, DATABASE_VERSION) {

	private val tracks = HashSet<Track>()

	private val log = PlatformUtil.getLog(TracksDbHelper::class.java)

	init {
		app.telegramHelper.addIncomingMessagesListener(object :
			TelegramHelper.TelegramIncomingMessagesListener {

			override fun onReceiveChatLocationMessages(
				chatId: Long, vararg messages: TdApi.Message
			) {
				messages.forEach { addMessage(chatId, it) }
			}

			override fun onDeleteChatLocationMessages(chatId: Long, messages: List<TdApi.Message>) {

			}

			override fun updateLocationMessages() {}
		})
		app.telegramHelper.addOutgoingMessagesListener(object :
			TelegramHelper.TelegramOutgoingMessagesListener {
			override fun onUpdateMessages(messages: List<TdApi.Message>) {
				messages.forEach { addMessage(it.chatId, it) }
			}

			override fun onDeleteMessages(chatId: Long, messages: List<Long>) {
			}

			override fun onSendLiveLocationError(code: Int, message: String) {
			}
		})
	}

	override fun onCreate(db: SQLiteDatabase) {
		db.execSQL(TRACK_TABLE_CREATE)
	}

	override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
	}

	private fun addMessage(chatId: Long, message: TdApi.Message) {
		log.debug(message)
		val content = message.content
		val track = when (content) {
			is TdApi.MessageLocation ->
				Track(
					message.senderUserId,
					chatId,
					content.location.latitude,
					content.location.longitude,
					0.0,
					0.0,
					0.0,
					Math.max(message.date, message.editDate)
				)
			is TelegramHelper.MessageLocation ->
				Track(
					message.senderUserId,
					chatId,
					content.lat,
					content.lon,
					0.0,
					0.0,
					0.0,
					content.lastUpdated
				)
			else -> {
				null
			}
		}
		if (track != null) {
			synchronized(tracks) {
				tracks.add(track)
			}
			insertData(
				track.userId,
				track.chatId,
				track.lat,
				track.lon,
				track.altitude,
				track.speed,
				track.hdop,
				track.date
			)
		}
	}

	fun insertData(
		userId: Int,
		chatId: Long,
		lat: Double,
		lon: Double,
		alt: Double,
		speed: Double,
		hdop: Double,
		time: Int
	) {
		execWithClose(UPDATE_SCRIPT, arrayOf(userId, chatId, lat, lon, alt, speed, hdop, time))
	}

	@Synchronized
	private fun execWithClose(script: String, objects: Array<Any>) {
		val db = writableDatabase
		try {
			db?.execSQL(script, objects)
		} catch (e: RuntimeException) {
			log.error(e.message, e)
		} finally {
			db?.close()
		}
	}

	internal fun getTracks(): Set<Track> {
		val res = HashSet<Track>()
		readableDatabase?.rawQuery(TRACK_TABLE_SELECT, null)?.apply {
			if (moveToFirst()) {
				do {
					val userId = getInt(0)
					val chatId = getLong(1)
					val lat = getDouble(2)
					val lon = getDouble(3)
					val altitude = getDouble(4)
					val speed = getDouble(5)
					val hdop = getDouble(6)
					val date = getInt(7)

					val track = Track(userId, chatId, lat, lon, altitude, speed, hdop, date)
					res.add(track)
					log.debug(track)
				} while (moveToNext())
			}
			close()
		}
		return res
	}

	companion object {

		private const val DATABASE_NAME = "tracks"
		private const val DATABASE_VERSION = 1

		private const val TRACK_TABLE_NAME = "track"
		private const val TRACK_COL_USER_ID = "user_id"
		private const val TRACK_COL_CHAT_ID = "chat_id"
		private const val TRACK_COL_DATE = "date"
		private const val TRACK_COL_LAT = "lat"
		private const val TRACK_COL_LON = "lon"
		private const val TRACK_COL_ALTITUDE = "altitude"
		private const val TRACK_COL_SPEED = "speed"
		private const val TRACK_COL_HDOP = "hdop"

		private const val TRACK_TABLE_CREATE =
			"CREATE TABLE $TRACK_TABLE_NAME ($TRACK_COL_USER_ID long, $TRACK_COL_CHAT_ID long, $TRACK_COL_LAT double, $TRACK_COL_LON double, $TRACK_COL_ALTITUDE double, $TRACK_COL_SPEED double, $TRACK_COL_HDOP double, $TRACK_COL_DATE long )"

		private const val UPDATE_SCRIPT =
			"INSERT INTO $TRACK_TABLE_NAME ($TRACK_COL_USER_ID, $TRACK_COL_CHAT_ID, $TRACK_COL_LAT, $TRACK_COL_LON, $TRACK_COL_ALTITUDE, $TRACK_COL_SPEED, $TRACK_COL_HDOP, $TRACK_COL_DATE) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"

		private const val TRACK_TABLE_SELECT =
			"SELECT $TRACK_COL_USER_ID, $TRACK_COL_CHAT_ID, $TRACK_COL_LAT, $TRACK_COL_LON, $TRACK_COL_ALTITUDE, $TRACK_COL_SPEED, $TRACK_COL_HDOP, $TRACK_COL_DATE FROM $TRACK_TABLE_NAME"

		private const val TRACK_TABLE_CLEAR = "DELETE FROM $TRACK_TABLE_NAME"
	}

	data class Track(
		val userId: Int,
		val chatId: Long,
		val lat: Double,
		val lon: Double,
		val altitude: Double,
		val speed: Double,
		val hdop: Double,
		val date: Int
	)

	fun writeToFile() {
		var text = ""
		app.tracksDbHelper.getTracks().forEach {
			text += "\n--------------\n" + it.toString()
		}
		val path = app.getExternalFilesDir(null)
		WriteTracksLogs().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, text, path.absolutePath)
	}

	private class WriteTracksLogs : AsyncTask<String, String, Void?>() {

		private val log = PlatformUtil.getLog(WriteTracksLogs::class.java)

		override fun doInBackground(vararg params: String?): Void? {
			val text = params[0]
			val path = params[1]
			if (text != null && path != null) {
				val file = File(path, TRACKS_LOG_FILE_NAME)
				val stream = FileOutputStream(file)
				try {
					stream.write(text.toByteArray())
				} catch (e: IOException) {
					log.error(e)
				} finally {
					stream.close()
				}
			}
			return null
		}
	}
}