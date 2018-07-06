package net.osmand.telegram.helpers

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.widget.ImageView
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
			val content = messages.firstOrNull()?.content
			if (content is TdApi.MessageLocation) {
				res.lat = content.location.latitude
				res.lon = content.location.longitude
			}
		}
		return res
	}

	fun messageToLocationItem(helper: TelegramHelper, message: TdApi.Message): LocationItem? {
		val content = message.content
		return when (content) {
			is MessageOsmAndBotLocation -> botMessageToLocationItem(content)
			is TdApi.MessageLocation -> locationMessageToLocationItem(helper, message)
			else -> null
		}
	}

	private fun botMessageToLocationItem(content: MessageOsmAndBotLocation): LocationItem? {
		return if (content.isValid()) {
			LocationItem().apply {
				name = content.name
				lat = content.lat
				lon = content.lon
				placeholderId = R.drawable.ic_group
			}
		} else {
			null
		}
	}

	private fun locationMessageToLocationItem(
		helper: TelegramHelper,
		message: TdApi.Message
	): LocationItem? {
		val user = helper.getUser(message.senderUserId) ?: return null
		val content = message.content as TdApi.MessageLocation
		return LocationItem().apply {
			name = "${user.firstName} ${user.lastName}".trim()
			if (name.isEmpty()) {
				name = user.username
			}
			if (name.isEmpty()) {
				name = user.phoneNumber
			}
			lat = content.location.latitude
			lon = content.location.longitude
			photoPath = helper.getUserPhotoPath(user)
			placeholderId = R.drawable.ic_group
		}
	}

	class ChatItem {
		var title: String = ""
			internal set
		var lat: Double = 0.0
			internal set
		var lon: Double = 0.0
			internal set
		var photoPath: String? = null
			internal set
		var placeholderId: Int = 0
			internal set
	}

	class LocationItem {
		var name: String = ""
			internal set
		var lat: Double = 0.0
			internal set
		var lon: Double = 0.0
			internal set
		var photoPath: String? = null
			internal set
		var placeholderId: Int = 0
			internal set
	}
}
