@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package net.osmand.shared.util

import kotlinx.cinterop.BetaInteropApi
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
import platform.Foundation.localeIdentifier
import platform.Foundation.stringByFoldingWithOptions
import kotlin.concurrent.Volatile

/** null indicates that Core is unavailable and Foundation should handle the operation. */
interface IOSSearchCollationProvider {
	fun contains(locale: String, base: String, part: String): Boolean?
	fun equalsAtAny(locale: String, base: String, part: String, length: Int, offsets: IntArray, count: Int): Boolean?
}

object IOSSearchCollation {
	@Volatile var provider: IOSSearchCollationProvider? = null
}

@kotlin.native.concurrent.ThreadLocal
private var lastCollator: IOSCollator? = null

actual fun primaryCollator(): KCollator {
	val current = NSLocale.currentLocale
	val locale = when (current.languageCode) {
		"ro", "cs", "sk" -> NSLocale(localeIdentifier = "en_US")
		else -> current
	}
	return lastCollator?.takeIf { it.locale == locale } ?: IOSCollator(locale).also { lastCollator = it }
}

private class IOSCollator(val locale: NSLocale) : KCollator {
	private val identifier = locale.localeIdentifier

	@OptIn(BetaInteropApi::class)
	private fun foldCase(text: String): String =
		NSString.create(string = text).stringByFoldingWithOptions(NSCaseInsensitiveSearch, locale)

	@OptIn(BetaInteropApi::class)
	override fun compare(source: String, target: String): Int {
		// Fold both operands first so Foundation orders expansions consistently across comparisons.
		val left = foldCase(source)
		val right = foldCase(target)
		return when (NSString.create(string = left).compare(right,
			NSCaseInsensitiveSearch or NSDiacriticInsensitiveSearch or NSWidthInsensitiveSearch,
			NSMakeRange(0u, left.length.toULong()), locale)) {
			NSOrderedSame -> 0
			NSOrderedAscending -> -1
			else -> 1
		}

	}

	override fun equals(source: String, target: String): Boolean = compare(source, target) == 0

	override fun contains(base: String, part: String): Boolean =
		IOSSearchCollation.provider?.contains(identifier, base, part) ?: super.contains(base, part)

	override fun equalsAtAny(base: String, part: String, length: Int, offsets: IntArray, count: Int): Boolean {
		require(count in 0..offsets.size && length in 0..base.length)
		if (count == 0) return false
		return IOSSearchCollation.provider?.equalsAtAny(identifier, base, part, length, offsets, count)
			?: super.equalsAtAny(base, part, length, offsets, count)
	}
}
