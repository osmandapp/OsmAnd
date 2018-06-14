package net.osmand.telegram

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.TelegramService
import net.osmand.telegram.utils.AndroidUtils

class OnNavigationServiceAlarmReceiver : BroadcastReceiver() {
	@SuppressLint("MissingPermission")
	override fun onReceive(context: Context, intent: Intent) {
		val lock = TelegramService.getLock(context)
		val app = context.applicationContext as TelegramApplication
		val service = app.telegramService
		// do not do nothing
		if (lock.isHeld || service == null) {
			return
		}
		lock.acquire(10 * 60 * 1000L /*10 minutes*/)

		// request location updates
		val locationManager = service.getSystemService(Context.LOCATION_SERVICE) as LocationManager
		try {
			if (AndroidUtils.isLocationPermissionAvailable(app)) {
				locationManager.requestLocationUpdates(service.serviceOffProvider, 0, 0f, service)
				val handler = service.handler
				if (service.serviceOffInterval > service.serviceError && handler != null) {
					handler.postDelayed({
						// if lock is not anymore held
						if (lock.isHeld) {
							lock.release()
							locationManager.removeUpdates(service)
						}
					}, service.serviceError)
				}
			}
		} catch (e: RuntimeException) {
			e.printStackTrace()
		}
	}
}