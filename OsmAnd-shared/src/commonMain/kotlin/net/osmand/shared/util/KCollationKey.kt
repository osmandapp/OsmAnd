package net.osmand.shared.util

/**
 * A name reduced to what a primary-strength collator compares, so that comparing two names is
 * comparing two char arrays.
 *
 * `java.text.Collator` at PRIMARY strength, which is what OsmAnd-java matches names with, ignores
 * case, accents and separators. `CollatorStringMatcher` hands it text that is already lowercased
 * and stripped of diacritics, so the only differences left for the collator to fold are:
 * - the separators, which it skips entirely: spaces, control characters, every kind of dash, the
 *   soft hyphen and the zero width characters. `a-b` and `ab` are equal to it, `no.` and `no` are
 *   not, because a full stop is not one of them;
 * - three letters that expand: `æ` is `ae`, `œ` is `oe`, `þ` is `th`.
 *
 * Both sets were taken from the collator itself rather than guessed, by asking it for the collation
 * key of every character of the basic plane; `KCollationKeyTest` checks them against it and against
 * a corpus of real names. Nothing else folds: of the 60167 characters that survive lowercasing and
 * diacritic stripping, no two are primary-equal unless both are separators, and no character
 * expands into three.
 *
 * So a name's key is its aligned text with the separators dropped and those three letters expanded,
 * and `collator.equals(a, b)` is `key(a) == key(b)`. The matcher builds one key per name instead of
 * calling a collator once per character position, which on iOS is also one bridge crossing per name
 * instead of hundreds.
 *
 * [starts] keeps the link back to the text, so that a stretch of the name can be compared without
 * cutting a substring out of it first.
 *
 * Deliberately locale independent, where java takes the collator of the device's locale. Locale
 * rules make the same map read differently on two phones - the java side already forces US rules
 * for romanian, czech and slovak to get away from that - and iOS matches names without a locale.
 */
class KCollationKey private constructor(
	/** Lowercased and aligned text: what a java matcher indexes into. */
	val text: String,
	private val key: CharArray,
	private val keyLength: Int,
	/** Key offset where `text[i]` begins, with `starts[text.length]` the whole key length. */
	private val starts: IntArray
) {

	val length: Int
		get() = text.length

	fun keyEquals(other: KCollationKey): Boolean {
		if (keyLength != other.keyLength) {
			return false
		}
		for (i in 0 until keyLength) {
			if (key[i] != other.key[i]) {
				return false
			}
		}
		return true
	}

	/** Whether the key of `text[from until to]` is the whole key of [other]. */
	fun regionKeyEquals(from: Int, to: Int, other: KCollationKey): Boolean {
		val start = starts[from]
		if (starts[to] - start != other.keyLength) {
			return false
		}
		for (i in 0 until other.keyLength) {
			if (key[start + i] != other.key[i]) {
				return false
			}
		}
		return true
	}

	/**
	 * Whether any stretch of this name has the key of [other], within the [maxSourceLength]
	 * characters that the java `ccontains` loop reaches from each starting position.
	 */
	fun containsKeyOf(other: KCollationKey, maxSourceLength: Int): Boolean {
		var end = 0
		val lastStart = text.length - other.text.length + 1
		for (pos in 0..lastStart) {
			val targetEnd = starts[pos] + other.keyLength
			if (end < pos) {
				end = pos
			}
			while (end < text.length && starts[end] < targetEnd) {
				end++
			}
			if (starts[end] != targetEnd) {
				// an expanded letter straddles the boundary, so no stretch ends here
				continue
			}
			if (end - pos > maxSourceLength) {
				continue
			}
			if (regionKeyEquals(pos, end, other)) {
				return true
			}
		}
		return false
	}

	override fun toString(): String = key.concatToString(0, keyLength)

	companion object {

		/** Lowercases and aligns [source] first, as `CollatorStringMatcher` does. */
		fun of(source: String): KCollationKey = ofAligned(lowercaseAndAlignChars(source))

		/** For text that went through [lowercaseAndAlignChars] already. */
		fun ofAligned(aligned: String): KCollationKey {
			val key = CharArray(aligned.length * 2)
			val starts = IntArray(aligned.length + 1)
			var at = 0
			for (i in aligned.indices) {
				starts[i] = at
				val ch = aligned[i]
				when {
					isSeparator(ch) -> {}
					ch == 'æ' -> {
						key[at++] = 'a'
						key[at++] = 'e'
					}
					ch == 'œ' -> {
						key[at++] = 'o'
						key[at++] = 'e'
					}
					ch == 'þ' -> {
						key[at++] = 't'
						key[at++] = 'h'
					}
					else -> key[at++] = ch
				}
			}
			starts[aligned.length] = at
			return KCollationKey(aligned, key, at, starts)
		}

		fun lowercaseAndAlignChars(fullText: String): String =
			KSearchAlgorithms.alignChars(fullText.lowercase())

		/** What `collator.equals(source, target)` answers at primary strength. */
		fun primaryEquals(source: String, target: String): Boolean = of(source).keyEquals(of(target))

		/**
		 * The characters a primary-strength collator skips: it gives every one of them the same
		 * collation key as the empty string.
		 *
		 * Taken from `java.text.Collator` over the whole basic plane, see the class comment. The
		 * combining mark ranges are in it for completeness; diacritic stripping has already removed
		 * them by the time a key is built.
		 */
		fun isSeparator(ch: Char): Boolean {
			val code = ch.code
			if (code > 0x0020 && code < 0x007F) {
				return code == 0x002D // hyphen-minus, the one separator among the printable ascii
			}
			return code <= 0x0020
					|| code <= 0x00A0 // 0x007F..0x00A0, the c1 controls and the no-break space
					|| code == 0x00AD
					|| code in 0x0300..0x0345
					|| code in 0x0360..0x0361
					|| code in 0x0483..0x0486
					|| code in 0x2000..0x2015
					|| code in 0x20D0..0x20E1
					|| code == 0x2212
					|| code == 0x3000
					|| code == 0xFEFF
		}
	}
}
