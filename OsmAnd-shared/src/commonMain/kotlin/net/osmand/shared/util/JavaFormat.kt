package net.osmand.shared.util

import kotlin.math.abs

/**
 * Numbers as `String.format` of OsmAnd-java writes them, for the copies of java classes whose text
 * the tests compare: the expect `String.format` of iOS does not know `%,d`.
 */
internal object JavaFormat {

	/** `%.nf` of java's `Formatter`: the digits of the double as java writes it, rounded half up. */
	fun fixed(value: Double, digits: Int): String {
		if (value.isNaN()) {
			return "NaN"
		}
		if (value.isInfinite()) {
			return if (value > 0) "Infinity" else "-Infinity"
		}
		val negative = value < 0 || (value == 0.0 && 1 / value < 0)
		val text = abs(value).toString()
		val e = text.indexOfFirst { it == 'E' || it == 'e' }
		val mantissa = if (e >= 0) text.substring(0, e) else text
		val dot = mantissa.indexOf('.')
		val all = if (dot >= 0) mantissa.substring(0, dot) + mantissa.substring(dot + 1) else mantissa
		val point = (if (dot >= 0) dot else mantissa.length) + (if (e >= 0) text.substring(e + 1).toInt() else 0)
		// the digits of the integer part and the fraction that stay, at least one before the point
		val kept = IntArray(maxOf(point, 1) + digits)
		val shift = kept.size - digits - point
		for (i in all.indices) {
			val at = i + shift
			if (at in kept.indices) {
				kept[at] = all[i] - '0'
			}
		}
		val next = point + digits
		if (next in all.indices && next >= 0 && all[next] >= '5') {
			var i = kept.size - 1
			while (i >= 0) {
				if (kept[i] < 9) {
					kept[i]++
					break
				}
				kept[i] = 0
				i--
			}
			if (i < 0) {
				return (if (negative) "-1" else "1") + format(kept, digits)
			}
		}
		return (if (negative) "-" else "") + format(kept, digits)
	}

	/** `%,d` of java's `Formatter` in an English locale: "1,234,567". */
	fun grouped(value: Long): String {
		val text = value.toString()
		val start = if (value < 0) 1 else 0
		val sb = StringBuilder(text.length + text.length / 3)
		if (start > 0) {
			sb.append('-')
		}
		for (i in start until text.length) {
			if (i > start && (text.length - i) % 3 == 0) {
				sb.append(',')
			}
			sb.append(text[i])
		}
		return sb.toString()
	}

	private fun format(kept: IntArray, digits: Int): String {
		val sb = StringBuilder()
		for (i in kept.indices) {
			if (i == kept.size - digits) {
				sb.append('.')
			}
			sb.append(kept[i])
		}
		return sb.toString()
	}
}
