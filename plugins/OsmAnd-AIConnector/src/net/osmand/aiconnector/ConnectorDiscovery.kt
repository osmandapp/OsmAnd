package net.osmand.aiconnector

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log

/**
 * Announces the server on the local network as a DNS-SD service, so the computer finds the phone by name
 * (osmand_mcp_bridge.py) and keeps working when the router gives the phone a new address.
 * The access key is never announced.
 */
class ConnectorDiscovery(ctx: Context) {

	companion object {
		private const val TAG = "OsmAndAiConnector"
		const val SERVICE_TYPE = "_osmand-mcp._tcp"
	}

	private val nsd = ctx.getSystemService(NsdManager::class.java)
	private var listener: NsdManager.RegistrationListener? = null

	fun register(port: Int) {
		unregister()
		val info = NsdServiceInfo().apply {
			serviceName = "OsmAnd on ${Build.MODEL}"
			serviceType = SERVICE_TYPE
			setPort(port)
			setAttribute("path", "/mcp")
			setAttribute("version", BuildConfig.VERSION_NAME)
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
