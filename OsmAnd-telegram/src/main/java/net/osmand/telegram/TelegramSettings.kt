package net.osmand.telegram

import android.content.Context
import net.osmand.telegram.utils.OsmandFormatter.MetricsConstants
import net.osmand.telegram.utils.OsmandFormatter.SpeedConstants

class TelegramSettings(private val app: TelegramApplication) {

    companion object {

        private const val SETTINGS_NAME = "osmand_telegram_settings"

        private const val SHARE_LOCATION_CHATS_KEY = "share_location_chats"
        private const val SHOW_ON_MAP_CHATS_KEY = "show_on_map_chats"

        private const val METRICS_CONSTANTS_KEY = "metrics_constants"
        private const val SPEED_CONSTANTS_KEY = "speed_constants"

        private const val SHOW_NOTIFICATION_ALWAYS_KEY = "show_notification_always"
    }

    private var shareLocationChats: Set<String> = emptySet()
    private var showOnMapChats: Set<String> = emptySet()

    var metricsConstants = MetricsConstants.KILOMETERS_AND_METERS
    var speedConstants = SpeedConstants.KILOMETERS_PER_HOUR

    var showNotificationAlways = true

    init {
        read()
    }

    fun hasAnyChatToShareLocation(): Boolean {
        return shareLocationChats.isNotEmpty()
    }

    fun isSharingLocationToChat(chatTitle: String): Boolean {
        return shareLocationChats.contains(chatTitle)
    }

    fun removeNonexistingChats(presentChatTitles: List<String>) {
        val shareLocationChats = shareLocationChats.toMutableList()
        shareLocationChats.intersect(presentChatTitles)
        this.shareLocationChats = shareLocationChats.toHashSet()
    }

    fun shareLocationToChat(chatTitle: String, share: Boolean) {
        val shareLocationChats = shareLocationChats.toMutableList()
        if (share) {
            shareLocationChats.add(chatTitle)
        } else {
            shareLocationChats.remove(chatTitle)
        }
        this.shareLocationChats = shareLocationChats.toHashSet()
    }

    fun getShareLocationChats() = ArrayList(shareLocationChats)

    fun save() {
        val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
        val edit = prefs.edit()

        val shareLocationChatsSet = mutableSetOf<String>()
        val shareLocationChats = ArrayList(shareLocationChats)
        for (chatTitle in shareLocationChats) {
            shareLocationChatsSet.add(chatTitle)
        }
        edit.putStringSet(SHARE_LOCATION_CHATS_KEY, shareLocationChatsSet)

        val showOnMapChatsSet = mutableSetOf<String>()
        val showOnMapChats = ArrayList(showOnMapChats)
        for (chatTitle in showOnMapChats) {
            showOnMapChatsSet.add(chatTitle)
        }
        edit.putStringSet(SHOW_ON_MAP_CHATS_KEY, showOnMapChatsSet)

        edit.putString(METRICS_CONSTANTS_KEY, metricsConstants.name)
        edit.putString(SPEED_CONSTANTS_KEY, speedConstants.name)

        edit.putBoolean(SHOW_NOTIFICATION_ALWAYS_KEY, showNotificationAlways)

        edit.apply()
    }

    fun read() {
        val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)

        val shareLocationChats = mutableSetOf<String>()
        val shareLocationChatsSet = prefs.getStringSet(SHARE_LOCATION_CHATS_KEY, mutableSetOf())
        for (chatTitle in shareLocationChatsSet) {
            shareLocationChats.add(chatTitle)
        }
        this.shareLocationChats = shareLocationChats

        val showOnMapChats = mutableSetOf<String>()
        val showOnMapChatsSet = prefs.getStringSet(SHOW_ON_MAP_CHATS_KEY, mutableSetOf())
        for (chatTitle in showOnMapChatsSet) {
            showOnMapChats.add(chatTitle)
        }
        this.showOnMapChats = showOnMapChats

        metricsConstants = MetricsConstants.valueOf(prefs.getString(METRICS_CONSTANTS_KEY, MetricsConstants.KILOMETERS_AND_METERS.name))
        speedConstants = SpeedConstants.valueOf(prefs.getString(SPEED_CONSTANTS_KEY, SpeedConstants.KILOMETERS_PER_HOUR.name))

        showNotificationAlways = prefs.getBoolean(SHOW_NOTIFICATION_ALWAYS_KEY, true)
    }
}
