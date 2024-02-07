package net.osmand.telegram

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Handler
import androidx.core.content.ContextCompat
import net.osmand.PlatformUtil
import net.osmand.telegram.TelegramSettings.LocationSource.GOOGLE_PLAY_SERVICES
import net.osmand.telegram.helpers.*
import net.osmand.telegram.helpers.OsmandAidlHelper.OsmandHelperListener
import net.osmand.telegram.helpers.OsmandAidlHelper.UpdatesListener
import net.osmand.telegram.helpers.location.AndroidApiLocationServiceHelper
import net.osmand.telegram.helpers.location.GmsLocationServiceHelper
import net.osmand.telegram.helpers.location.LocationServiceHelper
import net.osmand.telegram.notifications.NotificationHelper
import net.osmand.telegram.ui.TrackerLogcatActivity
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.telegram.utils.UiUtils
import java.io.File

class TelegramApplication : Application() {

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
		osmandAidlHelper.addConnectionListener(object : OsmandHelperListener {
			override fun onOsmandConnectionStateChanged(connected: Boolean) {
				if (connected) {
					osmandAidlHelper.clearNavDrawerItems("net.osmand.telegram")
					osmandAidlHelper.clearNavDrawerItems("net.osmand.telegram.debug")
					osmandAidlHelper.setNavDrawerItems(
						applicationContext.packageName,
						listOf(getString(R.string.app_name_short)),
						listOf("osmand_telegram://main_activity"),
						listOf("ic_action_location_sharing_app"),
						listOf(-1)
					)
					showLocationHelper.setupMapLayer()
					showLocationHelper.addDirectionContextMenuButton()
					showLocationHelper.startShowingLocation()
					showLocationHelper.addOrUpdateStatusWidget(-1, false)
				}
			}
		})
		osmandAidlHelper.setUpdatesListener(object : UpdatesListener {
			override fun update() {
				if (settings.hasAnyChatToShowOnMap()) {
					showLocationHelper.startUpdateMessagesTask()
				}
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

	private fun startTelegramService(intent: Int) {
		var i = intent
		val serviceIntent = Intent(this, TelegramService::class.java)
		val telegramService = telegramService
		if (telegramService != null) {
			i = intent or telegramService.usedBy
			telegramService.stopSelf()
		}
		serviceIntent.putExtra(TelegramService.USAGE_INTENT, i)
		serviceIntent.putExtra(TelegramService.SEND_LOCATION_INTERVAL, settings.sendMyLocInterval)
		ContextCompat.startForegroundService(this, serviceIntent)
	}

	fun startMyLocationService() {
		startTelegramService(TelegramService.USED_BY_MY_LOCATION)
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

	fun createLocationServiceHelper(): LocationServiceHelper {
		return if (settings.locationSource == GOOGLE_PLAY_SERVICES) {
			GmsLocationServiceHelper(this)
		} else {
			AndroidApiLocationServiceHelper(this)
		}
	}

	fun runInUIThread(action: (() -> Unit)) {
		uiHandler.post(action)
	}

	fun runInUIThread(action: (() -> Unit), delay: Long) {
		uiHandler.postDelayed(action, delay)
	}

	fun sendCrashLog(file: File) {
		val intent = Intent(Intent.ACTION_SEND)
		intent.putExtra(Intent.EXTRA_EMAIL, arrayOf("crash@osmand.net"))
		intent.putExtra(Intent.EXTRA_STREAM, AndroidUtils.getUriForFile(this, file))
		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
		intent.type = "vnd.android.cursor.dir/email"
		intent.putExtra(Intent.EXTRA_SUBJECT, "OsmAnd bug")
		val text = StringBuilder()
		text.append("\nDevice : ").append(Build.DEVICE)
		text.append("\nBrand : ").append(Build.BRAND)
		text.append("\nModel : ").append(Build.MODEL)
		text.append("\nProduct : ").append(Build.PRODUCT)
		text.append("\nBuild : ").append(Build.DISPLAY)
		text.append("\nVersion : ").append(Build.VERSION.RELEASE)
		text.append("\nApp : ").append(getString(R.string.app_name_short))
		try {
			val info = packageManager.getPackageInfo(packageName, 0)
			if (info != null) {
				text.append("\nApk Version : ").append(info.versionName).append(" ").append(info.versionCode)
			}
		} catch (e: PackageManager.NameNotFoundException) {
			PlatformUtil.getLog(TrackerLogcatActivity::class.java).error("", e)
		}
		intent.putExtra(Intent.EXTRA_TEXT, text.toString())
		val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
		chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		startActivity(chooserIntent)
	}
}
