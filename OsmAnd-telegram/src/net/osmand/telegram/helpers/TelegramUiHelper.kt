package net.osmand.telegram.helpers

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
import net.osmand.data.LatLon
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.TelegramHelper.MessageOsmAndBotLocation
import org.drinkless.td.libcore.telegram.TdApi

object TelegramUiHelper {

	fun setupPhoto(
		app: TelegramApplication,
		iv: ImageView?,
		photoPath: String?,
		placeholderId: Int = R.drawable.ic_group
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
			drawable = app.uiUtils.getThemedIcon(placeholderId)
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
			title = chat.title
			photoPath = chat.photo?.small?.local?.path
			placeholderId = R.drawable.ic_group
		}
		val chatType = chat.type
		if (chatType is TdApi.ChatTypePrivate && !helper.isBot(chatType.userId)) {
			res.userId = chatType.userId
			val content = messages.firstOrNull()?.content
			if (content is TdApi.MessageLocation) {
				res.latLon = LatLon(content.location.latitude, content.location.longitude)
			}
		}
		return res
	}

	fun messageToLocationItem(
		helper: TelegramHelper,
		chat: TdApi.Chat,
		message: TdApi.Message
	): LocationItem? {
		val content = message.content
		return when (content) {
			is MessageOsmAndBotLocation -> botMessageToLocationItem(chat, content)
			is TdApi.MessageLocation -> locationMessageToLocationItem(helper, chat, message)
			else -> null
		}
	}

	private fun botMessageToLocationItem(
		chat: TdApi.Chat,
		content: MessageOsmAndBotLocation
	): LocationItem? {
		return if (content.isValid()) {
			LocationItem().apply {
				chatTitle = chat.title
				name = content.name
				latLon = LatLon(content.lat, content.lon)
				placeholderId = R.drawable.ic_group
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
		val content = message.content as TdApi.MessageLocation
		return LocationItem().apply {
			chatTitle = chat.title
			name = "${user.firstName} ${user.lastName}".trim()
			if (name.isEmpty()) {
				name = user.username
			}
			if (name.isEmpty()) {
				name = user.phoneNumber
			}
			latLon = LatLon(content.location.latitude, content.location.longitude)
			photoPath = helper.getUserPhotoPath(user)
			placeholderId = R.drawable.ic_group
			senderUserId = message.senderUserId
		}
	}

	class ChatItem {
		var title: String = ""
			internal set
		var latLon: LatLon? = null
			internal set
		var photoPath: String? = null
			internal set
		var placeholderId: Int = 0
			internal set
		var userId: Int = 0
			internal set

		fun canBeOpenedOnMap() = latLon != null && userId != 0
	}

	class LocationItem {
		var chatTitle: String = ""
			internal set
		var name: String = ""
			internal set
		var latLon: LatLon? = null
			internal set
		var photoPath: String? = null
			internal set
		var placeholderId: Int = 0
			internal set
		var senderUserId: Int = 0
			internal set

		fun canBeOpenedOnMap() = latLon != null
	}
}
