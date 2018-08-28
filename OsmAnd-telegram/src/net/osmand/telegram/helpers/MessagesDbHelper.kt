package net.osmand.telegram.helpers

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.osmand.telegram.TelegramApplication
import org.drinkless.td.libcore.telegram.TdApi

class MessagesDbHelper(val app: TelegramApplication) {

	private val messages = HashSet<Message>()

	private val sqliteHelper: SQLiteHelper

	init {
		sqliteHelper = SQLiteHelper(app)
		sqliteHelper.getMessages().forEach {
			app.telegramHelper.loadMessage(it.chatId, it.messageId)
		}
		app.telegramHelper.addIncomingMessagesListener(object :
			TelegramHelper.TelegramIncomingMessagesListener {

			override fun onReceiveChatLocationMessages(
				chatId: Long, vararg messages: TdApi.Message
			) {
				messages.forEach { addMessage(chatId, it.id) }
			}

			override fun onDeleteChatLocationMessages(chatId: Long, messages: List<TdApi.Message>) {
				messages.forEach { removeMessage(chatId, it.id) }
			}

			override fun updateLocationMessages() {}
		})
	}

	fun saveMessages() {
		clearMessages()
		synchronized(messages) {
			sqliteHelper.addMessages(messages)
		}
	}
	
	fun clearMessages() {
		sqliteHelper.clearMessages()
	}

	private fun addMessage(chatId: Long, messageId: Long) {
		synchronized(messages) {
			messages.add(Message(chatId, messageId))
		}
	}

	private fun removeMessage(chatId: Long, messageId: Long) {
		synchronized(messages) {
			messages.remove(Message(chatId, messageId))
		}
	}

	private class SQLiteHelper(context: Context) :
		SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

		override fun onCreate(db: SQLiteDatabase) {
			db.execSQL(MESSAGES_TABLE_CREATE)
		}

		override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
			db.execSQL(MESSAGES_TABLE_DELETE)
			onCreate(db)
		}

		internal fun addMessages(messages: Set<Message>) {
			messages.forEach {
				writableDatabase?.execSQL(MESSAGES_TABLE_INSERT, arrayOf(it.chatId, it.messageId))
			}
		}

		internal fun getMessages(): Set<Message> {
			val res = HashSet<Message>()
			readableDatabase?.rawQuery(MESSAGES_TABLE_SELECT, null)?.apply {
				if (moveToFirst()) {
					do {
						res.add(Message(getLong(0), getLong(1)))
					} while (moveToNext())
				}
				close()
			}
			return res
		}

		internal fun clearMessages() {
			writableDatabase?.execSQL(MESSAGES_TABLE_CLEAR)
		}

		companion object {

			private const val DB_NAME = "messages.db"
			private const val DB_VERSION = 1

			private const val MESSAGES_TABLE_NAME = "messages"
			private const val MESSAGES_COL_CHAT_ID = "chat_id"
			private const val MESSAGES_COL_MESSAGE_ID = "message_id"

			private const val MESSAGES_TABLE_CREATE =
				"CREATE TABLE IF NOT EXISTS $MESSAGES_TABLE_NAME (" +
						"$MESSAGES_COL_CHAT_ID LONG, " +
						"$MESSAGES_COL_MESSAGE_ID LONG)"

			private const val MESSAGES_TABLE_DELETE = "DROP TABLE IF EXISTS $MESSAGES_TABLE_NAME"

			private const val MESSAGES_TABLE_SELECT =
				"SELECT $MESSAGES_COL_CHAT_ID, $MESSAGES_COL_MESSAGE_ID FROM $MESSAGES_TABLE_NAME"

			private const val MESSAGES_TABLE_CLEAR = "DELETE FROM $MESSAGES_TABLE_NAME"

			private const val MESSAGES_TABLE_INSERT = "INSERT INTO $MESSAGES_TABLE_NAME (" +
					"$MESSAGES_COL_CHAT_ID,  $MESSAGES_COL_MESSAGE_ID) VALUES (?, ?)"
		}
	}

	private data class Message(val chatId: Long, val messageId: Long)
}
