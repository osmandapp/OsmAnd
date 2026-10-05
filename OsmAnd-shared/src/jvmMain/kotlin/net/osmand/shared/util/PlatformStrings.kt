package net.osmand.shared.util

actual fun internString(value: String): String = value.intern()
