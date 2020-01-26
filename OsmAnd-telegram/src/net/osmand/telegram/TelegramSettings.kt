package net.osmand.telegram

import android.content.Context
import android.location.LocationManager
import android.support.annotation.ColorRes
import android.support.annotation.DrawableRes
import android.support.annotation.StringRes
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import net.osmand.PlatformUtil
import net.osmand.telegram.helpers.OsmandAidlHelper
import net.osmand.telegram.helpers.ShowLocationHelper
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.utils.*
import net.osmand.telegram.utils.OsmandFormatter.MetricsConstants
import net.osmand.telegram.utils.OsmandFormatter.SpeedConstants
import org.drinkless.td.libcore.telegram.TdApi
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.collections.ArrayList

val ADDITIONAL_ACTIVE_TIME_VALUES_SEC = listOf(15 * 60L, 30 * 60L, 60 * 60L, 180 * 60L)

const val SHARE_DEVICES_KEY = "devices"

const val LIVE_TRACKS_KEY = "live_tracks"

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
private val MIN_LOCATION_DISTANCE = listOf(0f, 2.0f, 5.0f, 10.0f, 20.0f, 30.0f, 50.0f)
private val MIN_LOCATION_ACCURACY = listOf(0f, 1.0f, 2.0f, 5.0f, 10.0f, 15.0f, 20.0f, 50.0f, 100.0f)
private val MIN_LOCATION_SPEED = listOf(0f, 0.000001f, 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f)

const val SHARE_TYPE_MAP = "Map"
const val SHARE_TYPE_TEXT =  "Text"
const val SHARE_TYPE_MAP_AND_TEXT = "Map_and_text"
private val SHARE_TYPE_VALUES = listOf(SHARE_TYPE_MAP, SHARE_TYPE_TEXT, SHARE_TYPE_MAP_AND_TEXT)

private const val SEND_MY_LOC_DEFAULT_INDEX = 6
private const val STALE_LOC_DEFAULT_INDEX = 0
private const val LOC_HISTORY_DEFAULT_INDEX = 7
private const val SHARE_TYPE_DEFAULT_INDEX = 2
private const val MIN_LOCATION_DISTANCE_INDEX = 0
private const val MIN_LOCATION_ACCURACY_INDEX = 0
private const val MIN_LOCATION_SPEED_INDEX = 0

private val BUFFER_TIME = listOf(60 * 60L, 2 * 60 * 60L, 4 * 60 * 60L, 8 * 60 * 60L,
		12 * 60 * 60L, 24 * 60 * 60L)
private const val BUFFER_TIME_INDEX = 0
private const val BUFFER_TIME_KEY = "buffer_time"
private const val FREE_TIMELINE_INFO_SHOWN_TIME_KEY = "free_timeline_info_shown_time"

private const val SETTINGS_NAME = "osmand_telegram_settings"

private const val SHARE_LOCATION_CHATS_KEY = "share_location_chats"
private const val HIDDEN_ON_MAP_CHATS_KEY = "hidden_on_map_chats"

private const val SHARING_MODE_KEY = "current_sharing_mode"

private const val METRICS_CONSTANTS_KEY = "metrics_constants"
private const val SPEED_CONSTANTS_KEY = "speed_constants"
private const val UTC_OFFSET_CONSTANTS_KEY = "utc_offset_constants"

private const val SEND_MY_LOC_INTERVAL_KEY = "send_my_loc_interval"
private const val STALE_LOC_TIME_KEY = "stale_loc_time"
private const val LOC_HISTORY_TIME_KEY = "loc_history_time"
private const val SHARE_TYPE_KEY = "share_type"

private const val MIN_LOCATION_DISTANCE_KEY = "min_location_distance"
private const val MIN_LOCATION_ACCURACY_KEY = "min_location_accuracy"
private const val MIN_LOCATION_SPEED_KEY = "min_location_speed"

private const val APP_TO_CONNECT_PACKAGE_KEY = "app_to_connect_package"

private const val DEFAULT_VISIBLE_TIME_SECONDS = 60 * 60L // 1 hour

private const val TITLES_REPLACED_WITH_IDS = "changed_to_chat_id"

private const val LIVE_NOW_SORT_TYPE_KEY = "live_now_sort_type"

private const val SHARE_CHATS_INFO_KEY = "share_chats_info"

private const val BATTERY_OPTIMISATION_ASKED = "battery_optimisation_asked"

private const val MONITORING_ENABLED = "monitoring_enabled"

private const val SHOW_GPS_POINTS = "show_gps_points"

private const val PROXY_ENABLED = "proxy_enabled"
private const val PROXY_PREFERENCES_KEY = "proxy_preferences"

private const val SHARING_INITIALIZATION_TIME = 60 * 2L // 2 minutes
private const val WAITING_TDLIB_TIME = 3 // 3 seconds

private const val GPS_UPDATE_EXPIRED_TIME = 60 * 3L // 3 minutes

private const val LAST_CHATS_INFO_KEY = "last_chats_info"

class TelegramSettings(private val app: TelegramApplication) {

	private val log = PlatformUtil.getLog(TelegramSettings::class.java)

