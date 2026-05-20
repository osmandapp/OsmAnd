package net.osmand.shared.util

expect object SharedNumberFormatter {
	/**
	 * Formats a double value to a string with a specified maximum number of fractional digits.
	 * Uses US locale implicitly to ensure consistent dot separator across platforms.
	 */
	fun formatDecimal(value: Double, maxDigits: Int = 1): String
}