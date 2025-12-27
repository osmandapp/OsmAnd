package net.osmand.shared.api

import net.osmand.shared.api.NetworkAPI.NetworkResponse

class NetworkAPIImpl : NetworkAPI {

	override fun hasProxy(): Boolean {
		TODO("Not yet implemented")
	}

	override fun setProxy(host: String?, port: Int) {
		TODO("Not yet implemented")
	}

	override fun sendGetRequest(
		url: String,
		auth: String?,
		useGzip: Boolean,
		userAgent: String
	): NetworkResponse {
		TODO("Not yet implemented")
	}
}