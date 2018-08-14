package net.osmand.telegram

import android.content.Context
import net.osmand.telegram.helpers.OsmandAidlHelper
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.utils.OsmandFormatter.MetricsConstants
import net.osmand.telegram.utils.OsmandFormatter.SpeedConstants

val SEND_MY_LOC_VALUES_SEC =
	listOf(1L, 2L, 3L, 5L, 10L, 15L, 30L, 60L, 90L, 2 * 60L, 3 * 60L, 5 * 60L)
val STALE_LOC_VALUES_SEC =
	listOf(1 * 60L, 2 * 60L, 5 * 60L, 10 * 60L, 15 * 60L, 30 * 60L, 60 * 60L)
val LOC_HISTORY_VALUES_SEC = listOf(
	5 * 60L,
	15 * 60L,
	30 * 60L,
	1 * 60 * 60L,
	2 * 60 * 60L,
	3 * 60 * 60L,
	5 * 60 * 60L,
	8 * 60 * 60L,
	12 * 60 * 60L,
	24 * 60 * 60L
)

private const val SEND_MY_LOC_DEFAULT_INDEX = 6
private const val STALE_LOC_DEFAULT_INDEX = 4
private const val LOC_HISTORY_DEFAULT_INDEX = 2

private const val SETTINGS_NAME = "osmand_telegram_settings"

private const val SHARE_LOCATION_CHATS_KEY = "share_location_chats"
private const val SHOW_ON_MAP_CHATS_KEY = "show_on_map_chats"

private const val METRICS_CONSTANTS_KEY = "metrics_constants"
private const val SPEED_CONSTANTS_KEY = "speed_constants"

private const val SEND_MY_LOC_INTERVAL_KEY = "send_my_loc_interval"
private const val STALE_LOC_TIME_KEY = "stale_loc_time"
private const val LOC_HISTORY_TIME_KEY = "loc_history_time"

private const val APP_TO_CONNECT_PACKAGE_KEY = "app_to_connect_package"

private const val DEFAULT_VISIBLE_TIME_SECONDS = 60 * 60L // 1 hour

private const val TITLES_REPLACED_WITH_IDS = "changed_to_chat_id"

class TelegramSettings(private val app: TelegramApplication) {

	private var chatLivePeriods = mutableMapOf<Long, Long>()
	var chatShareLocStartSec = mutableMapOf<Long, Long>()

	private var shareLocationChats: Set<Long> = emptySet()
	private var showOnMapChats: Set<Long> = emptySet()

	var metricsConstants = MetricsConstants.KILOMETERS_AND_METERS
	var speedConstants = SpeedConstants.KILOMETERS_PER_HOUR

	var sendMyLocInterval = SEND_MY_LOC_VALUES_SEC[SEND_MY_LOC_DEFAULT_INDEX]
	var staleLocTime = STALE_LOC_VALUES_SEC[STALE_LOC_DEFAULT_INDEX]
	var locHistoryTime = LOC_HISTORY_VALUES_SEC[LOC_HISTORY_DEFAULT_INDEX]

	var appToConnectPackage = OsmandAidlHelper.OSMAND_PLUS_PACKAGE_NAME

	init {
		updatePrefs()
		read()
	}

	fun hasAnyChatToShareLocation() = shareLocationChats.isNotEmpty()

	fun isSharingLocationToChat(chatId: Long) = shareLocationChats.contains(chatId)

	fun hasAnyChatToShowOnMap() = showOnMapChats.isNotEmpty()

	fun isShowingChatOnMap(chatId: Long) = showOnMapChats.contains(chatId)

	fun removeNonexistingChats(presentChatIds: List<Long>) {
		val shareLocationChats = shareLocationChats.toMutableList()
		shareLocationChats.intersect(presentChatIds)
		this.shareLocationChats = shareLocationChats.toHashSet()

		val showOnMapChats = showOnMapChats.toMutableList()
		showOnMapChats.intersect(presentChatIds)
		this.showOnMapChats = showOnMapChats.toHashSet()

		chatLivePeriods = chatLivePeriods.filter { (key, _) ->
			presentChatIds.contains(key)
		}.toMutableMap()

		chatShareLocStartSec = chatShareLocStartSec.filter { (key, _) ->
			presentChatIds.contains(key)
		}.toMutableMap()
	}

