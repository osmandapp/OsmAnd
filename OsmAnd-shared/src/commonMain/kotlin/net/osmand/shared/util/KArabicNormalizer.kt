package net.osmand.shared.util

/**
 * Copy of `net.osmand.util.ArabicNormalizer` in OsmAnd-java.
 *
 * Folds an arabic name onto the letters a search can match: the diacritics go, the letters that
 * differ only by a hamza or a madda become their base, and arabic-indic digits become ascii ones.
 *
 * The java original spells the diacritic classes as regular expressions; every one of them matches
 * a single character, so they are ranges here instead, which is the same set without a matcher.
 */
object KArabicNormalizer {

	private val DIACRITIC_REPLACE = charArrayOf(
		'ؤ', 'و', // waw hamza above -> waw
		'ة', 'ه', // ta marbuta -> ha
		'ي', 'ى', // ya -> alif maksura
		'ئ', 'ى', // ya hamza above -> alif maksura
		'آ', 'ا', // alif madda above -> alif
		'أ', 'ا', // alif hamza above -> alif
		'إ', 'ا'  // alif hamza below -> alif
	)

	private const val ARABIC_DIGITS = "٠١٢٣٤٥٦٧٨٩"
	private const val DIGITS_REPLACEMENT = "0123456789"

	private const val ARABIC_BLOCK_FIRST = '؀'
	private const val ARABIC_BLOCK_LAST = 'ۿ'

	fun isSpecialArabic(text: String?): Boolean {
		if (text.isNullOrEmpty()) {
			return false
		}
		if (!isArabicBlock(text[0])) {
			return false
		}
		for (c in text) {
			if (isDiacritic(c) || isArabicDigit(c) || isNeedReplace(c)) {
				return true
			}
		}
		return false
	}

	fun normalize(text: String?): String? {
		if (text.isNullOrEmpty()) {
			return text
		}
		val result = StringBuilder(text.length)
		for (c in text) {
			if (isDiacritic(c)) {
				continue
			}
			result.append(replacement(c))
		}
		return replaceDigits(result.toString())
	}

	/** Null for an empty string, as in the java original, which its callers read as "leave it alone". */
	private fun replaceDigits(text: String): String? {
		if (text.isEmpty()) {
			return null
		}
		if (!isArabicBlock(text[0])) {
			return text
		}
		val chars = text.toCharArray()
		for (i in chars.indices) {
			val digit = ARABIC_DIGITS.indexOf(chars[i])
			if (digit >= 0) {
				chars[i] = DIGITS_REPLACEMENT[digit]
			}
		}
		return chars.concatToString()
	}

	private fun replacement(c: Char): Char {
		var i = 0
		while (i < DIACRITIC_REPLACE.size) {
			if (DIACRITIC_REPLACE[i] == c) {
				return DIACRITIC_REPLACE[i + 1]
			}
			i += 2
		}
		return c
	}

	private fun isArabicBlock(c: Char): Boolean = c in ARABIC_BLOCK_FIRST..ARABIC_BLOCK_LAST

	private fun isDiacritic(c: Char): Boolean =
		(c in 'ً'..'ٟ')
				|| (c in 'ؐ'..'ؚ')
				|| (c in 'ۖ'..'ۭ')
				|| c == 'ـ'
				|| c == 'ٰ'

	private fun isNeedReplace(c: Char): Boolean {
		var i = 0
		while (i < DIACRITIC_REPLACE.size) {
			if (DIACRITIC_REPLACE[i] == c) {
				return true
			}
			i += 2
		}
		return false
	}

	private fun isArabicDigit(c: Char): Boolean = c in '٠'..'٩'
}
