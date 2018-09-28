package net.osmand.telegram.helpers

import android.text.TextUtils
import net.osmand.PlatformUtil
import net.osmand.telegram.helpers.TelegramHelper.TelegramAuthenticationParameterType.*
import net.osmand.telegram.utils.GRAYSCALE_PHOTOS_DIR
import net.osmand.telegram.utils.GRAYSCALE_PHOTOS_EXT
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.Client.ResultHandler
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.AuthorizationState
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet


class TelegramHelper private constructor() {

	companion object {
		const val OSMAND_BOT_USERNAME = "osmand_bot"

		private val log = PlatformUtil.getLog(TelegramHelper::class.java)
		private const val CHATS_LIMIT = 100
		private const val IGNORED_ERROR_CODE = 406

		private const val DEVICE_PREFIX = "Device: "
		private const val LOCATION_PREFIX = "Location: "
		private const val LAST_LOCATION_PREFIX = "Last location: "

		private const val FEW_SECONDS_AGO = "few seconds ago"
		private const val SECONDS_AGO_SUFFIX = " seconds ago"
		private const val MINUTES_AGO_SUFFIX = " minutes ago"
		private const val HOURS_AGO_SUFFIX = " hours ago"
		private const val UTC_FORMAT_SUFFIX = " UTC"

		private val UTC_DATE_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
			timeZone = TimeZone.getTimeZone("UTC")
		}

		private val UTC_TIME_FORMAT = SimpleDateFormat("HH:mm:ss", Locale.US).apply {
			timeZone = TimeZone.getTimeZone("UTC")
		}
		// min and max values for the Telegram API
		const val MIN_LOCATION_MESSAGE_LIVE_PERIOD_SEC = 61
		const val MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC = 60 * 60 * 24 - 1 // one day

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

	private val users = ConcurrentHashMap<Int, TdApi.User>()
	private val basicGroups = ConcurrentHashMap<Int, TdApi.BasicGroup>()
	private val supergroups = ConcurrentHashMap<Int, TdApi.Supergroup>()
	private val secretChats = ConcurrentHashMap<Int, TdApi.SecretChat>()

	private val chats = ConcurrentHashMap<Long, TdApi.Chat>()
	private val chatList = TreeSet<OrderedChat>()
	private val chatLiveMessages = ConcurrentHashMap<Long, TdApi.Message>()

	private val downloadChatFilesMap = ConcurrentHashMap<String, TdApi.Chat>()
	private val downloadUserFilesMap = ConcurrentHashMap<String, TdApi.User>()

	// value.content can be TdApi.MessageLocation or MessageOsmAndBotLocation
	private val usersLocationMessages = ConcurrentHashMap<Long, TdApi.Message>()

	private val usersFullInfo = ConcurrentHashMap<Int, TdApi.UserFullInfo>()
	private val basicGroupsFullInfo = ConcurrentHashMap<Int, TdApi.BasicGroupFullInfo>()
	private val supergroupsFullInfo = ConcurrentHashMap<Int, TdApi.SupergroupFullInfo>()

	var appDir: String? = null
	private var libraryLoaded = false
	private var telegramAuthorizationRequestHandler: TelegramAuthorizationRequestHandler? = null

	private var client: Client? = null
	private var currentUser: TdApi.User? = null

	private var haveFullChatList: Boolean = false
	private var needRefreshActiveLiveLocationMessages: Boolean = true
	private var requestingActiveLiveLocationMessages: Boolean = false

	private var authorizationState: AuthorizationState? = null
	private var haveAuthorization = false

	private val defaultHandler = DefaultHandler()
	private val liveLocationMessageUpdatesHandler = LiveLocationMessageUpdatesHandler()

	private var updateLiveMessagesExecutor: ScheduledExecutorService? = null

	var listener: TelegramListener? = null
	private val incomingMessagesListeners = HashSet<TelegramIncomingMessagesListener>()
	private val fullInfoUpdatesListeners = HashSet<FullInfoUpdatesListener>()

	fun addIncomingMessagesListener(listener: TelegramIncomingMessagesListener) {
		incomingMessagesListeners.add(listener)
	}