	private var shareChatsInfo = ConcurrentHashMap<Long, ShareChatInfo>()
	private var hiddenOnMapChats: Set<Long> = emptySet()
	private var shareDevices: Set<DeviceBot> = emptySet()
	private var liveTracksInfo = emptyList<LiveTrackInfo>()
	var lastChatsInfo = LinkedList<LastChatInfo>()

	var sharingStatusChanges = ConcurrentLinkedQueue<SharingStatus>()

	var currentSharingMode = ""
		private set

	var currentProxyPref: ProxyPref = ProxySOCKS5Pref(-1, "", -1, "", "")
		private set

	var metricsConstants = MetricsConstants.KILOMETERS_AND_METERS
	var speedConstants = SpeedConstants.KILOMETERS_PER_HOUR
	var utcOffset = DataConstants.UTC_FORMAT

	var sendMyLocInterval = SEND_MY_LOC_VALUES_SEC[SEND_MY_LOC_DEFAULT_INDEX]
	var staleLocTime = STALE_LOC_VALUES_SEC[STALE_LOC_DEFAULT_INDEX]
	var locHistoryTime = LOC_HISTORY_VALUES_SEC[LOC_HISTORY_DEFAULT_INDEX]
	var shareTypeValue = SHARE_TYPE_VALUES[SHARE_TYPE_DEFAULT_INDEX]

	var minLocationDistance = MIN_LOCATION_DISTANCE[MIN_LOCATION_DISTANCE_INDEX]
	var minLocationAccuracy = MIN_LOCATION_ACCURACY[MIN_LOCATION_ACCURACY_INDEX]
	var minLocationSpeed = MIN_LOCATION_SPEED[MIN_LOCATION_SPEED_INDEX]

	var appToConnectPackage = ""
		private set

	var liveNowSortType = LiveNowSortType.SORT_BY_DISTANCE

	val gpsAndLocPrefs = listOf(SendMyLocPref(), StaleLocPref(), LocHistoryPref(), ShareTypePref(),
			BufferTimePref())
	val gpxLoggingPrefs = listOf(MinLocationDistance(), MinLocationAccuracy(), MinLocationSpeed())
	val unitsAndFormatsPrefs = listOf(UnitsOfSpeed(), UnitsOfLength(), UtcOffset())

	var batteryOptimisationAsked = false

	var monitoringEnabled = false

	var showGpsPoints = false

	var proxyEnabled = false

	var bufferTime = BUFFER_TIME[BUFFER_TIME_INDEX]

	var freeTimelineInfoShownTime = 0L

	init {
		updatePrefs()
		read()
		applyProxyPref()
	}

	fun hasAnyChatToShareLocation() = shareChatsInfo.isNotEmpty()

	fun isSharingLocationToChat(chatId: Long) = shareChatsInfo.containsKey(chatId)

	fun isSharingLocationToUser(userId: Int) = shareChatsInfo.values.any { it.userId == userId }

	fun hasAnyChatToShowOnMap() = !hiddenOnMapChats.containsAll(getLiveNowChats())

	fun hasAnyLiveTracksToShowOnMap(): Boolean {
		val time = System.currentTimeMillis() - locHistoryTime * 1000
		val locations = app.locationMessages.getLastLocationMessagesSinceTime(time)
		locations.forEach { loc ->
			if (liveTracksInfo.any { it.userId == loc.userId && it.chatId == loc.chatId && it.deviceName == loc.deviceName }) {
				return true
			}
		}
		return false
	}

	fun getLiveTracksInfo() = liveTracksInfo

	fun getLiveTrackInfo(userId: Int, chatId: Long, deviceName: String) =
		liveTracksInfo.firstOrNull { it.userId == userId && it.chatId == chatId && it.deviceName == deviceName }

	fun isShowingChatOnMap(chatId: Long) = !hiddenOnMapChats.contains(chatId)

	fun isLiveTrackEnabled(userId: Int, chatId: Long, deviceName: String) =
		liveTracksInfo.any { (it.chatId == chatId && it.userId == userId && it.deviceName == deviceName) }

