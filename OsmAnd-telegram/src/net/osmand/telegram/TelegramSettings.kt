package net.osmand.telegram

import android.content.Context
import net.osmand.telegram.utils.OsmandFormatter.MetricsConstants
import net.osmand.telegram.utils.OsmandFormatter.SpeedConstants

private const val SETTINGS_NAME = "osmand_telegram_settings"

private const val SHARE_LOCATION_CHATS_KEY = "share_location_chats"
private const val SHOW_ON_MAP_CHATS_KEY = "show_on_map_chats"

private const val METRICS_CONSTANTS_KEY = "metrics_constants"
private const val SPEED_CONSTANTS_KEY = "speed_constants"

private const val SEND_MY_LOCATION_INTERVAL_KEY = "send_my_location_interval"
private const val SEND_MY_LOCATION_INTERVAL_DEFAULT = 5L * 1000 // 5 seconds

private const val USER_LOCATION_EXPIRE_TIME_KEY = "user_location_expire_time"
private const val CHANGED_TO_CHAT_ID_KEY = "changed_to_chat_id"
private const val USER_LOCATION_EXPIRE_TIME_DEFAULT = 15L * 60 * 1000 // 15 minutes

class TelegramSettings(private val app: TelegramApplication) {

	private var shareLocationChats: Set<Long> = emptySet()
	private var showOnMapChats: Set<Long> = emptySet()

	var metricsConstants = MetricsConstants.KILOMETERS_AND_METERS
	var speedConstants = SpeedConstants.KILOMETERS_PER_HOUR

	var sendMyLocationInterval = SEND_MY_LOCATION_INTERVAL_DEFAULT
	var userLocationExpireTime = USER_LOCATION_EXPIRE_TIME_DEFAULT
	var afterMovingChatTitleToChatId = false

	init {
		read()
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
		edit.putBoolean(CHANGED_TO_CHAT_ID_KEY, afterMovingChatTitleToChatId)

		edit.putLong(SEND_MY_LOCATION_INTERVAL_KEY, sendMyLocationInterval)

		edit.apply()
	}

	fun read() {
		val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
		afterMovingChatTitleToChatId =
				prefs.getBoolean(CHANGED_TO_CHAT_ID_KEY, false)

		val shareLocationChats = mutableSetOf<Long>()
		val shareLocationChatsSet = prefs.getStringSet(SHARE_LOCATION_CHATS_KEY, mutableSetOf())
        
		val showOnMapChats = mutableSetOf<Long>()
		val showOnMapChatsSet = prefs.getStringSet(SHOW_ON_MAP_CHATS_KEY, mutableSetOf())

		if (!afterMovingChatTitleToChatId) {
			showOnMapChatsSet.clear()
			shareLocationChatsSet.clear()
			afterMovingChatTitleToChatId = true
		}
        
        for (chatId in shareLocationChatsSet) {
            shareLocationChats.add(chatId.toLong())
        }
        this.shareLocationChats = shareLocationChats

        for (chatId in showOnMapChatsSet) {
			showOnMapChats.add(chatId.toLong())
		}
		this.showOnMapChats = showOnMapChats

		metricsConstants = MetricsConstants.valueOf(
			prefs.getString(METRICS_CONSTANTS_KEY, MetricsConstants.KILOMETERS_AND_METERS.name)
		)
		speedConstants = SpeedConstants.valueOf(
			prefs.getString(SPEED_CONSTANTS_KEY, SpeedConstants.KILOMETERS_PER_HOUR.name)
		)

		sendMyLocationInterval =
				prefs.getLong(SEND_MY_LOCATION_INTERVAL_KEY, SEND_MY_LOCATION_INTERVAL_DEFAULT)
		userLocationExpireTime =
				prefs.getLong(USER_LOCATION_EXPIRE_TIME_KEY, USER_LOCATION_EXPIRE_TIME_DEFAULT)
	}
}
