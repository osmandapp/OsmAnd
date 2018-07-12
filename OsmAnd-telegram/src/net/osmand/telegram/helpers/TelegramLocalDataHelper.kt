package net.osmand.telegram.helpers

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.osmand.telegram.TelegramApplication
import java.util.*


class TelegramLocalDataHelper internal constructor(app: TelegramApplication) {

    private val dbHelper: TelegramLocalDataDbHelper

    private var savedChats: MutableList<ChatProps> = ArrayList<ChatProps>()

    init {
        dbHelper = TelegramLocalDataDbHelper(app)
    }

    internal fun refreshCachedData() {
        savedChats = dbHelper.readSavedChatProps()
    }

    fun clearAll() {
        savedChats.clear()
        dbHelper.clearAllChatProps()
    }

    fun getSavedChatProps(): List<ChatProps> {
        return ArrayList<ChatProps>(savedChats)
    }

    fun addOrUpdateChatProps(id: Long, expireTime: Long) {
        var chatProps = getChatProps(id)
        if (chatProps != null) {
            chatProps.expireTime = expireTime
            dbHelper.updateSavedChatProps(chatProps)
        } else {
            chatProps = ChatProps(id, expireTime)
            savedChats.add(chatProps)
            dbHelper.addSavedChatProps(chatProps)
        }
    }

    fun removeChatPropsFromSaved(id: Long) {
        val savedChatProps = getChatProps(id)
        if (savedChatProps != null) {
            savedChats.remove(savedChatProps)
            dbHelper.removeSavedChatProps(savedChatProps)
        }
    }

    fun isChatPropsSaved(id: Long): Boolean {
        return getChatProps(id) != null
    }

    private fun getChatProps(id: Long): ChatProps? {
        for (chatProps in savedChats) {
            if (chatProps.id == id) {
                return chatProps
            }
        }
        return null
    }

    fun getChatExpireTime(id: Long): Long? {
        for (chatProps in savedChats) {
            if (chatProps.id == id) {
                return chatProps.expireTime
            }
        }
        return null
    }

    private class TelegramLocalDataDbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_TABLE)
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            db.execSQL(DROP_TABLE)
            onCreate(db)
        }

        internal fun clearAllChatProps() {
            writableDatabase?.use { conn ->
                conn.execSQL("DELETE FROM $TABLE_NAME")
            }
        }

        internal fun readSavedChatProps(): MutableList<ChatProps> {
            val res = ArrayList<ChatProps>()
            writableDatabase?.use { conn ->
                val query = "SELECT  * FROM $TABLE_NAME"
                val cursor = conn.rawQuery(query, null)
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            res.add(readChatProps(cursor))
                        } while (cursor.moveToNext())
                    }
                    cursor.close()
                }
            }
            return res
        }

        internal fun addSavedChatProps(chatProps: ChatProps) {
            writableDatabase?.use { conn ->
                val query = "INSERT INTO $TABLE_NAME ($CHAT_ID, $CHAT_EXPIRE_TIME) VALUES (?, ?)"
                conn.execSQL(query, arrayOf<Any>(chatProps.id, chatProps.expireTime))
            }
        }

        internal fun updateSavedChatProps(chatProps: ChatProps) {
            writableDatabase?.use { conn ->
                val query = "UPDATE $TABLE_NAME SET $CHAT_EXPIRE_TIME = '${chatProps.expireTime}' WHERE $CHAT_ID = ${chatProps.id}"
                conn.execSQL(query, arrayOf<Any>())
            }
        }

        internal fun removeSavedChatProps(chatProps: ChatProps) {
            writableDatabase?.use { conn ->
                conn.execSQL("DELETE FROM $TABLE_NAME WHERE $CHAT_ID = ? AND $CHAT_EXPIRE_TIME = ?",
                        arrayOf<Any>(chatProps.id, chatProps.expireTime))
            }
        }

        private fun readChatProps(cursor: Cursor): ChatProps {
            val id = cursor.getInt(cursor.getColumnIndex(CHAT_ID)).toLong()
            val expireTime = cursor.getString(cursor.getColumnIndex(CHAT_EXPIRE_TIME)).toLong()

            return ChatProps(id, expireTime)
        }

        companion object {

            private const val DB_VERSION = 1
            private const val DB_NAME = "TelegramChatsDb"
            private const val TABLE_NAME = "Chats"
            private const val CHAT_ID = "chat_id"
            private const val CHAT_EXPIRE_TIME = "chat_expire_time"
            private const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME ($CHAT_ID INTEGER PRIMARY KEY,$CHAT_EXPIRE_TIME INTEGER);"
            private const val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
        }
    }
}