	fun shareLocationToChat(
		chatId: Long,
		share: Boolean,
		livePeriod: Long = DEFAULT_VISIBLE_TIME_SECONDS
	) {
		val shareLocationChats = shareLocationChats.toMutableList()
		if (share) {
			val lp: Long = when {
				livePeriod < TelegramHelper.MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC -> TelegramHelper.MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong()
				livePeriod > TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC -> TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong()
				else -> livePeriod
			}
			chatLivePeriods[chatId] = lp
			chatShareLocStartSec[chatId] = (System.currentTimeMillis()/1000)
			shareLocationChats.add(chatId)
		} else {
			shareLocationChats.remove(chatId)
			chatLivePeriods.remove(chatId)
			chatShareLocStartSec.remove(chatId)
		}
		this.shareLocationChats = shareLocationChats.toHashSet()
	}

	fun getChatLivePeriod(chatId: Long) = chatLivePeriods[chatId]

	fun getChatLivePeriods(): Map<Long, Long> {
		return chatLivePeriods.filter {
			getChatLiveMessageExpireTime(it.key) > 0
		}
	}

	fun getChatShareLocStartSec(chatId: Long) = chatShareLocStartSec[chatId]

	fun getChatLiveMessageExpireTime(chatId: Long): Long {
		val startTime = getChatShareLocStartSec(chatId)
		val livePeriod = getChatLivePeriod(chatId)
		return if (startTime != null && livePeriod != null) {
			livePeriod - ((System.currentTimeMillis() / 1000) - startTime)
		} else {
			0
		}
	}
	
	fun updateChatShareLocStartSec(chatId: Long, startTime: Long) {
		chatShareLocStartSec[chatId] = startTime
	} 
	
	fun stopSharingLocationToChats() {
		this.shareLocationChats = emptySet()
		this.chatLivePeriods.clear()
		this.chatShareLocStartSec.clear()
	}

	fun showChatOnMap(chatId: Long, show: Boolean) {
		val showOnMapChats = showOnMapChats.toMutableList()
		if (show) {
			showOnMapChats.add(chatId)
		} else {
			showOnMapChats.remove(chatId)
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

		edit.putLong(SEND_MY_LOC_INTERVAL_KEY, sendMyLocInterval)
		edit.putLong(STALE_LOC_TIME_KEY, staleLocTime)
		edit.putLong(LOC_HISTORY_TIME_KEY, locHistoryTime)

		edit.putString(APP_TO_CONNECT_PACKAGE_KEY, appToConnectPackage)

		edit.apply()
	}

	fun read() {
		val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)

		val shareLocationChats = mutableSetOf<Long>()
		val shareLocationChatsSet = prefs.getStringSet(SHARE_LOCATION_CHATS_KEY, mutableSetOf())
		for (chatId in shareLocationChatsSet) {
			shareLocationChats.add(chatId.toLong())
		}
		this.shareLocationChats = shareLocationChats

		val showOnMapChats = mutableSetOf<Long>()
		val showOnMapChatsSet = prefs.getStringSet(SHOW_ON_MAP_CHATS_KEY, mutableSetOf())
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

		val sendMyLocDef = SEND_MY_LOC_VALUES_SEC[SEND_MY_LOC_DEFAULT_INDEX]
		sendMyLocInterval = prefs.getLong(SEND_MY_LOC_INTERVAL_KEY, sendMyLocDef)
		val staleLocDef = STALE_LOC_VALUES_SEC[STALE_LOC_DEFAULT_INDEX]
		staleLocTime = prefs.getLong(STALE_LOC_TIME_KEY, staleLocDef)
		val locHistoryDef = LOC_HISTORY_VALUES_SEC[LOC_HISTORY_DEFAULT_INDEX]
		locHistoryTime = prefs.getLong(LOC_HISTORY_TIME_KEY, locHistoryDef)

		appToConnectPackage = prefs.getString(
			APP_TO_CONNECT_PACKAGE_KEY, OsmandAidlHelper.OSMAND_PLUS_PACKAGE_NAME
		)
	}

	private fun updatePrefs() {
		val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
		val idsInUse = prefs.getBoolean(TITLES_REPLACED_WITH_IDS, false)
		if (!idsInUse) {
			val edit = prefs.edit()

			edit.putStringSet(SHARE_LOCATION_CHATS_KEY, emptySet())
			edit.putStringSet(SHOW_ON_MAP_CHATS_KEY, emptySet())
			edit.putBoolean(TITLES_REPLACED_WITH_IDS, true)

			edit.apply()
		}
	}
}
