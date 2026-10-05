package net.osmand.shared.routing

import net.osmand.shared.util.collections.KTIntArrayList
import kotlin.jvm.JvmField

/**
 * A point that forces the router to treat one spot on a road differently - the "avoid road" markers
 * the app drops, and the direction points OsmAndMapCreator loads from GeoJSON.
 *
 * The point is not part of any map. While a tile is being loaded the router looks for the road it
 * belongs to, splices an extra point into that road's geometry and gives it [types], so the routing
 * rules see the tags below as if the map had carried them. [connected], [connectedx], [connectedy]
 * and [distance] are that bookkeeping: a road closer to the point takes it over from the one that
 * held it before, and the point it left behind is retyped as deleted.
 *
 * That state belongs to one calculation, which is why [RoutingConfiguration.Builder] copies every
 * point it was given rather than handing the same objects to two routes.
 */
class DirectionPoint(private val lat: Double, private val lon: Double) {

	private val tags = LinkedHashMap<String, String>()

	/** How far the point sits from [connected]; only a closer road may take it over. */
	@JvmField
	var distance: Double = Double.MAX_VALUE

	/** The road the point is currently spliced into. */
	@JvmField
	var connected: RouteDataObject? = null

	/** Route types the spliced point carries, the tags below resolved against the road's region. */
	@JvmField
	val types = KTIntArrayList()

	@JvmField
	var connectedx: Int = 0

	@JvmField
	var connectedy: Int = 0

	/** The same place and tags, with the routing bookkeeping back at its starting state. */
	constructor(other: DirectionPoint) : this(other.lat, other.lon) {
		tags.putAll(other.tags)
	}

	fun getLatitude(): Double = lat

	fun getLongitude(): Double = lon

	fun getTags(): MutableMap<String, String> = tags

	fun getTag(key: String): String? = tags[key]

	/**
	 * Keys are lower cased, as they were when these points were OSM nodes and went through
	 * `Entity.putTag`: the router looks the tags up in the routing rules, which are lower case.
	 */
	fun putTag(key: String, value: String): String? = tags.put(key.lowercase(), value)

	/** The heading the point applies to, or NaN when it applies whichever way the road runs. */
	fun getAngle(): Double {
		val angle = getTag(ANGLE_TAG) ?: return Double.NaN
		try {
			return angle.toDouble()
		} catch (e: NumberFormatException) {
			throw RuntimeException(e)
		}
	}

	companion object {
		const val TAG = "osmand_dp"
		const val DELETE_TYPE = "osmand_delete_point"
		const val CREATE_TYPE = "osmand_add_point"
		const val ANGLE_TAG = "apply_direction_angle"
		const val MAX_ANGLE_DIFF = 45.0 // in degrees
	}
}
