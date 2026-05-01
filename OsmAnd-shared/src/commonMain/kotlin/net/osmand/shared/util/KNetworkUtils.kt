package net.osmand.shared.util

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout

object KNetworkUtils {

    const val USER_AGENT = "Mozilla/5.0 (OsmAnd; Kotlin)"

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