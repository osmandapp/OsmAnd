package net.osmand.shared.util

import net.osmand.shared.data.KQuadRect
import net.osmand.shared.extensions.format
import net.osmand.shared.io.KFile
import kotlin.math.max
import kotlin.math.min

object KAlgorithms {
	private const val CHAR_TO_SPLIT = ','

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

	/**
	 * Joins the [symbol] separated parts of [ref] back together, dropping empty parts and any part
	 * that repeats the one before it. Road refs are often tagged as "A1;A1;A2".
	 */
	fun splitAndClearRepeats(ref: String, symbol: String): String {
		val res = StringBuilder()
		var prev = ""
		for (s in ref.split(symbol)) {
			if (isEmpty(s) || prev == s) {
				continue
			}
			if (res.isNotEmpty()) {
				res.append(symbol)
			}
			res.append(s)
			prev = s
		}
		return res.toString()
	}

	fun isDigit(c: Char): Boolean {
		return c in '0'..'9'
	}

	/**
	 * Index right after the leading decimal number of [value], -1 when it does not start with one.
	 * A trailing dot is not part of the number, so "40." reports 2.
	 */
	fun findFirstNumberEndIndex(value: String): Int {
		var i = 0
		if (value.isNotEmpty() && value[0] == '-') {
			i++
		}
		var state = 0 // 0 - no number, 1 - 1st digits, 2 - dot, 3 - last digits
		while (i < value.length && (isDigit(value[i]) || value[i] == '.')) {
			if (value[i] == '.') {
				if (state == 2) {
					return i - 1
				}
				if (state != 1) {
					return -1
				}
				state = 2
			} else {
				if (state == 2) {
					// last digits
					state = 3
				} else if (state == 0) {
					// first digits started
					state = 1
				}
			}
			i++
		}
		if (state == 2) {
			// invalid number like 40. correct to -> '40'
			return i - 1
		}
		if (state == 0) {
			return -1
		}
		return i
	}

	fun colorToString(color: Int): String {
		return if ((0xFF000000.toInt() and color) == 0xFF000000.toInt()) {
			"#%06X".format(color and 0x00FFFFFF)
		} else {
			"#%08X".format(color)
		}
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

	fun decodeStringSet(s: String): Set<String> {
		return decodeStringSet(s, CHAR_TO_SPLIT.toString())
	}

	fun decodeStringSet(s: String, split: String): Set<String> {
		if (s.isEmpty()) {
			return emptySet()
		}
		return s.split(split).toSet()
	}

	fun <T> encodeCollection(collection: Collection<T>): String {
		return encodeCollection(collection, CHAR_TO_SPLIT.toString())
	}

	fun <T> encodeCollection(collection: Collection<T>, split: String): String {
		if (collection.isNotEmpty()) {
			val sb = StringBuilder()
			for (item in collection) {
				sb.append(item).append(split)
			}
			return sb.toString()
		}
		return ""
	}

	fun extendRectToContainPoint(mapRect: KQuadRect, longitude: Double, latitude: Double) {
		mapRect.left = if (mapRect.left == 0.0) longitude else min(mapRect.left, longitude)
		mapRect.right = max(mapRect.right, longitude)
		mapRect.bottom = if (mapRect.bottom == 0.0) latitude else min(mapRect.bottom, latitude)
		mapRect.top = max(mapRect.top, latitude)
	}

	fun extendRectToContainRect(mapRect: KQuadRect, gpxRect: KQuadRect) {
		mapRect.left = if (mapRect.left == 0.0) gpxRect.left else min(mapRect.left, gpxRect.left)
		mapRect.right = max(mapRect.right, gpxRect.right)
		mapRect.top = max(mapRect.top, gpxRect.top)
		mapRect.bottom = if (mapRect.bottom == 0.0) gpxRect.bottom else min(mapRect.bottom, gpxRect.bottom)
	}

	fun capitalizeFirstLetter(s: String?): String? {
		return if (!s.isNullOrEmpty()) {
			s[0].uppercaseChar().toString() + if (s.length > 1) s.substring(1) else ""
		} else {
			s
		}
	}

	fun getFileNameWithoutExtension(file: KFile): String {
		return getFileNameWithoutExtension(file.name())
	}

	fun getFileNameWithoutExtension(name: String?): String {
		return name?.substringBeforeLast('.', name) ?: ""
	}

	fun getFileWithoutDirs(name: String): String {
		val i: Int = name.lastIndexOf('/')
		if (i != -1) {
			return name.substring(i + 1)
		}
		return name
	}
}
