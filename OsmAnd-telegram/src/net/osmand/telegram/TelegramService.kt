package net.osmand.telegram

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.*
import android.util.Log
import android.widget.Toast
import net.osmand.PlatformUtil
import net.osmand.telegram.TelegramSettings.ShareChatInfo
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.helpers.TelegramHelper.*
import net.osmand.telegram.notifications.TelegramNotification.NotificationType
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.OsmandLocationUtils
import org.drinkless.td.libcore.telegram.TdApi
import java.util.*

private const val UPDATE_WIDGET_INTERVAL_MS = 1000L // 1 sec
private const val UPDATE_LIVE_MESSAGES_INTERVAL_MS = 10000L // 10 sec
private const val UPDATE_LIVE_TRACKS_INTERVAL_MS = 30000L // 30 sec

class TelegramService : Service(), LocationListener, TelegramIncomingMessagesListener,
	TelegramOutgoingMessagesListener {

	private val log = PlatformUtil.getLog(TelegramService::class.java)

	private fun app() = application as TelegramApplication
	private val binder = LocationServiceBinder()
	private var shouldCleanupResources: Boolean = false

	private var updateShareInfoHandler: Handler? = null
	private var mHandlerThread = HandlerThread("SharingServiceThread")

	private var updateTracksHandler: Handler? = null
	private var tracksHandlerThread = HandlerThread("TracksUpdateServiceThread")

	private var updateWidgetHandler: Handler? = null
	private var updateWidgetThread = HandlerThread("WidgetUpdateServiceThread")

	var handler: Handler? = null
		private set
	var usedBy = 0
		private set
	var serviceOffProvider: String = LocationManager.GPS_PROVIDER
		private set
	var serviceOffInterval = 0L
		private set
	var serviceErrorInterval = 0L
		private set
	var sendLocationInterval = 0L
		private set

	private var lastLocationSentTime = 0L
	private var pendingIntent: PendingIntent? = null

	class LocationServiceBinder : Binder()

	override fun onCreate() {
		super.onCreate()
		mHandlerThread.start()
		tracksHandlerThread.start()
		updateWidgetThread.start()
		updateShareInfoHandler = Handler(mHandlerThread.looper)
		updateTracksHandler = Handler(tracksHandlerThread.looper)
		updateWidgetHandler = Handler(updateWidgetThread.looper)
	}

	override fun onBind(intent: Intent): IBinder? {
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
				val app = app()
				if (app.settings.sendMyLocInterval >= OFF_INTERVAL_THRESHOLD && serviceOffInterval == 0L) {
					serviceOffInterval = app.settings.sendMyLocInterval
					setupServiceErrorInterval()
					setupAlarm()
				}
				app.notificationHelper.refreshNotification(NotificationType.LOCATION)
			}
			isUsedByUsersLocations(usedBy) -> removeLocationUpdates()
		}
	}

	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		val app = app()
		handler = Handler()
		val usageIntent = intent.getIntExtra(USAGE_INTENT, 0)
		usedBy = usageIntent or usedBy

		serviceOffInterval = intent.getLongExtra(USAGE_OFF_INTERVAL, 0)
		sendLocationInterval = intent.getLongExtra(SEND_LOCATION_INTERVAL, 0)
		setupServiceErrorInterval()

		app.telegramService = this
		app.telegramHelper.addIncomingMessagesListener(this)
		app.telegramHelper.addOutgoingMessagesListener(this)

		if (isUsedByMyLocation(usedBy)) {
			initLocationUpdates()
			startShareInfoUpdates()
			startWidgetUpdates()
		}
		if (isUsedByUsersLocations(usedBy)) {
			app.telegramHelper.startLiveMessagesUpdates(app.settings.sendMyLocInterval)
			startTracksUpdates()
		}

		val locationNotification = app.notificationHelper.locationNotification
		val notification = app.notificationHelper.buildNotification(locationNotification)
		startForeground(locationNotification.telegramNotificationId, notification)
		app.notificationHelper.refreshNotification(locationNotification.type)
		return Service.START_REDELIVER_INTENT
	}

	private fun setupServiceErrorInterval() {
		serviceErrorInterval = serviceOffInterval / 5
		// 1. not more than 12 mins
		serviceErrorInterval = Math.min(serviceErrorInterval, 12 * 60 * 1000)
		// 2. not less than 30 seconds
		serviceErrorInterval = Math.max(serviceErrorInterval, 30 * 1000)
		// 3. not more than serviceOffInterval
		serviceErrorInterval = Math.min(serviceErrorInterval, serviceOffInterval)
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

		if (!isContinuous()) {
			val lock = getLock(this)
			if (lock.isHeld) {
				lock.release()
			}
		}

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
		val location = getFirstTimeRunDefaultLocation()
		app().shareLocationHelper.updateLocation(location)
	}

	private fun initLocationUpdates() {
		val firstLocation = getFirstTimeRunDefaultLocation()
		app().shareLocationHelper.updateLocation(firstLocation)

		// requesting
		if (isContinuous()) {
			// request location updates
			val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
			try {
				locationManager.requestLocationUpdates(serviceOffProvider, 0, 0f, this@TelegramService)
			} catch (e: SecurityException) {
				Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_LONG).show()
				Log.d(PlatformUtil.TAG, "Location service permission not granted") //$NON-NLS-1$
			} catch (e: IllegalArgumentException) {
				Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_LONG).show()
				Log.d(PlatformUtil.TAG, "GPS location provider not available") //$NON-NLS-1$
			}
		} else {
			setupAlarm()
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
	private fun getFirstTimeRunDefaultLocation(): net.osmand.Location? {
		val app = app()
		if (!AndroidUtils.isLocationPermissionAvailable(app)) {
			return null
		}
		val service = app.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		val ps = service.getProviders(true) ?: return null
		val providers = ArrayList(ps)
		// note, passive provider is from API_LEVEL 8 but it is a constant, we can check for it.
		// constant should not be changed in future
		val passiveFirst = providers.indexOf("passive") // LocationManager.PASSIVE_PROVIDER
		// put passive provider to first place
		if (passiveFirst > -1) {
			providers.add(0, providers.removeAt(passiveFirst))
		}
		// find location
		var location: net.osmand.Location? = null
		for (provider in providers) {
			val loc = convertLocation(service.getLastKnownLocation(provider))
			if (loc != null && (location == null || loc.hasAccuracy() && loc.accuracy < location.accuracy)) {
				location = loc
			}
		}
		return location
	}

	private fun setupAlarm() {
		val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
		pendingIntent = PendingIntent.getBroadcast(this, 0, Intent(this, OnTelegramServiceAlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
		alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 500, serviceOffInterval, pendingIntent)
	}

	private fun removeLocationUpdates() {
		// remove updates
		val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
		try {
			locationManager.removeUpdates(this)
		} catch (e: SecurityException) {
			Log.d(PlatformUtil.TAG, "Location service permission not granted")
		}
	}

	private fun isContinuous(): Boolean {
		return serviceOffInterval == 0L
	}

	override fun onLocationChanged(l: Location?) {
		val location = convertLocation(l)
		if (!isContinuous()) {
			// unregister listener and wait next time
			val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
			try {
				locationManager.removeUpdates(this)
			} catch (e: Throwable) {
				Log.d(PlatformUtil.TAG, "Location service permission not granted") //$NON-NLS-1$
			}

			val lock = getLock(this)
			if (lock.isHeld) {
				lock.release()
			}
			app().shareLocationHelper.updateLocation(location)
		} else if (System.currentTimeMillis() - lastLocationSentTime > sendLocationInterval * 1000) {
			lastLocationSentTime = System.currentTimeMillis()
			app().shareLocationHelper.updateLocation(location)
		}
	}

	override fun onProviderDisabled(provider: String) {
		Toast.makeText(this, getString(R.string.location_service_no_gps_available), Toast.LENGTH_LONG).show()
	}

	override fun onProviderEnabled(provider: String) {}

	override fun onStatusChanged(provider: String, status: Int, extras: Bundle) {}

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
			TelegramHelper.MESSAGE_TYPE_MAP -> shareInfo.pendingTdLibMap--
		}
	}

	companion object {

		const val USED_BY_MY_LOCATION: Int = 1
		const val USED_BY_USERS_LOCATIONS: Int = 2
		const val USAGE_INTENT = "SERVICE_USED_BY"
		const val USAGE_OFF_INTERVAL = "SERVICE_OFF_INTERVAL"
		const val SEND_LOCATION_INTERVAL = "SEND_LOCATION_INTERVAL"

		const val OFF_INTERVAL_THRESHOLD: Long = 30000L

		private var lockStatic: PowerManager.WakeLock? = null

		@Synchronized
		fun getLock(context: Context): PowerManager.WakeLock {
			var lockStatic = lockStatic
			return if (lockStatic == null) {
				val mgr = context.getSystemService(Context.POWER_SERVICE) as PowerManager
				lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OsmandServiceLock")
				this.lockStatic = lockStatic
				lockStatic
			} else {
				lockStatic
			}
		}

		fun isUsedByMyLocation(usedBy: Int): Boolean {
			return (usedBy and USED_BY_MY_LOCATION) > 0
		}

		fun isUsedByUsersLocations(usedBy: Int): Boolean {
			return (usedBy and USED_BY_USERS_LOCATIONS) > 0
		}

		fun isOffIntervalDepended(usedBy: Int): Boolean {
			return isUsedByMyLocation(usedBy)
		}

		fun normalizeOffInterval(interval: Long): Long {
			return if (interval < OFF_INTERVAL_THRESHOLD) 0 else interval
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
