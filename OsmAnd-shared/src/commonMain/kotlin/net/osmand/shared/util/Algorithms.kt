package net.osmand.shared.util

object Algorithms {
	private const val BUFFER_SIZE = 1024
	//private val log = PlatformUtil.getLog(Algorithms::class.java)

	private val CHARS_TO_NORMALIZE_KEY = charArrayOf('’')
	private val CHARS_TO_NORMALIZE_VALUE = charArrayOf('\'')

	private const val HTML_PATTERN = "<(\"[^\"]*\"|'[^']*'|[^'\">])*>"

	const val ZIP_FILE_SIGNATURE = 0x504b0304
	const val XML_FILE_SIGNATURE = 0x3c3f786d
	const val OBF_FILE_SIGNATURE = 0x08029001
	const val SQLITE_FILE_SIGNATURE = 0x53514C69
	const val BZIP_FILE_SIGNATURE = 0x425a
	const val GZIP_FILE_SIGNATURE = 0x1f8b

	fun normalizeSearchText(s: String): String {
		var norm = false
		for (i in s.indices) {
			val ch = s[i]
			for (j in CHARS_TO_NORMALIZE_KEY.indices) {
				if (ch == CHARS_TO_NORMALIZE_KEY[j]) {
					norm = true
					break
				}
			}
			if (norm) break
		}
		if (!norm) {
			return s
		}
		var result = s
		for (k in CHARS_TO_NORMALIZE_KEY.indices) {
			result = result.replace(CHARS_TO_NORMALIZE_KEY[k], CHARS_TO_NORMALIZE_VALUE[k])
		}
		return result
	}

	/**
	 * Split string by words and convert to lowercase, use as delimiter all chars except letters and digits
	 * @param str input string
	 * @return result words list
	 */
	fun splitByWordsLowercase(str: String): List<String> {
		val splitStr = mutableListOf<String>()
		var prev = -1
		for (i in 0..str.length) {
			if (i == str.length || (!str[i].isLetter() && !str[i].isDigit())) {
				if (prev != -1) {
					val subStr = str.substring(prev, i)
					splitStr.add(subStr.lowercase())
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

	fun isEmpty(c: Collection<*>?): Boolean {
		return c == null || c.isEmpty()
	}

	fun isEmpty(map: Map<*, *>?): Boolean {
		return map == null || map.isEmpty()
	}

	fun <T> isEmpty(array: Array<T>?): Boolean {
		return array.isNullOrEmpty()
	}

	fun emptyIfNull(s: String?): String {
		return s ?: ""
	}

	fun trimIfNotNull(s: String?): String? {
		return s?.trim()
	}

	fun isEmpty(s: CharSequence?): Boolean {
		return s.isNullOrEmpty()
	}

	fun isBlank(s: String?): Boolean {
		return s == null || s.trim().isEmpty()
	}

	fun hash(vararg values: Any?): Int {
		return values.contentHashCode()
	}

	fun stringsEqual(s1: String?, s2: String?): Boolean {
		return s1 == s2
	}

	fun parseLongSilently(input: String?, def: Long): Long {
		return if (!isEmpty(input)) {
			try {
				input?.toLong() ?: def
			} catch (e: NumberFormatException) {
				def
			}
		} else {
			def
		}
	}

	fun parseIntSilently(input: String?, def: Int): Int {
		return if (!isEmpty(input)) {
			try {
				input?.toInt() ?: def
			} catch (e: NumberFormatException) {
				def
			}
		} else {
			def
		}
	}

	fun parseDoubleSilently(input: String?, def: Double): Double {
		return if (!isEmpty(input)) {
			try {
				input?.toDouble() ?: def
			} catch (e: NumberFormatException) {
				def
			}
		} else {
			def
		}
	}

	fun parseFloatSilently(input: String?, def: Float): Float {
		return if (!isEmpty(input)) {
			try {
				input?.toFloat() ?: def
			} catch (e: NumberFormatException) {
				def
			}
		} else {
			def
		}
	}

	fun colorToString(color: Int): String {
		return if ((0xFF000000.toInt() and color) == 0xFF000000.toInt()) {
			"#" + format(6, (color and 0x00FFFFFF).toString(16))
		} else {
			"#" + format(8, color.toString(16))
		}
	}

	private fun format(i: Int, hexString: String): String {
		var formattedString = hexString
		while (formattedString.length < i) {
			formattedString = "0$formattedString"
		}
		return formattedString
	}

	/**
	 * Parse the color string, and return the corresponding color-int.
	 * If the string cannot be parsed, throws an IllegalArgumentException
	 * exception. Supported formats are:
	 * #RRGGBB
	 * #AARRGGBB
	 */
	fun parseColor(colorString: String): Int {
		if (colorString.startsWith("#")) {
			var colorStr = colorString
			if (colorStr.length == 4) {
				colorStr =
					"#" + colorStr[1] + colorStr[1] + colorStr[2] + colorStr[2] + colorStr[3] + colorStr[3]
			}
			val color = colorStr.substring(1).toLong(16)
			return when (colorStr.length) {
				7 -> (color or 0x00000000ff000000).toInt() // Set the alpha value
				9 -> color.toInt()
				else -> throw IllegalArgumentException("Unknown color $colorString")
			}
		} else {
			throw IllegalArgumentException("Unknown color $colorString")
		}
	}

}
