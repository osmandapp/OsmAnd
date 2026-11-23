package net.osmand.shared.util

import java.util.Locale

actual fun internString(value: String): String = value.intern()

internal actual fun lowercaseWithDefaultLocale(value: String): String = value.lowercase(Locale.getDefault())
