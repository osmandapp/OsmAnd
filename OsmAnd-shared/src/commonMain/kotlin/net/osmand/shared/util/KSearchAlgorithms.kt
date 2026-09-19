package net.osmand.shared.util

/**
 * Copy of the part of `net.osmand.util.SearchAlgorithms` in OsmAnd-java that the obf reader needs:
 * folding a name onto what a search can match, splitting a query into tokens, and the suffix
 * dictionary and bounding boxes of the poi name index.
 *
 * What stayed in java is the search pipeline's own: encoding suffixes, dropping common words, and
 * the reader helpers that take a protobuf stream.
 *
 * Two deliberate differences from the java original, both about the default locale:
 * - lowercasing here is locale independent. Java lowercases a name with the device's locale, which
 *   turns `I` into `ı` on a turkish phone and makes the same map read differently on two devices.
 *   Obj-C `-lowercaseString`, which iOS matches names with today, is locale independent too.
 * - code points above the basic plane count as letters. Common code cannot ask for the category of
 *   one, and the supplementary characters that appear in place names - the cjk extensions, the
 *   historic scripts - are letters; only a query holding an emoji would tokenize differently.
 */
object KSearchAlgorithms {

	const val SUFFIX_DICT_MARKER_RAW_ESCAPE = ''
	const val SUFFIX_DICT_MARKER_BASE = 0xE100
	const val SUFFIX_DICT_MARKER_MAX = 0xF8FF

	/** Compatible with the default writer split `""`. */
	const val EMPTY_SUFFIX_DICTIONARY_SENTINEL = ""
	const val OLD_EMPTY_SUFFIX_DICTIONARY_SENTINEL = ""

	// remove () subcities
	private val CHARS_TO_NORMALIZE_KEY = charArrayOf('’', 'ʼ', '(', ')', '´', '`', '′', '‵', 'ʹ')
	private val CHARS_TO_NORMALIZE_VALUE = charArrayOf('\'', '\'', ' ', ' ', '\'', '\'', '\'', '\'', '\'')
	private val APOSTROPHES = charArrayOf('\'', '’', 'ʼ', '´', '`', '′', '‵', 'ʹ')

	fun split(name: String): List<String> {
		var prev = -1
		val namesToAdd = mutableListOf<String>()
		var i = 0
		while (i <= name.length) {
			var tokenCharacter = false
			var currentCodePointCharCount = 1
			if (i != name.length) {
				val codePoint = codePointAt(name, i)
				currentCodePointCharCount = charCount(codePoint)
				tokenCharacter = isTokenCharacter(name, i, prev != -1)
						|| codePoint == '\''.code || codePoint == '.'.code // dr.luth
			}
			if (!tokenCharacter) {
				if (prev != -1) {
					namesToAdd.add(name.substring(prev, i).lowercase())
					prev = -1
				}
			} else {
				if (prev == -1) {
					prev = i
				}
			}
			i += currentCodePointCharCount
		}
		return namesToAdd
	}

	/** Unique normalized tokens of the query, plus arabic-normalized variants when applicable. */
	fun splitAndNormalize(query: String, unique: Boolean): List<String> =
		splitAndNormalize(query, null, unique)

	fun splitAndNormalize(query: String, original: MutableList<String>?, unique: Boolean): List<String> {
		val normalizedQuery = canonicalizePunctuation(query)
		val queryTokens = mutableListOf<String>()
		for (token in split(normalizedQuery)) {
			val normalizedToken = normalizeToken(token)
			if (normalizedToken.isNotEmpty()) {
				queryTokens.add(normalizedToken)
				original?.add(token)
			}
		}
		if (KArabicNormalizer.isSpecialArabic(normalizedQuery)) {
			val arabic = KArabicNormalizer.normalize(normalizedQuery)
			if (arabic != null && arabic != normalizedQuery) {
				queryTokens.clear()
				original?.clear()
				for (token in split(arabic)) {
					val normalizedToken = normalizeToken(token)
					if (normalizedToken.isNotEmpty()) {
						queryTokens.add(normalizedToken)
						original?.add(token)
					}
				}
			}
		}
		if (unique) {
			val distinct = LinkedHashSet(queryTokens)
			queryTokens.clear()
			queryTokens.addAll(distinct)
		}
		return queryTokens
	}

