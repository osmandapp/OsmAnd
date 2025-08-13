package net.osmand.telegram.helpers

import android.text.TextUtils
import net.osmand.PlatformUtil
import net.osmand.telegram.SHARE_TYPE_MAP
import net.osmand.telegram.SHARE_TYPE_MAP_AND_TEXT
import net.osmand.telegram.SHARE_TYPE_TEXT
import net.osmand.telegram.TelegramSettings
import net.osmand.telegram.TelegramSettings.ShareChatInfo
import net.osmand.telegram.helpers.TelegramHelper.TelegramAuthParamType.CODE
import net.osmand.telegram.helpers.TelegramHelper.TelegramAuthParamType.PASSWORD
import net.osmand.telegram.helpers.TelegramHelper.TelegramAuthParamType.PHONE_NUMBER
import net.osmand.telegram.utils.GRAYSCALE_PHOTOS_DIR
import net.osmand.telegram.utils.GRAYSCALE_PHOTOS_EXT
import net.osmand.telegram.utils.OsmandLocationUtils
import net.osmand.telegram.utils.OsmandLocationUtils.DEVICE_PREFIX
import net.osmand.telegram.utils.OsmandLocationUtils.USER_TEXT_LOCATION_TITLE
import net.osmand.util.Algorithms
import org.drinkless.tdlib.Client
import org.drinkless.tdlib.Client.ResultHandler
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.AuthorizationState
import org.drinkless.tdlib.TdApi.User
import java.io.File
import java.util.TreeSet
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


class TelegramHelper private constructor() {

	companion object {
		const val OSMAND_BOT_USERNAME = "osmand_bot"

		private val log = PlatformUtil.getLog(TelegramHelper::class.java)
		private const val CHATS_LIMIT = 100
		private const val NOT_FOUND_ERROR_CODE = 404
		private const val IGNORED_ERROR_CODE = 406
		private const val MESSAGE_CANNOT_BE_EDITED_ERROR_CODE = 5

		private const val MAX_SEARCH_ITEMS = Int.MAX_VALUE

		// min and max values for the Telegram API
		const val MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC = 61
		const val MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC = 60 * 60 * 24 - 1 // one day

		const val MAX_LOCATION_MESSAGE_HISTORY_SCAN_SEC = 60 * 60 * 24 // one day

		const val MESSAGE_TYPE_MAP = 1
		const val MESSAGE_TYPE_TEXT = 2
		const val MESSAGE_TYPE_BOT = 3

		private var helper: TelegramHelper? = null

		val instance: TelegramHelper
			get() {
				if (helper == null) {
					helper = TelegramHelper()
				}
				return helper!!
			}
	}

	var messageActiveTimeSec: Long = 0

	var lastTelegramUpdateTime: Int = 0

	private val users = ConcurrentHashMap<Long, TdApi.User>()
	private val contacts = ConcurrentHashMap<Long, TdApi.User>()
	private val basicGroups = ConcurrentHashMap<Long, TdApi.BasicGroup>()
	private val supergroups = ConcurrentHashMap<Long, TdApi.Supergroup>()
	private val secretChats = ConcurrentHashMap<Int, TdApi.SecretChat>()

	private val chats = ConcurrentHashMap<Long, TdApi.Chat>()
	private val chatList = TreeSet<OrderedChat>()

	private val downloadChatFilesMap = ConcurrentHashMap<String, TdApi.Chat>()
	private val downloadUserFilesMap = ConcurrentHashMap<String, TdApi.User>()

	// value.content can be TdApi.MessageLocation or MessageOsmAndBotLocation
	private val usersLocationMessages = ConcurrentHashMap<Long, TdApi.Message>()

	private val usersFullInfo = ConcurrentHashMap<Long, TdApi.UserFullInfo>()
	private val basicGroupsFullInfo = ConcurrentHashMap<Long, TdApi.BasicGroupFullInfo>()
	private val supergroupsFullInfo = ConcurrentHashMap<Long, TdApi.SupergroupFullInfo>()

	var appDir: String? = null
	private var libraryLoaded = false
	private var telegramAuthorizationRequestHandler: TelegramAuthorizationRequestHandler? = null

	private var client: Client? = null
	private var currentUser: TdApi.User? = null
	private var osmandBot: TdApi.User? = null

	private var hasSuccessfulChatsLoad: Boolean = false
	private var needRefreshActiveLiveLocationMessages: Boolean = true
	private var requestingActiveLiveLocationMessages: Boolean = false

	private var authorizationState: AuthorizationState? = null
	private var haveAuthorization = false

	private val defaultHandler = DefaultHandler()

	private var updateLiveMessagesExecutor: ScheduledExecutorService? = null

	var listener: TelegramListener? = null
	private val incomingMessagesListeners = HashSet<TelegramIncomingMessagesListener>()
	private val outgoingMessagesListeners = HashSet<TelegramOutgoingMessagesListener>()
	private val fullInfoUpdatesListeners = HashSet<FullInfoUpdatesListener>()
	private val searchListeners = HashSet<TelegramSearchListener>()

	fun addIncomingMessagesListener(listener: TelegramIncomingMessagesListener) {
		incomingMessagesListeners.add(listener)
	}

	fun removeIncomingMessagesListener(listener: TelegramIncomingMessagesListener) {
		incomingMessagesListeners.remove(listener)
	}

	fun addOutgoingMessagesListener(listener: TelegramOutgoingMessagesListener) {
		outgoingMessagesListeners.add(listener)
	}

	fun removeOutgoingMessagesListener(listener: TelegramOutgoingMessagesListener) {
		outgoingMessagesListeners.remove(listener)
	}

	fun addFullInfoUpdatesListener(listener: FullInfoUpdatesListener) {
		fullInfoUpdatesListeners.add(listener)
	}

	fun removeFullInfoUpdatesListener(listener: FullInfoUpdatesListener) {
		fullInfoUpdatesListeners.remove(listener)
	}

	fun addSearchListener(listener: TelegramSearchListener) {
		searchListeners.add(listener)
	}

	fun removeSearchListener(listener: TelegramSearchListener) {
		searchListeners.remove(listener)
	}

	fun getChatList(): TreeSet<OrderedChat> {
		synchronized(chatList) {
			return TreeSet(chatList.filter { !it.isChannel })
		}
	}

	fun getChatListIds() = getChatList().map { it.chatId }

	fun getContacts() = contacts

	fun getChatIds() = chats.keys().toList()

	fun getChat(id: Long) = chats[id]

	fun getUser(id: Long) = if (id == getCurrentUserId()) currentUser else users[id]

	fun getOsmandBot() = osmandBot

	fun getCurrentUser() = currentUser

	fun getCurrentUserId() = currentUser?.id ?: -1

	fun getUserMessage(user: TdApi.User) =
		usersLocationMessages.values.firstOrNull { OsmandLocationUtils.getSenderMessageId(it) == user.id }

	fun getChatMessages(chatId: Long) =
		usersLocationMessages.values.filter { it.chatId == chatId }

	fun getMessages() = usersLocationMessages.values.toList()

