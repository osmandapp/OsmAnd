package net.osmand.telegram.helpers

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import net.osmand.data.LatLon
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.TelegramHelper.MessageOsmAndBotLocation
import net.osmand.telegram.helpers.TelegramHelper.MessageUserTextLocation
import org.drinkless.td.libcore.telegram.TdApi

object TelegramUiHelper {

	fun setupPhoto(
		app: TelegramApplication,
		iv: ImageView?,
		photoPath: String?,
		placeholderId: Int,
		useThemedIcon: Boolean
	) {
		if (iv == null) {
			return
		}
		var drawable: Drawable? = null
		var bitmap: Bitmap? = null
		if (photoPath != null && photoPath.isNotEmpty()) {
			bitmap = app.uiUtils.getCircleBitmap(photoPath)
		}
		if (bitmap == null) {
			drawable = if (useThemedIcon) {
				app.uiUtils.getThemedIcon(placeholderId)
			} else {
				app.uiUtils.getIcon(placeholderId)
			}
		}
		if (bitmap != null) {
			iv.setImageBitmap(bitmap)
		} else {
			iv.setImageDrawable(drawable)
		}
	}

	fun chatToChatItem(
		helper: TelegramHelper,
		chat: TdApi.Chat,
		messages: List<TdApi.Message>
	): ChatItem {
		val res = ChatItem().apply {
			chatId = chat.id
			chatTitle = chat.title
			name = chat.title
			photoPath = chat.photo?.small?.local?.path
			placeholderId = R.drawable.img_user_picture
		}
		val type = chat.type
		if (type is TdApi.ChatTypePrivate || type is TdApi.ChatTypeSecret) {
			val userId = helper.getUserIdFromChatType(type)
			val chatWithBot = helper.isBot(userId)
			res.privateChat = true
			res.chatWithBot = chatWithBot
			if (!chatWithBot) {
				res.userId = userId
				val user = helper.getUser(userId)
				val message = messages.firstOrNull { it.viaBotUserId == 0 }
				if (message != null) {
					res.lastUpdated = helper.getLastUpdatedTime(message)
					val content = message.content
					if (content is TdApi.MessageLocation) {
						res.latLon = LatLon(content.location.latitude, content.location.longitude)
					} else if (content is MessageUserTextLocation) {
						res.latLon = LatLon(content.lat, content.lon)
					}
				}
				if (user != null) {
					res.grayscalePhotoPath = helper.getUserGreyPhotoPath(user)
				}
			}
		} else if (type is TdApi.ChatTypeBasicGroup) {
			res.placeholderId = R.drawable.img_group_picture
			res.membersCount = helper.getBasicGroupFullInfo(type.basicGroupId)?.members?.size ?: 0
		} else if (type is TdApi.ChatTypeSupergroup) {
			res.placeholderId = R.drawable.img_group_picture
			res.membersCount = helper.getSupergroupFullInfo(type.supergroupId)?.memberCount ?: 0
		}
		if (!res.privateChat) {
			res.liveMembersCount = messages.size
		}

		return res
	}

	fun getUserName(user: TdApi.User): String {
		var name = "${user.firstName} ${user.lastName}".trim()
		if (name.isEmpty()) {
			name = user.username
		}
		if (name.isEmpty()) {
			name = user.phoneNumber
		}
		return name
	}

	fun messageToLocationItem(
		helper: TelegramHelper,
		chat: TdApi.Chat,
		message: TdApi.Message
	): LocationItem? {
		val content = message.content
		return when (content) {
			is MessageOsmAndBotLocation -> botMessageToLocationItem(chat, content)
			is MessageUserTextLocation -> locationMessageToLocationItem(helper, chat, message)
			is TdApi.MessageLocation -> locationMessageToLocationItem(helper, chat, message)
			else -> null
		}
	}
	
	fun messageToChatItem(
		helper: TelegramHelper,
		chat: TdApi.Chat,
		message: TdApi.Message
	): ChatItem? {
		val content = message.content
		return when (content) {
			is MessageOsmAndBotLocation -> botMessageToChatItem(helper, chat, content)
			is TdApi.MessageLocation -> locationMessageToChatItem(helper, chat, message)
			is MessageUserTextLocation -> locationMessageToChatItem(helper, chat, message)
			else -> null
		}
	}

