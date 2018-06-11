package net.osmand.telegram.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Binder
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import net.osmand.PlatformUtil
import net.osmand.telegram.R
import net.osmand.telegram.TelegramApplication

class MyLocationService : Service(), LocationListener {

	private val binder = LocationServiceBinder()
	private fun app() = application as TelegramApplication

	var handler: Handler? = null

	class LocationServiceBinder : Binder()

	override fun onBind(intent: Intent): IBinder? {
		return binder
	}

	fun stopIfNeeded(ctx: Context) {
		val serviceIntent = Intent(ctx, MyLocationService::class.java)
		ctx.stopService(serviceIntent)
	}

	override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
		handler = Handler()
		val app = app()

		app.myLocationService = this

		// requesting
		// request location updates
		val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
		try {
			locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this@MyLocationService)
		} catch (e: SecurityException) {
			Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_LONG).show()
			Log.d(PlatformUtil.TAG, "Location service permission not granted")
		} catch (e: IllegalArgumentException) {
			Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_LONG).show()
			Log.d(PlatformUtil.TAG, "GPS location provider not available")
		}

		val shareLocationNotification = app.notificationHelper.shareLocationNotification
		val notification = app.notificationHelper.buildNotification(shareLocationNotification)
		startForeground(shareLocationNotification.telegramNotificationId, notification)
		app.notificationHelper.refreshNotification(shareLocationNotification.type)
		return Service.START_REDELIVER_INTENT
	}

	override fun onDestroy() {
		super.onDestroy()
		val app = app()
		app.myLocationService = null

		// remove updates
		val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
		try {
			locationManager.removeUpdates(this)
		} catch (e: SecurityException) {
			Log.d(PlatformUtil.TAG, "Location service permission not granted")
		}

		// remove notification
		stopForeground(java.lang.Boolean.TRUE)
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
		if (app.myLocationService != null) {
			// Do not stop service after UI task was dismissed
			//this@MyLocationService.stopSelf()
		}
	}

	companion object {

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