	fun normalizeToken(token: String?): String {
		if (token == null) {
			return ""
		}
		return KUnicode.normalizeNFC(token).lowercase()
	}

	/** Folds punctuation variants so that equivalent search text is tokenized the same way. */
	fun canonicalizePunctuation(s: String): String {
		if (!KAlgorithms.containsChar(s, CHARS_TO_NORMALIZE_KEY)) {
			return s
		}
		var result = s
		for (k in CHARS_TO_NORMALIZE_KEY.indices) {
			result = result.replace(CHARS_TO_NORMALIZE_KEY[k], CHARS_TO_NORMALIZE_VALUE[k])
		}
		return result
	}

	/** Splits by everything that is not a letter or a digit, and lowercases what is left. */
	fun splitByWordsLowercase(str: String): List<String> {
		val splitStr = mutableListOf<String>()
		var prev = -1
		for (i in 0..str.length) {
			if (i == str.length || (!str[i].isLetter() && !str[i].isDigit())) {
				if (prev != -1) {
					splitStr.add(str.substring(prev, i).lowercase())
					prev = -1
				}
			} else {
				if (prev == -1) {
					prev = i
				}
			}
		}
		return splitStr
	}

	fun removeQuotes(s: String): String {
		if (s.contains("«") || s.contains("»")) {
			return s.replace("«", "").replace("»", "")
		}
		return s
	}

	/** Folds a name onto the letters a search can match: no apostrophes, no quotes, no diacritics. */
	fun alignChars(fullText: String): String {
		var result = fullText
		if (KArabicNormalizer.isSpecialArabic(result)) {
			result = KArabicNormalizer.normalize(result) ?: result
		}
		result = removeApostrophes(result)
		result = replaceGermanSS(result)
		result = removeQuotes(result)
		result = KUnicode.stripDiacritics(result)
		return result
	}

	fun removeApostrophes(s: String): String {
		if (!KAlgorithms.containsChar(s, APOSTROPHES)) {
			return s
		}
		val sb = StringBuilder(s.length)
		for (c in s) {
			var apostrophe = false
			for (d in APOSTROPHES) {
				if (d == c) {
					apostrophe = true
					break
				}
			}
			if (!apostrophe) {
				sb.append(c)
			}
		}
		return sb.toString()
	}

	fun replaceGermanSS(fullText: String): String {
		if (fullText.indexOf('ß') == -1) {
			return fullText
		}
		return fullText.replace("ß", "ss")
	}

	/** Decodes a raw suffix entry, or a delta entry that reuses a prefix of the previous suffix. */
	fun nameIndexDecodeDictionarySuffix(previousSuffix: String?, encodedSuffix: String): String {
		if (encodedSuffix.isEmpty()) {
			return ""
		}
		val markerCodePoint = codePointAt(encodedSuffix, 0)
		if (markerCodePoint in SUFFIX_DICT_MARKER_BASE..SUFFIX_DICT_MARKER_MAX) {
			checkNotNull(previousSuffix) { "Delta-encoded suffix dictionary entry requires previous suffix" }
			val commonPrefixCodePointLength = markerCodePoint - SUFFIX_DICT_MARKER_BASE
			val prefixEndOffset = offsetByCodePoints(
				previousSuffix,
				minOf(commonPrefixCodePointLength, countCodePoints(previousSuffix))
			)
			val suffixRemainder = encodedSuffix.substring(charCount(markerCodePoint))
			return KUnicode.normalizeNFC(previousSuffix.substring(0, prefixEndOffset) + suffixRemainder)
		}
		return KUnicode.normalizeNFC(decodeRawSuffix(encodedSuffix))
	}

	private fun decodeRawSuffix(encodedSuffix: String): String {
		if (encodedSuffix.isEmpty()) {
			return ""
		}
		val markerCodePoint = codePointAt(encodedSuffix, 0)
		if (markerCodePoint == SUFFIX_DICT_MARKER_RAW_ESCAPE.code) {
			return encodedSuffix.substring(charCount(markerCodePoint))
		}
		return encodedSuffix
	}

