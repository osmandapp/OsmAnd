package net.osmand.telegram.helpers

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.text.TextUtils
import net.osmand.aidl.map.ALatLon
import net.osmand.aidl.maplayer.point.AMapPoint
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.TelegramHelper.MessageOsmAndBotLocation
import net.osmand.telegram.utils.AndroidUtils
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File

class ShowLocationHelper(private val app: TelegramApplication) {

	companion object {
		const val MAP_LAYER_ID = "telegram_layer"
	}

	private val telegramHelper = app.telegramHelper
	private val osmandAidlHelper = app.osmandAidlHelper

	var showingLocation: Boolean = false
		private set

	fun setupMapLayer() {
		execOsmandApi {
			osmandAidlHelper.addMapLayer(MAP_LAYER_ID, "Telegram", 5.5f, null)
		}
	}

	fun updateLocationsOnMap() {
		execOsmandApi {
			val messages = telegramHelper.getMessages()
			for (message in messages) {
				val chatTitle = telegramHelper.getChat(message.chatId)?.title
				val date = Math.max(message.date, message.editDate) * 1000L
				val expired = System.currentTimeMillis() - date > app.settings.userLocationExpireTime
				if (chatTitle != null && expired) {
					removeMapPoint(chatTitle, message)
				}
			}
		}
	}

	fun addLocationToMap(message: TdApi.Message) {
		execOsmandApi {
			val chatTitle = telegramHelper.getChat(message.chatId)?.title
			val content = message.content
			if (chatTitle != null && content is TdApi.MessageLocation) {
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
						app.grantUriPermission(OsmandAidlHelper.OSMAND_PACKAGE_NAME, photoUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
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
				osmandAidlHelper.addMapPoint(MAP_LAYER_ID, "${chatTitle}_${message.senderUserId}", userName, userName,
						chatTitle, Color.RED, ALatLon(content.location.latitude, content.location.longitude), null, params)
			} else if (chatTitle != null && content is MessageOsmAndBotLocation && content.isValid()) {
				val name = content.name
				setupMapLayer()
				osmandAidlHelper.addMapPoint(MAP_LAYER_ID, "${chatTitle}_$name", name, name,
						chatTitle, Color.RED, ALatLon(content.lat, content.lon), null, null)
			}
		}
	}

	fun showChatMessages(chatTitle: String) {
		execOsmandApi {
			val messages = telegramHelper.getChatMessages(chatTitle)
			for (message in messages) {
				addLocationToMap(message)
			}
		}
	}

	fun hideChatMessages(chatTitle: String) {
		execOsmandApi {
			val messages = telegramHelper.getChatMessages(chatTitle)
			for (message in messages) {
				val user = telegramHelper.getUser(message.senderUserId)
				if (user != null) {
					removeMapPoint(chatTitle, message)
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

	private fun removeMapPoint(chatTitle: String, message: TdApi.Message) {
		val content = message.content
		if (content is TdApi.MessageLocation) {
			osmandAidlHelper.removeMapPoint(MAP_LAYER_ID, "${chatTitle}_${message.senderUserId}")
		} else if (content is MessageOsmAndBotLocation) {
			osmandAidlHelper.removeMapPoint(MAP_LAYER_ID, "${chatTitle}_${content.name}")
		}
	}

	private fun execOsmandApi(action: (() -> Unit)) {
		if (osmandAidlHelper.isOsmandConnected()) {
			action.invoke()
		} else if (osmandAidlHelper.isOsmandBound()) {
			osmandAidlHelper.connectOsmand()
		}
	}
}