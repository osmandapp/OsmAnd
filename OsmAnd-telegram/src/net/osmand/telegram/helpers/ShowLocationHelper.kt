package net.osmand.telegram.helpers

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.TextUtils
import net.osmand.aidl.map.ALatLon
import net.osmand.aidl.maplayer.point.AMapPoint
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.utils.AndroidUtils
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File

class ShowLocationHelper(private val app: TelegramApplication) {

	companion object {
		private const val MAP_LAYER_ID = "telegram_layer"
	}

	private val telegramHelper = app.telegramHelper
	private val osmandHelper = app.osmandHelper

	var showingLocation: Boolean = false
		private set

	fun setupMapLayer() {
		osmandHelper.addMapLayer(MAP_LAYER_ID, "Telegram", 5.5f, null)
	}

	fun showLocationOnMap(chatTitle: String, message: TdApi.Message) {
		if (osmandHelper.isOsmandConnected()) {
			val content = message.content
			if (content is TdApi.MessageLocation) {
				var userName = ""
				var photoUri: Uri? = null
				val user = telegramHelper.getUser(message.senderUserId)
				if (user != null) {
					userName = "${user.firstName} ${user.lastName}".trim()
					if (userName.isEmpty()) {
						userName = user.username
					}
					if (userName.isEmpty()) {
						userName = user.phoneNumber
					}
					val photoPath = telegramHelper.getUserPhotoPath(user)
					if (!TextUtils.isEmpty(photoPath)) {
						photoUri = AndroidUtils.getUriForFile(app, File(photoPath))
						app.grantUriPermission("net.osmand.plus", photoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
					}
				}
				if (userName.isEmpty()) {
					userName = message.senderUserId.toString()
				}
				setupMapLayer()
				val params = mutableMapOf<String, String>()
				if (photoUri != null) {
					params[AMapPoint.POINT_IMAGE_URI_PARAM] = photoUri.toString()
				}
				osmandHelper.addMapPoint(MAP_LAYER_ID, "${chatTitle}_${message.senderUserId}", userName, userName,
						chatTitle, Color.RED, ALatLon(content.location.latitude, content.location.longitude), null, params)
			}
		} else if (osmandHelper.isOsmandBound()) {
			osmandHelper.connectOsmand()
		}
	}

	fun showChatMessages(chatTitle: String) {
		if (osmandHelper.isOsmandConnected()) {
			val messages = telegramHelper.getChatMessages(chatTitle)
			for (message in messages) {
				showLocationOnMap(chatTitle, message)
			}
		}
	}

	fun hideChatMessages(chatTitle: String) {
		if (osmandHelper.isOsmandConnected()) {
			val messages = telegramHelper.getChatMessages(chatTitle)
			for (message in messages) {
				val user = telegramHelper.getUser(message.senderUserId)
				if (user != null) {
					osmandHelper.removeMapPoint(MAP_LAYER_ID, "${chatTitle}_${message.senderUserId}")
				}
			}
		}
	}

	fun startShowingLocation() {
		if (!showingLocation) {
			showingLocation = true
			app.startUserLocationService()
		}
	}

	fun stopShowingLocation() {
		if (showingLocation) {
			showingLocation = false
			app.stopUserLocationService()
		}
	}
}