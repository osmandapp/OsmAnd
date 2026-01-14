package net.osmand.shared.api

import android.util.Base64
import net.osmand.shared.api.NetworkAPI.NetworkResponse
import net.osmand.shared.util.LoggerFactory
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.MalformedURLException
import java.net.Proxy
import java.net.URL
import java.util.zip.GZIPInputStream
import kotlin.text.Charsets.UTF_8

class NetworkAPIImpl : NetworkAPI {

	companion object {
		private val LOG = LoggerFactory.getLogger("NetworkAPIImpl")
	}

	private var proxy: Proxy? = null

	override fun hasProxy(): Boolean {
		return proxy != null
	}

	override fun setProxy(host: String?, port: Int) {
		if (!host.isNullOrEmpty() && port > 0) {
			val isa = InetSocketAddress(host, port)
			proxy = Proxy(Proxy.Type.HTTP, isa)
		} else {
			proxy = null
		}
	}

	@Throws(MalformedURLException::class, IOException::class)
	fun getHttpURLConnection(urlString: String): HttpURLConnection {
		val url = URL(urlString)
		return if (proxy != null) {
			url.openConnection(proxy) as HttpURLConnection
		} else {
			url.openConnection() as HttpURLConnection
		}
	}

	override fun sendGetRequest(
		url: String,
		auth: String?,
		useGzip: Boolean,
		userAgent: String
	): NetworkResponse {
		return try {
			LOG.info("GET : $url")

			val conn = getHttpURLConnection(url)
			conn.doInput = true
			conn.doOutput = false
			conn.requestMethod = "GET"
			conn.setRequestProperty("User-Agent", userAgent)

			if (auth != null) {
				val encodedAuth = Base64.encodeToString(auth.toByteArray(UTF_8), Base64.NO_WRAP)
				conn.setRequestProperty("Authorization", "Basic $encodedAuth")
			}
			if (useGzip) {
				conn.setRequestProperty("Accept-Encoding", "gzip")
			}
			LOG.info("Response code and message : ${conn.responseCode} ${conn.responseMessage}")

			if (conn.responseCode != 200) {
				val error = conn.responseMessage ?: "HTTP Error ${conn.responseCode}"
				return NetworkResponse(null, error)
			}
			val contentEncoding = conn.getHeaderField("Content-Encoding")
			val inputStream = if (useGzip && "gzip".equals(contentEncoding, ignoreCase = true)) {
				GZIPInputStream(conn.inputStream)
			} else {
				conn.inputStream
			}
			val responseBody = inputStream.bufferedReader(UTF_8).use { reader ->
				val sb = StringBuilder()
				var line: String? = reader.readLine()
				var first = true

				while (line != null) {
					if (first) {
						first = false
					} else {
						sb.append("\n")
					}
					sb.append(line)
					line = reader.readLine()
				}
				sb.toString()
			}
			NetworkResponse(responseBody, null)
		} catch (e: Exception) {
			LOG.error(e.message, e)
			NetworkResponse(null, e.message ?: "Unknown error")
		}
	}
}