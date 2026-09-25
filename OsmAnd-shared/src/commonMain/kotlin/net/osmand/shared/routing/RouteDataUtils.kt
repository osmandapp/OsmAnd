package net.osmand.shared.routing

import net.osmand.shared.util.KAlgorithms
import kotlin.jvm.JvmStatic

/** Helpers shared by the routing data model. */
object RouteDataUtils {

	/** Speed reported for roads tagged `maxspeed=none`, in m/s. */
	const val NONE_MAX_SPEED = 40f

	/**
	 * Converts an osm `maxspeed` value to m/s, [def] if the value carries no number.
	 */
	@JvmStatic
	fun parseSpeed(v: String, def: Float): Float {
		if (v == "none") {
			return NONE_MAX_SPEED
		}
		val i = KAlgorithms.findFirstNumberEndIndex(v)
		if (i > 0) {
			// the arithmetic runs in double and narrows once, the way the java original did, so the
			// speeds that reach the router are bit for bit the ones it saw before
			var f = v.substring(0, i).toFloat()
			f = (f / 3.6).toFloat() // km/h -> m/s
			if (v.contains("mph")) {
				f = (f * 1.6).toFloat()
			}
			return f
		}
		return def
	}
}
