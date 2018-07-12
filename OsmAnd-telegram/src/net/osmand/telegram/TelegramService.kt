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
import net.osmand.telegram.helpers.TelegramHelper.TelegramIncomingMessagesListener
import net.osmand.telegram.notifications.TelegramNotification.NotificationType
import net.osmand.telegram.utils.AndroidUtils
import org.drinkless.td.libcore.telegram.TdApi
import java.util.*
import java.util.concurrent.Executors

class TelegramService : Service(), LocationListener, TelegramIncomingMessagesListener {

	private fun app() = application as TelegramApplication
	private val binder = LocationServiceBinder()
	private val executor = Executors.newSingleThreadExecutor()
	private var shouldCleanupResources: Boolean = false

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

	override fun onBind(intent: Intent): IBinder? {
		return binder
	}

	fun stopIfNeeded(ctx: Context, usageIntent: Int) {
		if (usedBy and usageIntent > 0) {
			usedBy -= usageIntent
		}
		if (usedBy == 0) {
			shouldCleanupResources = false
			val serviceIntent = Intent(ctx, TelegramService::class.java)
			ctx.stopService(serviceIntent)
		} else if (isUsedByMyLocation(usedBy)) {
			val app = app()
			if (app.settings.sendMyLocationInterval >= OFF_INTERVAL_THRESHOLD && serviceOffInterval == 0L) {
				serviceOffInterval = app.settings.sendMyLocationInterval
				setupServiceErrorInterval()
				setupAlarm()
			}
			app.notificationHelper.refreshNotification(NotificationType.LOCATION)
		}
	}

	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		val app = app()
		handler = Handler()
		usedBy = intent.getIntExtra(USAGE_INTENT, 0)

		serviceOffInterval = intent.getLongExtra(USAGE_OFF_INTERVAL, 0)
		sendLocationInterval = intent.getLongExtra(SEND_LOCATION_INTERVAL, 0)
		setupServiceErrorInterval()

		app.telegramService = this
		app.telegramHelper.addIncomingMessagesListener(this)

		if (isUsedByMyLocation(usedBy)) {
			initLocationUpdates()
		}
		if (isUsedByUsersLocations(usedBy)) {
			app.telegramHelper.startLiveMessagesUpdates()
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
		app.telegramService = null

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
		for (provider in providers) {
			val location = convertLocation(service.getLastKnownLocation(provider))
			if (location != null) {
				return location
			}
		}
		return null
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
		if (l != null) {
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
			} else if (System.currentTimeMillis() - lastLocationSentTime > sendLocationInterval) {
				lastLocationSentTime = System.currentTimeMillis()
				app().shareLocationHelper.updateLocation(location)
			}
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
		val app = app()
		if (app.settings.isShowingChatOnMap(chatId)) {
			ShowMessagesTask(app).executeOnExecutor(executor, *messages)
		}
	}

	override fun updateLocationMessages() {
		UpdateMessagesTask(app()).executeOnExecutor(executor)
	}

	private class ShowMessagesTask(private val app: TelegramApplication) : AsyncTask<TdApi.Message, Void, Void?>() {

		override fun doInBackground(vararg messages: TdApi.Message): Void? {
			for (message in messages) {
				app.showLocationHelper.addLocationToMap(message)
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
