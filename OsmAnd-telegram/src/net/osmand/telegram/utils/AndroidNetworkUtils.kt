package net.osmand.telegram.utils

import android.os.AsyncTask
import net.osmand.PlatformUtil
import net.osmand.telegram.TelegramApplication
import java.io.BufferedOutputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


object AndroidNetworkUtils {

	private val log = PlatformUtil.getLog(AndroidNetworkUtils::class.java)

	interface OnRequestResultListener {
		fun onResult(result: String?)
	}

	fun sendRequestAsync(
		app: TelegramApplication,
		urlText: String,
		json: String?,
		userOperation: String,
		toastAllowed: Boolean,
		post: Boolean,
		listener: OnRequestResultListener?
	) {
		SendRequestTask(app, urlText, json, userOperation, toastAllowed, post, listener).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
	}

	private class SendRequestTask(
		private val app: TelegramApplication,
		private val url: String,
		private val json: String?,
		private val userOperation: String,
		private val toastAllowed: Boolean,
		private val post: Boolean,
		private val listener: OnRequestResultListener?
	) : AsyncTask<Void, Void, String?>() {

		override fun doInBackground(vararg params: Void): String? {
			return try {
				sendRequest(app, url, json, userOperation, toastAllowed, post)
			} catch (e: Exception) {
				log.error(e.message, e)
				null
			}
		}

		override fun onPostExecute(response: String?) {
			listener?.onResult(response)
		}
	}

	fun sendRequest(
		app: TelegramApplication,
		url: String,
		jsonBody: String?,
		userOperation: String,
		toastAllowed: Boolean,
		post: Boolean
	): String? {
		var connection: HttpURLConnection? = null
		try {
			connection = getHttpURLConnection(url)
			connection.setRequestProperty("Accept-Charset", "UTF-8")
			connection.setRequestProperty("User-Agent", app.packageName)
			connection.connectTimeout = 15000
			if (jsonBody != null && post) {
				connection.doInput = true
				connection.doOutput = true
				connection.useCaches = false
				connection.requestMethod = "POST"

				connection.setRequestProperty("Accept", "application/json")
				connection.setRequestProperty("Content-Type", "application/json")
				connection.setRequestProperty("Content-Length", jsonBody.toByteArray(charset("UTF-8")).size.toString())

				connection.setFixedLengthStreamingMode(jsonBody.toByteArray(charset("UTF-8")).size)

				val output = BufferedOutputStream(connection.outputStream)

				output.write(jsonBody.toByteArray(charset("UTF-8")))
				output.flush()
				output.close()

			} else {
				connection.requestMethod = "GET"
				connection.connect()
			}

			if (connection.responseCode != HttpURLConnection.HTTP_OK) {
				if (toastAllowed) {
					val msg = (userOperation + " " + "Failed: " + connection.responseMessage)
					log.error(msg)
				}
			} else {
				val responseBody = StringBuilder()
				responseBody.setLength(0)
				val i = connection.inputStream
				if (i != null) {
					val input = BufferedReader(InputStreamReader(i, "UTF-8"), 256)
					var s: String? = input.readLine()
					var f = true
					while (s != null) {
						if (!f) {
							responseBody.append("\n")
						} else {
							f = false
						}
						responseBody.append(s)
						s = input.readLine()
					}
					try {
						input.close()
						i.close()
					} catch (e: Exception) {
						// ignore exception
					}

				}
				return responseBody.toString()
			}
		} catch (e: NullPointerException) {
			if (toastAllowed) {
				val msg = (userOperation + " " + "Failed - $e" + ": " + connection?.responseMessage)
				log.error(msg)
			}
		} catch (e: MalformedURLException) {
			if (toastAllowed) {
				val msg = (userOperation + " " + "Failed - $e" + ": " + connection?.responseMessage)
				log.error(msg)
			}
		} catch (e: IOException) {
			if (toastAllowed) {
				val msg = (userOperation + " " + "Failed - $e" + ": " + connection?.responseMessage)
				log.error(msg)
			}
		} finally {
			connection?.disconnect()
		}

		return null
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