package net.osmand.shared.util

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import kotlinx.coroutines.runBlocking
import kotlin.jvm.JvmStatic

object KNetworkUtils {

    private const val CONNECT_TIMEOUT = 30_000L
    private const val READ_TIMEOUT = CONNECT_TIMEOUT * 2
    private const val CALL_TIMEOUT = CONNECT_TIMEOUT + READ_TIMEOUT

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

}