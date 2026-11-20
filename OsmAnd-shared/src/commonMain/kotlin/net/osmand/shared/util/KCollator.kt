package net.osmand.shared.util

/**
 * Copy of `net.osmand.Collator` in OsmAnd-java: comparing names the way a reader of that language
 * would, ignoring case and accents.
 *
 * This is for putting names in order. Deciding whether a name matches what was typed goes through
 * [KCollatorStringMatcher], whose keys sample the platform space weight once and then compare
 * without further platform calls.
 */
interface KCollator {

	fun compare(source: String, target: String): Int

	fun equals(source: String, target: String): Boolean
}

/** Ignores case and accents, as `OsmAndCollator.primaryCollator()` does. */
expect fun primaryCollator(): KCollator