	fun updateLiveTrack(userId: Int, chatId: Long, deviceName: String, enable: Boolean) {
		val tracksInfo = liveTracksInfo.toMutableList()
		if (enable) {
			val colorIndex = if (tracksInfo.size > 0) (tracksInfo.last().colorIndex + 1) % ShowLocationHelper.GPX_COLORS_COUNT else 0
			tracksInfo.add(LiveTrackInfo(userId, chatId, deviceName, colorIndex))
		} else {
			tracksInfo.removeAll { it.userId == userId && it.chatId == chatId && it.deviceName == deviceName }
		}
		liveTracksInfo = tracksInfo.toList()
	}

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
				shareInfo.shouldSendViaBotTextMessage = true
				shareInfo.shouldSendViaBotMapMessage = true
			}
			prepareForSharingNewMessages()
		}
		currentSharingMode = sharingMode
	}

	fun updateCurrentProxyPref(proxyPref: ProxyPref, proxyEnabled: Boolean) {
		this.proxyEnabled = proxyEnabled
		currentProxyPref = proxyPref
		applyProxyPref()
	}

	fun updateProxySetting(enable: Boolean) {
		this.proxyEnabled = enable
		if (enable) {
			app.telegramHelper.enableProxy(currentProxyPref.id)
		} else {
			app.telegramHelper.disableProxy()
		}
	}

	fun applyProxyPref() {
		val proxyId = currentProxyPref.id
		if (proxyId != -1) {
			app.telegramHelper.editProxyPref(currentProxyPref, proxyEnabled)
		} else {
			app.telegramHelper.addProxyPref(currentProxyPref, proxyEnabled)
		}
	}

	fun prepareForSharingNewMessages() {
		shareChatsInfo.forEach { (_, shareInfo) ->
			prepareForSharingNewMessages(shareInfo)
		}
	}

	fun prepareForSharingNewMessages(chatsIds: List<Long>) {
		chatsIds.forEach {
			shareChatsInfo[it]?.also { shareInfo ->
				prepareForSharingNewMessages(shareInfo)
			}
		}
	}

	fun prepareForSharingNewMessages(shareInfo: ShareChatInfo) {
		shareInfo.pendingTdLibText = 0
		shareInfo.pendingTdLibMap = 0
		shareInfo.currentTextMessageId = -1L
		shareInfo.currentMapMessageId = -1L
		shareInfo.pendingTextMessage = false
		shareInfo.pendingMapMessage = false
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

	fun getLastSuccessfulSendTime(): Long {
		val lastSuccessTextSend = shareChatsInfo.values.maxBy { it.lastTextSuccessfulSendTime }?.lastTextSuccessfulSendTime ?: -1
		val lastSuccessMapSend = shareChatsInfo.values.maxBy { it.lastMapSuccessfulSendTime }?.lastMapSuccessfulSendTime ?: -1
		return Math.max(lastSuccessTextSend, lastSuccessMapSend)
	}

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
		val shareInfo = shareChatsInfo[message.chatId]
		val content = message.content
		val isOsmAndBot = app.telegramHelper.isOsmAndBot(OsmandLocationUtils.getSenderMessageId(message)) || app.telegramHelper.isOsmAndBot(message.viaBotUserId)
		if (shareInfo != null) {
			when (content) {
				is TdApi.MessageLocation -> {
					val state = message.sendingState
					if (state != null) {
						if (state.constructor == TdApi.MessageSendingStatePending.CONSTRUCTOR) {
							shareInfo.pendingMapMessage = true
							log.debug("updateShareInfo MAP ${message.id} MessageSendingStatePending")
							shareInfo.oldMapMessageId = message.id
							if (isOsmAndBot) {
								shareInfo.shouldSendViaBotMapMessage = false
							}
						} else if (state.constructor == TdApi.MessageSendingStateFailed.CONSTRUCTOR) {
							shareInfo.hasSharingError = true
							shareInfo.pendingMapMessage = false
							if (!isOsmAndBot) {
								shareInfo.pendingTdLibMap--
							}
							log.debug("updateShareInfo MAP ${message.id} MessageSendingStateFailed")
						}
					} else {
						shareInfo.currentMapMessageId = message.id
						shareInfo.pendingMapMessage = false
						shareInfo.lastMapSuccessfulSendTime = System.currentTimeMillis() / 1000
						if (!isOsmAndBot) {
							shareInfo.pendingTdLibMap--
							if (shareTypeValue == SHARE_TYPE_MAP) {
								shareInfo.sentMessages++
							}
						} else {
							shareInfo.shouldSendViaBotMapMessage = false
						}
						log.debug("updateShareInfo MAP ${message.id} SUCCESS pendingTdLibMap: ${shareInfo.pendingTdLibMap}")
					}
				}
				is TdApi.MessageText -> {
					val state = message.sendingState
					if (state != null) {
						if (state.constructor == TdApi.MessageSendingStatePending.CONSTRUCTOR) {
							log.debug("updateShareInfo TEXT ${message.id} MessageSendingStatePending")
							shareInfo.pendingTextMessage = true
							shareInfo.oldTextMessageId = message.id
							if (isOsmAndBot) {
								shareInfo.shouldSendViaBotTextMessage = false
							}
						} else if (state.constructor == TdApi.MessageSendingStateFailed.CONSTRUCTOR) {
							log.debug("updateShareInfo TEXT ${message.id} MessageSendingStateFailed")
							shareInfo.hasSharingError = true
							shareInfo.pendingTextMessage = false
							if (!isOsmAndBot) {
								shareInfo.pendingTdLibText--
							}
						}
					} else {
						shareInfo.currentTextMessageId = message.id
						shareInfo.updateTextMessageId++
						shareInfo.pendingTextMessage = false
						shareInfo.lastTextSuccessfulSendTime = System.currentTimeMillis() / 1000
						if (!isOsmAndBot) {
							shareInfo.pendingTdLibText--
							shareInfo.sentMessages++
						} else {
							shareInfo.shouldSendViaBotTextMessage = false
						}
						log.debug("updateShareInfo TEXT ${message.id} SUCCESS pendingTdLibMap: ${shareInfo.pendingTdLibText}")
					}
				}
			}
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
					chatsIds = newSharingStatus.chatsIds
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
			shareChatInfo.shouldSendViaBotTextMessage = true
			shareChatInfo.shouldSendViaBotMapMessage = true
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
			val currentTimeMillis = System.currentTimeMillis()
			val currentTime = currentTimeMillis / 1000
			statusChangeTime = currentTimeMillis
			val lm = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
			val gpsEnabled = try {
				if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
					val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
					val gpsActive = loc != null && ((currentTimeMillis - loc.time) / 1000) < GPS_UPDATE_EXPIRED_TIME
					val lastSentLocationExpired = ((currentTimeMillis - app.shareLocationHelper.lastLocationUpdateTime) / 1000) > GPS_UPDATE_EXPIRED_TIME
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
				val initTime = (currentTime - shareInfo.start) < SHARING_INITIALIZATION_TIME
				var initSending = false
				initSending = initSending || (shareInfo.lastTextSuccessfulSendTime == -1L && shareTypeValue != SHARE_TYPE_MAP)
				initSending = initSending || (shareInfo.lastMapSuccessfulSendTime == -1L && shareTypeValue != SHARE_TYPE_TEXT)
				if (initTime && initSending) {
					initializing = true
				} else {
					val textSharingError = !shareInfo.lastTextMessageHandled && currentTime - shareInfo.lastSendTextMessageTime > WAITING_TDLIB_TIME
					val mapSharingError = !shareInfo.lastMapMessageHandled && currentTime - shareInfo.lastSendMapMessageTime > WAITING_TDLIB_TIME
					if (shareInfo.hasSharingError
						|| (shareTypeValue == SHARE_TYPE_MAP_AND_TEXT && (textSharingError || mapSharingError))
						|| textSharingError && (shareTypeValue == SHARE_TYPE_TEXT)
						|| mapSharingError && (shareTypeValue == SHARE_TYPE_MAP)
					) {
						sendChatsErrors = true
						locationTime = Math.max(shareInfo.lastTextSuccessfulSendTime, shareInfo.lastMapSuccessfulSendTime)
						chatsIds.add(shareInfo.chatId)
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
						locationTime = app.shareLocationHelper.lastLocationUpdateTime / 1000
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
			shareChatsInfo[chatId]?.shouldSendViaBotMapMessage = true
			shareChatsInfo[chatId]?.shouldSendViaBotTextMessage = true
		}
		val currentTextMessageId = shareChatsInfo[chatId]?.currentTextMessageId
		if (messages.contains(currentTextMessageId)) {
			shareChatsInfo[chatId]?.currentTextMessageId = -1
			shareChatsInfo[chatId]?.updateTextMessageId = 1
			shareChatsInfo[chatId]?.shouldSendViaBotMapMessage = true
			shareChatsInfo[chatId]?.shouldSendViaBotTextMessage = true
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
		edit.putString(UTC_OFFSET_CONSTANTS_KEY, utcOffset)

		edit.putLong(SEND_MY_LOC_INTERVAL_KEY, sendMyLocInterval)
		edit.putLong(STALE_LOC_TIME_KEY, staleLocTime)
		edit.putLong(LOC_HISTORY_TIME_KEY, locHistoryTime)

		edit.putFloat(MIN_LOCATION_DISTANCE_KEY, minLocationDistance)
		edit.putFloat(MIN_LOCATION_ACCURACY_KEY, minLocationAccuracy)
		edit.putFloat(MIN_LOCATION_SPEED_KEY, minLocationSpeed)

		edit.putString(SHARE_TYPE_KEY, shareTypeValue)

		edit.putString(APP_TO_CONNECT_PACKAGE_KEY, appToConnectPackage)

		edit.putString(LIVE_NOW_SORT_TYPE_KEY, liveNowSortType.name)

		edit.putBoolean(BATTERY_OPTIMISATION_ASKED, batteryOptimisationAsked)

		edit.putBoolean(MONITORING_ENABLED, monitoringEnabled)

		edit.putBoolean(SHOW_GPS_POINTS, showGpsPoints)

		edit.putBoolean(PROXY_ENABLED, proxyEnabled)

		edit.putLong(BUFFER_TIME_KEY, bufferTime)

		edit.putLong(FREE_TIMELINE_INFO_SHOWN_TIME_KEY, freeTimelineInfoShownTime)

		val jArray = convertShareChatsInfoToJson()
		if (jArray != null) {
			edit.putString(SHARE_CHATS_INFO_KEY, jArray.toString())
		}

		val jsonObject = convertShareDevicesToJson()
		if (jsonObject != null) {
			edit.putString(SHARE_DEVICES_KEY, jsonObject.toString())
		}

		val jsonObjectProxy = convertProxyPrefToJson()
		if (jsonObjectProxy != null) {
			edit.putString(PROXY_PREFERENCES_KEY, jsonObjectProxy.toString())
		}

		val jsonArrayLiveTracks = convertLiveTracksInfoToJson()
		if (jsonArrayLiveTracks != null) {
			edit.putString(LIVE_TRACKS_KEY, jsonArrayLiveTracks.toString())
		}

		val jArrayLastInfo = convertLastChatsInfoToJson()
		if (jArrayLastInfo != null) {
			edit.putString(LAST_CHATS_INFO_KEY, jArrayLastInfo.toString())
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
		utcOffset = prefs.getString(UTC_OFFSET_CONSTANTS_KEY, DataConstants.UTC_FORMAT)

		try {
			parseShareChatsInfo(JSONArray(prefs.getString(SHARE_CHATS_INFO_KEY, "")))
		} catch (e: JSONException) {
			e.printStackTrace()
		}

		try {
			parseLastChatsInfo(JSONArray(prefs.getString(LAST_CHATS_INFO_KEY, "")))
		} catch (e: JSONException) {
			log.error(e)
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

		val minLocationDistanceDef = MIN_LOCATION_DISTANCE[MIN_LOCATION_DISTANCE_INDEX]
		minLocationDistance = prefs.getFloat(MIN_LOCATION_DISTANCE_KEY, minLocationDistanceDef)
		val minLocationPrecisionDef = MIN_LOCATION_ACCURACY[MIN_LOCATION_ACCURACY_INDEX]
		minLocationAccuracy = prefs.getFloat(MIN_LOCATION_ACCURACY_KEY, minLocationPrecisionDef)
		val minLocationSpeedDef = MIN_LOCATION_SPEED[MIN_LOCATION_SPEED_INDEX]
		minLocationSpeed = prefs.getFloat(MIN_LOCATION_SPEED_KEY, minLocationSpeedDef)

		val currentUserId = app.telegramHelper.getCurrentUserId()
		currentSharingMode = prefs.getString(SHARING_MODE_KEY, if (currentUserId != -1) currentUserId.toString() else "")

		val defPackage = if (AppConnect.getInstalledApps(app).size == 1) AppConnect.getInstalledApps(app).first().appPackage else ""
		appToConnectPackage = prefs.getString(APP_TO_CONNECT_PACKAGE_KEY, defPackage)

		liveNowSortType = LiveNowSortType.valueOf(
			prefs.getString(LIVE_NOW_SORT_TYPE_KEY, LiveNowSortType.SORT_BY_DISTANCE.name)
		)

		batteryOptimisationAsked = prefs.getBoolean(BATTERY_OPTIMISATION_ASKED,false)

		monitoringEnabled = prefs.getBoolean(MONITORING_ENABLED,false)

		showGpsPoints = prefs.getBoolean(SHOW_GPS_POINTS, false)

		proxyEnabled = prefs.getBoolean(PROXY_ENABLED, false)

		bufferTime = prefs.getLong(BUFFER_TIME_KEY, BUFFER_TIME[BUFFER_TIME_INDEX])

		freeTimelineInfoShownTime = prefs.getLong(FREE_TIMELINE_INFO_SHOWN_TIME_KEY, 0L)

		try {
			parseProxyPreferences(JSONObject(prefs.getString(PROXY_PREFERENCES_KEY, "")))
		} catch (e: JSONException) {
			log.error(e)
		}

		try {
			parseLiveTracks(JSONArray(prefs.getString(LIVE_TRACKS_KEY, "")))
		} catch (e: JSONException) {
			log.error(e)
		}
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

	private fun convertLiveTracksInfoToJson(): JSONArray? {
		return try {
			JSONArray().apply {
				liveTracksInfo.forEach { liveTrackInfo ->
					val obj = JSONObject()
					obj.put(LiveTrackInfo.USER_ID, liveTrackInfo.userId)
					obj.put(LiveTrackInfo.CHAT_ID, liveTrackInfo.chatId)
					obj.put(LiveTrackInfo.DEVICE_NAME, liveTrackInfo.deviceName)
					obj.put(LiveTrackInfo.COLOR_INDEX, liveTrackInfo.colorIndex)
					put(obj)
				}
			}
		} catch (e: JSONException) {
			log.error(e)
			null
		}
	}

	private fun convertProxyPrefToJson(): JSONObject? {
		return try {
			val proxyPref = currentProxyPref
			JSONObject().apply {
				put(ProxyPref.PROXY_ID, proxyPref.id)
				put(ProxyPref.TYPE_ID, proxyPref.type)
				put(ProxyPref.SERVER_ID, proxyPref.server)
				put(ProxyPref.PORT_ID, proxyPref.port)
				if (proxyPref is ProxyMTProtoPref) {
					put(ProxyMTProtoPref.KEY_ID, proxyPref.key)
				} else if (proxyPref is ProxySOCKS5Pref) {
					put(ProxySOCKS5Pref.LOGIN_ID, proxyPref.login)
					put(ProxySOCKS5Pref.PASSWORD_ID, proxyPref.password)
				}
			}
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
				obj.put(ShareChatInfo.LAST_TEXT_SUCCESSFUL_SEND_TIME_KEY, chatInfo.lastTextSuccessfulSendTime)
				obj.put(ShareChatInfo.LAST_MAP_SUCCESSFUL_SEND_TIME_KEY, chatInfo.lastMapSuccessfulSendTime)
				obj.put(ShareChatInfo.LAST_SEND_MAP_TIME_KEY, chatInfo.lastSendMapMessageTime)
				obj.put(ShareChatInfo.LAST_SEND_TEXT_TIME_KEY, chatInfo.lastSendTextMessageTime)
				obj.put(ShareChatInfo.PENDING_TEXT_MESSAGE_KEY, chatInfo.pendingTextMessage)
				obj.put(ShareChatInfo.PENDING_MAP_MESSAGE_KEY, chatInfo.pendingMapMessage)
				obj.put(ShareChatInfo.SENT_MESSAGES_KEY, chatInfo.sentMessages)
				jArray.put(obj)
			}
			jArray
		} catch (e: JSONException) {
			log.error(e)
			null
		}
	}

	private fun convertLastChatsInfoToJson(): JSONArray? {
		return try {
			val jArray = JSONArray()
			lastChatsInfo.forEach { lastInfo ->
				val obj = JSONObject()
				obj.put(LastChatInfo.CHAT_ID_KEY, lastInfo.chatId)
				obj.put(LastChatInfo.PERIOD_KEY, lastInfo.period)
				jArray.put(obj)
			}
			jArray
		} catch (e: JSONException) {
			log.error(e)
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
				lastTextSuccessfulSendTime = obj.optLong(ShareChatInfo.LAST_TEXT_SUCCESSFUL_SEND_TIME_KEY)
				lastMapSuccessfulSendTime = obj.optLong(ShareChatInfo.LAST_MAP_SUCCESSFUL_SEND_TIME_KEY)
				lastSendMapMessageTime = obj.optInt(ShareChatInfo.LAST_SEND_MAP_TIME_KEY)
				lastSendTextMessageTime = obj.optInt(ShareChatInfo.LAST_SEND_TEXT_TIME_KEY)
				pendingTextMessage = obj.optBoolean(ShareChatInfo.PENDING_TEXT_MESSAGE_KEY)
				pendingMapMessage = obj.optBoolean(ShareChatInfo.PENDING_MAP_MESSAGE_KEY)
				sentMessages = obj.optInt(ShareChatInfo.SENT_MESSAGES_KEY)
			}
			shareChatsInfo[shareInfo.chatId] = shareInfo
		}
	}

	private fun parseProxyPreferences(jsonObject: JSONObject) {
		val proxyId = jsonObject.optInt(ProxyPref.PROXY_ID)
		val typeString = jsonObject.optString(ProxyPref.TYPE_ID)
		val server = jsonObject.optString(ProxyPref.SERVER_ID)
		val port = jsonObject.optInt(ProxyPref.PORT_ID)
		val proxyPref = when {
			ProxyType.valueOf(typeString) == ProxyType.MTPROTO -> {
				val key = jsonObject.optString(ProxyMTProtoPref.KEY_ID)
				ProxyMTProtoPref(proxyId, server, port, key)
			}
			ProxyType.valueOf(typeString) == ProxyType.SOCKS5 -> {
				val login = jsonObject.optString(ProxySOCKS5Pref.LOGIN_ID)
				val password = jsonObject.optString(ProxySOCKS5Pref.PASSWORD_ID)
				ProxySOCKS5Pref(proxyId, server, port, login, password)
			}
			else -> null
		}
		if (proxyPref != null) {
			currentProxyPref = proxyPref
		}
	}

	private fun parseLiveTracks(json: JSONArray) {
		val list = mutableListOf<LiveTrackInfo>()
		for (i in 0 until json.length()) {
			val obj = json.getJSONObject(i)
			val userId = obj.optInt(LiveTrackInfo.USER_ID)
			val chatId = obj.optLong(LiveTrackInfo.CHAT_ID)
			val deviceName = obj.optString(LiveTrackInfo.DEVICE_NAME)
			val colorIndex = obj.optInt(LiveTrackInfo.COLOR_INDEX)

			list.add(LiveTrackInfo(userId, chatId, deviceName, colorIndex))
		}
		liveTracksInfo = list.toList()
	}

	private fun parseShareDevices(json: String) {
		shareDevices = OsmandApiUtils.parseJsonContents(json).toHashSet()
	}

	private fun parseLastChatsInfo(json: JSONArray) {
		for (i in 0 until json.length()) {
			val obj = json.getJSONObject(i)
			val lastInfo = LastChatInfo().apply {
				chatId = obj.optLong(LastChatInfo.CHAT_ID_KEY)
				period = obj.optLong(LastChatInfo.PERIOD_KEY)
			}
			lastChatsInfo.addLast(lastInfo)
		}
	}

	fun addTimePeriodToLastItem(id: Long, time: Long) {
		val lastInfo = lastChatsInfo.find { it.chatId == id }
		if (lastInfo == null) {
			addItemToSuggested(id, time)
		} else {
			val index = lastChatsInfo.indexOf(lastInfo)
			lastChatsInfo[index].period = time
		}
	}

	private fun addItemToSuggested(id: Long, time: Long) {
		val newLastInfo = LastChatInfo().apply {
			chatId = id
			period = time
		}
		if (lastChatsInfo.size < 5) {
			lastChatsInfo.addFirst(newLastInfo)
		} else {
			lastChatsInfo.removeLast()
			lastChatsInfo.addFirst(newLastInfo)
		}
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

	inner class SendMyLocPref : ListPreference(
		R.drawable.ic_action_share_location,
		R.string.send_my_location,
		R.string.send_my_location_desc
	) {

		override fun getCurrentValue() =
			OsmandFormatter.getFormattedDuration(app, sendMyLocInterval)

		override fun setCurrentValue(index: Int) {
			sendMyLocInterval = SEND_MY_LOC_VALUES_SEC[index]
			app.updateSendLocationInterval()
		}

		override fun getMenuItems() =
			SEND_MY_LOC_VALUES_SEC.map { OsmandFormatter.getFormattedDuration(app, it) }
	}

	inner class StaleLocPref : ListPreference(
		R.drawable.ic_action_time_span,
		R.string.stale_location,
		R.string.stale_location_desc
	) {

		override fun getCurrentValue() =
			OsmandFormatter.getFormattedDuration(app, staleLocTime)

		override fun setCurrentValue(index: Int) {
			staleLocTime = STALE_LOC_VALUES_SEC[index]
		}

		override fun getMenuItems() =
			STALE_LOC_VALUES_SEC.map { OsmandFormatter.getFormattedDuration(app, it) }
	}

	inner class LocHistoryPref : ListPreference(
		R.drawable.ic_action_location_history,
		R.string.location_history,
		R.string.location_history_desc
	) {

		override fun getCurrentValue() =
			OsmandFormatter.getFormattedDuration(app, locHistoryTime)

		override fun setCurrentValue(index: Int) {
			val value = LOC_HISTORY_VALUES_SEC[index]
			locHistoryTime = value
			app.telegramHelper.messageActiveTimeSec = value
		}

		override fun getMenuItems() =
			LOC_HISTORY_VALUES_SEC.map { OsmandFormatter.getFormattedDuration(app, it) }
	}

	inner class ShareTypePref : ListPreference(
		R.drawable.ic_action_location_history,
		R.string.send_location_as,
		R.string.send_location_as_descr
	) {

		override fun getCurrentValue(): String {
			return getTextValue(shareTypeValue)
		}

		override fun setCurrentValue(index: Int) {
			val newSharingType = SHARE_TYPE_VALUES[index]
			if (shareTypeValue != newSharingType && app.telegramHelper.getCurrentUser()?.id.toString() != currentSharingMode) {
				shareChatsInfo.forEach { (_, shareInfo) ->
					shareInfo.shouldSendViaBotTextMessage = true
					shareInfo.shouldSendViaBotMapMessage = true
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

	inner class MinLocationDistance :
		ListPreference(0, R.string.min_logging_distance, R.string.min_logging_distance_descr) {

		override fun getCurrentValue() = getFormattedValue(minLocationDistance)

		override fun setCurrentValue(index: Int) {
			minLocationDistance = MIN_LOCATION_DISTANCE[index]
		}

		override fun getMenuItems() = MIN_LOCATION_DISTANCE.map { getFormattedValue(it) }

		private fun getFormattedValue(value: Float): String {
			return if (value == 0f) app.getString(R.string.shared_string_select)
			else OsmandFormatter.getFormattedDistance(value, app)
		}
	}

	inner class MinLocationAccuracy :
		ListPreference(0, R.string.min_logging_accuracy, R.string.min_logging_accuracy_descr) {

		override fun getCurrentValue() = getFormattedValue(minLocationAccuracy)

		override fun setCurrentValue(index: Int) {
			minLocationAccuracy = MIN_LOCATION_ACCURACY[index]
		}

		override fun getMenuItems() = MIN_LOCATION_ACCURACY.map { getFormattedValue(it) }

		private fun getFormattedValue(value: Float): String {
			return if (value == 0f) app.getString(R.string.shared_string_select)
			else OsmandFormatter.getFormattedDistance(value, app)
		}
	}

	inner class MinLocationSpeed :
		ListPreference(0, R.string.min_logging_speed, R.string.min_logging_speed_descr) {

		override fun getCurrentValue() = getFormattedValue(minLocationSpeed)

		override fun setCurrentValue(index: Int) {
			minLocationSpeed = MIN_LOCATION_SPEED[index]
		}

		override fun getMenuItems() = MIN_LOCATION_SPEED.map { getFormattedValue(it) }

		private fun getFormattedValue(value: Float): String {
			return when (value) {
				MIN_LOCATION_SPEED[0] -> app.getString(R.string.shared_string_select)
				MIN_LOCATION_SPEED[1] -> "> 0"
				else -> OsmandFormatter.getFormattedSpeed(value, app)
			}
		}
	}

	inner class UnitsOfSpeed : ListPreference(
		R.drawable.ic_action_speed,
		R.string.unit_of_speed_system,
		R.string.unit_of_speed_system_descr
	) {

		override fun getCurrentValue() = speedConstants.toShortString(app)

		override fun setCurrentValue(index: Int) {
			speedConstants = SpeedConstants.values()[index]
		}

		override fun getMenuItems() = SpeedConstants.values().map { it.toShortString(app) }
	}

	inner class UnitsOfLength : ListPreference(
		R.drawable.ic_action_ruler_unit, R.string.unit_of_length,
		R.string.unit_of_length_descr
	) {

		override fun getCurrentValue() = metricsConstants.toHumanString(app)

		override fun setCurrentValue(index: Int) {
			metricsConstants = MetricsConstants.values()[index]
		}

		override fun getMenuItems() = MetricsConstants.values().map { it.toHumanString(app) }
	}

	inner class UtcOffset : ListPreference(
		R.drawable.ic_world_globe_dark, R.string.time_zone,
		R.string.time_zone_descr
	) {
		private val formattedUtcOffsets = DataConstants.utcOffsets.keys.toList()

		override fun getCurrentValue() = utcOffset

		override fun setCurrentValue(index: Int) {
			utcOffset = formattedUtcOffsets[index]
		}

		override fun getMenuItems() = formattedUtcOffsets
	}

	inner class BufferTimePref : ListPreference(R.drawable.ic_action_time_span, R.string.buffer_time,
			R.string.buffer_time_descr) {
		override fun getCurrentValue() = OsmandFormatter.getFormattedDuration(app, bufferTime)

		override fun setCurrentValue(index: Int) {
			bufferTime = BUFFER_TIME[index]
		}

		override fun getMenuItems(): List<String> {
			return BUFFER_TIME.map { OsmandFormatter.getFormattedDuration(app, it) }
		}
	}

	abstract inner class ListPreference(
		@DrawableRes val iconId: Int,
		@StringRes val titleId: Int,
		@StringRes val descriptionId: Int
	) {

		abstract fun getCurrentValue(): String

		abstract fun setCurrentValue(index: Int)

		abstract fun getMenuItems(): List<String>
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

			@DrawableRes
			fun getIconId(appPackage: String): Int {
				for (item in values()) {
					if (item.appPackage == appPackage) {
						return item.iconId
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

	data class LiveTrackInfo(val userId: Int, val chatId: Long, val deviceName: String, val colorIndex: Int) {
		companion object {

			internal const val USER_ID = "userId"
			internal const val CHAT_ID = "chatId"
			internal const val DEVICE_NAME = "deviceName"
			internal const val COLOR_INDEX = "colorIndex"
		}
	}

	enum class ProxyType {
		MTPROTO, SOCKS5
	}

	abstract class ProxyPref(
		var id: Int,
		var type: ProxyType,
		open var server: String,
		open var port: Int
	) {
		companion object {
			internal const val PROXY_ID = "proxyId"
			internal const val TYPE_ID = "type"
			internal const val SERVER_ID = "serverId"
			internal const val PORT_ID = "portId"
		}
	}

	class ProxyMTProtoPref(id: Int, server: String, port: Int, var key: String) :
		ProxyPref(id, ProxyType.MTPROTO, server, port) {
		companion object {
			internal const val KEY_ID = "key"
		}
	}

	class ProxySOCKS5Pref(
		id: Int,
		server: String,
		port: Int,
		var login: String,
		var password: String
	) :
		ProxyPref(id, ProxyType.SOCKS5, server, port) {
		companion object {
			internal const val LOGIN_ID = "login"
			internal const val PASSWORD_ID = "password"
		}
	}

	class SharingStatus {

		var title: String = ""
		var description: String = ""
		var locationTime: Long = -1
		var statusChangeTime: Long = -1
		var chatsIds: MutableList<Long> = mutableListOf()

		lateinit var statusType: SharingStatusType

		fun getTitle(app: TelegramApplication): CharSequence {
			return if (statusType != SharingStatusType.NOT_POSSIBLE_TO_SENT_TO_CHATS || chatsIds.isEmpty()) {
				title
			} else {
				val spannableString = SpannableStringBuilder(title)
				val iterator = chatsIds.iterator()
				while (iterator.hasNext()) {
					val chatId = iterator.next()
					val chatTitle = app.telegramHelper.getChat(chatId)?.title
					if (chatTitle != null) {
						val start = spannableString.length
						val newSpannable = if (iterator.hasNext()) " @$chatTitle," else " @$chatTitle."
						spannableString.append(newSpannable)
						spannableString.setSpan(ForegroundColorSpan(app.uiUtils.getActiveColor()), start, spannableString.length - 1, 0)
					}
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
		var lastTextSuccessfulSendTime = -1L
		var lastMapSuccessfulSendTime = -1L
		var lastSendTextMessageTime = -1
			set(value) {
				field = value
				lastTextMessageHandled = false
			}
		var lastSendMapMessageTime = -1
			set(value) {
				field = value
				lastMapMessageHandled = false
			}
		var sentMessages = 0
		var pendingTdLibText = 0
		var pendingTdLibMap = 0
		var pendingTextMessage = false
		var pendingMapMessage = false
		var shouldSendViaBotTextMessage = false
		var shouldSendViaBotMapMessage = false
		var hasSharingError = false
		var additionalActiveTime = ADDITIONAL_ACTIVE_TIME_VALUES_SEC[0]
		var lastMapMessageHandled = false
		var lastTextMessageHandled = false

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
			internal const val LAST_MAP_SUCCESSFUL_SEND_TIME_KEY = "lastMapSuccessfulSendTime"
			internal const val LAST_TEXT_SUCCESSFUL_SEND_TIME_KEY = "lastTextSuccessfulSendTime"
			internal const val LAST_SEND_MAP_TIME_KEY = "lastSendMapMessageTime"
			internal const val LAST_SEND_TEXT_TIME_KEY = "lastSendTextMessageTime"
			internal const val PENDING_TEXT_MESSAGE_KEY = "pendingTextMessage"
			internal const val PENDING_MAP_MESSAGE_KEY = "pendingMapMessage"
			internal const val SENT_MESSAGES_KEY = "sentMessages"
		}
	}

	class LastChatInfo {

		var chatId = -1L
		var period = -1L

		companion object {
			internal const val CHAT_ID_KEY = "chatId"
			internal const val PERIOD_KEY = "period"
		}
	}
}
