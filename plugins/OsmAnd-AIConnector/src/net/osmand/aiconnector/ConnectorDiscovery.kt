package net.osmand.aiconnector

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import java.security.MessageDigest

/**
 * Announces the server on the local network as a DNS-SD service, so the computer finds the phone by name
 * (osmand_mcp_bridge.py) and keeps working when the router gives the phone a new address.
 * The access key is never announced, only the first 8 hex digits of its SHA-256: the bridge sends the key only
 * to a service with them, not to any device on the Wi-Fi that announces the same service type.
 */
class ConnectorDiscovery(ctx: Context) {

	companion object {
		private const val TAG = "OsmAndAiConnector"
		const val SERVICE_TYPE = "_osmand-mcp._tcp"

		// osmand_mcp_bridge.py computes the same
		fun keyId(token: String): String = MessageDigest.getInstance("SHA-256")
			.digest(token.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }.take(8)
	}

	private val nsd = ctx.getSystemService(NsdManager::class.java)
	private var listener: NsdManager.RegistrationListener? = null

	fun register(port: Int, token: String) {
		unregister()
		val info = NsdServiceInfo().apply {
			serviceName = "OsmAnd on ${Build.MODEL}${BuildConfig.SERVICE_SUFFIX}"
			serviceType = SERVICE_TYPE
			setPort(port)
			setAttribute("path", "/mcp")
			setAttribute("version", BuildConfig.VERSION_NAME)
			setAttribute("key", keyId(token))
		}
		val l = object : NsdManager.RegistrationListener {
			override fun onServiceRegistered(info: NsdServiceInfo) {
				Log.i(TAG, "Announced as '${info.serviceName}' ($SERVICE_TYPE)")
			}

			override fun onRegistrationFailed(info: NsdServiceInfo, errorCode: Int) {
				Log.w(TAG, "Cannot announce the server: error $errorCode")
			}

			override fun onServiceUnregistered(info: NsdServiceInfo) {
			}

			override fun onUnregistrationFailed(info: NsdServiceInfo, errorCode: Int) {
			}
		}
		nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, l)
		listener = l
	}

	fun unregister() {
		val l = listener ?: return
		listener = null
		try {
			nsd.unregisterService(l)
		} catch (e: IllegalArgumentException) {
			// registration failed, nothing to remove
		}
	}
}
