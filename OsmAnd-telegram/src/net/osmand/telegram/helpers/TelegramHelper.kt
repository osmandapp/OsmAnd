package net.osmand.telegram.helpers

import android.text.TextUtils
import net.osmand.PlatformUtil
import net.osmand.telegram.helpers.TelegramHelper.TelegramAuthenticationParameterType.*
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.Client.ResultHandler
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.AuthorizationState
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.collections.HashSet


class TelegramHelper private constructor() {

	companion object {
		private val log = PlatformUtil.getLog(TelegramHelper::class.java)
		private const val CHATS_LIMIT = 100
		private const val IGNORED_ERROR_CODE = 406
		private const val UPDATE_LIVE_MESSAGES_INTERVAL_SEC = 30L
		private const val MESSAGE_ACTIVE_TIME_SEC = 24 * 60 * 60 // 24 hours
		private const val OSMAND_BOT_USERNAME = "osmand_bot"

		private var helper: TelegramHelper? = null

		val instance: TelegramHelper
			get() {
				if (helper == null) {
					helper = TelegramHelper()
				}
				return helper!!
			}
	}

	private val users = ConcurrentHashMap<Int, TdApi.User>()
	private val basicGroups = ConcurrentHashMap<Int, TdApi.BasicGroup>()
	private val supergroups = ConcurrentHashMap<Int, TdApi.Supergroup>()
	private val secretChats = ConcurrentHashMap<Int, TdApi.SecretChat>()

	private val chats = ConcurrentHashMap<Long, TdApi.Chat>()
	private val chatTitles = ConcurrentHashMap<String, Long>()
	private val chatList = TreeSet<OrderedChat>()
	private val chatLiveMessages = ConcurrentHashMap<Long, Long>()

	private val downloadChatFilesMap = ConcurrentHashMap<String, TdApi.Chat>()
	private val downloadUserFilesMap = ConcurrentHashMap<String, TdApi.User>()

	// Can contain TdApi.MessageLocation or TdApi.MessageText from osmand_bot
	private val usersLocationMessages = ConcurrentHashMap<Int, TdApi.Message>()

	private val usersFullInfo = ConcurrentHashMap<Int, TdApi.UserFullInfo>()
	private val basicGroupsFullInfo = ConcurrentHashMap<Int, TdApi.BasicGroupFullInfo>()
	private val supergroupsFullInfo = ConcurrentHashMap<Int, TdApi.SupergroupFullInfo>()

	var appDir: String? = null
	private var libraryLoaded = false
	private var telegramAuthorizationRequestHandler: TelegramAuthorizationRequestHandler? = null

	private var client: Client? = null

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

	fun addIncomingMessagesListener(listener: TelegramIncomingMessagesListener) {
		incomingMessagesListeners.add(listener)
	}

	fun removeIncomingMessagesListener(listener: TelegramIncomingMessagesListener) {
		incomingMessagesListeners.remove(listener)
	}

	fun getChatList(): TreeSet<OrderedChat> {
		synchronized(chatList) {
			return TreeSet(chatList.filter { !it.isChannel })
		}
	}

	fun getChatTitles() = chatTitles.keys().toList()

	fun getChat(id: Long) = chats[id]

	fun getUser(id: Int) = users[id]

	fun getUserMessage(user: TdApi.User) = usersLocationMessages[user.id]

	fun getMessageById(id: Long) = usersLocationMessages.values.firstOrNull { it.id == id }

	fun getChatMessages(chatTitle: String): List<TdApi.Message> {
		return usersLocationMessages.values.filter { chats[it.chatId]?.title == chatTitle }
	}

	fun getMessages() = usersLocationMessages.values.toList()

	fun getMessagesByChatIds(): Map<Long, List<TdApi.Message>> {
		val res = mutableMapOf<Long, MutableList<TdApi.Message>>()
		for (message in usersLocationMessages.values) {
			var messages = res[message.chatId]
			if (messages != null) {
				messages.add(message)
			} else {
				messages = mutableListOf(message)
				res[message.chatId] = messages
			}
		}
		return res
	}

