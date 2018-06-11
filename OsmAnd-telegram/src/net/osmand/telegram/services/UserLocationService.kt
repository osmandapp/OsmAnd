package net.osmand.telegram.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import net.osmand.telegram.TelegramApplication
import net.osmand.telegram.helpers.TelegramHelper.TelegramIncomingMessagesListener
import org.drinkless.td.libcore.telegram.TdApi
import java.util.concurrent.Executors

class UserLocationService : Service(), TelegramIncomingMessagesListener {

    private val binder = LocationServiceBinder()
    private fun app() = application as TelegramApplication
    private val executor = Executors.newSingleThreadExecutor()

    var handler: Handler? = null

    class LocationServiceBinder : Binder()

    override fun onBind(intent: Intent): IBinder? {
        return binder
    }

    override fun onReceiveChatLocationMessages(chatTitle: String, vararg messages: TdApi.Message) {
        val app = app()
        if (app.settings.isShowingChatOnMap(chatTitle)) {
            ShowMessagesTask(app, chatTitle).executeOnExecutor(executor, *messages)
        }
    }

    fun stopIfNeeded(ctx: Context) {
        val serviceIntent = Intent(ctx, UserLocationService::class.java)
        ctx.stopService(serviceIntent)
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        handler = Handler()
        val app = app()

        app.userLocationService = this
        app.telegramHelper.incomingMessagesListener = this

        val showLocationNotification = app.notificationHelper.showLocationNotification
        val notification = app.notificationHelper.buildNotification(showLocationNotification)
        startForeground(showLocationNotification.telegramNotificationId, notification)
        app.notificationHelper.refreshNotification(showLocationNotification.type)
        return Service.START_REDELIVER_INTENT
    }

    override fun onDestroy() {
        super.onDestroy()
        val app = app()
        app.telegramHelper.incomingMessagesListener = null
        app.userLocationService = null

        stopForeground(java.lang.Boolean.TRUE)
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val app = app()
        if (app.userLocationService != null) {
            // Do not stop service after UI task was dismissed
            //this@MyLocationService.stopSelf()
        }
    }

    class ShowMessagesTask(private val app: TelegramApplication, private val chatTitle: String) : AsyncTask<TdApi.Message, Void, Void?>() {

        override fun doInBackground(vararg messages: TdApi.Message): Void? {
            for (message in messages) {
                app.showLocationHelper.showLocationOnMap(chatTitle, message)
            }
            return null
        }
    }
}