	fun getMessagesByChatIds(messageExpTime: Long): Map<Long, List<TdApi.Message>> {
		val res = mutableMapOf<Long, MutableList<TdApi.Message>>()
		for (message in usersLocationMessages.values) {
			if (System.currentTimeMillis() / 1000 - OsmandLocationUtils.getLastUpdatedTime(message) < messageExpTime) {
				var messages = res[message.chatId]
				if (messages != null) {
					messages.add(message)
				} else {
					messages = mutableListOf(message)
					res[message.chatId] = messages
				}
			}
		}
		return res
	}

	fun getBasicGroupFullInfo(id: Long): TdApi.BasicGroupFullInfo? {
		val res = basicGroupsFullInfo[id]
		if (res == null) {
			requestBasicGroupFullInfo(id)
		}
		return res
	}

	fun getSupergroupFullInfo(id: Long): TdApi.SupergroupFullInfo? {
		val res = supergroupsFullInfo[id]
		if (res == null) {
			requestSupergroupFullInfo(id)
		}
		return res
	}

	fun isGroup(chat: TdApi.Chat): Boolean {
		return chat.type is TdApi.ChatTypeSupergroup || chat.type is TdApi.ChatTypeBasicGroup
	}

	fun isPrivateChat(chat: TdApi.Chat): Boolean = chat.type is TdApi.ChatTypePrivate

	fun isSecretChat(chat: TdApi.Chat): Boolean = chat.type is TdApi.ChatTypeSecret

	fun isChannel(chat: TdApi.Chat): Boolean {
		return chat.type is TdApi.ChatTypeSupergroup && (chat.type as TdApi.ChatTypeSupergroup).isChannel
	}

	enum class TelegramAuthParamType {
		PHONE_NUMBER,
		CODE,
		PASSWORD
	}

	enum class TelegramAuthorizationState {
		UNKNOWN,
		WAIT_PARAMETERS,
		WAIT_PHONE_NUMBER,
		WAIT_CODE,
		WAIT_PASSWORD,
		READY,
		LOGGING_OUT,
		CLOSING,
		CLOSED
	}

	interface TelegramListener {
		fun onTelegramStatusChanged(prevTelegramAuthorizationState: TelegramAuthorizationState,
									newTelegramAuthorizationState: TelegramAuthorizationState)

		fun onTelegramChatsRead()
		fun onTelegramChatsChanged()
		fun onTelegramChatChanged(chat: TdApi.Chat)
		fun onTelegramChatCreated(chat: TdApi.Chat)
		fun onTelegramUserChanged(user: TdApi.User)
		fun onTelegramError(code: Int, message: String)
	}

	interface TelegramIncomingMessagesListener {
		fun onReceiveChatLocationMessages(chatId: Long, vararg messages: TdApi.Message)
		fun onDeleteChatLocationMessages(chatId: Long, messages: List<TdApi.Message>)
		fun updateLocationMessages()
	}

	interface TelegramOutgoingMessagesListener {
		fun onUpdateMessages(messages: List<TdApi.Message>)
		fun onDeleteMessages(chatId: Long, messages: List<Long>)
		fun onSendLiveLocationError(code: Int, message: String, shareInfo: ShareChatInfo, messageType: Int)
	}

	interface FullInfoUpdatesListener {
		fun onBasicGroupFullInfoUpdated(groupId: Long, info: TdApi.BasicGroupFullInfo)
		fun onSupergroupFullInfoUpdated(groupId: Long, info: TdApi.SupergroupFullInfo)
	}

	interface TelegramAuthorizationRequestListener {
		fun onRequestTelegramAuthenticationParameter(parameterType: TelegramAuthParamType)
		fun onTelegramAuthorizationRequestError(code: Int, message: String)
		fun onTelegramUnsupportedAuthorizationState(authorizationState: String)
	}

	interface TelegramSearchListener {
		fun onSearchChatsFinished(obj: TdApi.Chats)
		fun onSearchPublicChatsFinished(obj: TdApi.Chats)
		fun onSearchContactsFinished(obj: TdApi.Users)
	}

	inner class TelegramAuthorizationRequestHandler(val telegramAuthorizationRequestListener: TelegramAuthorizationRequestListener) {

		fun applyAuthParam(type: TelegramAuthParamType, value: String) {
			if (TextUtils.isEmpty(value)) return
			log.info("Authorization: apply parameter ${type.name}")
			when (type) {
				PHONE_NUMBER -> client!!.send(
					TdApi.SetAuthenticationPhoneNumber(
						value,
						TdApi.PhoneNumberAuthenticationSettings()
					), AuthorizationRequestHandler()
				)
				CODE -> client!!.send(TdApi.CheckAuthenticationCode(value), AuthorizationRequestHandler())
				PASSWORD -> client!!.send(TdApi.CheckAuthenticationPassword(value), AuthorizationRequestHandler())
			}
		}
	}

	fun getTelegramAuthorizationState(): TelegramAuthorizationState {
		val authorizationState = this.authorizationState
				?: return TelegramAuthorizationState.UNKNOWN
		return when (authorizationState.constructor) {
			TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> TelegramAuthorizationState.WAIT_PARAMETERS
			TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> TelegramAuthorizationState.WAIT_PHONE_NUMBER
			TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> TelegramAuthorizationState.WAIT_CODE
			TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> TelegramAuthorizationState.WAIT_PASSWORD
			TdApi.AuthorizationStateReady.CONSTRUCTOR -> TelegramAuthorizationState.READY
			TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> TelegramAuthorizationState.LOGGING_OUT
			TdApi.AuthorizationStateClosing.CONSTRUCTOR -> TelegramAuthorizationState.CLOSING
			TdApi.AuthorizationStateClosed.CONSTRUCTOR -> TelegramAuthorizationState.CLOSED
			else -> TelegramAuthorizationState.UNKNOWN
		}
	}

	fun setTelegramAuthorizationRequestHandler(telegramAuthorizationRequestListener: TelegramAuthorizationRequestListener): TelegramAuthorizationRequestHandler {
		val handler = TelegramAuthorizationRequestHandler(telegramAuthorizationRequestListener)
		this.telegramAuthorizationRequestHandler = handler
		return handler
	}

	init {
		try {
			log.debug("Loading native tdlib...")
			System.loadLibrary("tdjni")
			Client.setLogMessageHandler(1) { level, message -> log.debug("$level: $message") }
			libraryLoaded = true
		} catch (e: Throwable) {
			log.error("Failed to load tdlib", e)
		}
	}

	fun init(): Boolean {
		return if (libraryLoaded) {
			// create client
			client = Client.create(UpdatesHandler(), null, null)
			true
		} else {
			false
		}
	}

	fun requestAuthorizationState() {
		client?.send(TdApi.GetAuthorizationState()) { obj ->
			if (obj is TdApi.AuthorizationState) {
				onAuthorizationStateUpdated(obj, true)
			}
		}
	}

	fun networkChange(networkType: TdApi.NetworkType) {
		client?.send(TdApi.SetNetworkType(networkType)) { obj ->
			log.debug(obj)
		}
	}

	fun isInit() = client != null && haveAuthorization

	fun getUserPhotoPath(user: TdApi.User?) = when {
		user == null -> null
		hasLocalUserPhoto(user) -> user.profilePhoto?.small?.local?.path
		else -> {
			if (hasRemoteUserPhoto(user)) {
				requestUserPhoto(user)
			}
			null
		}
	}

