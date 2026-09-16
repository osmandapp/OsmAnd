package net.osmand.shared.routing

import net.osmand.shared.binary.BinaryMapIndexReader
import net.osmand.shared.binary.HHRouteRegion
import net.osmand.shared.util.collections.KTIntObjectMap
import kotlin.jvm.JvmField

/**
 * One HH section a route is calculated over: the file it is in, which of the section's parameter
 * sets is used, and the vertices read from it by their file ids. [id] is the vertices' `mapId`.
 *
 * A copy of `HHRouteDataStructure.HHRouteRegionPointsCtx` in OsmAnd-java, which stays there for
 * android and tools; this copy is for iOS. Java's other constructor takes the tools' SQLite
 * `HHRoutingDB` instead of a file and is not copied.
 */
class HHRouteRegionPointsCtx(
	@JvmField val id: Short,
	@JvmField val fileRegion: HHRouteRegion,
	@JvmField val file: BinaryMapIndexReader,
	routingProfile: Int
) {

	@JvmField
	var routingProfile: Int = 0

	@JvmField
	val pntsByFileId: KTIntObjectMap<NetworkDBPoint> = KTIntObjectMap()

	init {
		if (routingProfile >= 0) {
			this.routingProfile = routingProfile
		}
	}

	fun getRoutingProfile(): Int = routingProfile

	fun getFileRegion(): HHRouteRegion = fileRegion

	fun getPoint(pntFileId: Int): NetworkDBPoint? = pntsByFileId[pntFileId]
}
