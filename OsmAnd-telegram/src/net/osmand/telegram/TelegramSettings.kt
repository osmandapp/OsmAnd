package net.osmand.telegram

import android.content.Context
import android.location.LocationManager
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import net.osmand.telegram.helpers.OsmandAidlHelper
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandApiUtils
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.telegram.utils.OsmandFormatter.MetricsConstants
import net.osmand.telegram.utils.OsmandFormatter.SpeedConstants
import org.drinkless.td.libcore.telegram.TdApi
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue

val ADDITIONAL_ACTIVE_TIME_VALUES_SEC = listOf(15 * 60L, 30 * 60L, 60 * 60L, 180 * 60L)

const val SHARE_DEVICES_KEY = "devices"

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

const val SHARE_TYPE_MAP = "Map"
const val SHARE_TYPE_TEXT =  "Text"
const val SHARE_TYPE_MAP_AND_TEXT = "Map_and_text"
private val SHARE_TYPE_VALUES = listOf(SHARE_TYPE_MAP, SHARE_TYPE_TEXT, SHARE_TYPE_MAP_AND_TEXT)

private const val SEND_MY_LOC_DEFAULT_INDEX = 6
private const val STALE_LOC_DEFAULT_INDEX = 0
private const val LOC_HISTORY_DEFAULT_INDEX = 7
private const val SHARE_TYPE_DEFAULT_INDEX = 2

private const val SETTINGS_NAME = "osmand_telegram_settings"

private const val SHARE_LOCATION_CHATS_KEY = "share_location_chats"
private const val HIDDEN_ON_MAP_CHATS_KEY = "hidden_on_map_chats"

private const val SHARING_MODE_KEY = "current_sharing_mode"

private const val METRICS_CONSTANTS_KEY = "metrics_constants"
private const val SPEED_CONSTANTS_KEY = "speed_constants"

private const val SEND_MY_LOC_INTERVAL_KEY = "send_my_loc_interval"
private const val STALE_LOC_TIME_KEY = "stale_loc_time"
private const val LOC_HISTORY_TIME_KEY = "loc_history_time"
private const val SHARE_TYPE_KEY = "share_type"

private const val APP_TO_CONNECT_PACKAGE_KEY = "app_to_connect_package"

private const val DEFAULT_VISIBLE_TIME_SECONDS = 60 * 60L // 1 hour

private const val TITLES_REPLACED_WITH_IDS = "changed_to_chat_id"

private const val LIVE_NOW_SORT_TYPE_KEY = "live_now_sort_type"

private const val SHARE_CHATS_INFO_KEY = "share_chats_info"

private const val BATTERY_OPTIMISATION_ASKED = "battery_optimisation_asked"

private const val MONITORING_ENABLED = "monitoring_enabled"

private const val SHARING_INITIALIZATION_TIME = 60 * 2L // 2 minutes

private const val GPS_UPDATE_EXPIRED_TIME = 60 * 3L // 3 minutes

class TelegramSettings(private val app: TelegramApplication) {

	private var shareChatsInfo = ConcurrentHashMap<Long, ShareChatInfo>()
	private var hiddenOnMapChats: Set<Long> = emptySet()
	private var shareDevices: Set<DeviceBot> = emptySet()

	var sharingStatusChanges = ConcurrentLinkedQueue<SharingStatus>()

	var currentSharingMode = ""
		private set

	var metricsConstants = MetricsConstants.KILOMETERS_AND_METERS
	var speedConstants = SpeedConstants.KILOMETERS_PER_HOUR

	var sendMyLocInterval = SEND_MY_LOC_VALUES_SEC[SEND_MY_LOC_DEFAULT_INDEX]
	var staleLocTime = STALE_LOC_VALUES_SEC[STALE_LOC_DEFAULT_INDEX]
	var locHistoryTime = LOC_HISTORY_VALUES_SEC[LOC_HISTORY_DEFAULT_INDEX]
	var shareTypeValue = SHARE_TYPE_VALUES[SHARE_TYPE_DEFAULT_INDEX]

	var appToConnectPackage = ""
		private set

	var liveNowSortType = LiveNowSortType.SORT_BY_DISTANCE

