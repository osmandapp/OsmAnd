package net.osmand.plus.wear

import android.os.Handler
import android.os.Looper
import android.os.SystemClock

import net.osmand.data.ValueHolder
import net.osmand.plus.OsmandApplication
import net.osmand.plus.routing.IRouteInformationListener
import net.osmand.plus.routing.IRoutingDataUpdateListener

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

	private fun publisher(app: OsmandApplication): WearProtocolPublisher =
		publisher ?: synchronized(this) {
			publisher ?: WearProtocolPublisher(app).also { publisher = it }
		}
}
