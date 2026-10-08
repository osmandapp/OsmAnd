package net.osmand.shared.util

/**
 * Display-only formatting of international phone numbers without libphonenumber:
 * "+33612345678" -> "+33 6 12 34 56 78". Numbers without a country code, with letters
 * or extensions are returned as is. The raw value must still be used to dial.
 */
object PhoneNumberFormatter {

	private const val ALLOWED_CHARS = "0123456789+-.()  "

	// ITU calling codes are prefix-free: 1 and 7 have one digit, these have two, the rest three
	private val TWO_DIGIT_CODES = setOf(
		"20", "27", "30", "31", "32", "33", "34", "36", "39", "40", "41", "43", "44", "45", "46",
		"47", "48", "49", "51", "52", "53", "54", "55", "56", "57", "58", "60", "61", "62", "63",
		"64", "65", "66", "81", "82", "84", "86", "90", "91", "92", "93", "94", "95", "98")

	// country code -> groups of a national number (without trunk prefix) of the expected length
	private val NUMBERING_PLANS = mapOf(
		"1" to intArrayOf(3, 3, 4),
		"7" to intArrayOf(3, 3, 2, 2),
		"33" to intArrayOf(1, 2, 2, 2, 2))

	fun format(value: String): String {
		if (!value.contains(';')) {
			return formatNumber(value)
		}
		return value.split(';').joinToString("; ") { formatNumber(it.trim()) }
	}

	private fun formatNumber(number: String): String {
		val value = number.trim()
		if (value.any { it !in ALLOWED_CHARS }) {
			return number
		}
		var digits = value.filter { it.isDigit() }
		if (value.startsWith("00")) {
			digits = digits.substring(2)
		} else if (!value.startsWith("+")) {
			return number
		}
		if (digits.length < 8 || value.indexOf('+', 1) != -1) {
			return number
		}
		val code = getCountryCode(digits)
		val national = digits.substring(code.length)
		val plan = NUMBERING_PLANS[code]
		val groups = if (plan != null && plan.sum() == national.length) {
			plan
		} else {
			if (isAlreadyGrouped(value)) {
				return number
			}
			getGenericGroups(national.length)
		}
		val result = StringBuilder("+").append(code)
		var start = 0
		for (size in groups) {
			result.append(' ').append(national, start, start + size)
			start += size
		}
		return result.toString()
	}

	private fun getCountryCode(digits: String): String {
		return when {
			digits[0] == '1' || digits[0] == '7' -> digits.substring(0, 1)
			digits.substring(0, 2) in TWO_DIGIT_CODES -> digits.substring(0, 2)
			else -> digits.substring(0, 3)
		}
	}

	// keep grouping written by the mapper: they know where the area code ends, we don't
	private fun isAlreadyGrouped(value: String): Boolean {
		return value.contains('(') || value.split(Regex("[^0-9]+")).count { it.isNotEmpty() } > 2
	}

	private fun getGenericGroups(length: Int): IntArray {
		if (length <= 4) {
			return intArrayOf(length)
		}
		val tail = when (length % 3) {
			0 -> intArrayOf()
			1 -> intArrayOf(4)
			else -> if (length >= 8) intArrayOf(4, 4) else intArrayOf(2, 3)
		}
		return IntArray((length - tail.sum()) / 3) { 3 } + tail
	}
}
