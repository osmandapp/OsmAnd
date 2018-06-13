package net.osmand.telegram

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
import net.osmand.OnNavigationServiceAlarmReceiver
import net.osmand.PlatformUtil
import net.osmand.telegram.helpers.TelegramHelper.TelegramIncomingMessagesListener
import net.osmand.telegram.notifications.TelegramNotification.NotificationType
import org.drinkless.td.libcore.telegram.TdApi
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
	var serviceError = 0L
		private set

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
		} else if (!needLocation()) {
			// Issue #3604
			val app = app()
			if (usedBy == 2 && app.settings.sendMyLocationInterval >= 30000 && serviceOffInterval == 0L) {
				serviceOffInterval = app.settings.sendMyLocationInterval
				// From onStartCommand:
				serviceError = serviceOffInterval / 5
				serviceError = Math.min(serviceError, 12 * 60 * 1000)
				serviceError = Math.max(serviceError, 30 * 1000)
				serviceError = Math.min(serviceError, serviceOffInterval)

				val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
				pendingIntent = PendingIntent.getBroadcast(this, 0, Intent(this, OnNavigationServiceAlarmReceiver::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
				alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 500, serviceOffInterval.toLong(), pendingIntent)
			}
			app.notificationHelper.refreshNotification(NotificationType.LOCATION)
		}
	}

	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		val app = app()
		handler = Handler()
		usedBy = intent.getIntExtra(USAGE_INTENT, 0)

		serviceOffInterval = intent.getLongExtra(USAGE_OFF_INTERVAL, 0)
		// use only gps provider
		serviceOffProvider = LocationManager.GPS_PROVIDER
		serviceError = serviceOffInterval / 5
		// 1. not more than 12 mins
		serviceError = Math.min(serviceError, 12 * 60 * 1000)
		// 2. not less than 30 seconds
		serviceError = Math.max(serviceError, 30 * 1000)
		// 3. not more than serviceOffInterval
		serviceError = Math.min(serviceError, serviceOffInterval)

		app.telegramService = this
		app.telegramHelper.incomingMessagesListener = this

		if (needLocation()) {
			initLocationUpdates()
		}

		val locationNotification = app.notificationHelper.locationNotification
		val notification = app.notificationHelper.buildNotification(locationNotification)
		startForeground(locationNotification.telegramNotificationId, notification)
		app.notificationHelper.refreshNotification(locationNotification.type)
		return Service.START_REDELIVER_INTENT
	}

	override fun onDestroy() {
		super.onDestroy()
		val app = app()
		app.telegramHelper.incomingMessagesListener = null
		app.telegramService = null

		usedBy = 0

		removeLocationUpdates()

		if (shouldCleanupResources) {
			app.cleanupResources()
		}

		// remove notification
		stopForeground(java.lang.Boolean.TRUE)
	}

	private fun initLocationUpdates() {
		// request location updates
		val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
		try {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this@TelegramService)
		} catch (e: SecurityException) {
			Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_LONG).show()
			Log.d(PlatformUtil.TAG, "Location service permission not granted")
		} catch (e: IllegalArgumentException) {
			Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_LONG).show()
			Log.d(PlatformUtil.TAG, "GPS location provider not available")
		}
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

	private fun needLocation(): Boolean {
		return (usedBy and USED_BY_MY_LOCATION) > 0
	}

	private fun isContinuous(): Boolean {
		return serviceOffInterval == 0L
	}

	override fun onLocationChanged(l: Location?) {
		if (l != null) {
			val location = convertLocation(l)
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

	override fun onReceiveChatLocationMessages(chatTitle: String, vararg messages: TdApi.Message) {
		val app = app()
		if (app.settings.isShowingChatOnMap(chatTitle)) {
			ShowMessagesTask(app, chatTitle).executeOnExecutor(executor, *messages)
		}
	}

	private class ShowMessagesTask(private val app: TelegramApplication, private val chatTitle: String) : AsyncTask<TdApi.Message, Void, Void?>() {

		override fun doInBackground(vararg messages: TdApi.Message): Void? {
			for (message in messages) {
				app.showLocationHelper.showLocationOnMap(chatTitle, message)
			}
			return null
		}
	}

	companion object {

		const val USED_BY_MY_LOCATION: Int = 1
		const val USED_BY_USERS_LOCATIONS: Int = 2
		const val USAGE_INTENT = "SERVICE_USED_BY"
		const val USAGE_OFF_INTERVAL = "SERVICE_OFF_INTERVAL"

		private var lockStatic: PowerManager.WakeLock? = null

		@Synchronized
		fun getLock(context: Context): PowerManager.WakeLock {
			var lockStatic = lockStatic
			if (lockStatic == null) {
				val mgr = context.getSystemService(Context.POWER_SERVICE) as PowerManager
				lockStatic = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "OsmandServiceLock")
				this.lockStatic = lockStatic
				return lockStatic
			} else {
				return lockStatic
			}
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
