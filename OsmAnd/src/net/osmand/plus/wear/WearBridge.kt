package net.osmand.plus.wear

import net.osmand.plus.OsmandApplication

/**
 * Entry point for everything that wants to push state to the watch.
 *
 * The publisher has to be a single long-lived instance: it deduplicates against the last
 * payload it sent, and a fresh instance per call would defeat that and keep the watch radio
 * busy. Later stages hook route and recording listeners into [publish] from here.
 */
object WearBridge {

	@Volatile
	private var publisher: WearProtocolPublisher? = null

	fun publish(app: OsmandApplication) {
		publisher(app).publish()
	}

	private fun publisher(app: OsmandApplication): WearProtocolPublisher =
		publisher ?: synchronized(this) {
			publisher ?: WearProtocolPublisher(app).also { publisher = it }
		}
}