	val gpsAndLocPrefs = listOf(SendMyLocPref(), StaleLocPref(), LocHistoryPref(), ShareTypePref())

	var batteryOptimisationAsked = false

	var monitoringEnabled = false

	init {
		updatePrefs()
		read()
	}

	fun hasAnyChatToShareLocation() = shareChatsInfo.isNotEmpty()

	fun isSharingLocationToChat(chatId: Long) = shareChatsInfo.containsKey(chatId)

	fun isSharingLocationToUser(userId: Int) = shareChatsInfo.values.any { it.userId == userId }

	fun hasAnyChatToShowOnMap() = !hiddenOnMapChats.containsAll(getLiveNowChats())

	fun isShowingChatOnMap(chatId: Long) = !hiddenOnMapChats.contains(chatId)

	fun removeNonexistingChats(presentChatIds: List<Long>) {
		val hiddenChats = hiddenOnMapChats.toMutableList()
		hiddenChats.intersect(presentChatIds)
		hiddenOnMapChats = hiddenChats.toHashSet()

		shareChatsInfo = ConcurrentHashMap(shareChatsInfo.filter { (key, _) -> presentChatIds.contains(key) })
	}

	fun shareLocationToChat(
		chatId: Long,
		share: Boolean,
		livePeriod: Long = DEFAULT_VISIBLE_TIME_SECONDS,
		addActiveTime: Long = ADDITIONAL_ACTIVE_TIME_VALUES_SEC[0]
	) {
		if (share) {
			var shareChatInfo = shareChatsInfo[chatId]
			if (shareChatInfo == null) {
				shareChatInfo = ShareChatInfo()
			}
			val chat = app.telegramHelper.getChat(chatId)
			if (chat != null && (chat.type is TdApi.ChatTypePrivate || chat.type is TdApi.ChatTypeSecret)) {
				shareChatInfo.userId = app.telegramHelper.getUserIdFromChatType(chat.type)
			}
			shareChatInfo.chatId = chatId
			updateChatShareInfo(shareChatInfo, livePeriod, addActiveTime)
			shareChatsInfo[chatId] = shareChatInfo
		} else {
			shareChatsInfo.remove(chatId)
		}
	}

	fun shareLocationToUser(
		userId: Int,
		livePeriod: Long = DEFAULT_VISIBLE_TIME_SECONDS,
		addActiveTime: Long = ADDITIONAL_ACTIVE_TIME_VALUES_SEC[0]
	) {
		val shareChatInfo = ShareChatInfo()
		shareChatInfo.userId = userId
		updateChatShareInfo(shareChatInfo, livePeriod, addActiveTime)
		app.telegramHelper.createPrivateChatWithUser(userId, shareChatInfo, shareChatsInfo)
	}

	fun updateShareDevices(list: List<DeviceBot>) {
		shareDevices = list.toHashSet()
	}

	fun addShareDevice(device: DeviceBot) {
		val devices = shareDevices.toMutableList()
		devices.add(device)
		shareDevices = devices.toHashSet()
	}

	fun updateCurrentSharingMode(sharingMode: String) {
		if (currentSharingMode != sharingMode) {
			shareChatsInfo.forEach { (_, shareInfo) ->
				shareInfo.shouldSendViaBotMessage = true
			}
		}
		currentSharingMode = sharingMode
	}

	fun getChatLivePeriod(chatId: Long) = shareChatsInfo[chatId]?.livePeriod

	fun getChatsShareInfo() = shareChatsInfo

	fun getShareDevices() = shareDevices

	fun containsShareDeviceWithName(name: String): Boolean {
		shareDevices.forEach {
			if (it.deviceName == name) {
				return true
			}
		}
		return false
	}

	fun getCurrentSharingDevice() = shareDevices.singleOrNull { it.externalId == currentSharingMode }

	fun getLastSuccessfulSendTime() = shareChatsInfo.values.maxBy { it.lastSuccessfulSendTimeMs }?.lastSuccessfulSendTimeMs ?: -1

