package net.osmand.telegram

import android.annotation.SuppressLint
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.*
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.*
import net.osmand.PlatformUtil
import net.osmand.telegram.TelegramSettings.ShareChatInfo
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.helpers.TelegramHelper.TelegramIncomingMessagesListener
import net.osmand.telegram.helpers.TelegramHelper.TelegramOutgoingMessagesListener
import net.osmand.telegram.notifications.TelegramNotification.NotificationType
import net.osmand.telegram.utils.AndroidUtils
import org.drinkless.td.libcore.telegram.TdApi

private const val UPDATE_WIDGET_INTERVAL_MS = 1000L // 1 sec
private const val UPDATE_LIVE_MESSAGES_INTERVAL_MS = 10000L // 10 sec
private const val UPDATE_LIVE_TRACKS_INTERVAL_MS = 30000L // 30 sec

class TelegramService : Service(), TelegramIncomingMessagesListener,
		TelegramOutgoingMessagesListener {
	
	private fun app() = application as TelegramApplication
	private val binder = LocationServiceBinder()
	private var shouldCleanupResources: Boolean = false

	private var updateShareInfoHandler: Handler? = null
	private var mHandlerThread = HandlerThread("SharingServiceThread")

	private var updateTracksHandler: Handler? = null
	private var tracksHandlerThread = HandlerThread("TracksUpdateServiceThread")

	private var updateWidgetHandler: Handler? = null
	private var updateWidgetThread = HandlerThread("WidgetUpdateServiceThread")

	// FusedLocationProviderClient - Main class for receiving location updates.
	private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

	// LocationRequest - Requirements for the location updates, i.e., how often you should receive
	// updates, the priority, etc.
	private lateinit var locationRequest: LocationRequest

	// LocationCallback - Called when FusedLocationProviderClient has a new Location.
	private lateinit var locationCallback: LocationCallback

	var usedBy = 0
		private set
	var sendLocationInterval = 0L
		private set

	private var lastLocationSentTime = 0L

	class LocationServiceBinder : Binder()

	override fun onCreate() {
		super.onCreate()
		mHandlerThread.start()
		tracksHandlerThread.start()
		updateWidgetThread.start()
		updateShareInfoHandler = Handler(mHandlerThread.looper)
		updateTracksHandler = Handler(tracksHandlerThread.looper)
		updateWidgetHandler = Handler(updateWidgetThread.looper)

		fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

		locationRequest = LocationRequest().apply {
			// Sets the desired interval for active location updates. This interval is inexact. You
			// may not receive updates at all if no location sources are available, or you may
			// receive them less frequently than requested. You may also receive updates more
			// frequently than requested if other applications are requesting location at a more
			// frequent interval.
			//
			// IMPORTANT NOTE: Apps running on Android 8.0 and higher devices (regardless of
			// targetSdkVersion) may receive updates less frequently than this interval when the app
			// is no longer in the foreground.
			interval = 1000

			// Sets the fastest rate for active location updates. This interval is exact, and your
			// application will never receive updates more frequently than this value.
			//fastestInterval = 500

			// Sets the maximum time when batched location updates are delivered. Updates may be
			// delivered sooner than this interval.
			maxWaitTime = 0

			priority = LocationRequest.PRIORITY_HIGH_ACCURACY
		}

		locationCallback = object : LocationCallback() {
			override fun onLocationResult(locationResult: LocationResult?) {
				super.onLocationResult(locationResult)
				val location = convertLocation(locationResult?.lastLocation)
				if (System.currentTimeMillis() - lastLocationSentTime > sendLocationInterval * 1000) {
					lastLocationSentTime = System.currentTimeMillis()
					app().shareLocationHelper.updateLocation(location)
				}
			}
		}
	}

	override fun onBind(intent: Intent): IBinder {
		return binder
	}

	fun stopIfNeeded(ctx: Context, usageIntent: Int) {
		if (usedBy and usageIntent > 0) {
			usedBy -= usageIntent
		}
		when {
			usedBy == 0 -> {
				shouldCleanupResources = false
				val serviceIntent = Intent(ctx, TelegramService::class.java)
				ctx.stopService(serviceIntent)
			}
			isUsedByMyLocation(usedBy) -> {
				app().notificationHelper.refreshNotification(NotificationType.LOCATION)
			}
			isUsedByUsersLocations(usedBy) -> removeLocationUpdates()
		}
	}

	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		val app = app()
		val usageIntent = intent.getIntExtra(USAGE_INTENT, 0)
		usedBy = usageIntent or usedBy

		sendLocationInterval = intent.getLongExtra(SEND_LOCATION_INTERVAL, 0)

		app.telegramHelper.addIncomingMessagesListener(this)
		app.telegramHelper.addOutgoingMessagesListener(this)

		app.telegramService = this

		val locationNotification = app.notificationHelper.locationNotification
		val notification = app.notificationHelper.buildNotification(locationNotification)
		startForeground(locationNotification.telegramNotificationId, notification)
		app.notificationHelper.refreshNotification(locationNotification.type)

		if (isUsedByMyLocation(usedBy)) {
			initLocationUpdates()
			startShareInfoUpdates()
			startWidgetUpdates()
		}
		if (isUsedByUsersLocations(usedBy)) {
			app.telegramHelper.startLiveMessagesUpdates(app.settings.sendMyLocInterval)
			startTracksUpdates()
		}
		app.shareLocationHelper.checkAndSendBufferMessages()

		return START_REDELIVER_INTENT
	}

	override fun onDestroy() {
		super.onDestroy()
		val app = app()
		app.telegramHelper.stopLiveMessagesUpdates()
		app.telegramHelper.removeIncomingMessagesListener(this)
		app.telegramHelper.removeOutgoingMessagesListener(this)
		app.settings.save()
		app.telegramService = null
		tracksHandlerThread.quit()
		mHandlerThread.quit()
		updateWidgetThread.quit()
		app().showLocationHelper.addOrUpdateStatusWidget(-1, false)

		usedBy = 0

		removeLocationUpdates()

		if (shouldCleanupResources) {
			app.cleanupResources()
		}

		// remove notification
		stopForeground(java.lang.Boolean.TRUE)
	}

	fun updateSendLocationInterval(newInterval: Long) {
		sendLocationInterval = newInterval
	}

	fun forceLocationUpdate() {
		getFirstTimeRunDefaultLocation { location ->
			app().shareLocationHelper.updateLocation(location)
		}
	}

	private fun initLocationUpdates() {
		getFirstTimeRunDefaultLocation { location ->
			app().shareLocationHelper.updateLocation(location)
		}

		// request location updates
		try {
			fusedLocationProviderClient.requestLocationUpdates(
					locationRequest, locationCallback, Looper.myLooper())
		} catch (unlikely: SecurityException) {
			Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_LONG).show()
			Log.d(PlatformUtil.TAG, "Lost location permissions. Couldn't request updates. $unlikely")
		} catch (e: IllegalArgumentException) {
			Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_LONG).show()
			Log.d(PlatformUtil.TAG, "GPS location provider not available")
		}
	}

	private fun startShareInfoUpdates() {
		updateShareInfoHandler?.postDelayed({
			if (isUsedByMyLocation(usedBy)) {
				app().shareLocationHelper.updateSendLiveMessages()
				startShareInfoUpdates()
			}
		}, UPDATE_LIVE_MESSAGES_INTERVAL_MS)
	}

	private fun startTracksUpdates() {
		updateTracksHandler?.postDelayed({
			if (isUsedByUsersLocations(usedBy)) {
				if (app().settings.hasAnyLiveTracksToShowOnMap()) {
					app().showLocationHelper.startUpdateTracksTask()
				}
				startTracksUpdates()
			}
		}, UPDATE_LIVE_TRACKS_INTERVAL_MS)
	}

	private fun startWidgetUpdates() {
		updateWidgetHandler?.postDelayed({
			if (isUsedByMyLocation(usedBy)) {
				val sharingStatus = app().settings.sharingStatusChanges.last()
				var isSending = sharingStatus.statusType == TelegramSettings.SharingStatusType.SENDING
				val sharingChats = app().settings.getShareLocationChats()
				var oldestTime = 0L
				if (sharingChats.isNotEmpty() && app().shareLocationHelper.sharingLocation) {
					sharingChats.forEach { id ->
						val bufferMessages = app().locationMessages.getBufferedMessagesForChat(id)
						if (bufferMessages.isNotEmpty()) {
							val newTime = bufferMessages[0].time
							if (oldestTime == 0L || newTime < oldestTime) {
								oldestTime = newTime
							}
						} else {
							oldestTime = 0L
						}
					}
				} else {
					isSending = false
					oldestTime = -1
				}
				app().showLocationHelper.addOrUpdateStatusWidget(oldestTime, isSending)
			} else {
				app().showLocationHelper.addOrUpdateStatusWidget(-1, false)
			}
			startWidgetUpdates()
		}, UPDATE_WIDGET_INTERVAL_MS)
	}

	@SuppressLint("MissingPermission")
	private fun getFirstTimeRunDefaultLocation(locationListener: (net.osmand.Location?) -> Unit) {
		val app = app()
		if (!AndroidUtils.isLocationPermissionAvailable(app)) {
			locationListener(null)
			return
		}
		fusedLocationProviderClient.lastLocation
				.addOnSuccessListener { location : Location? ->
					locationListener(convertLocation(location))
				}
	}

	private fun removeLocationUpdates() {
		// remove updates
		try {
			fusedLocationProviderClient.removeLocationUpdates(locationCallback)
		} catch (unlikely: SecurityException) {
			Log.d(PlatformUtil.TAG, "Lost location permissions. Couldn't remove updates. $unlikely")
		}
	}

	override fun onTaskRemoved(rootIntent: Intent) {
		val app = app()
		if (app.telegramService != null) {
			shouldCleanupResources = true
			// Do not stop service after UI task was dismissed
			//this@TelegramService.stopSelf()
		}
	}

	override fun onReceiveChatLocationMessages(chatId: Long, vararg messages: TdApi.Message) {
		app().showLocationHelper.startShowMessagesTask(chatId, *messages)
		messages.forEach {
			if (!it.isOutgoing) {
				app().locationMessages.addNewLocationMessage(it)
			}
		}
	}

	override fun onDeleteChatLocationMessages(chatId: Long, messages: List<TdApi.Message>) {
		app().showLocationHelper.startDeleteMessagesTask(chatId, messages)
	}

	override fun updateLocationMessages() {
		app().showLocationHelper.startUpdateMessagesTask()
	}

	override fun onUpdateMessages(messages: List<TdApi.Message>) {
		messages.forEach {
			app().settings.updateShareInfo(it)
			app().shareLocationHelper.checkAndSendBufferMessagesToChat(it.chatId)
			if (it.sendingState == null && !it.isOutgoing && (it.content is TdApi.MessageLocation || it.content is TdApi.MessageText)) {
				app().locationMessages.addNewLocationMessage(it)
			}
		}
	}

	override fun onDeleteMessages(chatId: Long, messages: List<Long>) {
		app().settings.onDeleteLiveMessages(chatId, messages)
	}

	override fun onSendLiveLocationError(code: Int, message: String, shareInfo: ShareChatInfo, messageType: Int) {
		Log.d(PlatformUtil.TAG, "Send live location error: $code - $message")
		when (messageType) {
			TelegramHelper.MESSAGE_TYPE_TEXT -> shareInfo.pendingTdLibText--
			TelegramHelper.MESSAGE_TYPE_MAP -> {
				shareInfo.pendingTdLibMap--
				shareInfo.currentMapMessageId = -1L
			}
		}
	}

	companion object {

		const val USED_BY_MY_LOCATION: Int = 1
		const val USED_BY_USERS_LOCATIONS: Int = 2
		const val USAGE_INTENT = "SERVICE_USED_BY"
		const val SEND_LOCATION_INTERVAL = "SEND_LOCATION_INTERVAL"

		fun isUsedByMyLocation(usedBy: Int): Boolean {
			return (usedBy and USED_BY_MY_LOCATION) > 0
		}

		fun isUsedByUsersLocations(usedBy: Int): Boolean {
			return (usedBy and USED_BY_USERS_LOCATIONS) > 0
		}

		fun convertLocation(l: Location?): net.osmand.Location? {
			if (l == null) {
				return null
			}
			val r = net.osmand.Location(l.provider)
			r.latitude = l.latitude
			r.longitude = l.longitude
			r.time = l.time
			if (l.hasAccuracy()) {
				r.accuracy = l.accuracy
			}
			if (l.hasSpeed()) {
				r.speed = l.speed
			}
			if (l.hasAltitude()) {
				r.altitude = l.altitude
			}
			if (l.hasBearing()) {
				r.bearing = l.bearing
			}
			if (l.hasAltitude()) {
				r.altitude = l.altitude
			}
			return r
		}
	}
}
