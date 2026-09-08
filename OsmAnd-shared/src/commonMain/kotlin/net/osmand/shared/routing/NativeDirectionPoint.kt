package net.osmand.shared.routing

import net.osmand.shared.util.KMapUtils
import kotlin.jvm.JvmField

/**
 * A [DirectionPoint] flattened for the C++ core, which does its own splicing.
 *
 * `java_wrap.cpp` calls `RoutingConfiguration.getNativeDirectionPoints()` and reads these three
 * fields off every element of the array it gets back, so their names and descriptors - `tags` is a
 * `[[Ljava/lang/String;` of key/value pairs - are part of that contract. Nothing writes them after
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