	fun removeIncomingMessagesListener(listener: TelegramIncomingMessagesListener) {
		incomingMessagesListeners.remove(listener)
	}

	fun addFullInfoUpdatesListener(listener: FullInfoUpdatesListener) {
		fullInfoUpdatesListeners.add(listener)
	}

	fun removeFullInfoUpdatesListener(listener: FullInfoUpdatesListener) {
		fullInfoUpdatesListeners.remove(listener)
	}

	fun getChatList(): TreeSet<OrderedChat> {
		synchronized(chatList) {
			return TreeSet(chatList.filter { !it.isChannel })
		}
	}

	fun getChatListIds() = getChatList().map { it.chatId }

	fun getChatIds() = chats.keys().toList()

	fun getChat(id: Long) = chats[id]

	fun getUser(id: Int) = users[id]

	fun getCurrentUser() = currentUser

	fun getUserMessage(user: TdApi.User) =
		usersLocationMessages.values.firstOrNull { it.senderUserId == user.id }

	fun getChatMessages(chatId: Long) =
		usersLocationMessages.values.filter { it.chatId == chatId }

	fun getMessages() = usersLocationMessages.values.toList()

	fun getChatLiveMessages() = chatLiveMessages

	fun getMessagesByChatIds(messageExpTime: Long): Map<Long, List<TdApi.Message>> {
		val res = mutableMapOf<Long, MutableList<TdApi.Message>>()
		for (message in usersLocationMessages.values) {
			if (System.currentTimeMillis() / 1000 - getLastUpdatedTime(message) < messageExpTime) {
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

	fun getBasicGroupFullInfo(id: Int): TdApi.BasicGroupFullInfo? {
		val res = basicGroupsFullInfo[id]
		if (res == null) {
			requestBasicGroupFullInfo(id)
		}
		return res
	}

	fun getSupergroupFullInfo(id: Int): TdApi.SupergroupFullInfo? {
		val res = supergroupsFullInfo[id]
		if (res == null) {
			requestSupergroupFullInfo(id)
		}
		return res
	}

	fun isGroup(chat: TdApi.Chat): Boolean {
		return chat.type is TdApi.ChatTypeSupergroup || chat.type is TdApi.ChatTypeBasicGroup
	}

	fun getLastUpdatedTime(message: TdApi.Message): Int {
		val content = message.content
		return if (content is MessageOsmAndBotLocation) {
			content.lastUpdated
		} else {
			Math.max(message.editDate, message.date)
		}
	} 

	fun isPrivateChat(chat: TdApi.Chat): Boolean = chat.type is TdApi.ChatTypePrivate
	
	fun isSecretChat(chat: TdApi.Chat): Boolean = chat.type is TdApi.ChatTypeSecret

	private fun isChannel(chat: TdApi.Chat): Boolean {
		return chat.type is TdApi.ChatTypeSupergroup && (chat.type as TdApi.ChatTypeSupergroup).isChannel
	}

	enum class TelegramAuthenticationParameterType {
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
		fun onTelegramUserChanged(user: TdApi.User)
		fun onTelegramError(code: Int, message: String)
		fun onSendLiveLocationError(code: Int, message: String)
	}

	interface TelegramIncomingMessagesListener {
		fun onReceiveChatLocationMessages(chatId: Long, vararg messages: TdApi.Message)
		fun onDeleteChatLocationMessages(chatId: Long, messages: List<TdApi.Message>)
		fun updateLocationMessages()
	}

	interface FullInfoUpdatesListener {
		fun onBasicGroupFullInfoUpdated(groupId: Int, info: TdApi.BasicGroupFullInfo)
		fun onSupergroupFullInfoUpdated(groupId: Int, info: TdApi.SupergroupFullInfo)
	}

	interface TelegramAuthorizationRequestListener {
		fun onRequestTelegramAuthenticationParameter(parameterType: TelegramAuthenticationParameterType)
		fun onTelegramAuthorizationRequestError(code: Int, message: String)
	}

	inner class TelegramAuthorizationRequestHandler(val telegramAuthorizationRequestListener: TelegramAuthorizationRequestListener) {

		fun applyAuthenticationParameter(parameterType: TelegramAuthenticationParameterType, parameterValue: String) {
			if (!TextUtils.isEmpty(parameterValue)) {
				when (parameterType) {
					PHONE_NUMBER -> client!!.send(TdApi.SetAuthenticationPhoneNumber(parameterValue, false, false), AuthorizationRequestHandler())
					CODE -> client!!.send(TdApi.CheckAuthenticationCode(parameterValue, "", ""), AuthorizationRequestHandler())
					PASSWORD -> client!!.send(TdApi.CheckAuthenticationPassword(parameterValue), AuthorizationRequestHandler())
				}
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
			Client.setLogVerbosityLevel(0)
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

	fun getUserGreyPhotoPath(user: TdApi.User): String? {
		return if (hasGrayscaleUserPhoto(user.id)) {
			"$appDir/$GRAYSCALE_PHOTOS_DIR${user.id}$GRAYSCALE_PHOTOS_EXT"
		} else {
			null
		}
	}

	fun getOsmAndBotDeviceName(message: TdApi.Message): String {
		var deviceName = ""
		if (message.replyMarkup is TdApi.ReplyMarkupInlineKeyboard) {
			val replyMarkup = message.replyMarkup as TdApi.ReplyMarkupInlineKeyboard
			try {
				deviceName = replyMarkup.rows[0][1].text.split("\\s".toRegex())[1]
			} catch (e: Exception) {

			}
		}
		return deviceName
	}

	fun getUserIdFromChatType(type: TdApi.ChatType) = when (type) {
		is TdApi.ChatTypePrivate -> type.userId
		is TdApi.ChatTypeSecret -> type.userId
		else -> 0
	}
	
	fun isOsmAndBot(userId: Int) = users[userId]?.username == OSMAND_BOT_USERNAME

	fun isBot(userId: Int) = users[userId]?.type is TdApi.UserTypeBot

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

	fun hasGrayscaleUserPhoto(userId: Int): Boolean {
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
						client!!.send(TdApi.DownloadFile(file.id, 10), defaultHandler)
					}
					else -> listener?.onTelegramError(-1, "Receive wrong response from TDLib: $obj")
				}
			}
		}
	}

	private fun requestChats(reload: Boolean = false) {
		synchronized(chatList) {
			if (reload) {
				chatList.clear()
				haveFullChatList = false
			}
			if (!haveFullChatList && CHATS_LIMIT > chatList.size) {
				// have enough chats in the chat list or chat list is too small
				var offsetOrder = java.lang.Long.MAX_VALUE
				var offsetChatId: Long = 0
				if (!chatList.isEmpty()) {
					val last = chatList.last()
					offsetOrder = last.order
					offsetChatId = last.chatId
				}
				client?.send(TdApi.GetChats(offsetOrder, offsetChatId, CHATS_LIMIT - chatList.size)) { obj ->
					when (obj.constructor) {
						TdApi.Error.CONSTRUCTOR -> {
							val error = obj as TdApi.Error
							if (error.code != IGNORED_ERROR_CODE) {
								listener?.onTelegramError(error.code, error.message)
							}
						}
						TdApi.Chats.CONSTRUCTOR -> {
							val chatIds = (obj as TdApi.Chats).chatIds
							if (chatIds.isEmpty()) {
								synchronized(chatList) {
									haveFullChatList = true
								}
							}
							// chats had already been received through updates, let's retry request
							requestChats()
						}
						else -> listener?.onTelegramError(-1, "Receive wrong response from TDLib: $obj")
					}
				}
				return
			}
		}
		listener?.onTelegramChatsRead()
	}

	private fun requestBasicGroupFullInfo(id: Int) {
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

	private fun requestSupergroupFullInfo(id: Int) {
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

	fun loadMessage(chatId: Long, messageId: Long) {
		requestMessage(chatId, messageId, this@TelegramHelper::addNewMessage)
	}

	private fun requestMessage(chatId: Long, messageId: Long, onComplete: (TdApi.Message) -> Unit) {
		client?.send(TdApi.GetMessage(chatId, messageId)) { obj ->
			if (obj is TdApi.Message) {
				onComplete(obj)
			}
		}
	}

	private fun addNewMessage(message: TdApi.Message) {
		if (message.isAppropriate()) {
			val fromBot = isOsmAndBot(message.senderUserId)
			val viaBot = isOsmAndBot(message.viaBotUserId)
			val oldContent = message.content
			if (oldContent is TdApi.MessageText) {
				message.content = parseOsmAndBotLocation(oldContent.text.text)
			} else if (oldContent is TdApi.MessageLocation && (fromBot || viaBot)) {
				message.content = parseOsmAndBotLocation(message)
			}
			removeOldMessages(message, fromBot, viaBot)
			usersLocationMessages[message.id] = message
			incomingMessagesListeners.forEach {
				it.onReceiveChatLocationMessages(message.chatId, message)
			}
		}
	}

	private fun removeOldMessages(newMessage: TdApi.Message, fromBot: Boolean, viaBot: Boolean) {
		val iterator = usersLocationMessages.entries.iterator()
		while (iterator.hasNext()) {
			val message = iterator.next().value
			if (newMessage.chatId == message.chatId) {
				val sameSender = newMessage.senderUserId == message.senderUserId
				val viaSameBot = newMessage.viaBotUserId == message.viaBotUserId
				if (fromBot || viaBot) {
					if ((fromBot && sameSender) || (viaBot && viaSameBot)) {
						val newCont = newMessage.content
						val cont = message.content
						if (newCont is MessageOsmAndBotLocation && cont is MessageOsmAndBotLocation) {
							if (newCont.name == cont.name) {
								iterator.remove()
							}
						}
					}
				} else if (sameSender) {
					iterator.remove()
				}
			}
		}
	}

	/**
	 * @chatId Id of the chat
	 * @livePeriod Period for which the location can be updated, in seconds; should be between 60 and 86400 for a live location and 0 otherwise.
	 * @latitude Latitude of the location
	 * @longitude Longitude of the location
	 */
	fun sendLiveLocationMessage(chatLivePeriods: Map<Long, Long>, latitude: Double, longitude: Double): Boolean {
		if (!requestingActiveLiveLocationMessages && haveAuthorization) {
			if (needRefreshActiveLiveLocationMessages) {
				getActiveLiveLocationMessages {
					sendLiveLocationImpl(chatLivePeriods, latitude, longitude)
				}
				needRefreshActiveLiveLocationMessages = false
			} else {
				sendLiveLocationImpl(chatLivePeriods, latitude, longitude)
			}
			return true
		}
		return false
	}

	fun stopSendingLiveLocationToChat(chatId: Long) {
		val msgId = chatLiveMessages[chatId]?.id
		if (msgId != null && msgId != 0L) {
			client?.send(
				TdApi.EditMessageLiveLocation(chatId, msgId, null, null),
				liveLocationMessageUpdatesHandler
			)
		}
		chatLiveMessages.remove(chatId)
		needRefreshActiveLiveLocationMessages = true
	}

	fun stopSendingLiveLocationMessages() {
		chatLiveMessages.forEach { (chatId, _ )->
			stopSendingLiveLocationToChat(chatId)
		}
	}
	
	fun getActiveLiveLocationMessages(onComplete: (() -> Unit)?) {
		requestingActiveLiveLocationMessages = true
		client?.send(TdApi.GetActiveLiveLocationMessages()) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						needRefreshActiveLiveLocationMessages = true
						listener?.onSendLiveLocationError(error.code, error.message)
					}
				}
				TdApi.Messages.CONSTRUCTOR -> {
					val messages = (obj as TdApi.Messages).messages
					chatLiveMessages.clear()
					if (messages.isNotEmpty()) {
						for (msg in messages) {
							val chatId = msg.chatId
							chatLiveMessages[chatId] = msg
						}
					}
					onComplete?.invoke()
				}
				else -> listener?.onSendLiveLocationError(-1, "Receive wrong response from TDLib: $obj")
			}
			requestingActiveLiveLocationMessages = false
		}
	}

