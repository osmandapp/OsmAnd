package net.osmand.shared.util

import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSCaseInsensitiveSearch
import platform.Foundation.NSDiacriticInsensitiveSearch
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSOrderedSame
import platform.Foundation.NSString
import platform.Foundation.NSWidthInsensitiveSearch
import platform.Foundation.compare
import platform.Foundation.create

/**
 * The options `OACollatorStringMatcher` compares names with on iOS today, which is what a primary
 * strength collator ignores: case, accents and the full width forms.
 *
 * No locale is passed, so names come out in the same order on every device.
 */
private val PRIMARY_OPTIONS =
	NSCaseInsensitiveSearch or NSDiacriticInsensitiveSearch or NSWidthInsensitiveSearch

actual fun primaryCollator(): KCollator = NSStringCollator

private object NSStringCollator : KCollator {

	@OptIn(BetaInteropApi::class)
	override fun compare(source: String, target: String): Int =
		when (NSString.create(string = source).compare(target, PRIMARY_OPTIONS)) {
			NSOrderedSame -> 0
			NSOrderedAscending -> -1
			else -> 1
		}

	@OptIn(BetaInteropApi::class)
	override fun equals(source: String, target: String): Boolean =
		NSString.create(string = source).compare(target, PRIMARY_OPTIONS) == NSOrderedSame
}
