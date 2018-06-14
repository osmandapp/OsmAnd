package net.osmand.telegram

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import net.osmand.telegram.utils.AndroidUtils

class OnTelegramServiceAlarmReceiver : BroadcastReceiver() {
	@SuppressLint("MissingPermission")
	override fun onReceive(context: Context, intent: Intent) {
		val lock = TelegramService.getLock(context)
		val app = context.applicationContext as TelegramApplication
		val service = app.telegramService
		// do not do nothing
		if (lock.isHeld || service == null) {
			return
		}
		lock.acquire(15 * 60 * 1000L)

		// request location updates
		val locationManager = service.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		try {
			if (AndroidUtils.isLocationPermissionAvailable(app)) {
				locationManager.requestLocationUpdates(service.serviceOffProvider, 0, 0f, service)
			}
			val handler = service.handler
			if (service.serviceOffInterval > service.serviceErrorInterval && handler != null) {
				handler.postDelayed({
					// if lock is not anymore held
					if (lock.isHeld) {
						lock.release()
						locationManager.removeUpdates(service)
					}
				}, service.serviceErrorInterval)
			}
		} catch (e: RuntimeException) {
			e.printStackTrace()
		}
	}
}