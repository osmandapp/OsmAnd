package net.osmand.shared.extensions

actual fun String.format(vararg args: Any?): String = String.format(this, *args)