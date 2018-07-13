package net.osmand.telegram.helpers

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import net.osmand.telegram.TelegramApplication
import java.util.*


class TelegramLocalDataHelper internal constructor(app: TelegramApplication) {

    private val dbHelper: TelegramLocalDataDbHelper

    private var savedChats: Set<ChatProps> = emptySet()
    private var shareLocationChats: Set<Long> = emptySet()
    private var showOnMapChats: Set<Long> = emptySet()

    init {
        dbHelper = TelegramLocalDataDbHelper(app)
        refreshCachedData()
    }

    fun refreshCachedData() {
        savedChats = dbHelper.readSavedChatProps()
        shareLocationChats = dbHelper.readSharedChatProps()
        showOnMapChats = dbHelper.readShownOnMapChatProps()
    }

    fun clearAll() {
        this.savedChats = emptySet()
        this.shareLocationChats = emptySet()
        this.showOnMapChats = emptySet()
        dbHelper.clearAllChatProps()
    }

    fun hasAnyChatToShareLocation() = shareLocationChats.isNotEmpty()

    fun isSharingLocationToChat(id: Long) = shareLocationChats.contains(id)

    fun hasAnyChatToShowOnMap() = showOnMapChats.isNotEmpty()

    fun isShowingChatOnMap(id: Long) = showOnMapChats.contains(id)

    fun removeNonexistingChats(presentChatIds: List<Long>) {
        val shareLocationChats = shareLocationChats.toMutableList()
        shareLocationChats.intersect(presentChatIds)
        this.shareLocationChats = shareLocationChats.toHashSet()

        val showOnMapChats = showOnMapChats.toMutableList()
        showOnMapChats.intersect(presentChatIds)
        this.showOnMapChats = showOnMapChats.toHashSet()
    }

    fun shareLocationToChat(id: Long, share: Boolean) {
        val shareLocationChats = shareLocationChats.toMutableList()
        if (share) {
            shareLocationChats.add(id)
        } else {
            shareLocationChats.remove(id)
        }
        this.shareLocationChats = shareLocationChats.toHashSet()
    }

    fun shareLocationToChats(ids: List<Long>, share: Boolean) {
        val shareLocationChats = shareLocationChats.toMutableList()
        if (share) {
            shareLocationChats.addAll(ids)
        } else {
            shareLocationChats.removeAll(ids)
        }
        this.shareLocationChats = shareLocationChats.toHashSet()
    }

    fun stopSharingLocationToChats() {
        this.shareLocationChats = emptySet()
    }

    fun showChatOnMap(id: Long, show: Boolean) {
        val showOnMapChats = showOnMapChats.toMutableList()
        if (show) {
            showOnMapChats.add(id)
        } else {
            showOnMapChats.remove(id)
        }
        this.showOnMapChats = showOnMapChats.toHashSet()
    }

    fun getShareLocationChats() = ArrayList(shareLocationChats)

    fun getShowOnMapChats() = ArrayList(showOnMapChats)

    fun getShowOnMapChatsCount() = showOnMapChats.size

    fun getSavedChatProps(): List<ChatProps> {
        return ArrayList<ChatProps>(savedChats)
    }

