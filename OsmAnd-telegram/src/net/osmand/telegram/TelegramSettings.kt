package net.osmand.telegram

import android.content.Context
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import net.osmand.telegram.helpers.OsmandAidlHelper
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.telegram.utils.OsmandFormatter.MetricsConstants
import net.osmand.telegram.utils.OsmandFormatter.SpeedConstants

private val SEND_MY_LOC_VALUES_SEC =
	listOf(1L, 2L, 3L, 5L, 10L, 15L, 30L, 60L, 90L, 2 * 60L, 3 * 60L, 5 * 60L)
private val STALE_LOC_VALUES_SEC =
	listOf(1 * 60L, 2 * 60L, 5 * 60L, 10 * 60L, 15 * 60L, 30 * 60L, 60 * 60L)
private val LOC_HISTORY_VALUES_SEC = listOf(
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
private val MESSAGE_ADD_ACTIVE_TIME_VALUES_SEC = listOf(15 * 60L, 30 * 60L, 60 * 60L, 180 * 60L)

private const val SEND_MY_LOC_DEFAULT_INDEX = 6
private const val STALE_LOC_DEFAULT_INDEX = 4
private const val LOC_HISTORY_DEFAULT_INDEX = 2

private const val SETTINGS_NAME = "osmand_telegram_settings"

private const val SHARE_LOCATION_CHATS_KEY = "share_location_chats"
private const val HIDDEN_ON_MAP_CHATS_KEY = "hidden_on_map_chats"

private const val SHARING_MODE_KEY = "current_sharing_mode"

private const val METRICS_CONSTANTS_KEY = "metrics_constants"
private const val SPEED_CONSTANTS_KEY = "speed_constants"

private const val SEND_MY_LOC_INTERVAL_KEY = "send_my_loc_interval"
private const val STALE_LOC_TIME_KEY = "stale_loc_time"
private const val LOC_HISTORY_TIME_KEY = "loc_history_time"

private const val APP_TO_CONNECT_PACKAGE_KEY = "app_to_connect_package"

private const val DEFAULT_VISIBLE_TIME_SECONDS = 60 * 60L // 1 hour

private const val TITLES_REPLACED_WITH_IDS = "changed_to_chat_id"

private const val LIVE_NOW_SORT_TYPE_KEY = "live_now_sort_type"

class TelegramSettings(private val app: TelegramApplication) {

	private var chatLivePeriods = mutableMapOf<Long, Long>()
	private var chatShareLocStartSec = mutableMapOf<Long, Long>()
	
	private var chatShareAddActiveTime = mutableMapOf<Long, Long>()

	private var shareLocationChats: Set<Long> = emptySet()
	private var hiddenOnMapChats: Set<Long> = emptySet()

	var shareDevicesIds = mutableMapOf<String, String>()
	var currentSharingMode = ""

	var metricsConstants = MetricsConstants.KILOMETERS_AND_METERS
	var speedConstants = SpeedConstants.KILOMETERS_PER_HOUR

	var sendMyLocInterval = SEND_MY_LOC_VALUES_SEC[SEND_MY_LOC_DEFAULT_INDEX]
	var staleLocTime = STALE_LOC_VALUES_SEC[STALE_LOC_DEFAULT_INDEX]
	var locHistoryTime = LOC_HISTORY_VALUES_SEC[LOC_HISTORY_DEFAULT_INDEX]

	var appToConnectPackage = ""
		private set

	var liveNowSortType = LiveNowSortType.SORT_BY_GROUP

	val gpsAndLocPrefs = listOf(SendMyLocPref(), StaleLocPref(), LocHistoryPref())

	init {
		updatePrefs()
		read()
	}

	fun hasAnyChatToShareLocation() = shareLocationChats.isNotEmpty()

	fun isSharingLocationToChat(chatId: Long) = shareLocationChats.contains(chatId)

	fun hasAnyChatToShowOnMap() = !hiddenOnMapChats.containsAll(getLiveNowChats())

	fun isShowingChatOnMap(chatId: Long) = !hiddenOnMapChats.contains(chatId)

	fun removeNonexistingChats(presentChatIds: List<Long>) {
		val shareLocationChats = shareLocationChats.toMutableList()
		shareLocationChats.intersect(presentChatIds)
		this.shareLocationChats = shareLocationChats.toHashSet()

		val hiddenChats = hiddenOnMapChats.toMutableList()
		hiddenChats.intersect(presentChatIds)
		hiddenOnMapChats = hiddenChats.toHashSet()

		chatLivePeriods = chatLivePeriods.filter { (key, _) ->
			presentChatIds.contains(key)
		}.toMutableMap()

		chatShareAddActiveTime = chatShareAddActiveTime.filter { (key, _) ->
			presentChatIds.contains(key)
		}.toMutableMap()

		chatShareLocStartSec = chatShareLocStartSec.filter { (key, _) ->
			presentChatIds.contains(key)
		}.toMutableMap()
	}

	fun shareLocationToChat(
		chatId: Long,
		share: Boolean,
		livePeriod: Long = DEFAULT_VISIBLE_TIME_SECONDS,
		addActiveTime: Long = MESSAGE_ADD_ACTIVE_TIME_VALUES_SEC[0]
	) {
		val shareLocationChats = shareLocationChats.toMutableList()
		if (share) {
			val lp: Long = when {
				livePeriod < TelegramHelper.MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC -> TelegramHelper.MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong()
				livePeriod > TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC -> TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong()
				else -> livePeriod
			}
			chatLivePeriods[chatId] = lp
			chatShareLocStartSec[chatId] = (System.currentTimeMillis() / 1000)
			chatShareAddActiveTime[chatId] = addActiveTime
			shareLocationChats.add(chatId)
		} else {
			shareLocationChats.remove(chatId)
			chatLivePeriods.remove(chatId)
			chatShareLocStartSec.remove(chatId)
			chatShareAddActiveTime.remove(chatId)
		}
		this.shareLocationChats = shareLocationChats.toHashSet()
	}

	fun updateShareDevicesIds(list: List<DeviceBot>) {
		shareDevicesIds.clear()
		list.forEach {
			shareDevicesIds[it.externalId] = it.deviceName
		}
	}
	
	fun getChatLivePeriod(chatId: Long) = chatLivePeriods[chatId]

	fun getChatAddActiveTime(chatId: Long) = chatShareAddActiveTime[chatId] ?: MESSAGE_ADD_ACTIVE_TIME_VALUES_SEC[0]

	fun getChatNextAddActiveTime(chatId: Long): Long {
		return if (chatShareAddActiveTime.containsKey(chatId)) {
			var index = MESSAGE_ADD_ACTIVE_TIME_VALUES_SEC.indexOf(chatShareAddActiveTime[chatId])
			if (MESSAGE_ADD_ACTIVE_TIME_VALUES_SEC.lastIndex > index) {
				MESSAGE_ADD_ACTIVE_TIME_VALUES_SEC[++index]
			} else {
				MESSAGE_ADD_ACTIVE_TIME_VALUES_SEC[index]
			}
		} else {
			MESSAGE_ADD_ACTIVE_TIME_VALUES_SEC[0]
		}
	}

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

	fun updateChatAddActiveTime(chatId: Long, newTime: Long) {
		chatShareAddActiveTime[chatId] = newTime
	}
	
	fun stopSharingLocationToChats() {
		this.shareLocationChats = emptySet()
		this.chatLivePeriods.clear()
		this.chatShareLocStartSec.clear()
		this.chatShareAddActiveTime.clear()
	}

	fun showChatOnMap(chatId: Long, show: Boolean) {
		val hiddenChats = hiddenOnMapChats.toMutableList()
		if (show) {
			hiddenChats.remove(chatId)
		} else {
			hiddenChats.add(chatId)
		}
		hiddenOnMapChats = hiddenChats.toHashSet()
	}

	fun getShareLocationChats() = ArrayList(shareLocationChats)

	fun getShowOnMapChats() = getLiveNowChats().minus(hiddenOnMapChats)

	fun getShowOnMapChatsCount() = getShowOnMapChats().size

	fun clear() {
		stopSharingLocationToChats()
		app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE).edit().clear().apply()
	}

	fun updateAppToConnect(appToConnectPackage: String) {
		app.showLocationHelper.stopShowingLocation()
		this.appToConnectPackage = appToConnectPackage
		app.osmandAidlHelper.reconnectOsmand()
	}

	fun save() {
		val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
		val edit = prefs.edit()

		val shareLocationChatsSet = mutableSetOf<String>()
		val shareLocationChats = ArrayList(shareLocationChats)
		for (chatId in shareLocationChats) {
			shareLocationChatsSet.add(chatId.toString())
		}
		edit.putStringSet(SHARE_LOCATION_CHATS_KEY, shareLocationChatsSet)

		val hiddenChatsSet = mutableSetOf<String>()
		val hiddenChats = ArrayList(hiddenOnMapChats)
		for (chatId in hiddenChats) {
			hiddenChatsSet.add(chatId.toString())
		}
		edit.putStringSet(HIDDEN_ON_MAP_CHATS_KEY, hiddenChatsSet)

		edit.putString(SHARING_MODE_KEY, currentSharingMode)

		edit.putString(METRICS_CONSTANTS_KEY, metricsConstants.name)
		edit.putString(SPEED_CONSTANTS_KEY, speedConstants.name)

		edit.putLong(SEND_MY_LOC_INTERVAL_KEY, sendMyLocInterval)
		edit.putLong(STALE_LOC_TIME_KEY, staleLocTime)
		edit.putLong(LOC_HISTORY_TIME_KEY, locHistoryTime)

		edit.putString(APP_TO_CONNECT_PACKAGE_KEY, appToConnectPackage)

		edit.putString(LIVE_NOW_SORT_TYPE_KEY, liveNowSortType.name)

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

		val hiddenChats = mutableSetOf<Long>()
		val hiddenChatsSet = prefs.getStringSet(HIDDEN_ON_MAP_CHATS_KEY, mutableSetOf())
		for (chatId in hiddenChatsSet) {
			hiddenChats.add(chatId.toLong())
		}
		hiddenOnMapChats = hiddenChats

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

		currentSharingMode = prefs.getString(SHARING_MODE_KEY, "")

		appToConnectPackage = prefs.getString(APP_TO_CONNECT_PACKAGE_KEY, "")

		liveNowSortType = LiveNowSortType.valueOf(
			prefs.getString(LIVE_NOW_SORT_TYPE_KEY, LiveNowSortType.SORT_BY_GROUP.name)
		)
	}

	private fun getLiveNowChats() = app.telegramHelper.getMessagesByChatIds(locHistoryTime).keys

	private fun updatePrefs() {
		val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
		val idsInUse = prefs.getBoolean(TITLES_REPLACED_WITH_IDS, false)
		if (!idsInUse) {
			val edit = prefs.edit()

			edit.putStringSet(SHARE_LOCATION_CHATS_KEY, emptySet())
			edit.putBoolean(TITLES_REPLACED_WITH_IDS, true)

			edit.apply()
		}
	}

	inner class SendMyLocPref : DurationPref(
		R.drawable.ic_action_share_location,
		R.string.send_my_location,
		R.string.send_my_location_desc,
		SEND_MY_LOC_VALUES_SEC
	) {

		override fun getCurrentValue() =
			OsmandFormatter.getFormattedDuration(app, sendMyLocInterval)

		override fun setCurrentValue(index: Int) {
			sendMyLocInterval = values[index]
			app.updateSendLocationInterval()
		}
	}

	inner class StaleLocPref : DurationPref(
		R.drawable.ic_action_time_span,
		R.string.stale_location,
		R.string.stale_location_desc,
		STALE_LOC_VALUES_SEC
	) {

		override fun getCurrentValue() =
			OsmandFormatter.getFormattedDuration(app, staleLocTime)

		override fun setCurrentValue(index: Int) {
			staleLocTime = values[index]
		}
	}

	inner class LocHistoryPref : DurationPref(
		R.drawable.ic_action_location_history,
		R.string.location_history,
		R.string.location_history_desc,
		LOC_HISTORY_VALUES_SEC
	) {

		override fun getCurrentValue() =
			OsmandFormatter.getFormattedDuration(app, locHistoryTime)

		override fun setCurrentValue(index: Int) {
			val value = values[index]
			locHistoryTime = value
			app.telegramHelper.messageActiveTimeSec = value
		}
	}

	abstract inner class DurationPref(
		@DrawableRes val iconId: Int,
		@StringRes val titleId: Int,
		@StringRes val descriptionId: Int,
		val values: List<Long>
	) {

		abstract fun getCurrentValue(): String

		abstract fun setCurrentValue(index: Int)

		fun getMenuItems() = values.map { OsmandFormatter.getFormattedDuration(app, it) }
	}

	enum class AppConnect(
		@DrawableRes val iconId: Int,
		@DrawableRes val whiteIconId: Int,
		val title: String,
		val appPackage: String,
		val showOnlyInstalled: Boolean
	) {
		OSMAND_PLUS(
			R.drawable.ic_logo_osmand_plus,
			R.drawable.ic_action_osmand_plus,
			"OsmAnd+",
			OsmandAidlHelper.OSMAND_PLUS_PACKAGE_NAME,
			false
		),
		OSMAND_FREE(
			R.drawable.ic_logo_osmand_free,
			R.drawable.ic_action_osmand_free,
			"OsmAnd",
			OsmandAidlHelper.OSMAND_FREE_PACKAGE_NAME,
			false
		),
		OSMAND_NIGHTLY(
			R.drawable.ic_logo_osmand_nightly,
			R.drawable.ic_action_osmand_free,
			"OsmAnd Nightly",
			OsmandAidlHelper.OSMAND_NIGHTLY_PACKAGE_NAME,
			true
		);

		companion object {

			@DrawableRes
			fun getWhiteIconId(appPackage: String): Int {
				for (item in values()) {
					if (item.appPackage == appPackage) {
						return item.whiteIconId
					}
				}
				return 0
			}

			fun getInstalledApps(context: Context) =
				values().filter { AndroidUtils.isAppInstalled(context, it.appPackage) }
		}
	}

	enum class LiveNowSortType(
		@DrawableRes val iconId: Int,
		@StringRes val titleId: Int,
		@StringRes val shortTitleId: Int
	) {
		SORT_BY_GROUP(
			R.drawable.ic_action_sort_by_group,
			R.string.shared_string_group,
			R.string.by_group
		),
		SORT_BY_NAME(
			R.drawable.ic_action_sort_by_name,
			R.string.shared_string_name,
			R.string.by_name
		),
		SORT_BY_DISTANCE(
			R.drawable.ic_action_sort_by_distance,
			R.string.shared_string_distance,
			R.string.by_distance
		);

		fun isSortByGroup() = this == SORT_BY_GROUP
	}

	class DeviceBot {
		var id: Long = -1
		var userId: Long = -1
		var chatId: Long = -1
		var deviceName: String = ""
		var externalId: String = ""
		var data: String = ""
	}
}
