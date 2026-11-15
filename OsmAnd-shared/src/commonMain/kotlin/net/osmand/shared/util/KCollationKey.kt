package net.osmand.shared.util

/**
 * A name reduced to a key so matching compares arrays instead of calling a platform collator
 * at each character position. Case and diacritics are aligned before the key is built; æ, œ and þ
 * expand to ae, oe and th. [starts] maps source positions to key offsets without substrings.
 *
 * Spaces have a primary weight in every matcher mode, as on Android. Ordinary space and its
 * 16 Unicode equivalents are written as U+0020 in the same pass that builds the key. Repeated,
 * leading and trailing spaces are retained. The original text stays available for word boundaries
 * and source-length limits; no normalized copy or second key is needed.
 *
 * Other ignored characters and letter expansions retain the desktop-JVM-derived rules. This is
 * not a complete implementation of Android ICU collation: locale-dependent rules and differences
 * for other separators are outside this key's contract. Tests distinguish that JVM reference from
 * space/matcher cases observed on Android.
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
					// Common names stay on the existing printable-ASCII path; spaces now have a weight.
					ch in ' '..'~' -> if (ch != '-') key[at++] = ch
					isUnicodeSpace(ch) -> key[at++] = ' '
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

		// java.text.Collator PRIMARY on Android API 37, checked in en-US/de-DE/ru-RU.
		private fun isUnicodeSpace(ch: Char): Boolean =
			ch == '\u00A0' || ch == '\u1680' || ch in '\u2000'..'\u200A' ||
					ch == '\u202F' || ch == '\u205F' || ch == '\u3000'

		fun lowercaseAndAlignChars(fullText: String): String =
			KSearchAlgorithms.alignChars(lowercase(fullText))

		/**
		 * What [String.lowercase] answers, char by char. Only `İ`, which lowercases into two chars,
		 * `Σ`, which lowercases by its neighbours, and surrogate pairs, which lowercase as one code
		 * point, need the whole string: text holding any of them goes to [String.lowercase].
		 */
		internal fun lowercase(text: String): String {
			var chars: CharArray? = null
			for (i in text.indices) {
				val c = text[i]
				val lower = when {
					c < '\u0080' -> if (c in 'A'..'Z') c + ('a' - 'A') else c
					c == 'İ' || c == 'Σ' || c.isSurrogate() -> return text.lowercase()
					else -> c.lowercaseChar()
				}
				if (lower != c) {
					if (chars == null) {
						chars = text.toCharArray()
					}
					chars[i] = lower
				}
			}
			return chars?.concatToString() ?: text
		}

		/** Primary key equality, including significant Android-equivalent spaces. */
		fun primaryEquals(source: String, target: String): Boolean = of(source).keyEquals(of(target))

		/** Characters omitted from the key: desktop JVM ignorables except significant spaces. */
		fun isSeparator(ch: Char): Boolean {
			val code = ch.code
			if (code >= 0x0020 && code < 0x007F) {
				return code == 0x002D // hyphen-minus, the one separator among the printable ascii
			}
			return code < 0x0020
					|| code in 0x007F..0x009F // c1 controls; U+00A0 has a space weight
					|| code == 0x00AD
					|| code in 0x0300..0x0345
					|| code in 0x0360..0x0361
					|| code in 0x0483..0x0486
					|| code in 0x200B..0x2015
					|| code in 0x20D0..0x20E1
					|| code == 0x2212
					|| code == 0xFEFF
		}
	}
}