    fun addOrUpdateChatProps(id: Long, expireTime: Long, share: Boolean = false, show: Boolean = false) {
        val savedChats = savedChats.toMutableList()
        var chatProps = getChatProps(id)
        shareLocationToChat(id, share)
        showChatOnMap(id, show)
        if (chatProps != null) {
            chatProps.expireTime = expireTime
            chatProps.sharing = share
            chatProps.shownOnMap = show
            dbHelper.updateSavedChatProps(chatProps)
        } else {
            chatProps = ChatProps(id, expireTime, share, show)
            savedChats.add(chatProps)
            dbHelper.addSavedChatProps(chatProps)
        }
        this.savedChats = savedChats.toHashSet()
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

        internal fun readSavedChatProps(): Set<ChatProps> {
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
            return res.toSet()
        }

        internal fun readSharedChatProps(): Set<Long> {
            val res = ArrayList<Long>()
            writableDatabase?.use { conn ->
                val query = "SELECT  $CHAT_ID FROM $TABLE_NAME WHERE $CHAT_LOCATION_SHARING = ?"
                val cursor = conn.rawQuery(query, arrayOf("1"))
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            val chatId = cursor.getInt(cursor.getColumnIndex(CHAT_ID))
                            res.add(chatId.toLong())
                        } while (cursor.moveToNext())
                    }
                    cursor.close()
                }
            }
            return res.toSet()
        }

        internal fun readShownOnMapChatProps(): Set<Long> {
            val res = ArrayList<Long>()
            writableDatabase?.use { conn ->
                val query = "SELECT  $CHAT_ID FROM $TABLE_NAME WHERE $CHAT_SHOW_ON_MAP = ?"
                val cursor = conn.rawQuery(query, arrayOf("1"))
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        do {
                            val chatId = cursor.getInt(cursor.getColumnIndex(CHAT_ID))
                            res.add(chatId.toLong())
                        } while (cursor.moveToNext())
                    }
                    cursor.close()
                }
            }
            return res.toSet()
        }

        internal fun addSavedChatProps(chatProps: ChatProps) {
            writableDatabase?.use { conn ->
                val query = "INSERT INTO $TABLE_NAME " +
                        "($CHAT_ID, $CHAT_EXPIRE_TIME, $CHAT_LOCATION_SHARING, $CHAT_SHOW_ON_MAP)" +
                        " VALUES (?, ?, ?, ?)"
                conn.execSQL(query, arrayOf<Any>(chatProps.id, chatProps.expireTime, chatProps.sharing, chatProps.shownOnMap))
            }
        }

        internal fun updateSavedChatProps(chatProps: ChatProps) {
            writableDatabase?.use { conn ->
                val query = "UPDATE $TABLE_NAME SET" +
                        " $CHAT_EXPIRE_TIME = '${chatProps.expireTime}'," +
                        " $CHAT_LOCATION_SHARING = '${chatProps.sharing}'," +
                        " $CHAT_SHOW_ON_MAP = '${chatProps.shownOnMap}" +
                        "' WHERE $CHAT_ID = ${chatProps.id}"
                conn.execSQL(query, arrayOf<Any>())
            }
        }

        internal fun removeSavedChatProps(chatProps: ChatProps) {
            writableDatabase?.use { conn ->
                conn.execSQL("DELETE FROM $TABLE_NAME WHERE $CHAT_ID = ?",
                        arrayOf<Any>(chatProps.id))
            }
        }

        private fun readChatProps(cursor: Cursor): ChatProps {
            val id = cursor.getInt(cursor.getColumnIndex(CHAT_ID)).toLong()
            val expireTime = cursor.getInt(cursor.getColumnIndex(CHAT_EXPIRE_TIME)).toLong()
            val sharing = cursor.getInt(cursor.getColumnIndex(CHAT_SHOW_ON_MAP)) == 1
            val shownOnMap = cursor.getInt(cursor.getColumnIndex(CHAT_LOCATION_SHARING)) == 1

            return ChatProps(id, expireTime, sharing, shownOnMap)
        }

        companion object {

            private const val DB_VERSION = 1
            private const val DB_NAME = "TelegramChatsDb"
            private const val TABLE_NAME = "ChatsProperties"
            private const val CHAT_ID = "chat_id"
            private const val CHAT_EXPIRE_TIME = "chat_expire_time"
            private const val CHAT_SHOW_ON_MAP = "chat_show_on_map"
            private const val CHAT_LOCATION_SHARING = "chat_location_sharing"
            private const val CREATE_TABLE = "CREATE TABLE $TABLE_NAME (" +
                    "$CHAT_ID INTEGER PRIMARY KEY," +
                    "$CHAT_EXPIRE_TIME INTEGER," +
                    "$CHAT_SHOW_ON_MAP INTEGER NOT NULL DEFAULT 0," +
                    "$CHAT_LOCATION_SHARING INTEGER NOT NULL DEFAULT 0);"
            private const val DROP_TABLE = "DROP TABLE IF EXISTS $TABLE_NAME"
        }
    }
}

class ChatProps(val id: Long, var expireTime: Long, var sharing: Boolean, var shownOnMap: Boolean) 