package net.osmand.telegram.helpers

import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.text.TextUtils
import net.osmand.aidl.map.ALatLon
import net.osmand.aidl.maplayer.point.AMapPoint
import net.osmand.aidl.mapmarker.AMapMarker
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.OsmandAidlHelper.ContextMenuButtonsListener
import net.osmand.telegram.helpers.TelegramUiHelper.ListItem
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.telegram.utils.OsmandLocationUtils
import net.osmand.telegram.utils.OsmandLocationUtils.MessageOsmAndBotLocation
import net.osmand.telegram.utils.OsmandLocationUtils.MessageUserLocation
import org.drinkless.td.libcore.telegram.TdApi
import java.io.File
import java.util.*
import java.util.concurrent.Executors

class ShowLocationHelper(private val app: TelegramApplication) {

	companion object {
		const val MAP_LAYER_ID = "telegram_layer"
		
		const val MIN_OSMAND_CALLBACK_VERSION_CODE = 320

		const val MAP_CONTEXT_MENU_BUTTON_ID = 1
		const val MAP_CONTEXT_MENU_BUTTONS_PARAMS_ID = "DIRECTION"
		const val DIRECTION_ICON_ID = "ic_action_start_navigation"
	}

	private val telegramHelper = app.telegramHelper
	private val osmandAidlHelper = app.osmandAidlHelper
	private val executor = Executors.newSingleThreadExecutor()

	private val points = mutableMapOf<String, TdApi.Message>()
	private val markers = mutableMapOf<String, AMapMarker>()

	var showingLocation: Boolean = false
		private set

	private var forcedStop: Boolean = false

	init {
		app.osmandAidlHelper.setContextMenuButtonsListener(object : ContextMenuButtonsListener {

			override fun onContextMenuButtonClicked(buttonId: Int, pointId: String, layerId: String) {
				updateDirectionMarker(pointId)
			}

		})
	}

	fun setupMapLayer() {
		osmandAidlHelper.execOsmandApi {
			osmandAidlHelper.addMapLayer(MAP_LAYER_ID, "Telegram", 5.5f, null)
		}
	}

	private fun updateDirectionMarker(pointId: String) {
		val message = points[pointId]
		if (message != null) {
			val aLatLon = getALatLonFromMessage(message.content)
			val name = getNameFromMessage(message)
			if (aLatLon != null) {
				val marker = AMapMarker(ALatLon(aLatLon.latitude, aLatLon.longitude), name)
				val markerPrev = markers[pointId]
				var markerUpdated: Boolean
				if (markerPrev != null) {
					markerUpdated = app.osmandAidlHelper.updateMapMarker(markerPrev, marker)
					if (!markerUpdated) {
						app.osmandAidlHelper.removeMapMarker(markerPrev.latLon.latitude, markerPrev.latLon.longitude, name)
						markerUpdated = app.osmandAidlHelper.addMapMarker(marker)
					}
				} else {
					markerUpdated = app.osmandAidlHelper.addMapMarker(marker)
				}
				if (markerUpdated) {
					markers[pointId] = marker
				}
			}
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
				generatePointDetails(item.bearing?.toFloat(), item.altitude?.toFloat(), item.precision?.toFloat()),
				generatePointParams(if (stale) item.grayscalePhotoPath else item.photoPath, stale, item.speed?.toFloat())
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
			val chat = telegramHelper.getChat(message.chatId)
			val chatTitle = chat?.title
			val isGroup = chat != null && telegramHelper.isGroup(chat)
			val content = message.content
			val date = OsmandLocationUtils.getLastUpdatedTime(message)
			val stale = System.currentTimeMillis() / 1000 - date > app.settings.staleLocTime
			val aLatLon = getALatLonFromMessage(content)
			val details = if (content is OsmandLocationUtils.MessageLocation) generatePointDetails(content.bearing.toFloat(), content.altitude.toFloat(), content.hdop.toFloat()) else null
			val name = getNameFromMessage(message)
			val senderId = OsmandLocationUtils.getSenderMessageId(message)
			val pointId = if (content is MessageOsmAndBotLocation) "${chatId}_${content.deviceName}" else "${chatId}_$senderId"

			if (aLatLon != null && chatTitle != null) {
				if ((content is TdApi.MessageLocation || (content is MessageUserLocation && content.isValid()))) {
					var photoPath: String? = null
					val user = telegramHelper.getUser(senderId)
					if (user != null) {
						photoPath = if (stale) {
							telegramHelper.getUserGreyPhotoPath(user)
						} else {
							telegramHelper.getUserPhotoPath(user)
						}
					}
					setupMapLayer()
					val params = generatePointParams(photoPath, stale, if (content is MessageUserLocation) content.speed.toFloat() else null)

					val typeName = if (isGroup) chatTitle else OsmandFormatter.getListItemLiveTimeDescr(app, date, app.getString(R.string.last_response) + ": ")
					if (update) {
						osmandAidlHelper.updateMapPoint(MAP_LAYER_ID, pointId, name, name, typeName, Color.WHITE, aLatLon, details, params)
					} else {
						osmandAidlHelper.addMapPoint(MAP_LAYER_ID, pointId, name, name, typeName, Color.WHITE, aLatLon, details, params)
					}
					points[pointId] = message
				} else if (content is MessageOsmAndBotLocation && content.isValid()) {
					setupMapLayer()
					val params = generatePointParams(null, stale, content.speed.toFloat())
					if (update) {
						osmandAidlHelper.updateMapPoint(MAP_LAYER_ID, pointId, name, name, chatTitle, Color.WHITE, aLatLon, details, params)
					} else {
						osmandAidlHelper.addMapPoint(MAP_LAYER_ID, pointId, name, name, chatTitle, Color.WHITE, aLatLon, details, params)
					}
					points[pointId] = message
				}
				if (markers.containsKey(pointId)) {
					updateDirectionMarker(pointId)
				}
			}
		}
	}

