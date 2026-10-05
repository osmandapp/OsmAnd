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

		fun start(ctx: Context) {
			ctx.startForegroundService(Intent(ctx, ConnectorService::class.java))
		}

		fun stop(ctx: Context) {
			ctx.stopService(Intent(ctx, ConnectorService::class.java))
		}
	}

	private var server: McpHttpServer? = null
	private var bridge: OsmAndBridge? = null

	override fun onBind(intent: Intent?): IBinder? = null

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		startForegroundCompat()
		restart()
		return START_STICKY
	}

	private fun restart() {
		stopServer()
		val b = OsmAndBridge(applicationContext, ConnectorSettings.osmandPackage(this))
		val t = OsmAndTools(b)
		val s = McpHttpServer(ConnectorSettings.bindHost(this), ConnectorSettings.PORT, ConnectorSettings.token(this), t)
		try {
			s.start(60_000, true)
			bridge = b
			server = s
			tools = t
			runningUrl = ConnectorSettings.url(this)
			Log.i(TAG, "MCP server on $runningUrl")
		} catch (e: Exception) {
			runningUrl = null
			Log.e(TAG, "Cannot start the server", e)
		}
	}

	private fun stopServer() {
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

	private fun startForegroundCompat() {
		val nm = getSystemService(NotificationManager::class.java)
		nm.createNotificationChannel(NotificationChannel(CHANNEL, getString(R.string.notification_channel),
			NotificationManager.IMPORTANCE_LOW))
		val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
			PendingIntent.FLAG_IMMUTABLE)
		val notification = Notification.Builder(this, CHANNEL)
			.setSmallIcon(R.drawable.ic_notification)
			.setContentTitle(getString(R.string.notification_title))
			.setContentText(getString(R.string.notification_text))
			.setContentIntent(open)
			.setOngoing(true)
			.build()
		if (Build.VERSION.SDK_INT >= 34) {
			startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
		} else {
			startForeground(1, notification)
		}
	}
}
