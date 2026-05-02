package net.osmand.shared.api

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CFNetwork.CFNetworkCopySystemProxySettings
import platform.CFNetwork.kCFNetworkProxiesHTTPProxy
import platform.CoreFoundation.CFDictionaryContainsKey
import platform.CoreFoundation.CFRelease

@OptIn(ExperimentalForeignApi::class)
internal actual class NetworkProxyState actual constructor() {

	actual val proxyHost: String?
		get() = null

	actual val proxyPort: Int
		get() = 0

	actual val ktorProxyData: NetworkProxyData?
		get() = null

	actual fun hasProxy(): Boolean {
		val httpProxyKey = kCFNetworkProxiesHTTPProxy ?: return false
		val settings = CFNetworkCopySystemProxySettings() ?: return false
		return try {
			CFDictionaryContainsKey(settings, httpProxyKey)
		} finally {
			CFRelease(settings)
		}
	}

	actual fun setProxy(host: String?, port: Int) {
		// Proxy is configured externally on iOS.
	}
}
