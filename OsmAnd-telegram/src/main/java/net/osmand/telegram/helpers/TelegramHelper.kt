package net.osmand.telegram.helpers

import android.text.TextUtils
import net.osmand.telegram.helpers.TelegramHelper.TelegramAuthenticationParameterType.*
import net.osmand.PlatformUtil
import org.drinkless.td.libcore.telegram.Client
import org.drinkless.td.libcore.telegram.Client.ResultHandler
import org.drinkless.td.libcore.telegram.TdApi
import org.drinkless.td.libcore.telegram.TdApi.AuthorizationState
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap


class TelegramHelper private constructor() {

    companion object {
        private val log = PlatformUtil.getLog(TelegramHelper::class.java)
        private const val CHATS_LIMIT = 100

        private var helper: TelegramHelper? = null

        private val users = ConcurrentHashMap<Int, TdApi.User>()
        private val basicGroups = ConcurrentHashMap<Int, TdApi.BasicGroup>()
        private val supergroups = ConcurrentHashMap<Int, TdApi.Supergroup>()
        private val secretChats = ConcurrentHashMap<Int, TdApi.SecretChat>()

        private val chats = ConcurrentHashMap<Long, TdApi.Chat>()
        private val chatList = TreeSet<OrderedChat>()

        private val usersFullInfo = ConcurrentHashMap<Int, TdApi.UserFullInfo>()
        private val basicGroupsFullInfo = ConcurrentHashMap<Int, TdApi.BasicGroupFullInfo>()
        private val supergroupsFullInfo = ConcurrentHashMap<Int, TdApi.SupergroupFullInfo>()

        val instance: TelegramHelper
            get() {
                if (helper == null) {
                    helper = TelegramHelper()
                }
                return helper!!
            }

        private fun setChatOrder(chat: TdApi.Chat, order: Long) {
            synchronized(chatList) {
                if (chat.order != 0L) {
                    chatList.remove(OrderedChat(chat.order, chat.id))
                }

                chat.order = order

                if (chat.order != 0L) {
                    chatList.add(OrderedChat(chat.order, chat.id))
                }
            }
        }
    }

    var appDir: String? = null
    private var libraryLoaded = false
    private var telegramAuthorizationRequestHandler: TelegramAuthorizationRequestHandler? = null

    private var client: Client? = null
    private var haveFullChatList: Boolean = false

    private var authorizationState: AuthorizationState? = null
    private var isHaveAuthorization = false

    private val defaultHandler = DefaultHandler()

    var listener: TelegramListener? = null

    fun getChatList(): TreeSet<OrderedChat> {
        synchronized(chatList) {
            return TreeSet<OrderedChat>(chatList)
        }
    }