	private fun botMessageToLocationItem(
		chat: TdApi.Chat,
		content: MessageOsmAndBotLocation
	): LocationItem? {
		return if (content.isValid()) {
			LocationItem().apply {
				chatId = chat.id
				chatTitle = chat.title
				name = content.name
				latLon = LatLon(content.lat, content.lon)
				placeholderId = R.drawable.img_user_picture
				lastUpdated = content.lastUpdated
			}
		} else {
			null
		}
	}

	private fun locationMessageToLocationItem(
		helper: TelegramHelper,
		chat: TdApi.Chat,
		message: TdApi.Message
	): LocationItem? {
		val user = helper.getUser(message.senderUserId) ?: return null
		val content = message.content
		return LocationItem().apply {
			chatId = chat.id
			chatTitle = chat.title
			name = TelegramUiHelper.getUserName(user)
			latLon = when (content) {
				is TdApi.MessageLocation -> LatLon(content.location.latitude, content.location.longitude)
				is MessageUserTextLocation -> LatLon(content.lat, content.lon)
				else -> null
			}
			photoPath = helper.getUserPhotoPath(user)
			grayscalePhotoPath = helper.getUserGreyPhotoPath(user)
			placeholderId = R.drawable.img_user_picture
			userId = message.senderUserId
			lastUpdated = helper.getLastUpdatedTime(message)
		}
	}

	private fun botMessageToChatItem(
		helper: TelegramHelper,
		chat: TdApi.Chat,
		content: MessageOsmAndBotLocation
	): ChatItem? {
		return if (content.isValid()) {
			ChatItem().apply {
				chatId = chat.id
				chatTitle = chat.title
				name = content.name
				latLon = LatLon(content.lat, content.lon)
				photoPath = chat.photo?.small?.local?.path
				placeholderId = R.drawable.img_user_picture
				privateChat = helper.isPrivateChat(chat) || helper.isSecretChat(chat)
				lastUpdated = content.lastUpdated
				chatWithBot = true
			}
		} else {
			null
		}
	}

	private fun locationMessageToChatItem(
		helper: TelegramHelper,
		chat: TdApi.Chat,
		message: TdApi.Message
	): ChatItem? {
		val user = helper.getUser(message.senderUserId) ?: return null
		val content = message.content
		return ChatItem().apply {
			chatId = chat.id
			chatTitle = chat.title
			name = TelegramUiHelper.getUserName(user)
			latLon = when (content) {
				is TdApi.MessageLocation -> LatLon(content.location.latitude, content.location.longitude)
				is MessageUserTextLocation -> LatLon(content.lat, content.lon)
				else -> null
			}
			if (helper.isGroup(chat)) {
				photoPath = helper.getUserPhotoPath(user)
				groupPhotoPath = chat.photo?.small?.local?.path
			} else {
				photoPath = chat.photo?.small?.local?.path
			}
			grayscalePhotoPath = helper.getUserGreyPhotoPath(user)
			placeholderId = R.drawable.img_user_picture
			userId = message.senderUserId
			privateChat = helper.isPrivateChat(chat) || helper.isSecretChat(chat)
			chatWithBot = helper.isBot(userId)
			lastUpdated = helper.getLastUpdatedTime(message)
		}
	}

	abstract class ListItem {

		var chatId: Long = 0
			internal set
		var chatTitle: String = ""
			internal set
		var name: String = ""
			internal set
		var latLon: LatLon? = null
			internal set
		var photoPath: String? = null
			internal set
		var grayscalePhotoPath: String? = null
			internal set
		var placeholderId: Int = 0
			internal set
		var userId: Int = 0
			internal set
		var lastUpdated: Int = 0
			internal set

		abstract fun canBeOpenedOnMap(): Boolean

		abstract fun getMapPointId(): String

		abstract fun getVisibleName(): String
	}

	class ChatItem : ListItem() {

		var groupPhotoPath: String? = null
			internal set
		var privateChat: Boolean = false
			internal set
		var chatWithBot: Boolean = false
			internal set
		var membersCount: Int = 0
			internal set
		var liveMembersCount: Int = 0
			internal set

		override fun canBeOpenedOnMap() = latLon != null

		override fun getMapPointId() = "${chatId}_$userId"

		override fun getVisibleName() = chatTitle
	}

	class LocationItem : ListItem() {

		override fun canBeOpenedOnMap() = latLon != null

		override fun getMapPointId(): String {
			val id = if (userId != 0) userId.toString() else name
			return "${chatId}_$id"
		}

		override fun getVisibleName() = name
	}
}