	fun stopSharingLocationToChats() {
		shareChatsInfo.clear()
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

	fun getShareLocationChats() = shareChatsInfo.keys

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

	fun updateShareInfo(message: TdApi.Message) {
		val shareChatInfo = shareChatsInfo[message.chatId]
		val content = message.content
		if (shareChatInfo != null) {
			when (content) {
				is TdApi.MessageLocation -> {
					shareChatInfo.currentMapMessageId = message.id
					shareChatInfo.pendingMapMessage = false
				}
				is TdApi.MessageText -> {
					shareChatInfo.currentTextMessageId = message.id
					shareChatInfo.updateTextMessageId++
					shareChatInfo.pendingTextMessage = false
				}
			}
			shareChatInfo.lastSuccessfulSendTimeMs = Math.max(message.editDate, message.date) * 1000L
		}
	}

	fun updateSharingStatusHistory() {
		val newSharingStatus = getNewSharingStatusHistoryItem()

		if (sharingStatusChanges.isNotEmpty()) {
			val lastSharingStatus = sharingStatusChanges.last()
			if (lastSharingStatus.statusType != newSharingStatus.statusType) {
				sharingStatusChanges.add(newSharingStatus)
			} else {
				lastSharingStatus.apply {
					statusChangeTime = newSharingStatus.statusChangeTime
					locationTime = newSharingStatus.locationTime
					chatsTitles = newSharingStatus.chatsTitles
					title = newSharingStatus.title

					if (statusType == SharingStatusType.INITIALIZING
						&& newSharingStatus.statusType == SharingStatusType.INITIALIZING) {
						if (!description.contains(newSharingStatus.description)) {
							description = "$description, ${newSharingStatus.description}"
						}
					} else {
						description = newSharingStatus.description
					}
				}
			}
		} else {
			sharingStatusChanges.add(newSharingStatus)
		}
	}

	private fun updateChatShareInfo(
		shareChatInfo: ShareChatInfo,
		livePeriod: Long = DEFAULT_VISIBLE_TIME_SECONDS,
		addActiveTime: Long = ADDITIONAL_ACTIVE_TIME_VALUES_SEC[0]
	) {
		val lp: Long = when {
			livePeriod < TelegramHelper.MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC -> TelegramHelper.MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong()
			else -> livePeriod
		}
		val currentTime = System.currentTimeMillis() / 1000
		val user = app.telegramHelper.getCurrentUser()
		if (user != null && currentSharingMode != user.id.toString() && shareChatInfo.start == -1L) {
			shareChatInfo.shouldSendViaBotMessage = true
		}

		shareChatInfo.start = currentTime
		if (shareChatInfo.livePeriod == -1L) {
			shareChatInfo.livePeriod = lp
		}
		shareChatInfo.userSetLivePeriod = lp
		shareChatInfo.userSetLivePeriodStart = currentTime
		shareChatInfo.currentMessageLimit = currentTime + Math.min(lp, TelegramHelper.MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC.toLong())
		shareChatInfo.additionalActiveTime = addActiveTime
	}

	private fun getNewSharingStatusHistoryItem(): SharingStatus {
		return SharingStatus().apply {
			statusChangeTime = System.currentTimeMillis()
			val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
			val gpsEnabled = try {
				if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
					val gpsActive = loc != null && ((statusChangeTime - loc.time) / 1000) < GPS_UPDATE_EXPIRED_TIME
					val lastSentLocationExpired = ((statusChangeTime - app.shareLocationHelper.lastLocationMessageSentTime) / 1000) > GPS_UPDATE_EXPIRED_TIME
					(gpsActive || !lastSentLocationExpired)
				} else {
					false
				}
			} catch (ex: Exception) {
				false
			}

			var initializing = false
			var sendChatsErrors = false

			shareChatsInfo.forEach { (_, shareInfo) ->
				if (shareInfo.lastSuccessfulSendTimeMs == -1L && ((statusChangeTime / 1000 - shareInfo.start) < SHARING_INITIALIZATION_TIME)) {
					initializing = true
				}
				if (shareInfo.hasSharingError) {
					sendChatsErrors = true
					locationTime = shareInfo.lastSuccessfulSendTimeMs
					val title = app.telegramHelper.getChat(shareInfo.chatId)?.title
					if (title != null) {
						chatsTitles.add(title)
					}
				}
			}

			if (sendChatsErrors) {
				title = app.getString(R.string.not_possible_to_send_to_telegram_chats)
				description = app.getString(R.string.last_updated_location)
				statusType = SharingStatusType.NOT_POSSIBLE_TO_SENT_TO_CHATS
			} else if (!initializing) {
				when {
					!gpsEnabled -> {
						locationTime = app.shareLocationHelper.lastLocationMessageSentTime
						if (locationTime <= 0) {
							locationTime = getLastSuccessfulSendTime()
						}
						title = app.getString(R.string.no_gps_connection)
						description = app.getString(R.string.last_updated_location)
						statusType = SharingStatusType.NO_GPS
					}
					!app.isInternetConnectionAvailable -> {
						locationTime = getLastSuccessfulSendTime()
						title = app.getString(R.string.no_internet_connection)
						description = app.getString(R.string.last_updated_location)
						statusType = SharingStatusType.NO_INTERNET
					}
					else -> {
						locationTime = getLastSuccessfulSendTime()
						statusType = SharingStatusType.SENDING
						if (locationTime == -1L) {
							title = app.getString(R.string.sending_location_messages)
							description = app.getString(R.string.waiting_for_response_from_telegram)
						} else {
							title = app.getString(R.string.successfully_sent_and_updated)
							description = app.getString(R.string.last_updated_location)
						}
					}
				}
			} else {
				if (gpsEnabled && app.isInternetConnectionAvailable) {
					title = app.getString(R.string.sending_location_messages)
					description = app.getString(R.string.waiting_for_response_from_telegram)
					statusType = SharingStatusType.SENDING
				} else {
					title = app.getString(R.string.initializing)
					statusType = SharingStatusType.INITIALIZING
					if (!gpsEnabled) {
						description = app.getString(R.string.searching_for_gps)
					} else if (!app.isInternetConnectionAvailable) {
						description = app.getString(R.string.connecting_to_the_internet)
					}
				}
			}
		}
	}

