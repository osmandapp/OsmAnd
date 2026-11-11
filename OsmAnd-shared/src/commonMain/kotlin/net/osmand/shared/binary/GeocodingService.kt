package net.osmand.shared.binary

import net.osmand.shared.binary.GeocodingUtilities.GeocodingResult
import net.osmand.shared.routing.RoutePlannerFrontEnd
import net.osmand.shared.routing.RoutingConfiguration
import net.osmand.shared.routing.RoutingConfiguration.RoutingMemoryLimits
import net.osmand.shared.routing.RoutingContext
import net.osmand.shared.util.KLock
import net.osmand.shared.util.KMapUtils
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.synchronized

/**
 * Resolves coordinates to the nearest address over the registry's installed maps.
 *
 * One service per registry owns the STREET_LOOKUP readers' execution context. Calls take turns
 * on their calling threads and reuse the road tiles between requests. The platform supplies its
 * default routing configuration and schedules calls off the main thread; it also formats and
 * delivers the result. File handles remain owned by the registry.
 */
class GeocodingService internal constructor(private val registry: ObfReaderRegistry) {

	private val lock = KLock()
	private var usedResources: List<BinaryMapReaderResource> = emptyList()
	private var readers: List<BinaryMapIndexReader> = emptyList()
	private var builder: RoutingConfiguration.Builder? = null
	private var context: RoutingContext? = null

	/**
	 * The closest address, or null when none is found, the call is cancelled, or a map cannot be
	 * read. [cancel] is only polled for cancellation; results are returned rather than published.
	 * A removed or replaced map invalidates the cached context on the next call. An in-flight call
	 * using that map discards its result, including one already held in the road tile cache.
	 */
	fun lookupAddress(
		lat: Double, lon: Double, defaultRoutingConfig: RoutingConfiguration.Builder,
		cancel: ResultMatcher<GeocodingResult>?
	): GeocodingResult? {
		if (cancel?.isCancelled() == true) return null
		return synchronized(lock) {
			try {
				if (cancel?.isCancelled() == true) return@synchronized null
				val resources = checkReaders(lat, lon)
				if (context == null || builder !== defaultRoutingConfig || resources !== usedResources) {
					resetContext()
					val available = resources.mapNotNull { resource ->
						resource.getReader(BinaryMapReaderResourceType.STREET_LOOKUP)?.let { resource to it }
					}
					usedResources = available.map { it.first }
					readers = available.map { it.second }
					builder = defaultRoutingConfig
					if (readers.isNotEmpty()) {
						val config = defaultRoutingConfig.build("geocoding", RoutingMemoryLimits(10, 10))
						context = RoutePlannerFrontEnd().buildRoutingContext(config, readers)
					}
				}
				val ctx = context ?: return@synchronized null
				if (cancel?.isCancelled() == true) return@synchronized null
				val utilities = GeocodingUtilities()
				val roads = utilities.reverseGeocodingSearch(ctx, lat, lon, false)
				if (cancel?.isCancelled() == true) return@synchronized null
				val addresses = utilities.findAddresses(readers, roads, cancel)
				if (cancel?.isCancelled() == true || usedResources.any { it.isClosed() }) {
					null
				} else {
					addresses.firstOrNull()
				}
			} catch (e: Exception) {
				resetContext()
				log.error("Exception during reverse geocoding", e)
				null
			}
		}
	}

	private fun checkReaders(lat: Double, lon: Double): List<BinaryMapReaderResource> {
		var resources = if (usedResources.any { it.isClosed() }) emptyList() else usedResources
		val x = KMapUtils.get31TileNumberX(lon)
		val y = KMapUtils.get31TileNumberY(lat)
		for (resource in registry.getFileReaders()) {
			if (!resource.isClosed()) {
				val header = resource.getShallowReader()
				if (header != null && header.containsRouteData(x, y, x, y) && header.containsAddressData()
					&& !resources.contains(resource)) {
					resources = resources + resource
				}
			}
		}
		return resources
	}

	private fun resetContext() {
		context = null
		builder = null
		readers = emptyList()
		usedResources = emptyList()
	}

	companion object {
		private val log = LoggerFactory.getLogger("GeocodingService")
	}
}
