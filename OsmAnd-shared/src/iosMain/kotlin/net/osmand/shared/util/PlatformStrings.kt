package net.osmand.shared.util

// Kotlin/Native has no string pool, strings stay as they were read.
actual fun internString(value: String): String = value
