package net.osmand.telegram

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Handler
import net.osmand.telegram.helpers.*
import net.osmand.telegram.helpers.OsmandAidlHelper.OsmandHelperListener
import net.osmand.telegram.helpers.OsmandAidlHelper.UpdatesListener
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
	lateinit var osmandAidlHelper: OsmandAidlHelper private set
	lateinit var locationProvider: TelegramLocationProvider private set
	lateinit var locationMessages: LocationMessages private set

	var telegramService: TelegramService? = null

	private val uiHandler = Handler()

	private val lastTimeInternetConnectionChecked: Long = 0
	private var internetConnectionAvailable = true

	override fun onCreate() {
		super.onCreate()
		telegramHelper.appDir = filesDir.absolutePath
		telegramHelper.init()

		settings = TelegramSettings(this)
		telegramHelper.messageActiveTimeSec = settings.locHistoryTime
		uiUtils = UiUtils(this)
		osmandAidlHelper = OsmandAidlHelper(this)
		osmandAidlHelper.listener = object : OsmandAidlHelper.OsmandHelperListener {
			override fun onOsmandConnectionStateChanged(connected: Boolean) {
				if (connected) {
					osmandAidlHelper.setNavDrawerItems(
						applicationContext.packageName,
						listOf(getString(R.string.app_name_short_online)),
						listOf("osmand_telegram://main_activity"),
						listOf("ic_action_location_sharing_app"),
						listOf(-1)
					)
					showLocationHelper.addDirectionContextMenuButton()
					if (settings.hasAnyChatToShowOnMap()) {
						showLocationHelper.startShowingLocation()
					}
				}
			}
		}
		osmandAidlHelper.setUpdatesListener(object : UpdatesListener {
			override fun update() {
				showLocationHelper.startUpdateMessagesTask()
			}
		})
		shareLocationHelper = ShareLocationHelper(this)
		showLocationHelper = ShowLocationHelper(this)
		notificationHelper = NotificationHelper(this)
		locationProvider = TelegramLocationProvider(this)
		locationMessages = LocationMessages(this)

		if (settings.hasAnyChatToShareLocation() && AndroidUtils.isLocationPermissionAvailable(this)) {
			shareLocationHelper.startSharingLocation()
		}
		if (settings.monitoringEnabled) {
			showLocationHelper.startShowingLocation()
		}
	}

	fun cleanupResources() {
		osmandAidlHelper.cleanupResources()
		telegramHelper.close()
	}

	fun stopSharingLocation() {
		settings.stopSharingLocationToChats()
		shareLocationHelper.stopSharingLocation()
		telegramHelper.stopSendingLiveLocationMessages(settings.getChatsShareInfo())
	}

	fun stopMonitoring() {
		settings.monitoringEnabled = false
		stopUserLocationService()
	}

	fun isAnyOsmAndInstalled() = TelegramSettings.AppConnect.getInstalledApps(this).isNotEmpty()

	fun isOsmAndChosen() = settings.appToConnectPackage.isNotEmpty()

	fun isOsmAndInstalled() = AndroidUtils.isAppInstalled(this, settings.appToConnectPackage)

	val isWifiConnected: Boolean
		get() {
			val mgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
			val ni = mgr.activeNetworkInfo
			return ni != null && ni.type == ConnectivityManager.TYPE_WIFI
		}

	val isMobileConnected: Boolean
		get() {
			val mgr = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
			val ni = mgr.activeNetworkInfo
			return ni != null && ni.type == ConnectivityManager.TYPE_MOBILE
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
		if (connected) {
			showLocationHelper.setupMapLayer()
			showLocationHelper.addDirectionContextMenuButton()
		}
	}

	private fun startTelegramService(intent: Int, serviceOffInterval: Long = 0) {
		var i = intent
		var interval = serviceOffInterval
		val serviceIntent = Intent(this, TelegramService::class.java)

		val telegramService = telegramService
		if (telegramService != null) {
			i = intent or telegramService.usedBy
			interval = if (TelegramService.isOffIntervalDepended(intent)) {
				Math.min(telegramService.serviceOffInterval, interval)
			} else {
				telegramService.serviceOffInterval
			}
			telegramService.stopSelf()
		}

		serviceIntent.putExtra(TelegramService.USAGE_INTENT, i)
		serviceIntent.putExtra(TelegramService.USAGE_OFF_INTERVAL, interval)
		serviceIntent.putExtra(TelegramService.SEND_LOCATION_INTERVAL, settings.sendMyLocInterval)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			startForegroundService(serviceIntent)
		} else {
			startService(serviceIntent)
		}
	}

	fun startMyLocationService() {
		val interval = settings.sendMyLocInterval
		startTelegramService(TelegramService.USED_BY_MY_LOCATION, TelegramService.normalizeOffInterval(interval))
	}

	fun stopMyLocationService() {
		telegramService?.stopIfNeeded(this, TelegramService.USED_BY_MY_LOCATION)
	}

	fun forceUpdateMyLocation() {
		telegramService?.forceLocationUpdate()
	}

	fun updateSendLocationInterval() {
		telegramService?.updateSendLocationInterval(settings.sendMyLocInterval)
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
