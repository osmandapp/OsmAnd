package net.osmand.aiconnector

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import net.osmand.aidlapi.IOsmAndAidlInterface
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Binds to OsmAnd's AIDL API v2 and reconnects on demand. */
class OsmAndBridge(private val ctx: Context, var preferredPackage: String) {

	companion object {
		const val AIDL_ACTION = "net.osmand.aidl.OsmandAidlServiceV2"
		val KNOWN_PACKAGES = listOf("net.osmand.dev", "net.osmand.plus", "net.osmand")
	}

	@Volatile
	private var api: IOsmAndAidlInterface? = null

	@Volatile
	var boundPackage: String? = null
		private set

	private var latch = CountDownLatch(1)
	private var bound = false

	private val connection = object : ServiceConnection {
		override fun onServiceConnected(name: ComponentName, service: IBinder) {
			api = IOsmAndAidlInterface.Stub.asInterface(service)
			latch.countDown()
		}

		override fun onServiceDisconnected(name: ComponentName) {
			api = null
		}

		override fun onBindingDied(name: ComponentName) {
			disconnect()
		}
	}

	fun installedPackages(): List<String> = KNOWN_PACKAGES.filter {
		try {
			ctx.packageManager.getPackageInfo(it, 0); true
		} catch (e: PackageManager.NameNotFoundException) {
			false
		}
	}

	@Synchronized
	fun get(): IOsmAndAidlInterface {
		api?.let { return it }
		if (!bound) {
			val candidates = listOf(preferredPackage)
			for (pkg in candidates) {
				latch = CountDownLatch(1)
				val intent = Intent(AIDL_ACTION).setPackage(pkg)
				if (ctx.bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
					bound = true
					boundPackage = pkg
					break
				}
				ctx.unbindService(connection)
			}
			if (!bound) throw IllegalStateException("No OsmAnd found to bind (tried $candidates)")
		}
		latch.await(10, TimeUnit.SECONDS)
		return api ?: throw IllegalStateException("OsmAnd ($boundPackage) did not connect within 10 s")
	}

	@Synchronized
	fun disconnect() {
		if (bound) {
			try {
				ctx.unbindService(connection)
			} catch (_: IllegalArgumentException) {
			}
		}
		bound = false
		api = null
		boundPackage = null
	}
}
