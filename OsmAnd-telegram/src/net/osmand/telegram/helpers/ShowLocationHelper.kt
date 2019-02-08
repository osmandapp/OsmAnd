package net.osmand.telegram.helpers

import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.text.TextUtils
import net.osmand.aidl.map.ALatLon
import net.osmand.aidl.maplayer.point.AMapPoint
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.TelegramUiHelper.ListItem
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandLocationUtils
import net.osmand.telegram.utils.OsmandLocationUtils.MessageOsmAndBotLocation
import net.osmand.telegram.utils.OsmandLocationUtils.MessageUserLocation
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.util.concurrent.Executors

class ShowLocationHelper(private val app: TelegramApplication) {

	companion object {
		const val MAP_LAYER_ID = "telegram_layer"
		
		const val MIN_OSMAND_CALLBACK_VERSION_CODE = 320
	}

	private val telegramHelper = app.telegramHelper
	private val osmandAidlHelper = app.osmandAidlHelper
	private val executor = Executors.newSingleThreadExecutor()

	var showingLocation: Boolean = false
		private set

	private var forcedStop: Boolean = false

	fun setupMapLayer() {
		osmandAidlHelper.execOsmandApi {
			osmandAidlHelper.addMapLayer(MAP_LAYER_ID, "Telegram", 5.5f, null)
		}
	}

	fun showLocationOnMap(item: ListItem, stale: Boolean = false) {
		if (item.latLon == null) {
			return
		}
		osmandAidlHelper.execOsmandApi {
			osmandAidlHelper.showMapPoint(
				MAP_LAYER_ID,
				item.getMapPointId(),
				item.getVisibleName(),
				item.getVisibleName(),
				item.chatTitle,
				Color.WHITE,
				ALatLon(item.latLon!!.latitude, item.latLon!!.longitude),
				null,
				generatePointParams(if (stale) item.grayscalePhotoPath else item.photoPath, stale)
			)
		}
	}
	
	fun updateLocationsOnMap() {
		osmandAidlHelper.execOsmandApi {
			val messages = telegramHelper.getMessages()
			for (message in messages) {
				val date = OsmandLocationUtils.getLastUpdatedTime(message)
				val messageShowingTime = System.currentTimeMillis() / 1000 - date
				if (messageShowingTime > app.settings.locHistoryTime) {
					removeMapPoint(message.chatId, message)
				} else if (app.settings.isShowingChatOnMap(message.chatId)) {
					addOrUpdateLocationOnMap(message, true)
				}
			}
		}
	}

