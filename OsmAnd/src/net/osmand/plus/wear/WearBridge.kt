package net.osmand.plus.wear

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import net.osmand.PlatformUtil
import net.osmand.data.ValueHolder
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.routing.IRouteInformationListener
import net.osmand.plus.routing.IRoutingDataUpdateListener
import net.osmand.wear.api.WearProtocol

/**
 * Entry point for everything that pushes state to the watch, and the owner of the routing
 * subscriptions that drive it.
 *
 * The publisher has to be a single long-lived instance: it deduplicates against the last
 * payload it sent, and a fresh instance per call would defeat that and keep the watch radio busy.
 */
object WearBridge {

	private const val MIN_PUBLISH_INTERVAL_MS = 1000L

	private val handler = Handler(Looper.getMainLooper())

	@Volatile
	private var publisher: WearProtocolPublisher? = null

	// RoutingHelper keeps listeners as weak references, so these fields are what stops them
	// from being collected — drop them and updates silently stop arriving.
	private var routeDataListener: IRoutingDataUpdateListener? = null
	private var routeInfoListener: IRouteInformationListener? = null

	private var lastPublishAt = 0L
	private var publishScheduled = false

	/**
	 * Subscribes to navigation updates. Called when the watch first makes contact, so a phone
	 * that never talks to a watch pays nothing.
	 */
	@Synchronized
	fun start(app: OsmandApplication) {
		if (routeDataListener != null) {
			return
		}
		val dataListener = IRoutingDataUpdateListener { requestPublish(app) }
		val infoListener = object : IRouteInformationListener {
			override fun newRouteIsCalculated(newRoute: Boolean, showToast: ValueHolder<Boolean>) {
				requestPublish(app)
			}

			override fun routeWasCancelled() = requestPublish(app)

			override fun routeWasFinished() = requestPublish(app)
		}
		routeDataListener = dataListener
		routeInfoListener = infoListener

		val routingHelper = app.routingHelper
		routingHelper.addRouteDataListener(dataListener)
		routingHelper.addListener(infoListener)
	}

	fun publish(app: OsmandApplication) {
		publisher(app).publish()
	}

	/**
	 * Re-announces this phone to the watch and pushes a fresh snapshot.
	 *
	 * The capability is normally declared by a resource, which Play services reads once when the
	 * app is installed and then serves from its own cache — so when that cache goes stale there
	 * is nothing the app can do about it by restarting. Declaring the same capability at runtime
	 * is the one lever that exists, and it is deliberately not wired into startup: it would bind
	 * to Play services on every launch, including on phones that will never see a watch.
	 *
	 * [onResult] receives a short line for the caller to show.
	 */
	fun refreshConnection(app: OsmandApplication, onResult: (String) -> Unit) {
		val capabilityClient = Wearable.getCapabilityClient(app)
		capabilityClient.addLocalCapability(WearProtocol.CAPABILITY_PHONE_APP)
			.addOnCompleteListener {
				start(app)
				publish(app)
				capabilityClient
					.getCapability(WearProtocol.CAPABILITY_WEAR_APP, CapabilityClient.FILTER_REACHABLE)
					.addOnSuccessListener { info ->
						val nodes = info.nodes
						onResult(
							if (nodes.isEmpty()) app.getString(R.string.wear_refresh_no_watch)
							else app.getString(
								R.string.wear_refresh_found,
								nodes.joinToString { node -> node.displayName }
							)
						)
					}
					.addOnFailureListener { error ->
						LOG.error("Watch lookup failed", error)
						onResult(app.getString(R.string.wear_refresh_failed))
					}
			}
	}

	/**
	 * Leading-edge throttle with a trailing publish: the first update goes out at once so a
	 * manoeuvre is never late, and the last one is never dropped, but a burst of GPS fixes
	 * cannot turn into a burst of Bluetooth traffic.
	 */
	private fun requestPublish(app: OsmandApplication) {
		val now = SystemClock.elapsedRealtime()
		val sinceLast = now - lastPublishAt
		if (sinceLast >= MIN_PUBLISH_INTERVAL_MS) {
			lastPublishAt = now
			publish(app)
		} else if (!publishScheduled) {
			publishScheduled = true
			handler.postDelayed({
				publishScheduled = false
				lastPublishAt = SystemClock.elapsedRealtime()
				publish(app)
			}, MIN_PUBLISH_INTERVAL_MS - sinceLast)
		}
	}

	private val LOG = PlatformUtil.getLog(WearBridge::class.java)

	private fun publisher(app: OsmandApplication): WearProtocolPublisher =
		publisher ?: synchronized(this) {
			publisher ?: WearProtocolPublisher(app).also { publisher = it }
		}
}