	// [zoom - default = 15 - 1km],[xzoom-left],[xzoom-right-delta],[y-top],[y-bottom-delta],...
	// input is boundary encoded - 4 first uints is bbox - of x31-left, y31-top, x31-right, y31-bottom
	fun encodeBboxForNameAtoms(zoom: Int, bbox31: IntArray): IntArray {
		val res = IntArray(bbox31.size + 1)
		res[0] = zoom
		val dz = 31 - zoom
		// support for array of bboxes could be added later
		// without it some width could be negative -180 meridian
		res[1] = bbox31[0] shr dz
		res[2] = maxOf(1, (bbox31[2] shr dz) - res[1])
		res[3] = bbox31[1] shr dz
		res[4] = maxOf(1, (bbox31[3] shr dz) - res[3])
		return res
	}

	/** Returns x31-left, y31-top, x31-right, y31-bottom. */
	fun decodeBboxForNameAtoms(vls: IntArray, x16: Int, y16: Int, zDec: Int): IntArray? {
		if (vls.size < 5) {
			return null
		}
		val zoom = vls[0]
		val res = IntArray(((vls.size - 1) / 4) * 4)
		var ind = 0
		while (ind < res.size) {
			res[ind] = ((x16 shr (16 - zoom)) - vls[ind + 1]) shl (zDec - zoom)
			res[ind + 1] = ((y16 shr (16 - zoom)) - vls[ind + 3]) shl (zDec - zoom)
			res[ind + 2] = ((vls[ind + 2] + 1) shl (zDec - zoom)) - 1 + res[ind]
			res[ind + 3] = ((vls[ind + 4] + 1) shl (zDec - zoom)) - 1 + res[ind + 1]
			ind += 4
		}
		return res
	}

	private fun isTokenCharacter(value: String, index: Int, tokenAlreadyStarted: Boolean): Boolean {
		val character = codePointAt(value, index)
		if (isLetterOrDigit(character)) {
			return true
		}
		val nextIndex = index + charCount(character)
		val previousIndex = if (index > 0) previousCodePointIndex(value, index) else -1

		val isHyphenNearNumber = character == '-'.code
				&& ((nextIndex < value.length && isDigit(codePointAt(value, nextIndex)))
				|| (previousIndex >= 0 && isDigit(codePointAt(value, previousIndex))))
		// dot belongs to word same as '''
		if (isHyphenNearNumber) {
			return true
		}
		if (!tokenAlreadyStarted || character > 0xFFFF) {
			return false
		}
		val category = character.toChar().category
		return category == CharCategory.NON_SPACING_MARK
				|| category == CharCategory.COMBINING_SPACING_MARK
				|| category == CharCategory.ENCLOSING_MARK
	}

	/** See the note on this class: everything above the basic plane counts as a letter. */
	private fun isLetterOrDigit(codePoint: Int): Boolean =
		if (codePoint > 0xFFFF) true else codePoint.toChar().let { it.isLetter() || it.isDigit() }

	private fun isDigit(codePoint: Int): Boolean =
		codePoint <= 0xFFFF && codePoint.toChar().isDigit()

	private fun codePointAt(value: String, index: Int): Int {
		val high = value[index]
		if (high.isHighSurrogate() && index + 1 < value.length) {
			val low = value[index + 1]
			if (low.isLowSurrogate()) {
				return 0x10000 + ((high.code - 0xD800) shl 10) + (low.code - 0xDC00)
			}
		}
		return high.code
	}

	private fun charCount(codePoint: Int): Int = if (codePoint > 0xFFFF) 2 else 1

	private fun previousCodePointIndex(value: String, index: Int): Int {
		val i = index - 1
		if (i > 0 && value[i].isLowSurrogate() && value[i - 1].isHighSurrogate()) {
			return i - 1
		}
		return i
	}

	private fun countCodePoints(value: String): Int {
		var count = 0
		var i = 0
		while (i < value.length) {
			i += charCount(codePointAt(value, i))
			count++
		}
		return count
	}

	/** Char index of the [codePoints]-th code point of [value]. */
	private fun offsetByCodePoints(value: String, codePoints: Int): Int {
		var i = 0
		var left = codePoints
		while (left > 0 && i < value.length) {
			i += charCount(codePointAt(value, i))
			left--
		}
		return i
	}
}
