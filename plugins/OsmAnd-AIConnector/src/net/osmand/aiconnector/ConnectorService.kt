package net.osmand.aiconnector

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log

/** Foreground service that keeps the MCP server and the OsmAnd connection alive. */
class ConnectorService : Service() {

	companion object {
		private const val TAG = "OsmAndAiConnector"
		private const val CHANNEL = "connector"

		/** URL of the running server, null when it is stopped. */
		@Volatile
		var runningUrl: String? = null
			private set

		@Volatile
		var tools: OsmAndTools? = null
			private set

		private const val EXTRA_FOREGROUND = "foreground"

		/**
		 * BACKGROUND: a foreground service, Doze does not stop it; SCREEN: a plain service the visible activity
		 * starts and stops, no notification.
		 */
		fun start(ctx: Context) {
			val background = ConnectorSettings.runMode(ctx) == ConnectorSettings.RunMode.BACKGROUND
			val intent = Intent(ctx, ConnectorService::class.java).putExtra(EXTRA_FOREGROUND, background)
			if (background) ctx.startForegroundService(intent) else ctx.startService(intent)
		}

		fun stop(ctx: Context) {
			ctx.stopService(Intent(ctx, ConnectorService::class.java))
		}
	}

	private var server: McpHttpServer? = null
	private var bridge: OsmAndBridge? = null
	private var discovery: ConnectorDiscovery? = null

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		val foreground = intent?.getBooleanExtra(EXTRA_FOREGROUND, true) ?: true
		if (foreground) {
			startForegroundCompat()
		} else {
			stopForeground(STOP_FOREGROUND_REMOVE)
		}
		restart()
		if (foreground) {
			updateNotification()
		}
		return if (foreground) START_STICKY else START_NOT_STICKY
	}

	private fun restart() {
		stopServer()
		val b = OsmAndBridge(applicationContext, ConnectorSettings.osmandPackage(this))
		val t = OsmAndTools(b)
		val s = McpHttpServer(ConnectorSettings.bindHost(this), ConnectorSettings.PORT, ConnectorSettings.token(this), t, readBridgeScript())
		try {
			s.start(60_000, true)
			bridge = b
			server = s
			tools = t
			runningUrl = ConnectorSettings.url(this)
			Log.i(TAG, "MCP server on $runningUrl")
			if (ConnectorSettings.access(this) == ConnectorSettings.Access.WIFI) {
				discovery = ConnectorDiscovery(this).also { it.register(ConnectorSettings.PORT) }
			}
		} catch (e: Exception) {
			runningUrl = null
			Log.e(TAG, "Cannot start the server", e)
		}
	}

	private fun readBridgeScript(): String? = try {
		assets.open(McpHttpServer.BRIDGE_FILE).bufferedReader().use { it.readText() }
	} catch (e: Exception) {
		Log.e(TAG, "No bridge script in assets", e)
		null
	}

	private fun stopServer() {
		discovery?.unregister()
		discovery = null
		server?.stop()
		bridge?.disconnect()
		server = null
		bridge = null
		tools = null
		runningUrl = null
	}

	override fun onDestroy() {
		stopServer()
		super.onDestroy()
	}

	private fun updateNotification() {
		getSystemService(NotificationManager::class.java).notify(1, buildNotification())
	}

	private fun buildNotification(): Notification {
		val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
			PendingIntent.FLAG_IMMUTABLE)
		val address = runningUrl?.removePrefix("http://")?.removeSuffix("/mcp")
		return Notification.Builder(this, CHANNEL)
			.setSmallIcon(R.drawable.ic_notification)
			.setContentTitle(getString(R.string.notification_title))
			.setContentText(if (address != null) getString(R.string.notification_text, address)
				else getString(R.string.status_cannot_start, ConnectorSettings.PORT))
			.setContentIntent(open)
			.setOngoing(true)
			.build()
	}

	private fun startForegroundCompat() {
		val nm = getSystemService(NotificationManager::class.java)
		nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.notification_channel),
			NotificationManager.IMPORTANCE_LOW))
		val notification = buildNotification()
		if (Build.VERSION.SDK_INT >= 34) {
			startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
		} else {
			startForeground(1, notification)
		}
	}
}
