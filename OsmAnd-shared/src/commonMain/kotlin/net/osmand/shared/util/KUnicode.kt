package net.osmand.shared.util

/**
 * Unicode normalisation for search, in one place: what OsmAnd-java gets from `UnicodeDiacritics`
 * and `java.text.Normalizer`, and OsmAnd-core from `ICU::stripDiacritics`.
 *
 * Normalising is platform work, see `PlatformUnicode`. What is here is the filter that keeps
 * strings away from it. Every name read out of an obf passes through [stripDiacritics] on its way
 * into the collator, and most names have nothing to strip, so most must never reach Foundation or
 * `java.text.Normalizer` at all.
 */
object KUnicode {

	/** Canonical decomposition, every non-spacing mark dropped, canonical composition. */
	fun stripDiacritics(input: String): String {
		if (input.isEmpty() || !mayHaveDiacritics(input)) {
			return input
		}
		val result = platformStripDiacritics(input)
		return if (result == input) input else result
	}

	/** Canonical composition (NFC). */
	fun normalizeNFC(input: String): String {
		if (input.isEmpty() || isAscii(input)) {
			// ascii holds no combining mark and composes with none, so it is nfc already
			return input
		}
		return platformNormalizeNFC(input)
	}

	/**
	 * True when [input] holds a non-spacing mark, or a character whose decomposition holds one.
	 *
	 * Deliberately conservative: a false positive costs one call into the platform normaliser, a
	 * false negative would leave a diacritic in and make two names differ that the java side
	 * considers equal.
	 */
	fun mayHaveDiacritics(input: String): Boolean {
		val table = bmpTable
		for (ch in input) {
			val code = ch.code
			if (table[code]) {
				return true
			}
			if (code in HIGH_SURROGATE_FIRST..HIGH_SURROGATE_LAST) {
				// start of a character above the basic plane, which may be a non-spacing mark;
				// common code cannot ask for the category of a code point that does not fit in a
				// Char. Rare enough in map names to hand every one of them to the platform.
				return true
			}
		}
		return false
	}

	private fun isAscii(input: String): Boolean {
		for (ch in input) {
			if (ch.code > 0x7F) {
				return false
			}
		}
		return true
	}

	/**
	 * One flag per code point of the basic plane, built once from [K_UNICODE_DIACRITIC_RANGES].
	 *
	 * 64 KB so that [mayHaveDiacritics] costs one array read per character. A binary search over
	 * the ranges would cost around nine comparisons per character instead, on every name of every
	 * search.
	 */
	private val bmpTable: BooleanArray by lazy {
		val table = BooleanArray(0x10000)
		var i = 0
		while (i < K_UNICODE_DIACRITIC_RANGES.size) {
			for (code in K_UNICODE_DIACRITIC_RANGES[i]..K_UNICODE_DIACRITIC_RANGES[i + 1]) {
				table[code] = true
			}
			i += 2
		}
		table
	}

	private const val HIGH_SURROGATE_FIRST = 0xD800
	private const val HIGH_SURROGATE_LAST = 0xDBFF
}