	fun addOrUpdateLocationOnMap(message: TdApi.Message, update: Boolean = false) {
		osmandAidlHelper.execOsmandApi {
			val chatId = message.chatId
			val chatTitle = telegramHelper.getChat(message.chatId)?.title
			val content = message.content
			val date = OsmandLocationUtils.getLastUpdatedTime(message)
			val stale = System.currentTimeMillis() / 1000 - date > app.settings.staleLocTime
			if (chatTitle != null && (content is TdApi.MessageLocation || (content is MessageUserLocation && content.isValid()))) {
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
					photoPath = if (stale) {
						telegramHelper.getUserGreyPhotoPath(user)
					} else {
						telegramHelper.getUserPhotoPath(user)
					}
				}
				if (userName.isEmpty()) {
					userName = message.senderUserId.toString()
				}
				setupMapLayer()
				val params = generatePointParams(photoPath, stale)
				val aLatLon = when (content) {
					is TdApi.MessageLocation -> ALatLon(content.location.latitude, content.location.longitude)
					is MessageUserLocation -> ALatLon(content.lat, content.lon)
					else -> null
				}
				if (aLatLon != null) {
					if (update) {
						osmandAidlHelper.updateMapPoint(MAP_LAYER_ID, "${chatId}_${message.senderUserId}", userName, userName,
							chatTitle, Color.WHITE, aLatLon, null, params)
					} else {
						osmandAidlHelper.addMapPoint(MAP_LAYER_ID, "${chatId}_${message.senderUserId}", userName, userName,
							chatTitle, Color.WHITE, aLatLon, null, params)
					}
				}
			} else if (chatTitle != null && content is MessageOsmAndBotLocation && content.isValid()) {
				val name = content.deviceName
				setupMapLayer()
				if (update) {
					osmandAidlHelper.updateMapPoint(MAP_LAYER_ID, "${chatId}_$name", name, name,
						chatTitle, Color.WHITE, ALatLon(content.lat, content.lon), null, generatePointParams(null, stale))
				} else {
					osmandAidlHelper.addMapPoint(MAP_LAYER_ID, "${chatId}_$name", name, name,
						chatTitle, Color.WHITE, ALatLon(content.lat, content.lon), null, generatePointParams(null, stale))
				}
			}
		}
	}

	fun showChatMessages(chatId: Long) {
		osmandAidlHelper.execOsmandApi {
			val messages = telegramHelper.getChatMessages(chatId)
			for (message in messages) {
				addOrUpdateLocationOnMap(message)
			}
		}
	}

	fun hideChatMessages(chatId: Long) {
		hideMessages(telegramHelper.getChatMessages(chatId))
	}

	fun hideMessages(messages: List<TdApi.Message>) {
		osmandAidlHelper.execOsmandApi {
			for (message in messages) {
				val user = telegramHelper.getUser(message.senderUserId)
				if (user != null) {
					removeMapPoint(message.chatId, message)
				}
			}
		}
	}

	fun startShowingLocation() {
		if (!showingLocation && !forcedStop) {
			showingLocation = if (isUseOsmandCallback() && !app.settings.monitoringEnabled) {
				osmandAidlHelper.registerForUpdates()
			} else {
				app.startUserLocationService()
				true
			}
		}
	}

	fun stopShowingLocation(force: Boolean = false) {
		forcedStop = force
		if (showingLocation) {
			showingLocation = false
			if (isUseOsmandCallback() && app.osmandAidlHelper.updatesCallbackRegistered()) {
				osmandAidlHelper.unregisterFromUpdates()
			} else if (!app.settings.monitoringEnabled) {
				app.stopUserLocationService()
			}
		}
	}

	fun changeUpdatesType() {
		if (!forcedStop) {
			if (app.settings.monitoringEnabled) {
				if (app.osmandAidlHelper.updatesCallbackRegistered()) {
					osmandAidlHelper.unregisterFromUpdates()
				}
				app.startUserLocationService()
				showingLocation = true
			} else {
				showingLocation = if (isUseOsmandCallback()) {
					app.stopUserLocationService()
					osmandAidlHelper.registerForUpdates()
				} else {
					if (app.settings.hasAnyChatToShowOnMap()) {
						app.startUserLocationService()
						true
					} else {
						app.stopUserLocationService()
						false
					}
				}
			}
		}
	}

	fun isUseOsmandCallback(): Boolean {
		val version = AndroidUtils.getAppVersionCode(app, app.settings.appToConnectPackage)
		return version >= MIN_OSMAND_CALLBACK_VERSION_CODE
	}

	fun startShowMessagesTask(chatId: Long, vararg messages: TdApi.Message) {
		if (app.settings.isShowingChatOnMap(chatId)) {
			ShowMessagesTask(app).executeOnExecutor(executor, *messages)
		}
	}

	fun startDeleteMessagesTask(chatId: Long, messages: List<TdApi.Message>) {
		if (app.settings.isShowingChatOnMap(chatId)) {
			DeleteMessagesTask(app).executeOnExecutor(executor, messages)
		}
	}

	fun startUpdateMessagesTask() {
		UpdateMessagesTask(app).executeOnExecutor(executor)
	}
	
	private class ShowMessagesTask(private val app: TelegramApplication) : AsyncTask<TdApi.Message, Void, Void?>() {

		override fun doInBackground(vararg messages: TdApi.Message): Void? {
			for (message in messages) {
				app.showLocationHelper.addOrUpdateLocationOnMap(message)
			}
			return null
		}
	}

	private class DeleteMessagesTask(private val app: TelegramApplication) : AsyncTask<List<TdApi.Message>, Void, Void?>() {

		override fun doInBackground(vararg messages: List<TdApi.Message>): Void? {
			for (list in messages) {
				app.showLocationHelper.hideMessages(list)
			}
			return null
		}
	}

	private class UpdateMessagesTask(private val app: TelegramApplication) : AsyncTask<Void, Void, Void?>() {

		override fun doInBackground(vararg params: Void?): Void? {
			app.showLocationHelper.updateLocationsOnMap()
			return null
		}
	}

	private fun generatePointParams(photoPath: String?, stale: Boolean): Map<String, String> {
		val photoUri = generatePhotoUri(photoPath, stale)
		app.grantUriPermission(
			app.settings.appToConnectPackage,
			photoUri,
			Intent.FLAG_GRANT_READ_URI_PERMISSION
		)
		return mapOf(
			AMapPoint.POINT_IMAGE_URI_PARAM to photoUri.toString(),
			AMapPoint.POINT_STALE_LOC_PARAM to stale.toString()
		)
	}

	private fun generatePhotoUri(photoPath: String?, stale: Boolean) =
		if (TextUtils.isEmpty(photoPath)) {
			val id = if (stale) R.drawable.img_user_placeholder_stale else R.drawable.img_user_placeholder_active
			AndroidUtils.resourceToUri(app, id)
		} else {
			AndroidUtils.getUriForFile(app, File(photoPath))
		}

	private fun removeMapPoint(chatId: Long, message: TdApi.Message) {
		val content = message.content
		if (content is TdApi.MessageLocation || content is MessageUserLocation) {
			osmandAidlHelper.removeMapPoint(MAP_LAYER_ID, "${chatId}_${message.senderUserId}")
		} else if (content is MessageOsmAndBotLocation) {
			osmandAidlHelper.removeMapPoint(MAP_LAYER_ID, "${chatId}_${content.deviceName}")
		}
	}
}