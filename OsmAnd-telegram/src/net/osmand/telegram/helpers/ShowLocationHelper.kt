package net.osmand.telegram.helpers

import android.content.Intent
import android.graphics.Color
import android.os.AsyncTask
import android.text.TextUtils
import net.osmand.PlatformUtil
import net.osmand.aidl.gpx.AGpxFile
import net.osmand.aidl.map.ALatLon
import net.osmand.aidl.maplayer.point.AMapPoint
import net.osmand.aidl.mapmarker.AMapMarker
import net.osmand.gpx.GPXFile
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.OsmandAidlHelper.ContextMenuButtonsListener
import net.osmand.telegram.helpers.TelegramUiHelper.ListItem
import net.osmand.telegram.ui.OPEN_MY_LOCATION_TAB_KEY
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandFormatter
import net.osmand.telegram.utils.OsmandLocationUtils
import net.osmand.telegram.utils.OsmandLocationUtils.MessageOsmAndBotLocation
import net.osmand.telegram.utils.OsmandLocationUtils.MessageUserLocation
import net.osmand.util.Algorithms
import org.drinkless.tdlib.TdApi
import org.drinkless.tdlib.TdApi.User
import java.io.File
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class ShowLocationHelper(private val app: TelegramApplication) {

	private val log = PlatformUtil.getLog(ShowLocationHelper::class.java)

	companion object {
		const val MAP_LAYER_ID = "telegram_layer"
		
		const val MIN_OSMAND_CALLBACK_VERSION_CODE = 320
		const val MIN_OSMAND_CREATE_IMPORT_DIRS_VERSION_CODE = 340
		const val MIN_OSMAND_SHARE_WIDGET_ICON_VERSION_CODE = 356

		const val MAP_CONTEXT_MENU_BUTTON_ID = 1
		const val MAP_CONTEXT_MENU_BUTTONS_PARAMS_ID = "DIRECTION"
		const val DIRECTION_ICON_ID = "ic_action_start_navigation"

		const val LIVE_TRACKS_DIR = "livetracks"

		private const val STATUS_WIDGET_ID = "status_widget"
		private const val STATUS_WIDGET_MENU_ICON = "widget_location_sharing_night"
		private const val STATUS_WIDGET_MENU_ICON_OLD = "ic_action_relative_bearing"
		private const val STATUS_WIDGET_ICON_OLD = "widget_relative_bearing_day"
		private const val STATUS_WIDGET_ICON_DAY = "widget_location_sharing_day"
		private const val STATUS_WIDGET_ICON_NIGHT = "widget_location_sharing_night"
		private const val STATUS_WIDGET_ON_ICON_DAY = "widget_location_sharing_on_day"
		private const val STATUS_WIDGET_ON_ICON_NIGHT = "widget_location_sharing_on_night"
		private const val STATUS_WIDGET_OFF_ICON_DAY = "widget_location_sharing_off_day"
		private const val STATUS_WIDGET_OFF_ICON_NIGHT = "widget_location_sharing_off_night"

		val GPX_COLORS = arrayOf(
			"red", "orange", "lightblue", "blue", "purple", "pink",
			"translucent_red", "translucent_orange", "translucent_lightblue",
			"translucent_blue", "translucent_purple", "translucent_pink"
		)
	}

	private val telegramHelper = app.telegramHelper
	private val osmandAidlHelper = app.osmandAidlHelper
	private val executor = Executors.newSingleThreadExecutor()
	private val liveTracksExecutor = Executors.newSingleThreadExecutor()

	private val points = ConcurrentHashMap<String, TdApi.Message>()
	private val markers = ConcurrentHashMap<String, AMapMarker>()

	var showingLocation: Boolean = false
		private set

	private var forcedStop: Boolean = false

	var shouldBlinkWidget: Boolean = false

	init {
		app.osmandAidlHelper.addConnectionListener(object : OsmandAidlHelper.OsmandHelperListener {
			override fun onOsmandConnectionStateChanged(connected: Boolean) {
				if (!connected && showingLocation && !app.settings.monitoringEnabled) {
					if (isUseOsmandCallback() && app.osmandAidlHelper.updatesCallbackRegistered()) {
						showingLocation = false
					}
				}
			}
		})
		app.osmandAidlHelper.setContextMenuButtonsListener(object : ContextMenuButtonsListener {

			override fun onContextMenuButtonClicked(buttonId: Int, pointId: String, layerId: String) {
				updateDirectionMarker(pointId, true)
			}

		})
	}

	fun setupMapLayer() {
		osmandAidlHelper.execOsmandApi {
			osmandAidlHelper.addMapLayer(MAP_LAYER_ID, "Telegram", 5.5f, null)
		}
	}

	private fun updateDirectionMarker(pointId: String, forceAdd: Boolean = false) {
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
						if (forceAdd) {
							app.osmandAidlHelper.removeMapMarker(markerPrev.latLon.latitude, markerPrev.latLon.longitude, name)
							markerUpdated = app.osmandAidlHelper.addMapMarker(marker)
						} else {
							markers.remove(pointId)
						}
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
		setupMapLayer()
		osmandAidlHelper.execOsmandApi {
			val pointId = item.getMapPointId()
			val name = item.name
			val aLatLon = ALatLon(item.latLon!!.latitude, item.latLon!!.longitude)
			val details = generatePointDetails(item.bearing?.toFloat(), item.altitude?.toFloat(), item.precision?.toFloat())
			val params = generatePointParams(if (stale) item.grayscalePhotoPath else item.photoPath, stale, item.speed?.toFloat(), item.bearing?.toFloat())

			osmandAidlHelper.addMapPoint(MAP_LAYER_ID, pointId, name, name, item.chatTitle, Color.WHITE, aLatLon, details, params)
			osmandAidlHelper.showMapPoint(MAP_LAYER_ID, pointId, name, name, item.chatTitle, Color.WHITE, aLatLon, details, params)
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
					var speed = 0f
					var bearing = 0f
					if (content is MessageUserLocation) {
						speed = content.speed.toFloat()
						bearing = content.bearing.toFloat()
					}
					val params = generatePointParams(photoPath, stale, speed, bearing)
					val typeName = if (isGroup) chatTitle else OsmandFormatter.getListItemLiveTimeDescr(
						app, date, R.string.last_response_date,
						R.string.last_response_duration
					)
					if (update) {
						osmandAidlHelper.updateMapPoint(MAP_LAYER_ID, pointId, name, name, typeName, Color.WHITE, aLatLon, details, params)
					} else {
						osmandAidlHelper.addMapPoint(MAP_LAYER_ID, pointId, name, name, typeName, Color.WHITE, aLatLon, details, params)
					}
					points[pointId] = message
				} else if (content is MessageOsmAndBotLocation && content.isValid()) {
					setupMapLayer()
					val params = generatePointParams(null, stale, content.speed.toFloat(), content.bearing.toFloat())
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

	fun addOrUpdateStatusWidget(time: Long, isSending: Boolean) {
		var noSubText = false
		var iconDay: String
		var iconNight: String
		val diffTime = (System.currentTimeMillis() - time) / 1000
		val menuIcon = if (isOsmandHasStatusWidgetIcon()) {
			STATUS_WIDGET_MENU_ICON
		} else {
			STATUS_WIDGET_MENU_ICON_OLD
		}
		val text = when {
			time > 0L && isSending -> {
				iconDay = STATUS_WIDGET_ON_ICON_DAY
				iconNight = STATUS_WIDGET_ON_ICON_NIGHT
				OsmandFormatter.getFormattedDurationForWidget(diffTime)
			}
			time > 0L && !isSending -> {
				iconDay = STATUS_WIDGET_ICON_DAY
				iconNight = STATUS_WIDGET_ICON_NIGHT
				if (diffTime >= 2 * 60) {
					OsmandFormatter.getFormattedDurationForWidget(diffTime)
				} else {
					noSubText = true
					app.getString(R.string.shared_string_error_short)
				}
			}
			time == 0L && isSending -> {
				iconDay = STATUS_WIDGET_ON_ICON_DAY
				iconNight = STATUS_WIDGET_ON_ICON_NIGHT
				app.getString(R.string.shared_string_ok)
			}
			time == 0L && !isSending -> {
				iconDay = STATUS_WIDGET_ICON_DAY
				iconNight = STATUS_WIDGET_ICON_NIGHT
				app.getString(R.string.shared_string_error_short)
			}
			else -> {
				iconDay = STATUS_WIDGET_OFF_ICON_DAY
				iconNight = STATUS_WIDGET_OFF_ICON_NIGHT
				app.getString(R.string.shared_string_start)
			}
		}
		if (!isOsmandHasStatusWidgetIcon()) {
			iconDay = STATUS_WIDGET_ICON_OLD
			iconNight = STATUS_WIDGET_ICON_OLD
		}
		val subText = when {
			time > 0 && !noSubText -> {
				if (text.length > 2) {
					app.getString(R.string.shared_string_hour_short)
				} else {
					app.getString(R.string.shared_string_minute_short)
				}
			}
			else -> ""
		}
		if (shouldBlinkWidget && isSending && isOsmandHasStatusWidgetIcon()) {
			BlinkWidgetTask(app, menuIcon, text, subText, getStatusWidgetIntent()).executeOnExecutor(executor)
			shouldBlinkWidget = false
		} else {
			osmandAidlHelper.addMapWidget(
					STATUS_WIDGET_ID,
					menuIcon,
					app.getString(R.string.status_widget_title),
					iconDay,
					iconNight,
					text, subText, 50, getStatusWidgetIntent())
		}
	}

	private fun getStatusWidgetIntent(): Intent {
		val startIntent = app.packageManager.getLaunchIntentForPackage(app.packageName)
		startIntent!!.addCategory(Intent.CATEGORY_LAUNCHER)
		startIntent.putExtra(OPEN_MY_LOCATION_TAB_KEY,true)
		return startIntent
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
			val user:User? = telegramHelper.getUser(senderId)
			if (user != null) {
				name = user.getName();
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

	private fun updateTracksOnMap() {
		val startTime = System.currentTimeMillis()
		osmandAidlHelper.execOsmandApi {
			val gpxFiles = getLiveGpxFiles()
			if (gpxFiles.isEmpty()) {
				return@execOsmandApi
			}

			val importedGpxFiles = osmandAidlHelper.importedGpxFiles
			gpxFiles.forEach {
				if (!checkAlreadyImportedGpx(importedGpxFiles, it)) {
					val listener = object : OsmandLocationUtils.SaveGpxListener {

						override fun onSavingGpxFinish(path: String) {
							log.debug("LiveTracks onSavingGpxFinish $path time ${startTime - System.currentTimeMillis()}")
							val uri = AndroidUtils.getUriForFile(app, File(path))
							val destinationPath = if (canOsmandCreateGpxDirs()) "$LIVE_TRACKS_DIR/${it.metadata.name}.gpx" else "${it.metadata.name}.gpx"
							val color = it.extensionsToRead["color"] ?: ""
							osmandAidlHelper.importGpxFromUri(uri, destinationPath, color, true)
							log.debug("LiveTracks importGpxFromUri finish time ${startTime - System.currentTimeMillis()}")
						}

						override fun onSavingGpxError(error: Exception) {
							log.error(error)
						}
					}
					OsmandLocationUtils.saveGpx(app, it, listener)
				}
			}
		}
	}

	private fun checkAlreadyImportedGpx(importedGpxFiles: List<AGpxFile>?, gpxFile: GPXFile): Boolean {
		if (importedGpxFiles != null && importedGpxFiles.isNotEmpty()) {
			val name = "${gpxFile.metadata.name}.gpx"
			val aGpxFile = importedGpxFiles.firstOrNull { it.fileName == name }

			if (aGpxFile != null) {
				val color = osmandAidlHelper.getGpxColor(aGpxFile.fileName)
				if (!color.isNullOrEmpty()) {
					gpxFile.extensionsToWrite["color"] = color
				}
				val startTimeImported = aGpxFile.details?.startTime
				val endTimeImported = aGpxFile.details?.endTime
				if (startTimeImported != null && endTimeImported != null) {
					val startTime = gpxFile.findPointToShow()?.time
					val endTime = gpxFile.lastPoint?.time
					if (aGpxFile.details?.startTime == startTime && endTimeImported == endTime) {
						return true
					}
				}
			}
		}
		return false
	}

	private fun getLiveGpxFiles(): List<GPXFile> {
		val currentTime = System.currentTimeMillis()
		val start = currentTime - app.settings.locHistoryTime * 1000
		val locationMessages = mutableListOf<LocationMessages.LocationMessage>()

		app.settings.getLiveTracksInfo().forEach {
			val messages = app.locationMessages.getMessagesForUserInChat(it.userId, it.chatId, it.deviceName, start, currentTime)
			if (messages.isNotEmpty()) {
				locationMessages.addAll(messages)
			}
		}

		return OsmandLocationUtils.convertLocationMessagesToGpxFiles(app, locationMessages)
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
			if (isUseOsmandCallback() && osmandAidlHelper.updatesCallbackRegistered()) {
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

	fun canOsmandCreateGpxDirs(): Boolean {
		val version = AndroidUtils.getAppVersionCode(app, app.settings.appToConnectPackage)
		return version >= MIN_OSMAND_CREATE_IMPORT_DIRS_VERSION_CODE
	}

	fun isOsmandHasStatusWidgetIcon(): Boolean {
		val version = AndroidUtils.getAppVersionCode(app, app.settings.appToConnectPackage)
		return version >= MIN_OSMAND_SHARE_WIDGET_ICON_VERSION_CODE
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

	fun startUpdateTracksTask() {
		UpdateTracksTask(app).executeOnExecutor(liveTracksExecutor)
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

	private class UpdateTracksTask(private val app: TelegramApplication) : AsyncTask<Void, Void, Void?>() {

		override fun doInBackground(vararg params: Void?): Void? {
			app.showLocationHelper.updateTracksOnMap()
			return null
		}
	}

	private class BlinkWidgetTask(private val app: TelegramApplication, private val menuIcon: String,
								  private val text: String, private val subText: String,
								  private val intent: Intent) : AsyncTask<Void, Void, Void?>() {

		override fun onPreExecute() {
			super.onPreExecute()
			app.osmandAidlHelper.addMapWidget(
					STATUS_WIDGET_ID,
					menuIcon,
					app.getString(R.string.status_widget_title),
					STATUS_WIDGET_OFF_ICON_DAY,
					STATUS_WIDGET_OFF_ICON_NIGHT,
					text, subText, 50, intent)
		}

		override fun doInBackground(vararg params: Void?): Void? {
			Thread.sleep(300)
			return null
		}

		override fun onPostExecute(result: Void?) {
			super.onPostExecute(result)
			app.osmandAidlHelper.addMapWidget(
					STATUS_WIDGET_ID,
					menuIcon,
					app.getString(R.string.status_widget_title),
					STATUS_WIDGET_ON_ICON_DAY,
					STATUS_WIDGET_ON_ICON_NIGHT,
					text, subText, 50, intent)
		}
	}

	private fun generatePointDetails(bearing: Float?, altitude: Float?, precision: Float?): List<String> {
		val details = mutableListOf<String>()
		if (bearing != null && bearing != 0.0f) {
			details.add(String.format(Locale.US, "${OsmandLocationUtils.BEARING_PREFIX}%.1f${OsmandLocationUtils.BEARING_SUFFIX} \n", bearing))
		}
		if (altitude != null && altitude != 0.0f) {
			details.add(String.format(Locale.US, "${OsmandLocationUtils.ALTITUDE_PREFIX}%.1f m\n", altitude))
		}
		if (precision != null && precision != 0.0f) {
			details.add(String.format(Locale.US, "${OsmandLocationUtils.HDOP_PREFIX}%d m\n", precision.toInt()))
		}

		return details
	}

	private fun generatePointParams(photoPath: String?, stale: Boolean, speed: Float?, bearing: Float?): Map<String, String> {
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
		if (bearing != 0.0f) {
			params[AMapPoint.POINT_BEARING_PARAM] = bearing.toString()
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
