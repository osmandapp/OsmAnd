package net.osmand.shared.util

import platform.Foundation.NSLocale
import platform.Foundation.NSString
import platform.Foundation.currentLocale
import platform.Foundation.lowercaseStringWithLocale

// Kotlin/Native has no string pool, strings stay as they were read.
actual fun internString(value: String): String = value

internal actual fun lowercaseWithDefaultLocale(value: String): String =
	(value as NSString).lowercaseStringWithLocale(NSLocale.currentLocale)
