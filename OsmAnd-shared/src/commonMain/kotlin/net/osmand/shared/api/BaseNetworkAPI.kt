package net.osmand.shared.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.runBlocking
import net.osmand.shared.util.LoggerFactory

abstract class BaseNetworkAPI : NetworkAPI {

    companion object {
        private const val CONNECT_TIMEOUT = 30_000L
        private const val READ_TIMEOUT = CONNECT_TIMEOUT * 2
        private const val CALL_TIMEOUT = CONNECT_TIMEOUT + READ_TIMEOUT
        private val LOG = LoggerFactory.getLogger("BaseNetworkAPI")
    }

    private val client by lazy {
        HttpClient {
            followRedirects = false
            expectSuccess = false

            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT
                socketTimeoutMillis = READ_TIMEOUT
                requestTimeoutMillis = CALL_TIMEOUT
            }
        }
    }

    override fun resolveRedirectUrl(
        url: String,
        userAgent: String
    ): String? = runBlocking {
        try {
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
        } catch (e: Throwable) {
            LOG.error("Got error from $url ${e.message}")
            null
        }
    }

}