package net.osmand.telegram

import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

class TelegramApplication : Application() {

    val telegramHelper: TelegramHelper = TelegramHelper.instance

    private val lastTimeInternetConnectionChecked: Long = 0
    private var internetConnectionAvailable = true

    override fun onCreate() {
        super.onCreate()
        telegramHelper.appDir = filesDir.absolutePath
    }

    val isWifiConnected: Boolean
        get() {
            val mgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val ni = mgr.activeNetworkInfo
            return ni != null && ni.type == ConnectivityManager.TYPE_WIFI
        }

    private val isInternetConnected: Boolean
        get() {
            val mgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val active = mgr.activeNetworkInfo
            if (active == null) {
                return false
            } else {
                val state = active.state
                return state != NetworkInfo.State.DISCONNECTED && state != NetworkInfo.State.DISCONNECTING
            }
        }

    // Check internet connection available every 15 seconds
    val isInternetConnectionAvailable: Boolean
        get() = isInternetConnectionAvailable(false)

    fun isInternetConnectionAvailable(update: Boolean): Boolean {
        val delta = System.currentTimeMillis() - lastTimeInternetConnectionChecked
        if (delta < 0 || delta > 15000 || update) {
            internetConnectionAvailable = isInternetConnected
        }
        return internetConnectionAvailable
    }
}