	private fun sendLiveLocationImpl(chatLivePeriods: Map<Long, Long>, latitude: Double, longitude: Double) {
		val location = TdApi.Location(latitude, longitude)
		chatLivePeriods.forEach { (chatId, livePeriod) ->
			val content = TdApi.InputMessageLocation(location, livePeriod.toInt())
			val msgId = chatLiveMessages[chatId]?.id
			if (msgId != null) {
				if (msgId != 0L) {
					client?.send(
						TdApi.EditMessageLiveLocation(chatId, msgId, null, location),
						liveLocationMessageUpdatesHandler
					)
				}
			} else {
				client?.send(
					TdApi.SendMessage(chatId, 0, false, true, null, content),
					liveLocationMessageUpdatesHandler
				)
			}
		}
	}

	/**
	 * @chatId Id of the chat
	 * @message Text of the message
	 */
	fun sendTextMessage(chatId: Long, message: String): Boolean {
		// initialize reply markup just for testing
		//val row = arrayOf(TdApi.InlineKeyboardButton("https://telegram.org?1", TdApi.InlineKeyboardButtonTypeUrl()), TdApi.InlineKeyboardButton("https://telegram.org?2", TdApi.InlineKeyboardButtonTypeUrl()), TdApi.InlineKeyboardButton("https://telegram.org?3", TdApi.InlineKeyboardButtonTypeUrl()))
		//val replyMarkup = TdApi.ReplyMarkupInlineKeyboard(arrayOf(row, row, row))

		if (haveAuthorization) {
			val content = TdApi.InputMessageText(TdApi.FormattedText(message, null), false, true)
			client?.send(TdApi.SendMessage(chatId, 0, false, true, null, content), defaultHandler)
			return true
		}
		return false
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

	private fun setChatOrder(chat: TdApi.Chat, order: Long) {
		synchronized(chatList) {
			val isChannel = isChannel(chat)

			if (chat.order != 0L) {
				chatList.remove(OrderedChat(chat.order, chat.id, isChannel))
			}

			chat.order = order

			if (chat.order != 0L) {
				chatList.add(OrderedChat(chat.order, chat.id, isChannel))
			}
		}
	}

	private inner class LiveLocationMessageUpdatesHandler : ResultHandler {
		override fun onResult(obj: TdApi.Object) {
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						needRefreshActiveLiveLocationMessages = true
						listener?.onSendLiveLocationError(error.code, error.message)
					}
				}
				else -> {
					if (obj is TdApi.Message) {
						when (obj.sendingState?.constructor) {
							TdApi.MessageSendingStateFailed.CONSTRUCTOR -> {
								needRefreshActiveLiveLocationMessages = true
								listener?.onSendLiveLocationError(-1, "Live location message ${obj.id} failed to send")
							}
						}
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

					val parameters = TdApi.TdlibParameters()
					parameters.databaseDirectory = File(appDir, "tdlib").absolutePath
					parameters.useMessageDatabase = true
					parameters.useSecretChats = true
					parameters.apiId = 293148
					parameters.apiHash = "d1942abd0f1364efe5020e2bfed2ed15"
					parameters.systemLanguageCode = "en"
					parameters.deviceModel = "Android"
					parameters.systemVersion = "OsmAnd Telegram"
					parameters.applicationVersion = "1.0"
					parameters.enableStorageOptimizer = true

					client!!.send(TdApi.SetTdlibParameters(parameters), AuthorizationRequestHandler())
				}
			}
			TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
				if (!info) {
					client!!.send(TdApi.CheckDatabaseEncryptionKey(), AuthorizationRequestHandler())
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
			else -> log.error("Unsupported authorization state: " + this.authorizationState!!)
		}
		val wasAuthorized = haveAuthorization
		haveAuthorization = this.authorizationState?.constructor == TdApi.AuthorizationStateReady.CONSTRUCTOR
		if (wasAuthorized != haveAuthorization) {
			needRefreshActiveLiveLocationMessages = true
			if (haveAuthorization) {
				requestChats(true)
				requestCurrentUser()
			}
		}
		val newAuthState = getTelegramAuthorizationState()
		listener?.onTelegramStatusChanged(prevAuthState, newAuthState)
	}

