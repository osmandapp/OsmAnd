package net.osmand.shared.search.core.spatial

import net.osmand.shared.search.core.spatial.SpatialPipelineSearch.SpatialPipelineContext
import net.osmand.shared.util.JavaFormat
import net.osmand.shared.util.collections.KTLongObjectMap
import kotlin.math.min

/**
 * The masks of the objects of the pipeline in the log of a search.
 *
 * A copy of `SpatialPipelineStats` in OsmAnd-java, which stays there for android and tools; this copy
 * is for iOS. Only `printTree`, which the log of the pipeline prints: `printTokenTree` and
 * `evaluateMaskIntersections` are called by nothing.
 */
object SpatialPipelineStats {

	fun printTree(prep: SpatialPipelineContext?) {
		if (prep == null) {
			println("=== MASK DISTRIBUTION: Prep or objectsById is null ===")
			return
		}

		val tokens = prep.tokens
		val totalObjects = prep.objectsById.size()

		// Group masks and count their frequencies using TLongObjectHashMap
		val maskFrequencies = KTLongObjectMap<Int>()

		for (o in prep.objectsById.values()) {
			var obj: SpatialPipelineObjectRes? = o
			while (obj != null) {
				val mask = obj.mainMask
				val count = maskFrequencies[mask]
				maskFrequencies.put(mask, if (count == null) 1 else count + 1)
				obj = obj.otherVariants
			}
		}

		// Convert the map entries to a list for sorting by popularity
		val maskList = ArrayList<LongArray>() // Entry format: [mask, count]
		maskFrequencies.forEach { mask, count ->
			maskList.add(longArrayOf(mask, count.toLong()))
		}

		// Sort in descending order by frequency (most frequent masks first)
		maskList.sortWith { a, b -> b[1].compareTo(a[1]) }

		// Print summary report
		println("==========================================================================================")
		println(
			"=== PREPARE: TOP POPULAR MASKS DISTRIBUTION (Total Objects: " + JavaFormat.grouped(totalObjects.toLong()) +
					" | Unique Masks: " + JavaFormat.grouped(maskList.size.toLong()) + ") ==="
		)
		println("==========================================================================================")
		println(
			"Rank".padEnd(6) + " | " + "Bits".padEnd(8) + " | " + "Count".padEnd(12) + " | " + "% Share".padEnd(10) +
					" | " + "Tokens (Bitmask representation)".padEnd(45)
		)
		println("------------------------------------------------------------------------------------------")

		val topN = min(50, maskList.size)
		for (i in 0 until topN) {
			val mask = maskList[i][0]
			val count = maskList[i][1]
			val bitCount = SpatialPipelineObjectRes.countCoveredTokens(mask)
			val share = (count * 100.0) / totalObjects

			val tokensRepresentation = SpatialPipelineObjectRes.formatMaskTokens(mask, tokens)

			println(
				"#" + (i + 1).toString().padEnd(5) + " | " + bitCount.toString().padEnd(8) + " | " +
						JavaFormat.grouped(count).padStart(12) + " | " + JavaFormat.fixed(share, 2).padStart(6) + "%    | " +
						tokensRepresentation.padEnd(45)
			)
		}
		println("==========================================================================================")
	}
}
