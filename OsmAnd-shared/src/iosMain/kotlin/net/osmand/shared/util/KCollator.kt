@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package net.osmand.shared.util

import kotlinx.cinterop.refTo
import net.osmand.shared.icu.closePrimaryCollator
import net.osmand.shared.icu.containsSpan
import net.osmand.shared.icu.equalsAtAnyOffset
import net.osmand.shared.icu.openPrimaryCollator
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
import kotlin.native.ref.createCleaner

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

// ICU accepts a zero length, but refTo needs an address even for an empty span.
private fun String.utf16() = ifEmpty { "\u0000" }.refTo(0)

private class IOSCollator(val locale: NSLocale) : KCollator {
	private val handle = checkNotNull(openPrimaryCollator(locale.localeIdentifier)) { "Cannot open ICU collator for $locale" }
	@Suppress("unused")
	private val cleaner = createCleaner(handle) { closePrimaryCollator(it) }

	@OptIn(BetaInteropApi::class)
	override fun compare(source: String, target: String): Int =
		when (NSString.create(string = source).compare(target,
			NSCaseInsensitiveSearch or NSDiacriticInsensitiveSearch or NSWidthInsensitiveSearch,
			NSMakeRange(0u, source.length.toULong()), locale)) {
			NSOrderedSame -> 0
			NSOrderedAscending -> -1
			else -> 1
		}

	override fun equals(source: String, target: String): Boolean = compare(source, target) == 0

	override fun contains(base: String, part: String): Boolean =
		containsSpan(handle, base.utf16(), base.length, part.utf16(), part.length) != 0

	override fun equalsAtAny(base: String, part: String, length: Int, offsets: IntArray, count: Int): Boolean {
		require(count in 0..offsets.size && length in 0..base.length)
		if (count == 0) return false
		val result = equalsAtAnyOffset(handle, base.utf16(), base.length, length, part.utf16(), part.length, offsets.refTo(0), count)
		require(result >= 0) { "Invalid collation span" }
		return result != 0
	}
}
