package net.osmand.shared.util

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSCaseInsensitiveSearch
import platform.Foundation.NSDiacriticInsensitiveSearch
import platform.Foundation.NSLocale
import platform.Foundation.NSMakeRange
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSOrderedSame
import platform.Foundation.NSString
import platform.Foundation.NSWidthInsensitiveSearch
import platform.Foundation.compare
import platform.Foundation.create
import platform.Foundation.currentLocale
import platform.Foundation.languageCode

/**
 * The options `OACollatorStringMatcher` compares names with on iOS today, which is what a primary
 * strength collator ignores: case, accents and the full width forms.
 *
 * Use the current locale, like Android's primary collator. Without a locale NSString puts
 * letters such as Ø and Ł after Z instead of comparing their primary letter weights.
 */
private val PRIMARY_OPTIONS =
	NSCaseInsensitiveSearch or NSDiacriticInsensitiveSearch or NSWidthInsensitiveSearch

actual fun primaryCollator(): KCollator {
	val current = NSLocale.currentLocale
	val locale = when (current.languageCode) {
		"ro", "cs", "sk" -> NSLocale(localeIdentifier = "en_US")
		else -> current
	}
	return NSStringCollator(locale)
}

private class NSStringCollator(private val locale: NSLocale) : KCollator {

	@OptIn(BetaInteropApi::class, ExperimentalForeignApi::class)
	override fun compare(source: String, target: String): Int =
		when (NSString.create(string = source).compare(
			target, PRIMARY_OPTIONS, NSMakeRange(0u, source.length.toULong()), locale
		)) {
			NSOrderedSame -> 0
			NSOrderedAscending -> -1
			else -> 1
		}

	override fun equals(source: String, target: String): Boolean = compare(source, target) == 0
}
