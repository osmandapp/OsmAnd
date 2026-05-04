package net.osmand.shared.api

internal actual class NetworkProxyState actual constructor() {

	private var proxyData: NetworkProxyData? = null

	actual val proxyHost: String?
		get() = proxyData?.host

	actual val proxyPort: Int
		get() = proxyData?.port ?: 0

	actual val ktorProxyData: NetworkProxyData?
		get() = proxyData

	actual fun hasProxy(): Boolean {
		return proxyData != null
	}

	actual fun setProxy(host: String?, port: Int) {
		proxyData = if (!host.isNullOrEmpty() && port > 0) {
			NetworkProxyData(host, port)
		} else {
			null
		}
	}
}