    fun getChat(id: Long): TdApi.Chat? {
        return chats[id]
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
        fun onTelegramError(code: Int, message: String)
        fun onSendLiveLicationError(code: Int, message: String)
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

    fun requestChats() {
        synchronized(chatList) {
            if (!haveFullChatList && CHATS_LIMIT > chatList.size) {
                // have enough chats in the chat list or chat list is too small
                var offsetOrder = java.lang.Long.MAX_VALUE
                var offsetChatId: Long = 0
                if (!chatList.isEmpty()) {
                    val last = chatList.last()
                    offsetOrder = last.order
                    offsetChatId = last.chatId
                }
                client?.send(TdApi.GetChats(offsetOrder, offsetChatId, CHATS_LIMIT - chatList.size), { obj ->
                    when (obj.constructor) {
                        TdApi.Error.CONSTRUCTOR -> {
                            val error = obj as TdApi.Error
                            listener?.onTelegramError(error.code, error.message)
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
                })
                return
            }
        }
        listener?.onTelegramChatsRead()
    }

    /**
     * @chatId Id of the chat
     * @livePeriod Period for which the location can be updated, in seconds; should be between 60 and 86400 for a live location and 0 otherwise.
     * @latitude Latitude of the location
     * @longitude Longitude of the location
     */
    fun sendLiveLocation(chatIds: List<Long>, livePeriod: Int = 61, latitude: Double, longitude: Double) {

        val lp = livePeriod.coerceAtLeast(61)
        val location = TdApi.Location(latitude, longitude)
        val content = TdApi.InputMessageLocation(location, lp)

        client?.send(TdApi.GetActiveLiveLocationMessages(), { obj ->
            when (obj.constructor) {
                TdApi.Error.CONSTRUCTOR -> {
                    val error = obj as TdApi.Error
                    listener?.onSendLiveLicationError(error.code, error.message)
                }
                TdApi.Messages.CONSTRUCTOR -> {
                    val messages = (obj as TdApi.Messages).messages
                    val processedChatIds = mutableListOf<Long>()
                    if (messages.isNotEmpty()) {
                        for (msg in messages) {
                            if (chatIds.contains(msg.chatId)) {
                                processedChatIds.add(msg.chatId)
                                client?.send(TdApi.EditMessageLiveLocation(msg.chatId, msg.id, null, location), { o->
                                    when (o.constructor) {
                                        TdApi.Error.CONSTRUCTOR -> {
                                            val error = o as TdApi.Error
                                            listener?.onSendLiveLicationError(error.code, error.message)
                                        }
                                        else -> listener?.onSendLiveLicationError(-1, "Receive wrong response from TDLib: $o")
                                    }
                                })
                            }
                        }
                    }
                    if (chatIds.size != processedChatIds.size) {
                        val mutableChatIds = chatIds.toMutableList()
                        mutableChatIds.removeAll(processedChatIds)
                        for (chatId in mutableChatIds) {
                            client?.send(TdApi.SendMessage(chatId, 0, false, true, null, content), { o->
                                when (o.constructor) {
                                    TdApi.Error.CONSTRUCTOR -> {
                                        val error = o as TdApi.Error
                                        listener?.onSendLiveLicationError(error.code, error.message)
                                    }
                                    else -> listener?.onSendLiveLicationError(-1, "Receive wrong response from TDLib: $o")
                                }
                            })
                        }
                    }
                }
                else -> listener?.onSendLiveLicationError(-1, "Receive wrong response from TDLib: $obj")
            }
        })
    }

    /**
     * @chatId Id of the chat
     * @message Text of the message
     */
    fun sendText(chatId: Long, message: String) {
        // initialize reply markup just for testing
        //val row = arrayOf(TdApi.InlineKeyboardButton("https://telegram.org?1", TdApi.InlineKeyboardButtonTypeUrl()), TdApi.InlineKeyboardButton("https://telegram.org?2", TdApi.InlineKeyboardButtonTypeUrl()), TdApi.InlineKeyboardButton("https://telegram.org?3", TdApi.InlineKeyboardButtonTypeUrl()))
        //val replyMarkup = TdApi.ReplyMarkupInlineKeyboard(arrayOf(row, row, row))

        val content = TdApi.InputMessageText(TdApi.FormattedText(message, null), false, true)
        client?.send(TdApi.SendMessage(chatId, 0, false, true, null, content), defaultHandler)
    }

    fun logout(): Boolean {
        return if (libraryLoaded) {
            isHaveAuthorization = false
            client!!.send(TdApi.LogOut(), defaultHandler)
            true
        } else {
            false
        }
    }

    fun close(): Boolean {
        return if (libraryLoaded) {
            isHaveAuthorization = false
            client!!.send(TdApi.Close(), defaultHandler)
            true
        } else {
            false
        }
    }

    private fun onAuthorizationStateUpdated(authorizationState: AuthorizationState?) {
        val prevAuthState = getTelegramAuthorizationState()
        if (authorizationState != null) {
            this.authorizationState = authorizationState
        }
        when (this.authorizationState!!.constructor) {
            TdApi.AuthorizationStateWaitTdlibParameters.CONSTRUCTOR -> {
                log.info("Init tdlib parameters")

                val parameters = TdApi.TdlibParameters()
                parameters.databaseDirectory = File(appDir, "tdlib").absolutePath
                parameters.useMessageDatabase = true
                parameters.useSecretChats = true
                parameters.apiId = 94575
                parameters.apiHash = "a3406de8d171bb422bb6ddf3bbd800e2"
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
                isHaveAuthorization = true
                log.info("Ready")
            }
            TdApi.AuthorizationStateLoggingOut.CONSTRUCTOR -> {
                isHaveAuthorization = false
                log.info("Logging out")
            }
            TdApi.AuthorizationStateClosing.CONSTRUCTOR -> {
                isHaveAuthorization = false
                log.info("Closing")
            }
            TdApi.AuthorizationStateClosed.CONSTRUCTOR -> {
                log.info("Closed")
            }
            else -> log.error("Unsupported authorization state: " + this.authorizationState!!)
        }
        val newAuthState = getTelegramAuthorizationState()
        listener?.onTelegramStatusChanged(prevAuthState, newAuthState)
    }

    class OrderedChat internal constructor(internal val order: Long, internal val chatId: Long) : Comparable<OrderedChat> {

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
                    synchronized(chat!!) {
                        if (chat.type !is TdApi.ChatTypeSupergroup || !(chat.type as TdApi.ChatTypeSupergroup).isChannel) {
                            chats[chat.id] = chat

                            val order = chat.order
                            chat.order = 0
                            setChatOrder(chat, order)
                        }
                    }
                    listener?.onTelegramChatsRead()
                }
                TdApi.UpdateChatTitle.CONSTRUCTOR -> {
                    val updateChat = obj as TdApi.UpdateChatTitle
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.title = updateChat.title
                    }
                }
                TdApi.UpdateChatPhoto.CONSTRUCTOR -> {
                    val updateChat = obj as TdApi.UpdateChatPhoto
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.photo = updateChat.photo
                    }
                }
                TdApi.UpdateChatLastMessage.CONSTRUCTOR -> {
                    val updateChat = obj as TdApi.UpdateChatLastMessage
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.lastMessage = updateChat.lastMessage
                        setChatOrder(chat, updateChat.order)
                    }
                }
                TdApi.UpdateChatOrder.CONSTRUCTOR -> {
                    val updateChat = obj as TdApi.UpdateChatOrder
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        setChatOrder(chat, updateChat.order)
                    }
                }
                TdApi.UpdateChatIsPinned.CONSTRUCTOR -> {
                    val updateChat = obj as TdApi.UpdateChatIsPinned
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.isPinned = updateChat.isPinned
                        setChatOrder(chat, updateChat.order)
                    }
                }
                TdApi.UpdateChatReadInbox.CONSTRUCTOR -> {
                    val updateChat = obj as TdApi.UpdateChatReadInbox
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.lastReadInboxMessageId = updateChat.lastReadInboxMessageId
                        chat.unreadCount = updateChat.unreadCount
                    }
                }
                TdApi.UpdateChatReadOutbox.CONSTRUCTOR -> {
                    val updateChat = obj as TdApi.UpdateChatReadOutbox
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.lastReadOutboxMessageId = updateChat.lastReadOutboxMessageId
                    }
                }
                TdApi.UpdateChatUnreadMentionCount.CONSTRUCTOR -> {
                    val updateChat = obj as TdApi.UpdateChatUnreadMentionCount
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.unreadMentionCount = updateChat.unreadMentionCount
                    }
                }
                TdApi.UpdateMessageMentionRead.CONSTRUCTOR -> {
                    val updateChat = obj as TdApi.UpdateMessageMentionRead
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.unreadMentionCount = updateChat.unreadMentionCount
                    }
                }

                TdApi.UpdateMessageSendSucceeded.CONSTRUCTOR -> {
                    val updateMessageSent = obj as TdApi.UpdateMessageSendSucceeded
                }

                TdApi.UpdateChatReplyMarkup.CONSTRUCTOR -> {
                    val updateChat = obj as TdApi.UpdateChatReplyMarkup
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.replyMarkupMessageId = updateChat.replyMarkupMessageId
                    }
                }
                TdApi.UpdateChatDraftMessage.CONSTRUCTOR -> {
                    val updateChat = obj as TdApi.UpdateChatDraftMessage
                    val chat = chats[updateChat.chatId]
                    synchronized(chat!!) {
                        chat.draftMessage = updateChat.draftMessage
                        setChatOrder(chat, updateChat.order)
                    }
                }
                TdApi.UpdateNotificationSettings.CONSTRUCTOR -> {
                    val update = obj as TdApi.UpdateNotificationSettings
                    if (update.scope is TdApi.NotificationSettingsScopeChat) {
                        val chat = chats[(update.scope as TdApi.NotificationSettingsScopeChat).chatId]
                        synchronized(chat!!) {
                            chat.notificationSettings = update.notificationSettings
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
            }// print("Unsupported update:" + newLine + object);
        }
    }

    private inner class AuthorizationRequestHandler : ResultHandler {
        override fun onResult(obj: TdApi.Object) {
            when (obj.constructor) {
                TdApi.Error.CONSTRUCTOR -> {
                    log.error("Receive an error: $obj")
                    val errorObj = obj as TdApi.Error
                    telegramAuthorizationRequestHandler?.telegramAuthorizationRequestListener?.onTelegramAuthorizationRequestError(errorObj.code, errorObj.message)
                    onAuthorizationStateUpdated(null) // repeat last action
                }
                TdApi.Ok.CONSTRUCTOR -> {
                }
                else -> log.error("Receive wrong response from TDLib: $obj")
            }// result is already received through UpdateAuthorizationState, nothing to do
        }
    }
}
