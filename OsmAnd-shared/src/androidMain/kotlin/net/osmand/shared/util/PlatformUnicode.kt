package net.osmand.shared.util

import java.text.Normalizer

internal actual fun platformStripDiacritics(value: String): String {
	val decomposed = Normalizer.normalize(value, Normalizer.Form.NFD)
	val filtered = StringBuilder(decomposed.length)
	var i = 0
	while (i < decomposed.length) {
		val codePoint = decomposed.codePointAt(i)
		i += Character.charCount(codePoint)
		if (Character.getType(codePoint) != Character.NON_SPACING_MARK.toInt()) {
			filtered.appendCodePoint(codePoint)
		}
	}
	return Normalizer.normalize(filtered, Normalizer.Form.NFC)
}

internal actual fun platformNormalizeNFC(value: String): String =
	Normalizer.normalize(value, Normalizer.Form.NFC)