	fun onDeleteLiveMessages(chatId: Long, messages: List<Long>) {
		val currentMapMessageId = shareChatsInfo[chatId]?.currentMapMessageId
		if (messages.contains(currentMapMessageId)) {
			shareChatsInfo[chatId]?.currentMapMessageId = -1
		}
		val currentTextMessageId = shareChatsInfo[chatId]?.currentTextMessageId
		if (messages.contains(currentTextMessageId)) {
			shareChatsInfo[chatId]?.currentTextMessageId = -1
			shareChatsInfo[chatId]?.updateTextMessageId = 1
		}
	}
	
	fun save() {
		val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)
		val edit = prefs.edit()

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

		edit.putString(SHARE_TYPE_KEY, shareTypeValue)

		edit.putString(APP_TO_CONNECT_PACKAGE_KEY, appToConnectPackage)

		edit.putString(LIVE_NOW_SORT_TYPE_KEY, liveNowSortType.name)

		edit.putBoolean(BATTERY_OPTIMISATION_ASKED, batteryOptimisationAsked)

		edit.putBoolean(MONITORING_ENABLED, monitoringEnabled)

		val jArray = convertShareChatsInfoToJson()
		if (jArray != null) {
			edit.putString(SHARE_CHATS_INFO_KEY, jArray.toString())
		}

		val jsonObject = convertShareDevicesToJson()
		if (jsonObject != null) {
			edit.putString(SHARE_DEVICES_KEY, jsonObject.toString())
		}