	private fun TdApi.Message.isAppropriate(): Boolean {
		if (isOutgoing || isChannelPost) {
			return false
		}
		val lastEdited = Math.max(date, editDate)
		if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - lastEdited > messageActiveTimeSec) {
			return false
		}
		val content = content
		return when (content) {
			is TdApi.MessageLocation -> true
			is TdApi.MessageText -> (isOsmAndBot(senderUserId) || isOsmAndBot(viaBotUserId))
					&& content.text.text.startsWith(DEVICE_PREFIX)
			else -> false
		}
	}

	private fun parseOsmAndBotLocation(message: TdApi.Message): MessageOsmAndBotLocation {
		val messageLocation = message.content as TdApi.MessageLocation
		return MessageOsmAndBotLocation().apply {
			name = getOsmAndBotDeviceName(message)
			lat = messageLocation.location.latitude
			lon = messageLocation.location.longitude
			lastUpdated = getLastUpdatedTime(message)
		}
	}

	private fun parseOsmAndBotLocationContent(oldContent:MessageOsmAndBotLocation, content: TdApi.MessageContent): MessageOsmAndBotLocation {
		val messageLocation = content as TdApi.MessageLocation
		return MessageOsmAndBotLocation().apply {
			name = oldContent.name
			lat = messageLocation.location.latitude
			lon = messageLocation.location.longitude
			lastUpdated = (System.currentTimeMillis() / 1000).toInt()
		}
	}

	private fun parseOsmAndBotLocation(text: String): MessageOsmAndBotLocation {
		val res = MessageOsmAndBotLocation()
		var locationNA = false
		for (s in text.lines()) {
			when {
				s.startsWith(DEVICE_PREFIX) -> {
					res.name = s.removePrefix(DEVICE_PREFIX)
				}
				s.startsWith(LOCATION_PREFIX) || s.startsWith(LAST_LOCATION_PREFIX) -> {
					var locStr: String
					var parse = true
					if (s.startsWith(LAST_LOCATION_PREFIX)) {
						locStr = s.removePrefix(LAST_LOCATION_PREFIX)
						if (!locationNA) {
							parse = false
						}
					} else {
						locStr = s.removePrefix(LOCATION_PREFIX)
						if (locStr.trim() == "n/a") {
							locationNA = true
							parse = false
						}
					}
					if (parse) {
						try {
							val (latS, lonS) = locStr.split(" ")
							val updatedS = locStr.substring(locStr.indexOf("("), locStr.length)

							res.lastUpdated =
									(parseTime(updatedS.removePrefix("(").removeSuffix(")")) / 1000).toInt()
							res.lat = latS.dropLast(1).toDouble()
							res.lon = lonS.toDouble()

						} catch (e: Exception) {
							e.printStackTrace()
						}
					}
				}
			}
		}
		return res
	}

	private fun parseTime(timeS: String): Long {
		try {
			when {
				timeS.endsWith(FEW_SECONDS_AGO) -> return System.currentTimeMillis() - 5000

				timeS.endsWith(SECONDS_AGO_SUFFIX) -> {
					val locStr = timeS.removeSuffix(SECONDS_AGO_SUFFIX)
					return System.currentTimeMillis() - locStr.toLong() * 1000
				}
				timeS.endsWith(MINUTES_AGO_SUFFIX) -> {
					val locStr = timeS.removeSuffix(MINUTES_AGO_SUFFIX)
					val minutes = locStr.toLong()
					return System.currentTimeMillis() - minutes * 60 * 1000
				}
				timeS.endsWith(HOURS_AGO_SUFFIX) -> {
					val locStr = timeS.removeSuffix(HOURS_AGO_SUFFIX)
					val hours = locStr.toLong()
					return (System.currentTimeMillis() -  hours * 60 * 60 * 1000)
				}
				timeS.endsWith(UTC_FORMAT_SUFFIX) -> {
					val locStr = timeS.removeSuffix(UTC_FORMAT_SUFFIX)
					val (latS, lonS) = locStr.split(" ")
					val date = UTC_DATE_FORMAT.parse(latS)
					val time = UTC_TIME_FORMAT.parse(lonS)
					val res = date.time + time.time
					return res
				}
			}
		} catch (e: Exception) {
			e.printStackTrace()
		}
		return 0
	}

	class MessageOsmAndBotLocation : TdApi.MessageContent() {

		var name: String = ""
			internal set
		var lat: Double = Double.NaN
			internal set
		var lon: Double = Double.NaN
			internal set
		var lastUpdated: Int = 0
			internal set

		override fun getConstructor() = -1

		fun isValid() = name != "" && lat != Double.NaN && lon != Double.NaN
	}

	class OrderedChat internal constructor(internal val order: Long, internal val chatId: Long, internal val isChannel: Boolean) : Comparable<OrderedChat> {

		override fun compareTo(other: OrderedChat): Int {
			if (this.order != other.order) {
				return if (other.order < this.order) -1 else 1
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
			val o = other as OrderedChat?
			return this.order == o!!.order && this.chatId == o.chatId
		}

		override fun hashCode(): Int {
			return (order + chatId).hashCode()
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
					users[updateUser.user.id] = updateUser.user
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
											client!!.send(TdApi.DownloadFile(file.id, 10), defaultHandler)
										}
										else -> listener?.onTelegramError(-1, "Receive wrong response from TDLib: $obj")
									}
								}
							}
						}
						val order = chat.order
						chat.order = 0
						setChatOrder(chat, order)
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
							setChatOrder(chat, updateChat.order)
						}
						//listener?.onTelegramChatsChanged()
					}
				}
				TdApi.UpdateChatOrder.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatOrder
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							setChatOrder(chat, updateChat.order)
						}
						listener?.onTelegramChatsChanged()
					}
				}
				TdApi.UpdateChatIsPinned.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatIsPinned
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.isPinned = updateChat.isPinned
							setChatOrder(chat, updateChat.order)
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
					val message = usersLocationMessages[updateMessageEdited.messageId]
					if (message == null) {
						updateMessageEdited.apply {
							requestMessage(chatId, messageId, this@TelegramHelper::addNewMessage)
						}
					} else {
						synchronized(message) {
							message.editDate = updateMessageEdited.editDate
						}
						incomingMessagesListeners.forEach {
							it.onReceiveChatLocationMessages(message.chatId, message)
						}
					}
				}
				TdApi.UpdateMessageContent.CONSTRUCTOR -> {
					val updateMessageContent = obj as TdApi.UpdateMessageContent
					val message = usersLocationMessages[updateMessageContent.messageId]
					if (message == null) {
						updateMessageContent.apply {
							requestMessage(chatId, messageId, this@TelegramHelper::addNewMessage)
						}
					} else {
						synchronized(message) {
							val newContent = updateMessageContent.newContent
							message.content = if (newContent is TdApi.MessageText) {
								parseOsmAndBotLocation(newContent.text.text)
							} else if (newContent is TdApi.MessageLocation &&
								(isOsmAndBot(message.senderUserId) || isOsmAndBot(message.viaBotUserId))) {
								parseOsmAndBotLocationContent(message.content as MessageOsmAndBotLocation, newContent)
							} else {
								newContent
							}
						}
						incomingMessagesListeners.forEach {
							it.onReceiveChatLocationMessages(message.chatId, message)
						}
					}
				}
				TdApi.UpdateNewMessage.CONSTRUCTOR -> {
					addNewMessage((obj as TdApi.UpdateNewMessage).message)
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
				TdApi.UpdateMessageSendSucceeded.CONSTRUCTOR -> {
					val updateMessageSendSucceeded = obj as TdApi.UpdateMessageSendSucceeded
					val message = updateMessageSendSucceeded.message
					chatLiveMessages[message.chatId] = message
				}
				TdApi.UpdateDeleteMessages.CONSTRUCTOR -> {
					val updateDeleteMessages = obj as TdApi.UpdateDeleteMessages
					if (updateDeleteMessages.isPermanent) {
						val chatId = updateDeleteMessages.chatId
						val deletedMessages = mutableListOf<TdApi.Message>()
						for (messageId in updateDeleteMessages.messageIds) {
							if (chatLiveMessages[chatId]?.id == messageId) {
								chatLiveMessages.remove(chatId)
							}
							usersLocationMessages.remove(messageId)
								?.also { deletedMessages.add(it) }
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
							setChatOrder(chat, updateChat.order)
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
