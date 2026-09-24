package net.osmand.shared.util

import net.osmand.shared.api.KStringMatcherMode
import kotlin.jvm.JvmStatic

/**
 * Copy of `net.osmand.CollatorStringMatcher` in OsmAnd-java: does a name start with, contain or
 * equal what was typed, ignoring case, accents and separators.
 *
 * Same modes and the same answers, reached differently. Java asks a collator whether two stretches
 * of text are equal, once per starting position and once per length, which for a name of 30
 * characters and a query of 5 is a few hundred collator calls and as many substrings - the java
 * source calls it "not effective code, it runs on each comparison". Here each string is reduced
 * once to a [KCollationKey], and the stretches are compared inside it without cutting substrings
 * out. On iOS that is also the difference between one crossing into Foundation per name and
 * several hundred.
 *
 * One deliberate difference. Java lowercases the query in the constructor but not in the static
 * [cmatches], and `java.text.Collator` only folds case for the scripts its locale rules name - for
 * everything else it compares by code point, so `cmatches` treats `Ш` and `ш` as different letters
 * while `matches` does not. Here the query is lowercased on both paths, which is what the instance
 * path and the iOS matcher already do; see also the notes on `KSearchAlgorithms`.
 *
 * `CollatorCompatTest` holds the two side by side over the names of the test maps.
 */
class KCollatorStringMatcher(part: String, mode: KStringMatcherMode) : KStringMatcher {

	/**
	 * A name as [cmatches] reduces it when it is the full name: its key, and the key of the name
	 * with its hyphens dropped when it has any, which is tried first. [key] is also what [cmatches]
	 * builds for the same text when it is the part.
	 */
	internal class PreparedName(fullName: String) {
		val key: KCollationKey = KCollationKey.of(fullName)
		val withoutHyphens: KCollationKey? =
			if (fullName.indexOf('-') != -1) KCollationKey.of(fullName.replace("-", "")) else null
	}

	val part: KCollationKey
	val mode: KStringMatcherMode

	init {
		var aligned = KCollationKey.lowercaseAndAlignChars(part)
		var resolved = mode
		if (aligned.isNotEmpty() && aligned[aligned.length - 1] == INCOMPLETE_DOT && !onlyDots(aligned)) {
			aligned = aligned.substring(0, aligned.length - 1)
			if (resolved == KStringMatcherMode.CHECK_EQUALS_FROM_SPACE) {
				resolved = KStringMatcherMode.CHECK_STARTS_FROM_SPACE
			} else if (resolved == KStringMatcherMode.CHECK_EQUALS) {
				resolved = KStringMatcherMode.CHECK_ONLY_STARTS_WITH
			}
		}
		this.part = KCollationKey.ofAligned(aligned)
		this.mode = resolved
	}

	override fun matches(name: String): Boolean = cmatchesKey(name, part, alignFull = true, mode = mode)

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
			cmatchesKey(fullName, KCollationKey.of(part), alignFull = true, mode = mode)

		/** For a part that was aligned already, such as one the caller reuses across names. */
		@JvmStatic
		fun cmatchesNoAlign(fullName: String, part: String, mode: KStringMatcherMode): Boolean =
			cmatchesKey(fullName, KCollationKey.ofAligned(part), alignFull = false, mode = mode)

		fun cmatchesKey(
			fullName: String,
			part: KCollationKey,
			alignFull: Boolean,
			mode: KStringMatcherMode
		): Boolean {
			if (fullName.indexOf('-') != -1) {
				// the hyphen is a separator to the collator, but dropping it also closes the gaps it
				// leaves in the word starts and the stretch lengths below, which the java copy tests
				if (cmatchesKey(fullName.replace("-", ""), part, alignFull = true, mode = mode)) {
					return true
				}
			}
			val name = if (alignFull) KCollationKey.of(fullName) else KCollationKey.ofAligned(fullName)
			return matchesKeys(name, part, mode)
		}

		/**
		 * What [cmatches] answers for the name [name] was prepared from, with the name's keys built
		 * once rather than on every call. For a caller that holds one name against many parts, as
		 * the walk over the string table of a name index does.
		 */
		internal fun cmatchesPrepared(name: PreparedName, part: KCollationKey, mode: KStringMatcherMode): Boolean {
			val withoutHyphens = name.withoutHyphens
			if (withoutHyphens != null && matchesKeys(withoutHyphens, part, mode)) {
				return true
			}
			return matchesKeys(name.key, part, mode)
		}

		private fun matchesKeys(name: KCollationKey, part: KCollationKey, mode: KStringMatcherMode): Boolean =
			when (mode) {
				KStringMatcherMode.CHECK_CONTAINS -> ccontains(name, part)
				KStringMatcherMode.CHECK_EQUALS_FROM_SPACE -> cstartsWith(name, part, true, true, true)
				KStringMatcherMode.CHECK_STARTS_FROM_SPACE -> cstartsWith(name, part, true, true, false)
				KStringMatcherMode.CHECK_STARTS_FROM_SPACE_NOT_BEGINNING -> cstartsWith(name, part, false, true, false)
				KStringMatcherMode.CHECK_ONLY_STARTS_WITH -> cstartsWith(name, part, true, false, false)
				KStringMatcherMode.CHECK_EQUALS -> cstartsWith(name, part, false, false, true)
				KStringMatcherMode.MULTISEARCH -> cstartsWith(part, name, true, true, true)
			}

		/** Whether [part] is contained in [base]. */
		fun ccontains(base: KCollationKey, part: KCollationKey): Boolean {
			if (base.length <= part.length) {
				return base.keyEquals(part)
			}
			// java reaches part.length() * 2 characters from each starting position, no further
			return base.containsKeyOf(part, part.length * 2)
		}

		/**
		 * Whether [searchIn] starts with [theStart], or, when [checkSpaces], whether any of its
		 * words does.
		 */
		fun cstartsWith(
			searchIn: KCollationKey,
			theStart: KCollationKey,
			checkBeginning: Boolean,
			checkSpaces: Boolean,
			equals: Boolean
		): Boolean {
			val searchInLength = searchIn.length
			val startLength = theStart.length
			if (startLength == 0) {
				return true
			}
			// this is not correct without (lowercaseAndAlignChars) because of Auhofstrasse != Auhofstraße
			if (startLength > searchInLength) {
				return false
			}
			// simulate starts with for collator
			if (checkBeginning && searchIn.regionKeyEquals(0, startLength, theStart)) {
				if (!equals) {
					return true
				}
				if (startLength == searchInLength || isSpace(searchIn.text[startLength])) {
					return true
				}
			}
			if (checkSpaces) {
				for (i in 1..searchInLength - startLength) {
					if (!isWordStart(searchIn.text, i, theStart.text)) {
						continue
					}
					if (!searchIn.regionKeyEquals(i, i + startLength, theStart)) {
						continue
					}
					if (!equals) {
						return true
					}
					if (i + startLength == searchInLength || isSpace(searchIn.text[i + startLength])) {
						return true
					}
				}
			}
			if (!checkBeginning && !checkSpaces && equals) {
				return searchIn.keyEquals(theStart)
			}
			return false
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
