package net.osmand.shared.util

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter

actual object SharedNumberFormatter {

	actual fun formatDecimal(value: Double, maxDigits: Int): String {
		val formatter = NSNumberFormatter().apply {
			maximumFractionDigits = maxDigits.toULong()
			minimumFractionDigits = 0uL
			decimalSeparator = "."
		}
		return formatter.stringFromNumber(NSNumber(value)) ?: value.toString()
	}
}