	private fun updateChatTitles() {
		chatTitles.clear()
		for (chatEntry in chats.entries) {
			chatTitles[chatEntry.value.title] = chatEntry.key
		}
	}

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
		fun onReceiveChatLocationMessages(chatTitle: String, vararg messages: TdApi.Message)
		fun updateLocationMessages()
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
			client!!.send(TdApi.GetAuthorizationState(), defaultHandler)
			true
		} else {
			false
		}
	}

	fun isInit() = client != null && haveAuthorization

	fun getUserPhotoPath(user: TdApi.User): String? {
		return if (hasLocalUserPhoto(user)) {
			user.profilePhoto?.small?.local?.path
		} else {
			if (hasRemoteUserPhoto(user)) {
				requestUserPhoto(user)
			}
			null
		}
	}

	fun startLiveMessagesUpdates() {
		stopLiveMessagesUpdates()

		val updateLiveMessagesExecutor = Executors.newSingleThreadScheduledExecutor()
		this.updateLiveMessagesExecutor = updateLiveMessagesExecutor
		updateLiveMessagesExecutor.scheduleWithFixedDelay({
			incomingMessagesListeners.forEach { it.updateLocationMessages() }
		}, UPDATE_LIVE_MESSAGES_INTERVAL_SEC, UPDATE_LIVE_MESSAGES_INTERVAL_SEC, TimeUnit.SECONDS)
	}

	fun stopLiveMessagesUpdates() {
		updateLiveMessagesExecutor?.shutdown()
		updateLiveMessagesExecutor?.awaitTermination(1, TimeUnit.MINUTES)
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
		updateChatTitles()
		listener?.onTelegramChatsRead()
	}

	private fun requestMessage(chatId: Long, messageId: Long, onComplete: (TdApi.Message) -> Unit) {
		client?.send(TdApi.GetMessage(chatId, messageId)) { obj ->
			when (obj.constructor) {
				TdApi.Error.CONSTRUCTOR -> {
					val error = obj as TdApi.Error
					if (error.code != IGNORED_ERROR_CODE) {
						listener?.onTelegramError(error.code, error.message)
					}
				}
				TdApi.Message.CONSTRUCTOR -> onComplete(obj as TdApi.Message)
			}
		}
	}

	private fun addNewMessage(message: TdApi.Message) {
		if (message.isAppropriate()) {
			usersLocationMessages[message.senderUserId] = message
			val chatTitle = chats[message.chatId]?.title
			if (chatTitle != null) {
				incomingMessagesListeners.forEach {
					it.onReceiveChatLocationMessages(chatTitle, message)
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
	fun sendLiveLocationMessage(chatTitles: List<String>, livePeriod: Int = 61, latitude: Double, longitude: Double): Boolean {
		if (!requestingActiveLiveLocationMessages && haveAuthorization) {
			if (needRefreshActiveLiveLocationMessages) {
				getActiveLiveLocationMessages {
					sendLiveLocationImpl(chatTitles, livePeriod, latitude, longitude)
				}
				needRefreshActiveLiveLocationMessages = false
			} else {
				sendLiveLocationImpl(chatTitles, livePeriod, latitude, longitude)
			}
			return true
		}
		return false
	}

	private fun getActiveLiveLocationMessages(onComplete: (() -> Unit)?) {
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
							chatLiveMessages[chatId] = msg.id
						}
					}
					onComplete?.invoke()
				}
				else -> listener?.onSendLiveLocationError(-1, "Receive wrong response from TDLib: $obj")
			}
			requestingActiveLiveLocationMessages = false
		}
	}

	private fun sendLiveLocationImpl(chatTitles: List<String>, livePeriod: Int = 61, latitude: Double, longitude: Double) {
		val lp = livePeriod.coerceAtLeast(61)
		val location = TdApi.Location(latitude, longitude)
		val content = TdApi.InputMessageLocation(location, lp)

		for (chatTitle in chatTitles) {
			val chatId = this.chatTitles[chatTitle]
			if (chatId != null) {
				val msgId = chatLiveMessages[chatId]
				if (msgId != null) {
					if (msgId != 0L) {
						client?.send(TdApi.EditMessageLiveLocation(chatId, msgId, null, location), liveLocationMessageUpdatesHandler)
					}
				} else {
					chatLiveMessages[chatId] = 0L
					client?.send(TdApi.SendMessage(chatId, 0, false, true, null, content), liveLocationMessageUpdatesHandler)
				}
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

	private fun onAuthorizationStateUpdated(authorizationState: AuthorizationState?) {
		val prevAuthState = getTelegramAuthorizationState()
		if (authorizationState != null) {
			this.authorizationState = authorizationState
		}
		when (this.authorizationState?.constructor) {
			TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
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
			TdApi.AuthorizationStateWaitEncryptionKey.CONSTRUCTOR -> {
				client!!.send(TdApi.CheckDatabaseEncryptionKey(), AuthorizationRequestHandler())
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
		if (TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()) - lastEdited > MESSAGE_ACTIVE_TIME_SEC) {
			return false
		}
		val content = content
		return when (content) {
			is TdApi.MessageLocation -> true
			is TdApi.MessageText -> {
				if (content.text.text.startsWith("{")) {
					// TODO: get user from library if null
					if (users[senderUserId]?.username == OSMAND_BOT_USERNAME) {
						return true
					}
				}
				false
			}
			else -> false
		}
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
					updateChatTitles()
					listener?.onTelegramChatsChanged()
				}
				TdApi.UpdateChatTitle.CONSTRUCTOR -> {
					val updateChat = obj as TdApi.UpdateChatTitle
					val chat = chats[updateChat.chatId]
					if (chat != null) {
						synchronized(chat) {
							chat.title = updateChat.title
						}
						updateChatTitles()
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
				TdApi.UpdateMessageContent.CONSTRUCTOR -> {
					val updateMessageContent = obj as TdApi.UpdateMessageContent
					val message = getMessageById(updateMessageContent.messageId)
					if (message == null) {
						updateMessageContent.apply {
							requestMessage(chatId, messageId, this@TelegramHelper::addNewMessage)
						}
					} else {
						synchronized(message) {
							message.content = updateMessageContent.newContent
						}
						val chatTitle = chats[message.chatId]?.title
						if (chatTitle != null) {
							incomingMessagesListeners.forEach {
								it.onReceiveChatLocationMessages(chatTitle, message)
							}
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
					chatLiveMessages[message.chatId] = message.id
				}
				TdApi.UpdateDeleteMessages.CONSTRUCTOR -> {
					val updateDeleteMessages = obj as TdApi.UpdateDeleteMessages
					if (updateDeleteMessages.isPermanent) {
						val chatId = updateDeleteMessages.chatId
						for (messageId in updateDeleteMessages.messageIds) {
							if (chatLiveMessages[chatId] == messageId) {
								chatLiveMessages.remove(chatId)
								break
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
				TdApi.UpdateNotificationSettings.CONSTRUCTOR -> {
					val update = obj as TdApi.UpdateNotificationSettings
					if (update.scope is TdApi.NotificationSettingsScopeChat) {
						val chat = chats[(update.scope as TdApi.NotificationSettingsScopeChat).chatId]
						if (chat != null) {
							synchronized(chat) {
								chat.notificationSettings = update.notificationSettings
							}
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
							return
						}
						val user = downloadUserFilesMap.remove(remoteId)
						if (user != null) {
							synchronized(user) {
								user.profilePhoto?.small = updateFile.file
							}
							listener?.onTelegramUserChanged(user)
							return
						}
					}
				}

				TdApi.UpdateUserFullInfo.CONSTRUCTOR -> {
					val updateUserFullInfo = obj as TdApi.UpdateUserFullInfo
					usersFullInfo[updateUserFullInfo.userId] = updateUserFullInfo.userFullInfo
				}
				TdApi.UpdateBasicGroupFullInfo.CONSTRUCTOR -> {
					val updateBasicGroupFullInfo = obj as TdApi.UpdateBasicGroupFullInfo
					basicGroupsFullInfo[updateBasicGroupFullInfo.basicGroupId] = updateBasicGroupFullInfo.basicGroupFullInfo
				}
				TdApi.UpdateSupergroupFullInfo.CONSTRUCTOR -> {
					val updateSupergroupFullInfo = obj as TdApi.UpdateSupergroupFullInfo
					supergroupsFullInfo[updateSupergroupFullInfo.supergroupId] = updateSupergroupFullInfo.supergroupFullInfo
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
