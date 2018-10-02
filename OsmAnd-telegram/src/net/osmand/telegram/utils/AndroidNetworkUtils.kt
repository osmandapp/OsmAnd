package net.osmand.telegram.utils

import android.os.AsyncTask
import net.osmand.PlatformUtil
import java.io.*
import java.net.*


object AndroidNetworkUtils {

	private val log = PlatformUtil.getLog(AndroidNetworkUtils::class.java)

	interface OnRequestResultListener {
		fun onResult(result: String)
	}

	fun sendRequestAsync(urlText: String, listener: OnRequestResultListener?) {
		SendRequestTask(urlText, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
	}

	private class SendRequestTask(
		private val urlText: String,
		private val listener: OnRequestResultListener?
	) : AsyncTask<Void, Void, String?>() {

		override fun doInBackground(vararg params: Void): String? {
			return try {
				sendRequest(urlText)
			} catch (e: Exception) {
				null
			}
		}

		override fun onPostExecute(response: String?) {
			if (response != null) {
				listener?.onResult(response)
			}
		}
	}

	fun sendRequest(urlText: String): String? {
		try {
			log.info("GET : $urlText")
			val conn = getHttpURLConnection(urlText)
			conn.doInput = true
			conn.doOutput = false
			conn.requestMethod = "GET"
			conn.setRequestProperty("User-Agent", "OsmAnd Sharing")
			log.info("Response code and message : " + conn.responseCode + " " + conn.responseMessage)
			if (conn.responseCode != 200) {
				return conn.responseMessage
			}
			val inputStream = conn.inputStream
			val responseBody = StringBuilder()
			responseBody.setLength(0)
			if (inputStream != null) {
				val bufferedInput =
					BufferedReader(InputStreamReader(inputStream, "UTF-8"))
				var s = bufferedInput.readLine()
				var first = true
				while (s != null) {
					if (first) {
						first = false
					} else {
						responseBody.append("\n")
					}
					responseBody.append(s)
					s = bufferedInput.readLine()
				}
				inputStream.close()
			}
			return responseBody.toString()
		} catch (e: IOException) {
			log.error(e.message, e)
			return e.message
		}
	}

	@Throws(MalformedURLException::class, IOException::class)
	fun getHttpURLConnection(urlString: String): HttpURLConnection {
		return getHttpURLConnection(URL(urlString))
	}

	@Throws(IOException::class)
	fun getHttpURLConnection(url: URL): HttpURLConnection {
		return url.openConnection() as HttpURLConnection
	}
}