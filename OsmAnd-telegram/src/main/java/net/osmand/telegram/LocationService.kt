package net.osmand.telegram

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
import net.osmand.telegram.notifications.TelegramNotification
import net.osmand.PlatformUtil

class LocationService : Service(), LocationListener {

    private val binder = LocationServiceBinder()

    var handler: Handler? = null

    class LocationServiceBinder : Binder()

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    fun stopIfNeeded(ctx: Context) {
        val serviceIntent = Intent(ctx, LocationService::class.java)
        ctx.stopService(serviceIntent)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        handler = Handler()
        val app = app()

        app.locationService = this

        // requesting
        // request location updates
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this@LocationService)
        } catch (e: SecurityException) {
            Toast.makeText(this, R.string.no_location_permission, Toast.LENGTH_LONG).show()
            Log.d(PlatformUtil.TAG, "Location service permission not granted")
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, R.string.gps_not_available, Toast.LENGTH_LONG).show()
            Log.d(PlatformUtil.TAG, "GPS location provider not available")
        }

        // registering icon at top level
        // Leave icon visible even for navigation for proper display
        val notification = app.notificationHelper.buildTopNotification()
        if (notification != null) {
            startForeground(TelegramNotification.TOP_NOTIFICATION_SERVICE_ID, notification)
            app.notificationHelper.refreshNotification(TelegramNotification.NotificationType.SHARE_LOCATION)
            //app.notificationHelper.refreshNotifications()
        }
        return Service.START_REDELIVER_INTENT
    }

    private fun app() = application as TelegramApplication

    override fun onDestroy() {
        super.onDestroy()
        val app = app()
        app.locationService = null

        // remove updates
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            locationManager.removeUpdates(this)
        } catch (e: SecurityException) {
            Log.d(PlatformUtil.TAG, "Location service permission not granted")
        }

        // remove notification
        stopForeground(java.lang.Boolean.TRUE)
        app.notificationHelper.updateTopNotification()

        app.runInUIThread({
            app.notificationHelper.refreshNotification(TelegramNotification.NotificationType.SHARE_LOCATION)
            //app.notificationHelper.refreshNotifications()
        }, 500)
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
        app.notificationHelper.removeNotifications()
        if (app.locationService != null) {
            this@LocationService.stopSelf()
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
