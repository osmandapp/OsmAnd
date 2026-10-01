package net.osmand.shared.routing

import net.osmand.shared.util.KMapUtils
import kotlin.jvm.JvmField

/**
 * A [DirectionPoint] flattened for the C++ core, which does its own splicing.
 *
 * A copy of `RoutingConfiguration.NativeDirectionPoint`, which stays in OsmAnd-java where
 * `java_wrap.cpp` reads these three fields off it; this copy is for iOS. Nothing writes them after
 * construction.
 */
class NativeDirectionPoint(latitude: Double, longitude: Double, tags: Map<String, String>) {

	@JvmField
	val x31: Int = KMapUtils.get31TileNumberX(longitude)

	@JvmField
	val y31: Int = KMapUtils.get31TileNumberY(latitude)

	@JvmField
	val tags: Array<Array<String>> = tags.entries.map { arrayOf(it.key, it.value) }.toTypedArray()
}
