package net.osmand.telegram.utils

import android.os.AsyncTask
import android.widget.Toast
import net.osmand.PlatformUtil
import net.osmand.osm.io.NetworkUtils
import net.osmand.telegram.TelegramApplication
import java.net.HttpURLConnection

object AndroidNetworkUtils {

	private const val CONNECTION_TIMEOUT = 15000
	private val log = PlatformUtil.getLog(AndroidNetworkUtils::class.java)

	fun sendRequestAsync(ctx: TelegramApplication, url: String) {

		object : AsyncTask<Void, Void, String>() {
			override fun doInBackground(vararg params: Void): String? {
				try {
					return sendRequest(ctx, url)
				} catch (e: Exception) {
					return null
				}

			}
		}.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null as Void?)
	}

	fun sendRequest(ctx: TelegramApplication, url: String): String? {
		var connection: HttpURLConnection? = null
		try {
			connection = NetworkUtils.getHttpURLConnection(url)
			if (connection != null) {
				connection.setRequestProperty("Accept-Charset", "UTF-8")
				connection.setRequestProperty("User-Agent", ctx.packageName)
				connection.connectTimeout = CONNECTION_TIMEOUT
				connection.requestMethod = "GET"
				connection.connect()
				if (connection.responseCode != HttpURLConnection.HTTP_OK) {
					Toast.makeText(ctx, connection.responseMessage, Toast.LENGTH_LONG).show()
				}
			}
		} catch (e: Exception) {
			log.error(e)
		} finally {
			connection?.disconnect()
		}

		return null
	}
}
