package net.osmand.shared.util

import net.osmand.shared.api.KStringMatcherMode
import kotlin.jvm.JvmStatic

/** Matches aligned text using primary-strength comparisons from the current platform. */
class KCollatorStringMatcher(part: String, mode: KStringMatcherMode) : KStringMatcher {

	class PreparedName(fullName: String) {
		internal val text = KSearchAlgorithms.lowercaseAndAlignChars(fullName)
		internal val withoutHyphens = if ('-' in fullName) KSearchAlgorithms.lowercaseAndAlignChars(fullName.replace("-", "")) else null
	}

	internal val collator = primaryCollator()
	val part: String
	val mode: KStringMatcherMode

	init {
		var aligned = KSearchAlgorithms.lowercaseAndAlignChars(part)
		var resolved = mode
		if (aligned.isNotEmpty() && aligned[aligned.length - 1] == INCOMPLETE_DOT && !onlyDots(aligned)) {
			aligned = aligned.substring(0, aligned.length - 1)
			if (resolved == KStringMatcherMode.CHECK_EQUALS_FROM_SPACE) {
				resolved = KStringMatcherMode.CHECK_STARTS_FROM_SPACE
			} else if (resolved == KStringMatcherMode.CHECK_EQUALS) {
				resolved = KStringMatcherMode.CHECK_ONLY_STARTS_WITH
			}
		}
		this.part = aligned
		this.mode = resolved
	}

	override fun matches(name: String): Boolean = cmatchesAligned(name, part, true, mode, collator)

	fun onlyDots(part: String): Boolean {
		for (c in part) {
			if (c != INCOMPLETE_DOT) {
				return false
			}
		}
		return true
	}

	companion object {

		const val INCOMPLETE_DOT = '.'

		@JvmStatic
		fun cmatches(fullName: String, part: String, mode: KStringMatcherMode): Boolean =
			cmatches(fullName, part, mode, primaryCollator())

		internal fun cmatches(fullName: String, part: String, mode: KStringMatcherMode, collator: KCollator): Boolean =
			cmatchesAligned(fullName, KSearchAlgorithms.lowercaseAndAlignChars(part), true, mode, collator)

		@JvmStatic
		fun cmatchesNoAlign(fullName: String, part: String, mode: KStringMatcherMode): Boolean =
			cmatchesAligned(fullName, part, false, mode, primaryCollator())

		internal fun cmatchesAligned(fullName: String, part: String, alignFull: Boolean, mode: KStringMatcherMode, collator: KCollator): Boolean {
			if ('-' in fullName && matchesText(KSearchAlgorithms.lowercaseAndAlignChars(fullName.replace("-", "")), part, mode, collator)) {
				return true
			}
			val text = if (alignFull) KSearchAlgorithms.lowercaseAndAlignChars(fullName) else fullName
			return matchesText(text, part, mode, collator)
		}

		internal fun cmatchesPrepared(name: PreparedName, part: String, mode: KStringMatcherMode, collator: KCollator = primaryCollator()): Boolean {
			val withoutHyphens = name.withoutHyphens
			return (withoutHyphens != null && matchesText(withoutHyphens, part, mode, collator)) ||
					matchesText(name.text, part, mode, collator)
		}

		private fun matchesText(searchIn: String, part: String, mode: KStringMatcherMode, collator: KCollator): Boolean =
			when (mode) {
				KStringMatcherMode.CHECK_CONTAINS -> collator.contains(searchIn, part)
				KStringMatcherMode.CHECK_EQUALS_FROM_SPACE -> cstartsWith(collator, searchIn, part, true, true, true)
				KStringMatcherMode.CHECK_STARTS_FROM_SPACE -> cstartsWith(collator, searchIn, part, true, true, false)
				KStringMatcherMode.CHECK_STARTS_FROM_SPACE_NOT_BEGINNING -> cstartsWith(collator, searchIn, part, false, true, false)
				KStringMatcherMode.CHECK_ONLY_STARTS_WITH -> cstartsWith(collator, searchIn, part, true, false, false)
				KStringMatcherMode.CHECK_EQUALS -> cstartsWith(collator, searchIn, part, false, false, true)
				KStringMatcherMode.MULTISEARCH -> cstartsWith(collator, part, searchIn, true, true, true)
			}

		private val beginning = intArrayOf(0)

		private fun cstartsWith(
			collator: KCollator,
			searchIn: String,
			theStart: String,
			checkBeginning: Boolean,
			checkSpaces: Boolean,
			equals: Boolean
		): Boolean {
			val searchInLength = searchIn.length
			val startLength = theStart.length
			if (startLength == 0) {
				return true
			}
			if (startLength > searchInLength) {
				return false
			}
			val wholeName = !checkBeginning && !checkSpaces && equals
			val length = if (wholeName) searchInLength else startLength
			val last = if (checkSpaces) searchInLength - length else 0
			val offsets = if (last > 0) IntArray(last + 1) else beginning
			var count = 0
			for (i in 0..last) {
				if (i == 0 && !checkBeginning && !wholeName) continue
				if (i > 0 && !isWordStart(searchIn, i, theStart)) continue
				val end = i + length
				if (equals && end < searchInLength && !isSpace(searchIn[end])) continue
				if (last > 0) offsets[count] = i
				count++
			}
			return count > 0 && collator.equalsAtAny(searchIn, theStart, length, offsets, count)
		}

		private fun isWordStart(searchIn: String, index: Int, part: String): Boolean {
			if (!isSpace(searchIn[index - 1])) {
				return false
			}
			val current = searchIn[index]
			if (!isSpace(current)) {
				return true
			}
			return current == '-' && part.length > 1 && part[0] == '-' && part[1].isDigit()
		}

		private fun isSpace(c: Char): Boolean = !c.isLetter() && !c.isDigit()
	}
}