	fun getUserGreyPhotoPath(user: TdApi.User?) = when {
		user == null -> null
		hasGrayscaleUserPhoto(user.id) -> "$appDir/$GRAYSCALE_PHOTOS_DIR${user.id}$GRAYSCALE_PHOTOS_EXT"
		else -> null
	}

	fun getUserIdFromChatType(type: TdApi.ChatType) = when (type) {
		is TdApi.ChatTypePrivate -> type.userId
		is TdApi.ChatTypeSecret -> type.userId
		else -> 0
	}

	fun isOsmAndBot(userId: Long) = users[userId]?.getName() == OSMAND_BOT_USERNAME

	fun isBot(userId: Long) = users[userId]?.type is TdApi.UserTypeBot

	fun startLiveMessagesUpdates(interval: Long) {
		stopLiveMessagesUpdates()

		val updateLiveMessagesExecutor = Executors.newSingleThreadScheduledExecutor()
		this.updateLiveMessagesExecutor = updateLiveMessagesExecutor
		updateLiveMessagesExecutor.scheduleWithFixedDelay({
			incomingMessagesListeners.forEach { it.updateLocationMessages() }
		}, interval, interval, TimeUnit.SECONDS)
	}

	fun stopLiveMessagesUpdates() {
		updateLiveMessagesExecutor?.shutdown()
		updateLiveMessagesExecutor?.awaitTermination(1, TimeUnit.MINUTES)
	}

	fun hasGrayscaleUserPhoto(userId: Long): Boolean {
		return File("$appDir/$GRAYSCALE_PHOTOS_DIR$userId$GRAYSCALE_PHOTOS_EXT").exists()
	}

	private fun hasLocalUserPhoto(user: TdApi.User): Boolean {
		val localPhoto = user.profilePhoto?.small?.local
		return if (localPhoto != null) {
			localPhoto.canBeDownloaded && localPhoto.isDownloadingCompleted && localPhoto.path.isNotEmpty()
		} else {
			false
		}
	}

	private fun hasRemoteUserPhoto(user: TdApi.User): Boolean {
		val remotePhoto = user.profilePhoto?.small?.remote
		return remotePhoto?.id?.isNotEmpty() ?: false
	}

	private fun requestUserPhoto(user: TdApi.User) {
		val remotePhoto = user.profilePhoto?.small?.remote
		if (remotePhoto != null && remotePhoto.id.isNotEmpty()) {
			downloadUserFilesMap[remotePhoto.id] = user
			client!!.send(TdApi.GetRemoteFile(remotePhoto.id, null)) { obj ->
				when (obj.constructor) {
					TdApi.Error.CONSTRUCTOR -> {
						val error = obj as TdApi.Error
						val code = error.code
						if (code != IGNORED_ERROR_CODE) {
							listener?.onTelegramError(code, error.message)
						}
					}
					TdApi.File.CONSTRUCTOR -> {
						val file = obj as TdApi.File
						client!!.send(TdApi.DownloadFile(file.id, 10, 0, 0, true), defaultHandler)
					}
					else -> listener?.onTelegramError(-1, "Receive wrong response from TDLib: $obj")
				}
			}
		}
	}

	private fun requestChats(reload: Boolean = false, onComplete: (() -> Unit)?) {
		synchronized(chatList) {
			if (reload) {
				chatList.clear()
				hasSuccessfulChatsLoad = false
			}
			if (chatList.size < CHATS_LIMIT) {
				client?.send(TdApi.LoadChats(TdApi.ChatListMain(), Int.MAX_VALUE)) { obj ->
					when (obj.constructor) {
						TdApi.Error.CONSTRUCTOR -> {
							synchronized(chatList) {
								val error = obj as TdApi.Error
								val loadedAllChats = hasSuccessfulChatsLoad && error.code == NOT_FOUND_ERROR_CODE
								if (!loadedAllChats && error.code != IGNORED_ERROR_CODE) {
									listener?.onTelegramError(error.code, error.message)
								}
							}
						}
						TdApi.Ok.CONSTRUCTOR -> {
							synchronized(chatList) {
								hasSuccessfulChatsLoad = true
							}
							// Some chats are received through updates, try to load more chats
							requestChats(false, this@TelegramHelper::scanChatsHistory)
							onComplete?.invoke()
						}
						else -> listener?.onTelegramError(-1, "Receive wrong response from TDLib: $obj")
					}
				}
				return
			}
		}
		listener?.onTelegramChatsRead()
	}

