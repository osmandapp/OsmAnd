package net.osmand.telegram

import android.content.Context
import net.osmand.telegram.utils.OsmandFormatter.MetricsConstants
import net.osmand.telegram.utils.OsmandFormatter.SpeedConstants

class TelegramSettings(private val app: TelegramApplication) {

    companion object {

        private const val SETTINGS_NAME = "osmand_telegram_settings"

        private const val SHARE_LOCATION_CHATS_KEY = "share_location_chats_key"
        private const val SHOW_ON_MAP_CHATS_KEY = "show_on_map_chats_key"

        private const val METRICS_CONSTANTS_KEY = "metrics_constants_key"
        private const val SPEED_CONSTANTS_KEY = "speed_constants_key"

        private const val SHOW_NOTIFICATION_ALWAYS_KEY = "show_notification_always_key"
    }

    private var shareLocationChats: Set<Long> = emptySet()
    private var showOnMapChats: Set<Long> = emptySet()

    var metricsConstants = MetricsConstants.KILOMETERS_AND_METERS
    var speedConstants = SpeedConstants.KILOMETERS_PER_HOUR

    var showNotificationAlways = true

    init {
        read()
    }

    fun hasAnyChatToShareLocation(): Boolean {
        return shareLocationChats.isNotEmpty()
    }

    fun isSharingLocationToChat(chatId: Long): Boolean {
        return shareLocationChats.contains(chatId)
    }

    fun shareLocationToChat(chatId: Long, share: Boolean) {
        val shareLocationChats = shareLocationChats.toMutableList()
        if (share) {
            shareLocationChats.add(chatId)
        } else {
            shareLocationChats.remove(chatId)
        }
        this.shareLocationChats = shareLocationChats.toHashSet()
    }

    fun getShareLocationChats() = ArrayList(shareLocationChats)

    fun save() {
        val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
        val edit = prefs.edit()

        val shareLocationChatsSet = mutableSetOf<String>()
        val shareLocationChats = ArrayList(shareLocationChats)
        for (chatId in shareLocationChats) {
            shareLocationChatsSet.add(chatId.toString())
        }
        edit.putStringSet(SHARE_LOCATION_CHATS_KEY, shareLocationChatsSet)

        val showOnMapChatsSet = mutableSetOf<String>()
        val showOnMapChats = ArrayList(showOnMapChats)
        for (chatId in showOnMapChats) {
            showOnMapChatsSet.add(chatId.toString())
        }
        edit.putStringSet(SHOW_ON_MAP_CHATS_KEY, showOnMapChatsSet)

        edit.putString(METRICS_CONSTANTS_KEY, metricsConstants.name)
        edit.putString(SPEED_CONSTANTS_KEY, speedConstants.name)

        edit.putBoolean(SHOW_NOTIFICATION_ALWAYS_KEY, showNotificationAlways)

        edit.apply()
    }

    fun read() {
        val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)

        val shareLocationChats = mutableSetOf<Long>()
        val shareLocationChatsSet = prefs.getStringSet(SHARE_LOCATION_CHATS_KEY, mutableSetOf())
        for (chatIdStr in shareLocationChatsSet) {
            val chatId = chatIdStr.toLongOrNull()
            if (chatId != null) {
                shareLocationChats.add(chatId)
            }
        }
        this.shareLocationChats = shareLocationChats

        val showOnMapChats = mutableSetOf<Long>()
        val showOnMapChatsSet = prefs.getStringSet(SHOW_ON_MAP_CHATS_KEY, mutableSetOf())
        for (chatIdStr in showOnMapChatsSet) {
            val chatId = chatIdStr.toLongOrNull()
            if (chatId != null) {
                showOnMapChats.add(chatId)
            }
        }
        this.showOnMapChats = showOnMapChats

        metricsConstants = MetricsConstants.valueOf(prefs.getString(METRICS_CONSTANTS_KEY, MetricsConstants.KILOMETERS_AND_METERS.name))
        speedConstants = SpeedConstants.valueOf(prefs.getString(SPEED_CONSTANTS_KEY, SpeedConstants.KILOMETERS_PER_HOUR.name))

        showNotificationAlways = prefs.getBoolean(SHOW_NOTIFICATION_ALWAYS_KEY, true)
    }
}
