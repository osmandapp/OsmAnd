package net.osmand.shared.api

import net.osmand.shared.util.KNetworkUtils

interface NetworkAPI {

	data class NetworkResponse(
		val response: String?,
		val error: String?
	)

	fun hasProxy(): Boolean

	fun setProxy(host: String?, port: Int)

	fun sendGetRequest(
		url: String,
		auth: String? = null,
		useGzip: Boolean = false,
		userAgent: String = KNetworkUtils.USER_AGENT
	): NetworkResponse

    fun resolveRedirectUrl(url: String): String? {
        return resolveRedirectUrl(url, KNetworkUtils.USER_AGENT)
    }

    fun resolveRedirectUrl(
        url: String,
        userAgent: String = KNetworkUtils.USER_AGENT
    ): String?
}