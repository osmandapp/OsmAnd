package net.osmand.telegram

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Handler
import net.osmand.telegram.helpers.OsmandAidlHelper
import net.osmand.telegram.helpers.OsmandAidlHelper.OsmandHelperListener
import net.osmand.telegram.helpers.ShareLocationHelper
import net.osmand.telegram.helpers.ShowLocationHelper
import net.osmand.telegram.helpers.TelegramHelper
import net.osmand.telegram.notifications.NotificationHelper
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.UiUtils

class TelegramApplication : Application(), OsmandHelperListener {

	val telegramHelper = TelegramHelper.instance
	lateinit var settings: TelegramSettings private set
	lateinit var uiUtils: UiUtils private set
	lateinit var shareLocationHelper: ShareLocationHelper private set
	lateinit var showLocationHelper: ShowLocationHelper private set
	lateinit var notificationHelper: NotificationHelper private set
	lateinit var osmandHelper: OsmandAidlHelper private set

	var telegramService: TelegramService? = null

	private val uiHandler = Handler()

	private val lastTimeInternetConnectionChecked: Long = 0
	private var internetConnectionAvailable = true

	override fun onCreate() {
		super.onCreate()
		telegramHelper.appDir = filesDir.absolutePath

		settings = TelegramSettings(this)
		uiUtils = UiUtils(this)
		osmandHelper = OsmandAidlHelper(this)
		shareLocationHelper = ShareLocationHelper(this)
		showLocationHelper = ShowLocationHelper(this)
		notificationHelper = NotificationHelper(this)

		if (settings.hasAnyChatToShareLocation() && AndroidUtils.isLocationPermissionAvailable(this)) {
			shareLocationHelper.startSharingLocation()
		}
		if (settings.hasAnyChatToShowOnMap()) {
			showLocationHelper.startShowingLocation()
		}
	}

	fun cleanupResources() {
		osmandHelper.cleanupResources()
		telegramHelper.close()
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

	override fun onOsmandConnectionStateChanged(connected: Boolean) {
		showLocationHelper.setupMapLayer()
	}

	private fun startTelegramService(intent: Int) {
		var i = intent
		val serviceIntent = Intent(this, TelegramService::class.java)

		val telegramService = telegramService
		if (telegramService != null) {
			i = intent or telegramService.usedBy
			telegramService.stopSelf()
		}

		serviceIntent.putExtra(TelegramService.USAGE_INTENT, i)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(serviceIntent)
		} else {
			startService(serviceIntent)
		}
	}

	fun startMyLocationService() {
		startTelegramService(TelegramService.USED_BY_MY_LOCATION)
	}

	fun stopMyLocationService() {
		telegramService?.stopIfNeeded(this, TelegramService.USED_BY_MY_LOCATION)
	}

	fun startUserLocationService() {
		startTelegramService(TelegramService.USED_BY_USERS_LOCATIONS)
	}

	fun stopUserLocationService() {
		telegramService?.stopIfNeeded(this, TelegramService.USED_BY_USERS_LOCATIONS)
	}

	fun runInUIThread(action: (() -> Unit)) {
		uiHandler.post(action)
	}

	fun runInUIThread(action: (() -> Unit), delay: Long) {
		uiHandler.postDelayed(action, delay)
	}
}