		edit.apply()
	}

	fun read() {
		val prefs = app.getSharedPreferences(SETTINGS_NAME, Context.MODE_PRIVATE)

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

		try {
			parseShareChatsInfo(JSONArray(prefs.getString(SHARE_CHATS_INFO_KEY, "")))
		} catch (e: JSONException) {
			e.printStackTrace()
		}

		parseShareDevices(prefs.getString(SHARE_DEVICES_KEY, ""))

		val sendMyLocDef = SEND_MY_LOC_VALUES_SEC[SEND_MY_LOC_DEFAULT_INDEX]
		sendMyLocInterval = prefs.getLong(SEND_MY_LOC_INTERVAL_KEY, sendMyLocDef)
		val staleLocDef = STALE_LOC_VALUES_SEC[STALE_LOC_DEFAULT_INDEX]
		staleLocTime = prefs.getLong(STALE_LOC_TIME_KEY, staleLocDef)
		val locHistoryDef = LOC_HISTORY_VALUES_SEC[LOC_HISTORY_DEFAULT_INDEX]
		locHistoryTime = prefs.getLong(LOC_HISTORY_TIME_KEY, locHistoryDef)
		val shareTypeDef = SHARE_TYPE_VALUES[SHARE_TYPE_DEFAULT_INDEX]
		shareTypeValue = prefs.getString(SHARE_TYPE_KEY, shareTypeDef)

		currentSharingMode = prefs.getString(SHARING_MODE_KEY, "")

		appToConnectPackage = prefs.getString(APP_TO_CONNECT_PACKAGE_KEY, "")

		liveNowSortType = LiveNowSortType.valueOf(
			prefs.getString(LIVE_NOW_SORT_TYPE_KEY, LiveNowSortType.SORT_BY_DISTANCE.name)
		)

		batteryOptimisationAsked = prefs.getBoolean(BATTERY_OPTIMISATION_ASKED,false)

		monitoringEnabled = prefs.getBoolean(MONITORING_ENABLED,false)
	}

	private fun convertShareDevicesToJson():JSONObject?{
		return try {
			val jsonObject = JSONObject()
			val jArray = JSONArray()
			shareDevices.forEach { device ->
				val obj = JSONObject()
				obj.put(DeviceBot.DEVICE_ID, device.id)
				obj.put(DeviceBot.USER_ID, device.userId)
				obj.put(DeviceBot.CHAT_ID, device.chatId)
				obj.put(DeviceBot.DEVICE_NAME, device.deviceName)
				obj.put(DeviceBot.EXTERNAL_ID, device.externalId)
				obj.put(DeviceBot.DATA, JSONObject(device.data))
				jArray.put(obj)
			}
			jsonObject.put(SHARE_DEVICES_KEY, jArray)
		} catch (e: JSONException) {
			e.printStackTrace()
			null
		}
	}

	private fun convertShareChatsInfoToJson(): JSONArray? {
		return try {
			val jArray = JSONArray()
			shareChatsInfo.forEach { (chatId, chatInfo) ->
				val obj = JSONObject()
				obj.put(ShareChatInfo.CHAT_ID_KEY, chatId)
				obj.put(ShareChatInfo.USER_ID_KEY, chatInfo.userId)
				obj.put(ShareChatInfo.START_KEY, chatInfo.start)
				obj.put(ShareChatInfo.LIVE_PERIOD_KEY, chatInfo.livePeriod)
				obj.put(ShareChatInfo.LIMIT_KEY, chatInfo.currentMessageLimit)
				obj.put(ShareChatInfo.UPDATE_TEXT_MESSAGE_ID_KEY, chatInfo.updateTextMessageId)
				obj.put(ShareChatInfo.CURRENT_MAP_MESSAGE_ID_KEY, chatInfo.currentMapMessageId)
				obj.put(ShareChatInfo.CURRENT_TEXT_MESSAGE_ID_KEY, chatInfo.currentTextMessageId)
				obj.put(ShareChatInfo.USER_SET_LIVE_PERIOD_KEY, chatInfo.userSetLivePeriod)
				obj.put(ShareChatInfo.USER_SET_LIVE_PERIOD_START_KEY, chatInfo.userSetLivePeriodStart)
				obj.put(ShareChatInfo.LAST_SUCCESSFUL_SEND_TIME_KEY, chatInfo.lastSuccessfulSendTimeMs)
				obj.put(ShareChatInfo.LAST_SEND_MAP_TIME_KEY, chatInfo.lastSendMapMessageTime)
				obj.put(ShareChatInfo.LAST_SEND_TEXT_TIME_KEY, chatInfo.lastSendTextMessageTime)
				jArray.put(obj)
			}
			jArray
		} catch (e: JSONException) {
			e.printStackTrace()
			null
		}
	}

	private fun parseShareChatsInfo(json: JSONArray) {
		for (i in 0 until json.length()) {
			val obj = json.getJSONObject(i)
			val shareInfo = ShareChatInfo().apply {
				chatId = obj.optLong(ShareChatInfo.CHAT_ID_KEY)
				userId = obj.optInt(ShareChatInfo.USER_ID_KEY)
				start = obj.optLong(ShareChatInfo.START_KEY)
				livePeriod = obj.optLong(ShareChatInfo.LIVE_PERIOD_KEY)
				currentMessageLimit = obj.optLong(ShareChatInfo.LIMIT_KEY)
				updateTextMessageId = obj.optInt(ShareChatInfo.UPDATE_TEXT_MESSAGE_ID_KEY)
				currentMapMessageId = obj.optLong(ShareChatInfo.CURRENT_MAP_MESSAGE_ID_KEY)
				currentTextMessageId = obj.optLong(ShareChatInfo.CURRENT_TEXT_MESSAGE_ID_KEY)
				userSetLivePeriod = obj.optLong(ShareChatInfo.USER_SET_LIVE_PERIOD_KEY)
				userSetLivePeriodStart = obj.optLong(ShareChatInfo.USER_SET_LIVE_PERIOD_START_KEY)
				lastSuccessfulSendTimeMs = obj.optLong(ShareChatInfo.LAST_SUCCESSFUL_SEND_TIME_KEY)
				lastSendMapMessageTime = obj.optInt(ShareChatInfo.LAST_SEND_MAP_TIME_KEY)
				lastSendTextMessageTime = obj.optInt(ShareChatInfo.LAST_SEND_TEXT_TIME_KEY)
			}
			shareChatsInfo[shareInfo.chatId] = shareInfo
		}
	}

	private fun parseShareDevices(json: String) {
		shareDevices = OsmandApiUtils.parseJsonContents(json).toHashSet()
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

	inner class ShareTypePref : DurationPref(
		R.drawable.ic_action_location_history,
		R.string.send_location_as,
		R.string.send_location_as_descr,
		emptyList()
	) {

		override fun getCurrentValue(): String {
			return getTextValue(shareTypeValue)
		}

		override fun setCurrentValue(index: Int) {
			val newSharingType = SHARE_TYPE_VALUES[index]
			if (shareTypeValue != newSharingType && app.telegramHelper.getCurrentUser()?.id.toString() != currentSharingMode) {
				shareChatsInfo.forEach { (_, shareInfo) ->
					shareInfo.shouldSendViaBotMessage = true
				}
			}
			shareTypeValue = newSharingType
		}

		override fun getMenuItems(): List<String> {
			return SHARE_TYPE_VALUES.map { getTextValue(it) }
		}

		private fun getTextValue(shareType: String): String {
			return when (shareType) {
				SHARE_TYPE_MAP -> app.getString(R.string.shared_string_map)
				SHARE_TYPE_TEXT -> app.getString(R.string.shared_string_text)
				SHARE_TYPE_MAP_AND_TEXT -> app.getString(R.string.map_and_text)
				else -> ""
			}
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

		open fun getMenuItems() = values.map { OsmandFormatter.getFormattedDuration(app, it) }
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

	enum class SharingStatusType(
		@DrawableRes val iconId: Int,
		@ColorRes val iconColorRes: Int,
		val canResendLocation: Boolean
	) {
		NO_INTERNET(
			R.drawable.ic_action_wifi_off,
			R.color.sharing_status_icon_error,
			true
		),
		SENDING(
			R.drawable.ic_action_share_location,
			R.color.sharing_status_icon_success,
			false
		),
		NOT_POSSIBLE_TO_SENT_TO_CHATS(
			R.drawable.ic_action_message_send_error,
			R.color.sharing_status_icon_error,
			true
		),
		NO_GPS(
			R.drawable.ic_action_location_off,
			R.color.sharing_status_icon_error,
			false
		),
		INITIALIZING(
			R.drawable.ic_action_connect,
			R.color.sharing_status_icon_error,
			false
		);
	}

	class DeviceBot {
		var id: Long = -1
		var userId: Long = -1
		var chatId: Long = -1
		var deviceName: String = ""
		var externalId: String = ""
		var data: String = ""

		companion object {

			internal const val DEVICE_ID = "id"
			internal const val USER_ID = "userId"
			internal const val CHAT_ID = "chatId"
			internal const val DEVICE_NAME = "deviceName"
			internal const val EXTERNAL_ID = "externalId"
			internal const val DATA = "data"
		}
	}

	class SharingStatus {

		var title: String = ""
		var description: String = ""
		var locationTime: Long = -1
		var statusChangeTime: Long = -1
		var chatsTitles: MutableList<String> = mutableListOf()

		lateinit var statusType: SharingStatusType

		fun getTitle(app: TelegramApplication): CharSequence {
			return if (statusType != SharingStatusType.NOT_POSSIBLE_TO_SENT_TO_CHATS || chatsTitles.isEmpty()) {
				title
			} else {
				val spannableString = SpannableStringBuilder(title)
				val iterator = chatsTitles.iterator()
				while (iterator.hasNext()) {
					val chatTitle = iterator.next()
					val start = spannableString.length
					val newSpannable = if (iterator.hasNext()) " @$chatTitle," else " @$chatTitle."
					spannableString.append(newSpannable)
					spannableString.setSpan(ForegroundColorSpan(app.uiUtils.getActiveColor()), start, spannableString.length - 1, 0)
				}
				spannableString
			}
		}
	}

	class ShareChatInfo {

		var chatId = -1L
		var userId = -1
		var start = -1L
		var livePeriod = -1L
		var updateTextMessageId = 1
		var currentMessageLimit = -1L
		var currentMapMessageId = -1L
		var oldMapMessageId = -1L
		var currentTextMessageId = -1L
		var oldTextMessageId = -1L
		var userSetLivePeriod = -1L
		var userSetLivePeriodStart = -1L
		var lastSuccessfulSendTimeMs = -1L
		var lastSendTextMessageTime = -1
		var lastSendMapMessageTime = -1
		var pendingTdLib = 0
		var pendingTextMessage = false
		var pendingMapMessage = false
		var shouldSendViaBotMessage = false
		var hasSharingError = false
		var shouldDeletePreviousMapMessage = false
		var shouldDeletePreviousTextMessage = false
		var additionalActiveTime = ADDITIONAL_ACTIVE_TIME_VALUES_SEC[0]

		fun getNextAdditionalActiveTime(): Long {
			var index = ADDITIONAL_ACTIVE_TIME_VALUES_SEC.indexOf(additionalActiveTime)
			return if (ADDITIONAL_ACTIVE_TIME_VALUES_SEC.lastIndex > index) {
				ADDITIONAL_ACTIVE_TIME_VALUES_SEC[++index]
			} else {
				ADDITIONAL_ACTIVE_TIME_VALUES_SEC[index]
			}
		}

		fun getChatLiveMessageExpireTime(): Long {
			return userSetLivePeriod - ((System.currentTimeMillis() / 1000) - start)
		}

		companion object {

			internal const val CHAT_ID_KEY = "chatId"
			internal const val USER_ID_KEY = "userId"
			internal const val START_KEY = "start"
			internal const val LIVE_PERIOD_KEY = "livePeriod"
			internal const val LIMIT_KEY = "limit"
			internal const val UPDATE_TEXT_MESSAGE_ID_KEY = "updateTextMessageId"
			internal const val CURRENT_MAP_MESSAGE_ID_KEY = "currentMapMessageId"
			internal const val CURRENT_TEXT_MESSAGE_ID_KEY = "currentTextMessageId"
			internal const val USER_SET_LIVE_PERIOD_KEY = "userSetLivePeriod"
			internal const val USER_SET_LIVE_PERIOD_START_KEY = "userSetLivePeriodStart"
			internal const val LAST_SUCCESSFUL_SEND_TIME_KEY = "lastSuccessfulSendTime"
			internal const val LAST_SEND_MAP_TIME_KEY = "lastSendMapMessageTime"
			internal const val LAST_SEND_TEXT_TIME_KEY = "lastSendTextMessageTime"
		}
	}
}