package net.osmand.shared.extensions

import kotlin.math.*

/**
 * Degrees to radians the way `MapUtils.toRadians` in OsmAnd-java does it - divide first, then
 * multiply - and not the way `Math.toRadians` does. The last bit follows the order, and it reaches
 * every distance the routing and the track analysis compute, so the two sides have to agree.
 */
fun Double.toRadians(): Double = this / 180.0 * PI

fun Double.toDegrees(): Double = this * (180.0 / PI)


