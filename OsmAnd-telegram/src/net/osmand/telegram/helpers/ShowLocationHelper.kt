package net.osmand.telegram.helpers

import android.content.Intent
import android.graphics.Color
import android.text.TextUtils
import net.osmand.aidl.map.ALatLon
import net.osmand.aidl.maplayer.point.AMapPoint
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.TelegramHelper.MessageOsmAndBotLocation
import net.osmand.telegram.helpers.TelegramUiHelper.ListItem
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

	fun showLocationOnMap(item: ListItem) {
		if (item.latLon == null) {
			return
		}
		execOsmandApi {
			osmandAidlHelper.showMapPoint(
				MAP_LAYER_ID,
				item.getMapPointId(),
				item.getVisibleName(),
				item.getVisibleName(),
				item.chatTitle,
				Color.RED,
				ALatLon(item.latLon!!.latitude, item.latLon!!.longitude),
				null,
				generatePhotoParams(item.photoPath)
			)
		}
	}

	fun updateLocationsOnMap() {
		execOsmandApi {
			val messages = telegramHelper.getMessages()
			for (message in messages) {
				val date = Math.max(message.date, message.editDate) * 1000L
				val expired = System.currentTimeMillis() - date > app.settings.locHistoryTime
				if (expired) {
					removeMapPoint(message.chatId, message)
				}
			}
		}
	}

	fun addLocationToMap(message: TdApi.Message) {
		execOsmandApi {
			val chatId = message.chatId
			val chatTitle = telegramHelper.getChat(message.chatId)?.title
			val content = message.content
			if (chatTitle != null && content is TdApi.MessageLocation) {
				var userName = ""
				var photoPath: String? = null
				val user = telegramHelper.getUser(message.senderUserId)
				if (user != null) {
					userName = "${user.firstName} ${user.lastName}".trim()
					if (userName.isEmpty()) {
						userName = user.username
					}
					if (userName.isEmpty()) {
						userName = user.phoneNumber
					}
					photoPath = telegramHelper.getUserPhotoPath(user)
				}
				if (userName.isEmpty()) {
					userName = message.senderUserId.toString()
				}
				setupMapLayer()
				val params = generatePhotoParams(photoPath)
				osmandAidlHelper.addMapPoint(MAP_LAYER_ID, "${chatId}_${message.senderUserId}", userName, userName,
						chatTitle, Color.RED, ALatLon(content.location.latitude, content.location.longitude), null, params)
			} else if (chatTitle != null && content is MessageOsmAndBotLocation && content.isValid()) {
				val name = content.name
				setupMapLayer()
				osmandAidlHelper.addMapPoint(MAP_LAYER_ID, "${chatId}_$name", name, name,
						chatTitle, Color.RED, ALatLon(content.lat, content.lon), null, null)
			}
		}
	}

	fun showChatMessages(chatId: Long) {
		execOsmandApi {
			val messages = telegramHelper.getChatMessages(chatId)
			for (message in messages) {
				addLocationToMap(message)
			}
		}
	}

	fun hideChatMessages(chatId: Long) {
		hideMessages(telegramHelper.getChatMessages(chatId))
	}

	fun hideMessages(messages: List<TdApi.Message>) {
		execOsmandApi {
			for (message in messages) {
				val user = telegramHelper.getUser(message.senderUserId)
				if (user != null) {
					removeMapPoint(message.chatId, message)
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

	private fun generatePhotoParams(photoPath: String?): Map<String, String>? {
		if (TextUtils.isEmpty(photoPath)) {
			return null
		}
		val photoUri = AndroidUtils.getUriForFile(app, File(photoPath))
		app.grantUriPermission(
			app.settings.appToConnectPackage,
			photoUri,
			Intent.FLAG_GRANT_READ_URI_PERMISSION
		)
		return mapOf(AMapPoint.POINT_IMAGE_URI_PARAM to photoUri.toString())
	}

	private fun removeMapPoint(chatId: Long, message: TdApi.Message) {
		val content = message.content
		if (content is TdApi.MessageLocation) {
			osmandAidlHelper.removeMapPoint(MAP_LAYER_ID, "${chatId}_${message.senderUserId}")
		} else if (content is MessageOsmAndBotLocation) {
			osmandAidlHelper.removeMapPoint(MAP_LAYER_ID, "${chatId}_${content.name}")
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