package net.osmand.shared.util

/**
 * Copy of `net.osmand.Collator` in OsmAnd-java: comparing names the way a reader of that language
 * would, ignoring case and accents.
 *
 * Used for ordering names and for comparing aligned name ranges during search.
 */
interface KCollator {

	fun compare(source: String, target: String): Int

	fun equals(source: String, target: String): Boolean

	/** Compare the first [count] fixed-length UTF-16 spans with [part]. */
	fun equalsAtAny(base: String, part: String, length: Int, offsets: IntArray, count: Int): Boolean =
		(0 until count).any { i ->
			val start = offsets[i]
			equals(base.substring(start, start + length), part)
		}

	/** Search the same UTF-16 ranges as the Java CHECK_CONTAINS matcher. */
	fun contains(base: String, part: String): Boolean {
		if (base.length <= part.length) {
			return equals(base, part)
		}
		for (pos in 0..base.length - part.length + 1) {
			val end = minOf(pos + part.length * 2, base.length)
			for (limit in end downTo pos) {
				if (equals(base.substring(pos, limit), part)) {
					return true
				}
			}
		}
		return false
	}
}

/** Ignores case and accents, as `OsmAndCollator.primaryCollator()` does. */
expect fun primaryCollator(): KCollator
