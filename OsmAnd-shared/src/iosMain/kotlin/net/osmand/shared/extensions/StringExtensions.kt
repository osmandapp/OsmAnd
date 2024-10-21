package net.osmand.shared.extensions

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun String.format(vararg args: Any?): String {
	val resultStringBuilder = StringBuilder()
	var index = 0
	var indexedArgs = 0

	while (index < this.length) {
		if (this[index] == '%' && index + 1 < this.length) {
			var argIndex: Int? = null
			var indexed = false
			var width: String? = null
			var precision: Int? = null
			var argType: Char? = null

			var formatSpecifierStart = index + 1

			// Check for indexed argument (e.g., %1$d)
			if (this[formatSpecifierStart].isDigit()) {
				var numberStart = formatSpecifierStart
				while (numberStart < this.length && this[numberStart].isDigit()) {
					numberStart++
				}
				if (numberStart < this.length && this[numberStart] == '$') {
					argIndex = this.substring(formatSpecifierStart, numberStart).toInt() - 1
					formatSpecifierStart = numberStart + 1
					indexed = true
				}
			}

			// Check for width and precision (e.g., %8.2f)
			var widthStart = formatSpecifierStart
			while (widthStart < this.length && this[widthStart].isDigit()) {
				widthStart++
			}

			if (widthStart > formatSpecifierStart) {
				width = this.substring(formatSpecifierStart, widthStart)
				formatSpecifierStart = widthStart
			}

			if (formatSpecifierStart < this.length && this[formatSpecifierStart] == '.') {
				val precisionStart = formatSpecifierStart + 1
				var precisionEnd = precisionStart
				while (precisionEnd < this.length && this[precisionEnd].isDigit()) {
					precisionEnd++
				}
				if (precisionEnd > precisionStart) {
					precision = this.substring(precisionStart, precisionEnd).toInt()
					formatSpecifierStart = precisionEnd
				}
			}

			// Get the argument type (e.g., 'd', 'f', ...)
			if (formatSpecifierStart < this.length) {
				argType = this[formatSpecifierStart]
			}

			when (argType) {
				's', '@' -> {
					val i = if (indexed && argIndex != null) argIndex else indexedArgs
					indexedArgs++
					resultStringBuilder.append(args[i])
					index = formatSpecifierStart
				}

				'd', 'f', 'x', 'X' -> {
					val formatStringBuilder = StringBuilder()
					formatStringBuilder.append('%')
					width?.let { formatStringBuilder.append(it) }
					precision?.let { formatStringBuilder.append('.').append(it) }
					formatStringBuilder.append(argType)
					val i = if (indexed && argIndex != null) argIndex else indexedArgs
					indexedArgs++

					val format = formatStringBuilder.toString()
					resultStringBuilder.append(when (args[i]) {
						is Int -> NSString.stringWithFormat(format, args[i] as Int)
						is Float -> NSString.stringWithFormat(format, args[i] as Float)
						is Double -> NSString.stringWithFormat(format, args[i] as Double)
						is Long -> NSString.stringWithFormat(format, args[i] as Long)
						else -> args[i]?.toString() ?: "null"
					})

					index = formatSpecifierStart
				}

				else -> {
					resultStringBuilder.append('%') // Handle case where '%' is not followed by known specifier
				}
			}
		} else {
			resultStringBuilder.append(this[index]) // Copy regular characters
		}
		index++
	}

	return resultStringBuilder.toString()
}