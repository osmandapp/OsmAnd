package net.osmand.shared.util

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSString
import platform.Foundation.create
import platform.Foundation.decomposedStringWithCanonicalMapping
import platform.Foundation.precomposedStringWithCanonicalMapping

/**
 * Non-spacing marks above the basic plane stay, where the jvm drops them: common code cannot ask
 * for the category of a code point that does not fit in a Char, and a surrogate's own category is
 * not Mn. They are combining marks of musical and historic notations, which no map name carries.
 */
@OptIn(BetaInteropApi::class)
internal actual fun platformStripDiacritics(value: String): String {
	val decomposed = NSString.create(string = value).decomposedStringWithCanonicalMapping
	val filtered = StringBuilder(decomposed.length)
	for (ch in decomposed) {
		if (ch.category != CharCategory.NON_SPACING_MARK) {
			filtered.append(ch)
		}
	}
	return NSString.create(string = filtered.toString()).precomposedStringWithCanonicalMapping
}

@OptIn(BetaInteropApi::class)
internal actual fun platformNormalizeNFC(value: String): String =
	NSString.create(string = value).precomposedStringWithCanonicalMapping
