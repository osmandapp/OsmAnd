package net.osmand.shared.api

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
		userAgent: String = "OsmAnd"
	): NetworkResponse
}