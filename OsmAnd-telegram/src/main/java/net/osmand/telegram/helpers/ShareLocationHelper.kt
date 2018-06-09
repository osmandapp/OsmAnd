package net.osmand.telegram.helpers

import net.osmand.Location
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.notifications.TelegramNotification.NotificationType

class ShareLocationHelper(private val app: TelegramApplication) {

    companion object {
        const val MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC = 60 * 60 * 24 - 1 // day
    }

    var sharingLocation: Boolean = false
        private set

    var duration: Long = 0
        private set

    var distance: Int = 0
        private set

    var lastLocationMessageSentTime: Long = 0

    private var lastTimeInMillis: Long = 0L

    private var lastLocation: Location? = null
        set(value) {
            if (lastTimeInMillis == 0L) {
                lastTimeInMillis = System.currentTimeMillis()
            } else {
                val currentTimeInMillis = System.currentTimeMillis()
                duration += currentTimeInMillis - lastTimeInMillis
                lastTimeInMillis = currentTimeInMillis
            }
            if (lastLocation != null && value != null) {
                distance += value.distanceTo(lastLocation).toInt()
            }
            field = value
        }

    fun updateLocation(location: Location?) {
        lastLocation = location

        if (location != null) {
            val shareLocationChats = app.settings.getShareLocationChats()
            if (shareLocationChats.isNotEmpty()) {
                app.telegramHelper.sendLiveLocation(shareLocationChats, MAX_LOCATION_MESSAGE_LIVE_PERIOD_SEC, location.latitude, location.longitude)
            }
            lastLocationMessageSentTime = System.currentTimeMillis()
        }
        refreshNotification()
    }

    fun startSharingLocation() {
        sharingLocation = true

        app.startLocationService()

        refreshNotification()
    }

    fun stopSharingLocation() {
        sharingLocation = false

        app.stopLocationService()
        lastLocation = null
        lastTimeInMillis = 0L
        distance = 0
        duration = 0

        refreshNotification()
    }

    fun pauseSharingLocation() {
        sharingLocation = false

        app.stopLocationService()
        lastLocation = null
        lastTimeInMillis = 0L

        refreshNotification()
    }

    private fun refreshNotification() {
        app.runInUIThread {
            app.notificationHelper.refreshNotification(NotificationType.SHARE_LOCATION)
        }
    }
}