	private fun getALatLonFromMessage(content: TdApi.MessageContent): ALatLon? {
		return when (content) {
			is TdApi.MessageLocation -> ALatLon(content.location.latitude, content.location.longitude)
			is OsmandLocationUtils.MessageLocation -> ALatLon(content.lat, content.lon)
			else -> null
		}
	}

	private fun getNameFromMessage(message: TdApi.Message): String {
		var name = ""
		val content = message.content
		val senderId = OsmandLocationUtils.getSenderMessageId(message)
		if ((content is TdApi.MessageLocation || (content is MessageUserLocation && content.isValid()))) {
			val user = telegramHelper.getUser(senderId)
			if (user != null) {
				name = "${user.firstName} ${user.lastName}".trim()
				if (name.isEmpty()) {
					name = user.username
				}
				if (name.isEmpty()) {
					name = user.phoneNumber
				}
			}
			if (name.isEmpty()) {
				name = senderId.toString()
			}
		} else if (content is MessageOsmAndBotLocation && content.isValid()) {
			name = content.deviceName
		}

		return name
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
				val user = telegramHelper.getUser(OsmandLocationUtils.getSenderMessageId(message))
				if (user != null) {
					removeMapPoint(message.chatId, message)
				}
			}
		}
	}

	fun addDirectionContextMenuButton() {
		osmandAidlHelper.addContextMenuButtons(app.packageName, MAP_CONTEXT_MENU_BUTTONS_PARAMS_ID, app.getString(R.string.direction), "", DIRECTION_ICON_ID, "", true, true, MAP_CONTEXT_MENU_BUTTON_ID)
	}

	fun removeDirectionContextMenuButton() {
		osmandAidlHelper.removeContextMenuButtons(MAP_CONTEXT_MENU_BUTTONS_PARAMS_ID)
	}

	fun startShowingLocation() {
		if (!showingLocation && !forcedStop) {
			showingLocation = if (isUseOsmandCallback() && !app.settings.monitoringEnabled) {
				osmandAidlHelper.registerForUpdates()
			} else {
				app.startUserLocationService()
				true
			}
			addDirectionContextMenuButton()
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
			removeDirectionContextMenuButton()
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

	private fun generatePointDetails(bearing: Float?, altitude: Float?, precision: Float?): List<String> {
		val details = mutableListOf<String>()
		if (bearing != null && bearing != 0.0f) {
			details.add(String.format(Locale.US, "${OsmandLocationUtils.BEARING_PREFIX}%.1f \n", bearing))
		}
		if (altitude != null && altitude != 0.0f) {
			details.add(String.format(Locale.US, "${OsmandLocationUtils.ALTITUDE_PREFIX}%.1f m\n", altitude))
		}
		if (precision != null && precision != 0.0f) {
			details.add(String.format(Locale.US, "${OsmandLocationUtils.HDOP_PREFIX}%d m\n", precision.toInt()))
		}

		return details
	}

	private fun generatePointParams(photoPath: String?, stale: Boolean, speed: Float?): Map<String, String> {
		val photoUri = generatePhotoUri(photoPath, stale)
		app.grantUriPermission(
			app.settings.appToConnectPackage,
			photoUri,
			Intent.FLAG_GRANT_READ_URI_PERMISSION
		)
		val params = mutableMapOf(
			AMapPoint.POINT_IMAGE_URI_PARAM to photoUri.toString(),
			AMapPoint.POINT_STALE_LOC_PARAM to stale.toString()
		)
		if (speed != 0.0f) {
			params[AMapPoint.POINT_SPEED_PARAM] = speed.toString()
		}

		return params
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
			osmandAidlHelper.removeMapPoint(MAP_LAYER_ID, "${chatId}_${OsmandLocationUtils.getSenderMessageId(message)}")
		} else if (content is MessageOsmAndBotLocation) {
			osmandAidlHelper.removeMapPoint(MAP_LAYER_ID, "${chatId}_${content.deviceName}")
		}
	}
}