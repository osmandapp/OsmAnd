package net.osmand.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.ProxyBuilder
import io.ktor.client.engine.http
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.runBlocking
import net.osmand.shared.util.LoggerFactory
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

open class NetworkAPI internal constructor(
	private val engineFactory: (() -> HttpClientEngine)?
) {

	data class NetworkResponse(
		val response: String?,
		val error: String?
	)

	constructor() : this(null)

	companion object {
		private const val CONNECT_TIMEOUT = 30_000L
		private const val READ_TIMEOUT = CONNECT_TIMEOUT * 2
		private const val CALL_TIMEOUT = CONNECT_TIMEOUT + READ_TIMEOUT
		private const val USER_AGENT = "Mozilla/5.0 (OsmAnd; Kotlin)"
		private val LOG = LoggerFactory.getLogger("NetworkAPI")
	}

	private val proxyState = NetworkProxyState()

	val proxyHost: String?
		get() = proxyState.proxyHost

	val proxyPort: Int
		get() = proxyState.proxyPort

	open fun hasProxy(): Boolean {
		return proxyState.hasProxy()
	}

	open fun setProxy(host: String?, port: Int) {
		proxyState.setProxy(host, port)
	}

	open fun sendGetRequest(
		url: String,
		auth: String? = null,
		useGzip: Boolean = false,
		userAgent: String = USER_AGENT
	): NetworkResponse {
		val client = createClient(useGzip)
		return try {
			LOG.info("GET : $url")

			runBlocking {
				val response: HttpResponse = client.request(url) {
					method = HttpMethod.Get
					header(HttpHeaders.UserAgent, userAgent)
					if (auth != null) {
						header(HttpHeaders.Authorization, "Basic ${encodeAuth(auth)}")
					}
				}

				LOG.info("Response code and message : ${response.status.value} ${response.status.description}")

				if (response.status.value != 200) {
					val error = response.status.description.ifBlank { "HTTP Error ${response.status.value}" }
					NetworkResponse(null, error)
				} else {
					NetworkResponse(response.bodyAsText(), null)
				}
			}
		} catch (e: Throwable) {
			LOG.error(e.message, e)
			NetworkResponse(null, e.message ?: "Unknown error")
		} finally {
			client.close()
		}
	}

	open fun resolveRedirectUrl(url: String): String? {
		return resolveRedirectUrl(url, USER_AGENT)
	}

	open fun resolveRedirectUrl(
		url: String,
		userAgent: String = USER_AGENT
	): String? {
		val client = createClient(useGzip = false)
		return try {
			runBlocking {
				val response: HttpResponse = client.request(url) {
					method = HttpMethod.Head
					header(HttpHeaders.UserAgent, userAgent)
				}

				val code = response.status.value
				if (code !in 300..399) {
					LOG.error("Got no Redirect from $url")
					return@runBlocking null
				}

				val location = response.headers[HttpHeaders.Location]
				if (location.isNullOrBlank()) {
					LOG.error("Got no Location from $url")
					return@runBlocking null
				}

				location
			}
		} catch (e: Throwable) {
			LOG.error("Got error from $url ${e.message}")
			null
		} finally {
			client.close()
		}
	}

	private fun createClient(useGzip: Boolean): HttpClient {
		val engineFactory = engineFactory
		return if (engineFactory != null) {
			HttpClient(engineFactory()) {
				configureCommon(useGzip)
			}
		} else {
			HttpClient {
				configureDefault(proxyState.ktorProxyData, useGzip)
			}
		}
	}

	private fun HttpClientConfig<*>.configureCommon(useGzip: Boolean) {
		followRedirects = false
		expectSuccess = false

		install(HttpTimeout) {
			connectTimeoutMillis = CONNECT_TIMEOUT
			socketTimeoutMillis = READ_TIMEOUT
			requestTimeoutMillis = CALL_TIMEOUT
		}

		if (useGzip) {
			install(ContentEncoding) {
				gzip()
			}
		}
	}

	private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.configureDefault(
		proxyData: NetworkProxyData?,
		useGzip: Boolean
	) {
		configureCommon(useGzip)

		if (proxyData != null) {
			engine {
				proxy = ProxyBuilder.http("http://${proxyData.host}:${proxyData.port}")
			}
		}
	}

	@OptIn(ExperimentalEncodingApi::class)
	private fun encodeAuth(auth: String): String {
		return Base64.encode(auth.encodeToByteArray())
	}
}
