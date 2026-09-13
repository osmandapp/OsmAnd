package net.osmand.shared.util

import platform.Foundation.NSString
import platform.Foundation.NSStringTransformToLatin
import platform.Foundation.decomposedStringWithCanonicalMapping
import platform.Foundation.precomposedStringWithCanonicalMapping
import platform.Foundation.stringByApplyingTransform

/**
 * Foundation's ICU transliteration, which is not the same engine as junidecode on the jvm: the two
 * agree on plain cyrillic and greek but not everywhere, so a name romanised here can differ from the
 * one android produces for the same map.
 *
 * The diacritics are stripped by decomposing rather than by a second
 * `NSStringTransformStripDiacritics` pass. That transform is the rule set `NFD; [:Mn:] Remove; NFC`,
 * and Foundation takes a transform identifier rather than a handle, so it rebuilds the rule based
 * transliterator on **every call** - on a simulator about 160 us per name, against 1.7 us for the
 * Any-Latin pass in front of it. Search calls this once per candidate without a `name:en`, tens of
 * thousands of times per query.
 *
 * The two spell the same result: checked over 22121 amenity names off three regional maps.
 */
internal actual fun toLatin(text: String): String {
	val latin = (text as NSString).stringByApplyingTransform(NSStringTransformToLatin, false)
		?: return text
	return stripDiacritics(latin)
}

/**
 * Decompose, drop the marks sitting on latin letters, compose again.
 *
 * Only latin ones: the point of this pass is that what Any-Latin could not romanise should not be
 * left with combining accents, and in scripts it did not touch a non spacing mark is not a
 * decoration - lao U+0EB1 and U+0EB5 are vowels, and removing them turns a word into a different
 * one. `NSStringTransformStripDiacritics` leaves them alone as well.
 */
private fun stripDiacritics(text: String): String {
	if (isAscii(text)) {
		return text
	}
	val decomposed = (text as NSString).decomposedStringWithCanonicalMapping
	var filtered: StringBuilder? = null
	var latinBase = false
	for (i in decomposed.indices) {
		val c = decomposed[i]
		val mark = c.category == CharCategory.NON_SPACING_MARK
		if (mark && latinBase) {
			if (filtered == null) {
				filtered = StringBuilder(decomposed.length).append(decomposed, 0, i)
			}
		} else {
			if (!mark) {
				// everything Any-Latin emits, up to and including the spacing modifier letters it
				// uses for the cyrillic soft and hard signs
				latinBase = c.code <= 0x2ff
			}
			filtered?.append(c)
		}
	}
	if (filtered == null) {
		return text
	}
	return (filtered.toString() as NSString).precomposedStringWithCanonicalMapping
}

/** Any-Latin leaves most names pure ascii, and ascii can carry no combining mark. */
private fun isAscii(text: String): Boolean {
	for (c in text) {
		if (c.code > 0x7f) {
			return false
		}
	}
	return true
}
