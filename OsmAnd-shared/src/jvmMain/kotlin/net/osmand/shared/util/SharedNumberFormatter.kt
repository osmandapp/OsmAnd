package net.osmand.shared.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

actual object SharedNumberFormatter {

	actual fun formatDecimal(value: Double, maxDigits: Int): String {
		val pattern = if (maxDigits > 0) "#." + "#".repeat(maxDigits) else "#"
		val symbols = DecimalFormatSymbols(Locale.US)
		return DecimalFormat(pattern, symbols).format(value)
	}
}