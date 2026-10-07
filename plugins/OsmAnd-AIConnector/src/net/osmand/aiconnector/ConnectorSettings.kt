package net.osmand.aiconnector

import android.content.Context
import android.content.SharedPreferences
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.UUID

/** Settings of the connector; kept on this phone only (the app has no backup). */
object ConnectorSettings {

	const val PORT = 8765

	/** OsmAnd apps the connector can drive, in the order they are offered. */
	val OSMAND_PACKAGES = linkedMapOf(
		"net.osmand.plus" to "OsmAnd+",
		"net.osmand" to "OsmAnd",
		"net.osmand.dev" to "OsmAnd~ (nightly)"
	)

	enum class Access { WIFI, USB }

	/** BACKGROUND: a foreground service with a notification; SCREEN: only while the app screen is open. */
	enum class RunMode { BACKGROUND, SCREEN }

	private fun prefs(ctx: Context): SharedPreferences =
		ctx.getSharedPreferences("connector", Context.MODE_PRIVATE)

	fun installedOsmAnd(ctx: Context): List<String> = OSMAND_PACKAGES.keys.filter {
		try {
			ctx.packageManager.getPackageInfo(it, 0)
			true
		} catch (e: Exception) {
			false
		}
	}

	fun osmandPackage(ctx: Context): String =
		prefs(ctx).getString("package", null)?.takeIf { it in installedOsmAnd(ctx) }
			?: installedOsmAnd(ctx).firstOrNull() ?: "net.osmand.plus"

	fun setOsmandPackage(ctx: Context, pack: String) = prefs(ctx).edit().putString("package", pack).apply()

	fun isEnabled(ctx: Context) = prefs(ctx).getBoolean("enabled", true)

	fun setEnabled(ctx: Context, enabled: Boolean) = prefs(ctx).edit().putBoolean("enabled", enabled).apply()

	fun access(ctx: Context): Access =
		if (prefs(ctx).getBoolean("usb", false)) Access.USB else Access.WIFI

	fun setAccess(ctx: Context, access: Access) = prefs(ctx).edit().putBoolean("usb", access == Access.USB).apply()

	fun runMode(ctx: Context): RunMode =
		if (prefs(ctx).getBoolean("screen_mode", false)) RunMode.SCREEN else RunMode.BACKGROUND

	fun setRunMode(ctx: Context, mode: RunMode) =
		prefs(ctx).edit().putBoolean("screen_mode", mode == RunMode.SCREEN).apply()

	/** Over Wi-Fi: the assistant starts the bridge that finds the phone by name, or uses the address directly. */
	fun useBridge(ctx: Context) = prefs(ctx).getBoolean("bridge", true)

	fun setUseBridge(ctx: Context, bridge: Boolean) = prefs(ctx).edit().putBoolean("bridge", bridge).apply()

	fun usesBridge(ctx: Context) = access(ctx) == Access.WIFI && useBridge(ctx)

	fun token(ctx: Context): String =
		prefs(ctx).getString("token", null) ?: newToken(ctx)

	fun newToken(ctx: Context): String =
		UUID.randomUUID().toString().also { prefs(ctx).edit().putString("token", it).apply() }

	/** The address the server listens on: all interfaces for Wi-Fi, only this device for USB (adb forward). */
	fun bindHost(ctx: Context) = if (access(ctx) == Access.WIFI) "0.0.0.0" else "127.0.0.1"

	fun wifiAddress(): String? = NetworkInterface.getNetworkInterfaces().toList()
		.filter { it.isUp && !it.isLoopback && (it.name.startsWith("wlan") || it.name.startsWith("eth")) }
		.flatMap { it.inetAddresses.toList() }
		.firstOrNull { it is Inet4Address && it.isSiteLocalAddress }
		?.hostAddress

	/** Where the computer keeps the stdio bridge that finds the phone on Wi-Fi by its DNS-SD name. */
	const val BRIDGE_PATH = "~/.osmand/" + McpHttpServer.BRIDGE_FILE

	/** Run once on the computer: downloads the bridge from the phone. */
	fun bridgeDownload(): String {
		val host = wifiAddress() ?: "PHONE_IP"
		return "mkdir -p ~/.osmand && curl -o $BRIDGE_PATH http://$host:$PORT/${McpHttpServer.BRIDGE_FILE}"
	}

	/** The URL the computer uses: the phone's Wi-Fi address, or localhost through adb forward. */
	fun url(ctx: Context): String {
		val host = if (access(ctx) == Access.WIFI) wifiAddress() ?: "PHONE_IP" else "127.0.0.1"
		return "http://$host:$PORT/mcp"
	}
}
