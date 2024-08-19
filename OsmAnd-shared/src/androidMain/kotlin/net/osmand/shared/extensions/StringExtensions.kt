package net.osmand.shared.extensions

actual fun String.format(vararg args: Any?): String {
	return String.format(this, *args)
}