	private fun requestBasicGroupFullInfo(id: Long) {
		client?.send(TdApi.GetBasicGroupFullInfo(id)) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.BasicGroupFullInfo.CONSTRUCTOR -> {
					val info = obj as TdApi.BasicGroupFullInfo
					basicGroupsFullInfo[id] = info
					fullInfoUpdatesListeners.forEach { it.onBasicGroupFullInfoUpdated(id, info) }
				}
			}
		}
	}

	fun sendViaBotLocationMessage(userId: Long, shareInfo: ShareChatInfo, location: TdApi.Location, device: TelegramSettings.DeviceBot, shareType:String) {
		log.debug("sendViaBotLocationMessage - ${shareInfo.chatId}")
		client?.send(TdApi.GetInlineQueryResults(userId, shareInfo.chatId, location, device.deviceName, "")) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
						shareInfo.shouldSendViaBotTextMessage = true
						shareInfo.shouldSendViaBotMapMessage = true
					}
				}
				TdApi.InlineQueryResults.CONSTRUCTOR -> {
					sendViaBotMessageFromQueryResults(shareInfo, obj as TdApi.InlineQueryResults, device.externalId, shareType)
				}
			}
		}
	}

	private fun sendViaBotMessageFromQueryResults(
		shareInfo: ShareChatInfo,
		inlineQueryResults: TdApi.InlineQueryResults,
		deviceId: String,
		shareType: String
	) {
		val queryResults = inlineQueryResults.results.asList()
		if (queryResults.isNotEmpty()) {
			val resultArticles = mutableListOf<TdApi.InlineQueryResultArticle>()
			queryResults.forEach {
				if (it is TdApi.InlineQueryResultArticle && it.id.substring(1) == deviceId) {
					val textLocationArticle = it.id.startsWith("t")
					val mapLocationArticle = it.id.startsWith("m")
					if (shareType == SHARE_TYPE_MAP && mapLocationArticle
						|| shareType == SHARE_TYPE_TEXT && textLocationArticle
						|| shareType == SHARE_TYPE_MAP_AND_TEXT && (textLocationArticle || mapLocationArticle)) {
						resultArticles.add(it)
					}
				}
			}
			resultArticles.forEach {
				shareInfo.lastTextMessageHandled = false
				val sendOptions = TdApi.MessageSendOptions(true, true, true, true, null, 0, 0, false)
				client?.send(TdApi.SendInlineQueryResultMessage(shareInfo.chatId, 0, null, sendOptions,
					inlineQueryResults.inlineQueryId, it.id, false)) { obj ->
					handleTextLocationMessageUpdate(obj, shareInfo, true)
				}
			}
		}
	}

	private fun requestSupergroupFullInfo(id: Long) {
		client?.send(TdApi.GetSupergroupFullInfo(id)) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.SupergroupFullInfo.CONSTRUCTOR -> {
					val info = obj as TdApi.SupergroupFullInfo
					supergroupsFullInfo[id] = info
					fullInfoUpdatesListeners.forEach { it.onSupergroupFullInfoUpdated(id, info) }
				}
			}
		}
	}

	private fun requestCurrentUser(){
		client?.send(TdApi.GetMe()) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.User.CONSTRUCTOR -> {
					val currUser = obj as TdApi.User
					currentUser = currUser
					if (!hasLocalUserPhoto(currUser) && hasRemoteUserPhoto(currUser)) {
						requestUserPhoto(currUser)
					}
				}
			}
		}
	}

	private fun requestContacts(){
		client?.send(TdApi.GetContacts()) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.Users.CONSTRUCTOR -> {
					val usersIds = obj as TdApi.Users
					usersIds.userIds.forEach {
						requestUser(it)
					}
				}
			}
		}
	}

	fun scanChatsHistory() {
		log.debug("scanChatsHistory: chatList: ${chatList.size}")
		chatList.forEach {
			scanChatHistory(it.chatId, 0, 0, 100, mutableListOf<TdApi.Message>())
		}
	}

	private fun scanChatHistory(
		chatId: Long,
		fromMessageId: Long,
		offset: Int,
		limit: Int,
		locations: MutableList<TdApi.Message>
	) {
		client?.send(TdApi.GetChatHistory(chatId, fromMessageId, offset, limit, false)) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.Messages.CONSTRUCTOR -> {
					val messages = (obj as TdApi.Messages).messages
					log.debug("scanChatHistory: chatId: $chatId fromMessageId: $fromMessageId size: ${messages.size}")
					if (messages.isNotEmpty()) {
						locations.addAll(messages.filter { it.isAppropriate() && !it.isOutgoing })
						val lastMessage = messages.last()
						val currentTime = System.currentTimeMillis() / 1000
						if (currentTime - Math.max(lastMessage.date, lastMessage.editDate) < MAX_LOCATION_MESSAGE_HISTORY_SCAN_SEC) {
							scanChatHistory(chatId, lastMessage.id, 0, 100, locations)
							log.debug("scanChatHistory searchMessageId: ${lastMessage.id}")
						} else {
							log.debug("scanChatHistory finishForChat: $chatId")
							processScannedLocationsForChat(chatId, locations)
						}
					} else {
						log.debug("scanChatHistory finishForChat: $chatId")
						processScannedLocationsForChat(chatId, locations)
					}
				}
			}
		}
	}

	private fun processScannedLocationsForChat(chatId: Long, locations: MutableList<TdApi.Message>) {
		if (locations.isNotEmpty()) {
			locations.sortBy { message -> OsmandLocationUtils.getLastUpdatedTime(message) }
			locations.forEach {
				updateLastMessage(it)
			}
			incomingMessagesListeners.forEach {
				it.onReceiveChatLocationMessages(chatId, *locations.toTypedArray())
			}
		}
	}

	fun requestUser(id: Long) {
		client?.send(TdApi.GetUser(id)) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.User.CONSTRUCTOR -> {
					val user = obj as TdApi.User
					contacts[user.id] = user
					if (!hasLocalUserPhoto(user) && hasRemoteUserPhoto(user)) {
						requestUserPhoto(user)
					}
				}
			}
		}
	}

	fun requestChat(id: Long) {
		client?.send(TdApi.GetChat(id)) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.Chat.CONSTRUCTOR -> {
					val chat = obj as TdApi.Chat
					chats[chat.id] = chat
					listener?.onTelegramChatChanged(chat)
				}
			}
		}
	}

	fun disableProxy() {
		client?.send(TdApi.DisableProxy()) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.Ok.CONSTRUCTOR -> {
				}
			}
		}
	}

	fun enableProxy(proxyId: Int) {
		client?.send(TdApi.EnableProxy(proxyId)) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.Ok.CONSTRUCTOR -> {
				}
			}
		}
	}

	fun addProxyPref(proxyPref: TelegramSettings.ProxyPref, enable: Boolean) {
		val proxyType: TdApi.ProxyType? = when (proxyPref) {
			is TelegramSettings.ProxyMTProtoPref -> TdApi.ProxyTypeMtproto(proxyPref.key)
			is TelegramSettings.ProxySOCKS5Pref -> TdApi.ProxyTypeSocks5(proxyPref.login, proxyPref.password)
			else -> null
		}
		client?.send(TdApi.AddProxy(proxyPref.server, proxyPref.port, enable, proxyType)) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.Proxy.CONSTRUCTOR -> {
					val proxy = (obj as TdApi.Proxy)
					proxyPref.id = proxy.id
				}
			}
		}
	}

	fun editProxyPref(proxyPref: TelegramSettings.ProxyPref, enable: Boolean) {
		val proxyType: TdApi.ProxyType? = when (proxyPref) {
			is TelegramSettings.ProxyMTProtoPref -> TdApi.ProxyTypeMtproto(proxyPref.key)
			is TelegramSettings.ProxySOCKS5Pref -> TdApi.ProxyTypeSocks5(proxyPref.login, proxyPref.password)
			else -> null
		}
		client?.send(TdApi.EditProxy(proxyPref.id, proxyPref.server, proxyPref.port, enable, proxyType)) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.Proxy.CONSTRUCTOR -> {
					val proxy = (obj as TdApi.Proxy)
					proxyPref.id = proxy.id
				}
			}
		}
	}

	fun createPrivateChatWithUser(
		userId: Long,
		shareInfo: ShareChatInfo,
		shareChatsInfo: ConcurrentHashMap<Long, ShareChatInfo>
	) {
		client?.send(TdApi.CreatePrivateChat(userId, false)) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					log.debug("createPrivateChatWithUser ERROR $obj")
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						shareInfo.hasTextSharingError = true
						shareInfo.hasMapSharingError = true
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.Chat.CONSTRUCTOR -> {
					shareInfo.chatId = (obj as TdApi.Chat).id
					shareChatsInfo[shareInfo.chatId] = shareInfo
					listener?.onTelegramChatCreated(obj)
				}
			}
		}
	}

	private fun requestMessage(chatId: Long, messageId: Long, onComplete: (TdApi.Message) -> Unit) {
		client?.send(TdApi.GetMessage(chatId, messageId)) { obj ->
			if (obj is TdApi.Message) {
				onComplete(obj)
			}
		}
	}

	private fun addNewMessage(message: TdApi.Message) {
		lastTelegramUpdateTime = Math.max(lastTelegramUpdateTime, Math.max(message.date, message.editDate))
		if (message.isAppropriate()) {
			log.debug("addNewMessage: ${message.id}")
			val fromBot = isOsmAndBot(OsmandLocationUtils.getSenderMessageId(message))
			val viaBot = isOsmAndBot(message.viaBotUserId)
			if (message.isOutgoing && !fromBot && !viaBot) {
				return
			}
			updateLastMessage(message)
			if (message.isOutgoing) {
				if (fromBot||viaBot) {
					outgoingMessagesListeners.forEach {
						it.onUpdateMessages(listOf(message))
					}
				}
			} else {
				incomingMessagesListeners.forEach {
					it.onReceiveChatLocationMessages(message.chatId, message)
				}
			}
		}
	}

	private fun updateLastMessage(message: TdApi.Message) {
		val oldMessage = usersLocationMessages.values.firstOrNull {
			OsmandLocationUtils.getSenderMessageId(it) == OsmandLocationUtils.getSenderMessageId(message)
					&& it.chatId == message.chatId && message.viaBotUserId == message.viaBotUserId
					&& OsmandLocationUtils.getOsmAndBotDeviceName(it) == OsmandLocationUtils.getOsmAndBotDeviceName(message)
		}
		if (oldMessage == null || (Math.max(message.editDate, message.date) > Math.max(oldMessage.editDate, oldMessage.date))) {
			message.content = OsmandLocationUtils.parseMessageContent(message, this)
			oldMessage?.let {
				usersLocationMessages.remove(it.id)
			}
			usersLocationMessages[message.id] = message
		}
	}

	fun stopSendingLiveLocationToChat(shareInfo: ShareChatInfo) {
		if (!shareInfo.isMapMessageIdPresent() && shareInfo.chatId != -1L) {
			shareInfo.lastSendMapMessageTime = (System.currentTimeMillis() / 1000).toInt()
			client?.send(
				TdApi.EditMessageLiveLocation(
					shareInfo.chatId, shareInfo.currentMapMessageId,
					null, null, 0, 0, 0
				)
			)
			{ obj ->
				handleMapLocationMessageUpdate(obj, shareInfo, false)
			}
		}
		needRefreshActiveLiveLocationMessages = true
	}

	fun stopSendingLiveLocationMessages(chatsShareInfo: Map<Long, ShareChatInfo>) {
		chatsShareInfo.forEach { (_, chatInfo) ->
			stopSendingLiveLocationToChat(chatInfo)
		}
	}

	fun sendNewTextLocation(shareInfo: ShareChatInfo, content: TdApi.InputMessageText) {
		shareInfo.updateTextMessageId = 1
		if (!shareInfo.pendingTextMessage) {
			shareInfo.pendingTextMessage = true
			shareInfo.pendingTdLibText++
			shareInfo.lastSendTextMessageTime = (System.currentTimeMillis() / 1000).toInt()
			log.error("sendNewTextLocation ${shareInfo.pendingTdLibText}")
			val sendOptions = TdApi.MessageSendOptions(true, true, true, true, null, 0, 0, false)
			client?.send(TdApi.SendMessage(shareInfo.chatId, 0, null, sendOptions, null, content)) { obj ->
				handleTextLocationMessageUpdate(obj, shareInfo, false)
			}
		}
	}

	fun editTextLocation(shareInfo: ShareChatInfo, content: TdApi.InputMessageText) {
		if (shareInfo.currentTextMessageId != -1L) {
			shareInfo.pendingTdLibText++
			shareInfo.lastSendTextMessageTime = (System.currentTimeMillis() / 1000).toInt()
			log.info("editTextLocation ${shareInfo.currentTextMessageId} pendingTdLibText: ${shareInfo.pendingTdLibText}")
			client?.send(TdApi.EditMessageText(shareInfo.chatId, shareInfo.currentTextMessageId, null, content)) { obj ->
				handleTextLocationMessageUpdate(obj, shareInfo, false)
			}
		}
	}

	fun sendNewMapLocation(shareInfo: ShareChatInfo, locationMessage: LocationMessages.BufferMessage) {
		needRefreshActiveLiveLocationMessages = true
		val location = TdApi.Location(locationMessage.lat, locationMessage.lon, locationMessage.hdop)
		val livePeriod =
			if (shareInfo.currentMessageLimit > (shareInfo.start + MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC)) {
				MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC
			} else {
				shareInfo.livePeriod.toInt()
			}
		val content = TdApi.InputMessageLocation(location, livePeriod, locationMessage.bearing.toInt(), 0)
		if (!shareInfo.pendingMapMessage) {
			shareInfo.pendingMapMessage = true
			shareInfo.pendingTdLibMap++
			shareInfo.lastSendMapMessageTime = (System.currentTimeMillis() / 1000).toInt()
			log.error("sendNewMapLocation ${shareInfo.pendingTdLibMap}")
			val sendOptions = TdApi.MessageSendOptions(false, true, true, true, null, 0, 0, false)
			client?.send(TdApi.SendMessage(shareInfo.chatId, 0, null, sendOptions, null, content)) { obj ->
				handleMapLocationMessageUpdate(obj, shareInfo, false)
			}
		}
	}

	fun editMapLocation(shareInfo: ShareChatInfo, locationMessage: LocationMessages.BufferMessage) {
		needRefreshActiveLiveLocationMessages = true
		val location = TdApi.Location(locationMessage.lat, locationMessage.lon, locationMessage.hdop)
		if (shareInfo.currentMapMessageId!=-1L) {
			shareInfo.pendingTdLibMap++
			shareInfo.lastSendMapMessageTime = (System.currentTimeMillis() / 1000).toInt()
			log.info("editMapLocation ${shareInfo.currentMapMessageId} pendingTdLibMap: ${shareInfo.pendingTdLibMap}")
			client?.send(TdApi.EditMessageLiveLocation(shareInfo.chatId, shareInfo.currentMapMessageId,
				null, location, locationMessage.bearing.toInt(), 0, 0)) { obj ->
				handleMapLocationMessageUpdate(obj, shareInfo, false)
			}
		}
	}

	private fun handleMapLocationMessageUpdate(obj: TdApi.Object, shareInfo: ShareChatInfo, isBot: Boolean) {
		shareInfo.lastMapMessageHandled = true
		val messageType = if (isBot) MESSAGE_TYPE_BOT else MESSAGE_TYPE_MAP
		when (obj.constructor) {
			TdApi.Error.CONSTRUCTOR -> {
				log.debug("handleMapLocationMessageUpdate - ERROR $obj")
				val error = obj as TdApi.Error
				if (error.code != IGNORED_ERROR_CODE) {
					shareInfo.hasMapSharingError = true
					needRefreshActiveLiveLocationMessages = true
					shareInfo.pendingMapMessage = false
					outgoingMessagesListeners.forEach {
						it.onSendLiveLocationError(error.code, error.message, shareInfo, messageType)
					}
				}
			}
			TdApi.Message.CONSTRUCTOR -> {
				if (obj is TdApi.Message) {
					when {
						obj.sendingState?.constructor == TdApi.MessageSendingStateFailed.CONSTRUCTOR -> {
							shareInfo.hasMapSharingError = true
							needRefreshActiveLiveLocationMessages = true
							shareInfo.pendingMapMessage = false
							log.debug("handleTextLocationMessageUpdate - MessageSendingStateFailed")
							outgoingMessagesListeners.forEach {
								it.onSendLiveLocationError(-1, "Map location message ${obj.id} failed to send", shareInfo, messageType)
							}
						}
						obj.sendingState?.constructor == TdApi.MessageSendingStatePending.CONSTRUCTOR -> {
							shareInfo.pendingMapMessage = true
							log.debug("handleMapLocationMessageUpdate - MessageSendingStatePending")
							outgoingMessagesListeners.forEach {
								it.onUpdateMessages(listOf(obj))
							}
						}
						else -> {
							shareInfo.hasMapSharingError = false
							shareInfo.pendingMapMessage = false
							log.debug("handleMapLocationMessageUpdate - MessageSendingStateSuccess")
							outgoingMessagesListeners.forEach {
								it.onUpdateMessages(listOf(obj))
							}
						}
					}
				}
			}
		}
	}

	private fun handleTextLocationMessageUpdate(obj: TdApi.Object, shareInfo: ShareChatInfo, isBot: Boolean) {
		shareInfo.lastTextMessageHandled = true
		val messageType = if (isBot) MESSAGE_TYPE_BOT else MESSAGE_TYPE_TEXT
		when (obj.constructor) {
			TdApi.Error.CONSTRUCTOR -> {
				log.debug("handleTextLocationMessageUpdate - ERROR $obj")
				val error = obj as TdApi.Error
				if (error.code != IGNORED_ERROR_CODE) {
					shareInfo.hasTextSharingError = true
					shareInfo.pendingTextMessage = false
					outgoingMessagesListeners.forEach {
						it.onSendLiveLocationError(error.code, error.message, shareInfo, messageType)
					}
				}
			}
			TdApi.Message.CONSTRUCTOR -> {
				if (obj is TdApi.Message) {
					when {
						obj.sendingState?.constructor == TdApi.MessageSendingStateFailed.CONSTRUCTOR -> {
							shareInfo.hasTextSharingError = true
							shareInfo.pendingTextMessage = false
							needRefreshActiveLiveLocationMessages = true
							log.debug("handleTextLocationMessageUpdate - MessageSendingStateFailed")
							outgoingMessagesListeners.forEach {
								it.onSendLiveLocationError(-1, "Text location message ${obj.id} failed to send", shareInfo, messageType)
							}
						}
						obj.sendingState?.constructor == TdApi.MessageSendingStatePending.CONSTRUCTOR -> {
							shareInfo.pendingTextMessage = true
							log.debug("handleTextLocationMessageUpdate - MessageSendingStatePending")
							outgoingMessagesListeners.forEach {
								it.onUpdateMessages(listOf(obj))
							}
						}
						else -> {
							shareInfo.hasTextSharingError = false
							shareInfo.pendingTextMessage = false
							log.debug("handleTextLocationMessageUpdate - MessageSendingStateSuccess")
							outgoingMessagesListeners.forEach {
								it.onUpdateMessages(listOf(obj))
							}
						}
					}
				}
			}
		}
	}

	fun searchChats(searchTerm: String) {
		client?.send(TdApi.SearchChats(searchTerm, MAX_SEARCH_ITEMS)) { obj ->
			checkChatsAndUsersSearch(obj)
		}
	}

	fun searchChatsOnServer(searchTerm: String) {
		client?.send(TdApi.SearchChatsOnServer(searchTerm, MAX_SEARCH_ITEMS)) { obj ->
			checkChatsAndUsersSearch(obj)
		}
	}

	fun searchPublicChats(searchTerm: String) {
		client?.send(TdApi.SearchPublicChats(searchTerm)) { obj ->
			checkChatsAndUsersSearch(obj, true)
		}
	}

	fun searchContacts(searchTerm: String) {
		client?.send(TdApi.SearchContacts(searchTerm, MAX_SEARCH_ITEMS)) { obj ->
			checkChatsAndUsersSearch(obj)
		}
	}

	private fun checkChatsAndUsersSearch(obj: TdApi.Object, publicChats: Boolean = false) {
		when (obj.constructor) {
			TdApi.Error.CONSTRUCTOR -> {
				val error = obj as TdApi.Error
				if (error.code != IGNORED_ERROR_CODE) {
					listener?.onTelegramError(error.code, error.message)
				}
			}
			TdApi.Chats.CONSTRUCTOR -> {
				val chats = obj as TdApi.Chats
				if (publicChats) {
					searchListeners.forEach { it.onSearchPublicChatsFinished(chats) }
				} else {
					searchListeners.forEach { it.onSearchChatsFinished(chats) }
				}
			}
			TdApi.Users.CONSTRUCTOR -> {
				val users = obj as TdApi.Users
				searchListeners.forEach { it.onSearchContactsFinished(users) }
			}
		}
	}

	fun logout(): Boolean {
		return if (libraryLoaded) {
			haveAuthorization = false
			client!!.send(TdApi.LogOut(), defaultHandler)
			true
		} else {
			false
		}
	}

	fun close(): Boolean {
		return if (libraryLoaded) {
			haveAuthorization = false
			client!!.send(TdApi.Close(), defaultHandler)
			true
		} else {
			false
		}
	}

	private fun setChatPositions(chat: TdApi.Chat, positions: Array<TdApi.ChatPosition?>) {
		synchronized(chatList) {
			synchronized(chat) {
				val isChannel = isChannel(chat)
				for (position in chat.positions) {
					if (position.list.constructor == TdApi.ChatListMain.CONSTRUCTOR) {
						chatList.remove(OrderedChat(chat.id, position, isChannel))
					}
				}
				chat.positions = positions
				for (position in chat.positions) {
					if (position.list.constructor == TdApi.ChatListMain.CONSTRUCTOR) {
						chatList.add(OrderedChat(chat.id, position, isChannel))
					}
				}
			}
		}
	}

	private fun onAuthorizationStateUpdated(authorizationState: AuthorizationState?, info: Boolean = false) {
		val prevAuthState = getTelegramAuthorizationState()
		if (authorizationState != null) {
			this.authorizationState = authorizationState
		}
		when (this.authorizationState?.constructor) {
			TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
				if (!info) {
					log.info("Init tdlib parameters")

					val parameters = TdApi.SetTdlibParameters()
					parameters.databaseDirectory = File(appDir, "tdlib").absolutePath
					parameters.useMessageDatabase = true
					parameters.useSecretChats = true
					parameters.apiId = 293148
					parameters.apiHash = "d1942abd0f1364efe5020e2bfed2ed15"
					parameters.systemLanguageCode = "en"
					parameters.deviceModel = "Android"
					parameters.systemVersion = "OsmAnd Telegram"
					parameters.applicationVersion = "1.0"
					client!!.send(parameters, AuthorizationRequestHandler())
				}
			}
			TdApi.AuthorizationStateWaitPhoneNumber.CONSTRUCTOR -> {
				log.info("Request phone number")
				telegramAuthorizationRequestHandler?.telegramAuthorizationRequestListener?.onRequestTelegramAuthenticationParameter(PHONE_NUMBER)
			}
			TdApi.AuthorizationStateWaitCode.CONSTRUCTOR -> {
				log.info("Request code")
				telegramAuthorizationRequestHandler?.telegramAuthorizationRequestListener?.onRequestTelegramAuthenticationParameter(CODE)
			}
			TdApi.AuthorizationStateWaitPassword.CONSTRUCTOR -> {
				log.info("Request password")
				telegramAuthorizationRequestHandler?.telegramAuthorizationRequestListener?.onRequestTelegramAuthenticationParameter(PASSWORD)
			}
			TdApi.AuthorizationStateReady.CONSTRUCTOR -> {
				log.info("Ready")
			}
			TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> {
				log.info("Logging out")
			}
			TdApi.AuthorizationStateClosing.CONSTRUCTOR -> {
				log.info("Closing")
			}
			TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
				log.info("Closed")
			}
			else -> {
				log.error("Unsupported authorization state: " + this.authorizationState!!)
				telegramAuthorizationRequestHandler?.telegramAuthorizationRequestListener?.onTelegramUnsupportedAuthorizationState("${this.authorizationState!!}")
			}
		}
		val wasAuthorized = haveAuthorization
		haveAuthorization = this.authorizationState?.constructor == TdApi.AuthorizationStateReady.CONSTRUCTOR
		if (wasAuthorized != haveAuthorization) {
			needRefreshActiveLiveLocationMessages = true
			if (haveAuthorization) {
				requestChats(true, null)
				requestCurrentUser()
				requestContacts()
			}
		}
		val newAuthState = getTelegramAuthorizationState()
		listener?.onTelegramStatusChanged(prevAuthState, newAuthState)
	}

	private fun TdApi.Message.isAppropriate(): Boolean {
		if (isChannelPost) {
			return false
		}
		val content = content
		val isUserTextLocation = (content is TdApi.MessageText) && content.text.text.startsWith(USER_TEXT_LOCATION_TITLE)
		val isOsmAndBot = isOsmAndBot(OsmandLocationUtils.getSenderMessageId(this)) || isOsmAndBot(viaBotUserId)
		if (!(isUserTextLocation || content is TdApi.MessageLocation || isOsmAndBot)) {
			return false
		}
		val lastEdited = Math.max(date, editDate)
		if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - lastEdited > messageActiveTimeSec) {
			return false
		}

		return when (content) {
			is TdApi.MessageLocation -> true
			is TdApi.MessageText -> (isOsmAndBot) && content.text.text.startsWith(DEVICE_PREFIX) || isUserTextLocation
			else -> false
		}
	}

	class OrderedChat internal constructor(
		internal val chatId: Long,
		internal val position: TdApi.ChatPosition,
		internal val isChannel: Boolean
	) : Comparable<OrderedChat> {

		override fun compareTo(other: OrderedChat): Int {
			if (this.position.order != other.position.order) {
				return if (other.position.order < this.position.order) -1 else 1
			}
			return if (this.chatId != other.chatId) {
				if (other.chatId < this.chatId) -1 else 1
			} else 0
		}

		override fun equals(other: Any?): Boolean {
			if (other == null) {
				return false
			}
			if (other !is OrderedChat) {
				return false
			}
			return this.chatId == other.chatId && this.position.order == other.position.order;
		}

		override fun hashCode(): Int {
			return (position.order + chatId).hashCode()
		}
	}

	private class DefaultHandler : ResultHandler {
		override fun onResult(obj: TdApi.Object) {}
	}

	private inner class UpdatesHandler : ResultHandler {
		override fun onResult(obj: TdApi.Object) {
			when (obj.constructor) {
				TdApi.UpdateAuthorizationState.CONSTRUCTOR -> onAuthorizationStateUpdated((obj as TdApi.UpdateAuthorizationState).authorizationState)

				TdApi.UpdateUser.CONSTRUCTOR -> {
					val updateUser = obj as TdApi.UpdateUser
					val user = updateUser.user
					users[updateUser.user.id] = user
					if (user.isContact) {
						contacts[user.id] = user
					}
					if (isOsmAndBot(user.id)) {
						osmandBot = user
					}
				}
				TdApi.UpdateUserStatus.CONSTRUCTOR -> {
					val updateUserStatus = obj as TdApi.UpdateUserStatus
					val user = users[updateUserStatus.userId]
					synchronized(user!!) {
						user.status = updateUserStatus.status
					}
				}
				TdApi.UpdateBasicGroup.CONSTRUCTOR -> {
					val updateBasicGroup = obj as TdApi.UpdateBasicGroup
					basicGroups[updateBasicGroup.basicGroup.id] = updateBasicGroup.basicGroup
				}
				TdApi.UpdateSupergroup.CONSTRUCTOR -> {
					val updateSupergroup = obj as TdApi.UpdateSupergroup
					supergroups[updateSupergroup.supergroup.id] = updateSupergroup.supergroup
				}
				TdApi.UpdateSecretChat.CONSTRUCTOR -> {
					val updateSecretChat = obj as TdApi.UpdateSecretChat
					secretChats[updateSecretChat.secretChat.id] = updateSecretChat.secretChat
				}

				TdApi.UpdateNewChat.CONSTRUCTOR -> {
					val updateNewChat = obj as TdApi.UpdateNewChat
					val chat = updateNewChat.chat
					synchronized(chat) {
						chats[chat.id] = chat
						val localPhoto = chat.photo?.small?.local
						val hasLocalPhoto = if (localPhoto != null) {
							localPhoto.canBeDownloaded && localPhoto.isDownloadingCompleted && localPhoto.path.isNotEmpty()
						} else {
							false
						}
						if (!hasLocalPhoto) {
							val remotePhoto = chat.photo?.small?.remote
							if (remotePhoto != null && remotePhoto.id.isNotEmpty()) {
								downloadChatFilesMap[remotePhoto.id] = chat
								client!!.send(TdApi.GetRemoteFile(remotePhoto.id, null)) { obj ->
									when (obj.constructor) {
										TdApi.Error.CONSTRUCTOR -> {
											val error = obj as TdApi.Error
											val code = error.code
											if (code != IGNORED_ERROR_CODE) {
												listener?.onTelegramError(code, error.message)
											}
										}
										TdApi.File.CONSTRUCTOR -> {
											val file = obj as TdApi.File
											client!!.send(TdApi.DownloadFile(file.id, 10, 0, 0, true), defaultHandler)
										}
										else -> listener?.onTelegramError(-1, "Receive wrong response from TDLib: $obj")
									}
								}
							}
						}
						val positions = chat.positions
						chat.positions = arrayOfNulls(0)
						setChatPositions(chat, positions)
					}
					listener?.onTelegramChatsChanged()
				}
				TdApi.UpdateChatTitle.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatTitle
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.title = updateChat.title
						}
						listener?.onTelegramChatChanged(chat)
					}
				}
				TdApi.UpdateChatPhoto.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatPhoto
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.photo = updateChat.photo
						}
						listener?.onTelegramChatChanged(chat)
					}
				}
				TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatLastMessage
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.lastMessage = updateChat.lastMessage
							setChatPositions(chat, updateChat.positions);
						}
						//listener?.onTelegramChatsChanged()
					}
				}
				TdApi.UpdateChatPosition.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatPosition
					if (updateChat.position.list.constructor == TdApi.ChatListMain.CONSTRUCTOR) {
						val chat = chats[updateChat.chatId]
						if (chat != null) {
							synchronized(chat) {
								var index = 0
								for (i in chat.positions.indices) {
									if (chat.positions[i].list.constructor == TdApi.ChatListMain.CONSTRUCTOR) {
										index = i
										break
									}
								}
								val length = chat.positions.size + (if (updateChat.position.order == 0L) 0 else 1) - if (index < chat.positions.size) 1 else 0
								val newPositions = arrayOfNulls<TdApi.ChatPosition>(length)

								var pos = 0
								if (updateChat.position.order != 0L) {
									newPositions[pos++] = updateChat.position
								}
								for (j in chat.positions.indices) {
									if (j != index) {
										newPositions[pos++] = chat.positions[j];
									}
								}
								setChatPositions(chat, newPositions)
							}
						}
						//listener?.onTelegramChatsChanged()
					}
				}
				TdApi.UpdateChatReadInbox.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatReadInbox
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.lastReadInboxMessageId = updateChat.lastReadInboxMessageId
							chat.unreadCount = updateChat.unreadCount
						}
					}
				}
				TdApi.UpdateChatReadOutbox.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatReadOutbox
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.lastReadOutboxMessageId = updateChat.lastReadOutboxMessageId
						}
					}
				}
				TdApi.UpdateChatUnreadMentionCount.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatUnreadMentionCount
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.unreadMentionCount = updateChat.unreadMentionCount
						}
					}
				}
				TdApi.UpdateMessageEdited.CONSTRUCTOR -> {
					val updateMessageEdited = obj as TdApi.UpdateMessageEdited
					lastTelegramUpdateTime = Math.max(lastTelegramUpdateTime, updateMessageEdited.editDate)
					val message = usersLocationMessages[updateMessageEdited.messageId]
					if (message != null) {
						synchronized(message) {
							message.editDate = updateMessageEdited.editDate
						}
					}
				}
				TdApi.UpdateMessageContent.CONSTRUCTOR -> {
					val updateMessageContent = obj as TdApi.UpdateMessageContent
					val message = usersLocationMessages[updateMessageContent.messageId]
					log.debug("UpdateMessageContent " + updateMessageContent.messageId)
					if (message == null) {
						updateMessageContent.apply {
							requestMessage(chatId, messageId, this@TelegramHelper::addNewMessage)
						}
					} else {
						synchronized(message) {
							lastTelegramUpdateTime = Math.max(lastTelegramUpdateTime, Math.max(message.date, message.editDate))
							message.content = updateMessageContent.newContent
							message.content = OsmandLocationUtils.parseMessageContent(message, this@TelegramHelper)
						}
						incomingMessagesListeners.forEach {
							it.onReceiveChatLocationMessages(message.chatId, message)
						}
					}
				}
				TdApi.UpdateNewMessage.CONSTRUCTOR -> {
					addNewMessage((obj as TdApi.UpdateNewMessage).message)
					log.debug("UpdateNewMessage " + obj.message.id)
				}
				TdApi.UpdateMessageMentionRead.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateMessageMentionRead
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.unreadMentionCount = updateChat.unreadMentionCount
						}
					}
				}
				TdApi.UpdateMessageSendFailed.CONSTRUCTOR -> {
					needRefreshActiveLiveLocationMessages = true
				}
				TdApi.UpdateDeleteMessages.CONSTRUCTOR -> {
					val updateDeleteMessages = obj as TdApi.UpdateDeleteMessages
					if (updateDeleteMessages.isPermanent) {
						val chatId = updateDeleteMessages.chatId
						val deletedMessages = mutableListOf<TdApi.Message>()
						for (messageId in updateDeleteMessages.messageIds) {
							usersLocationMessages.remove(messageId)
								?.also { deletedMessages.add(it) }
						}
						outgoingMessagesListeners.forEach {
							it.onDeleteMessages(chatId, updateDeleteMessages.messageIds.toList())
						}
						if (deletedMessages.isNotEmpty()) {
							incomingMessagesListeners.forEach {
								it.onDeleteChatLocationMessages(chatId, deletedMessages)
							}
						}
					}
				}
				TdApi.UpdateChatReplyMarkup.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatReplyMarkup
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.replyMarkupMessageId = updateChat.replyMarkupMessageId
						}
					}
				}
				TdApi.UpdateChatDraftMessage.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatDraftMessage
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.draftMessage = updateChat.draftMessage
							setChatPositions(chat, updateChat.positions)
						}
						//listener?.onTelegramChatsChanged()
					}
				}
				TdApi.UpdateChatNotificationSettings.CONSTRUCTOR -> {
					val update = obj as TdApi.UpdateChatNotificationSettings
					val chat = chats[update.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.notificationSettings = update.notificationSettings
						}
					}
				}

				TdApi.UpdateFile.CONSTRUCTOR -> {
					val updateFile = obj as TdApi.UpdateFile
					if (updateFile.file.local.isDownloadingCompleted) {
						val remoteId = updateFile.file.remote.id
						val chat = downloadChatFilesMap.remove(remoteId)
						if (chat != null) {
							synchronized(chat) {
								chat.photo?.small = updateFile.file
							}
							listener?.onTelegramChatChanged(chat)
						}
						val user = downloadUserFilesMap.remove(remoteId)
						if (user != null) {
							synchronized(user) {
								user.profilePhoto?.small = updateFile.file
							}
							listener?.onTelegramUserChanged(user)
						}
					}
				}

				TdApi.UpdateUserFullInfo.CONSTRUCTOR -> {
					val updateUserFullInfo = obj as TdApi.UpdateUserFullInfo
					usersFullInfo[updateUserFullInfo.userId] = updateUserFullInfo.userFullInfo
				}
				TdApi.UpdateBasicGroupFullInfo.CONSTRUCTOR -> {
					val updateBasicGroupFullInfo = obj as TdApi.UpdateBasicGroupFullInfo
					val id = updateBasicGroupFullInfo.basicGroupId
					if (basicGroupsFullInfo.containsKey(id)) {
						val info = updateBasicGroupFullInfo.basicGroupFullInfo
						basicGroupsFullInfo[id] = info
						fullInfoUpdatesListeners.forEach { it.onBasicGroupFullInfoUpdated(id, info) }
					}
				}
				TdApi.UpdateSupergroupFullInfo.CONSTRUCTOR -> {
					val updateSupergroupFullInfo = obj as TdApi.UpdateSupergroupFullInfo
					val id = updateSupergroupFullInfo.supergroupId
					if (supergroupsFullInfo.containsKey(id)) {
						val info = updateSupergroupFullInfo.supergroupFullInfo
						supergroupsFullInfo[id] = info
						fullInfoUpdatesListeners.forEach { it.onSupergroupFullInfoUpdated(id, info) }
					}
				}
				TdApi.UpdateMessageSendSucceeded.CONSTRUCTOR -> {
					val updateSucceeded = obj as TdApi.UpdateMessageSendSucceeded
					val message = updateSucceeded.message
					log.debug("UpdateMessageSendSucceeded: ${message.id} oldId: ${updateSucceeded.oldMessageId}")
					outgoingMessagesListeners.forEach {
						it.onUpdateMessages(listOf(message))
					}
				}
			}
		}
	}

	private inner class AuthorizationRequestHandler : ResultHandler {
		override fun onResult(obj: TdApi.Object) {
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					log.error("Receive an error: $obj")
					val errorObj = obj as TdApi.Error
					if (errorObj.code != IGNORED_ERROR_CODE) {
						telegramAuthorizationRequestHandler?.telegramAuthorizationRequestListener?.onTelegramAuthorizationRequestError(errorObj.code, errorObj.message)
						onAuthorizationStateUpdated(null) // repeat last action
					}
				}
				TdApi.Ok.CONSTRUCTOR -> {
				}
				else -> log.error("Receive wrong response from TDLib: $obj")
			}// result is already received through UpdateAuthorizationState, nothing to do
		}
	}
}

fun User.getName(): String {
	var name = "${this.firstName} ${this.lastName}".trim()
	if (name.isEmpty()) {
		this.usernames?.let {
			val userNames = it.activeUsernames
			if(!Algorithms.isEmpty(userNames)) {
				name = userNames[0]
			}
		}
	}
	if (name.isEmpty()) {
		name = this.phoneNumber
	}
	return name
}