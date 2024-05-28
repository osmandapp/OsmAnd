package net.osmand.shared.extensions

import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual fun String.format(vararg args: Any?): String {
	@Suppress("UNCHECKED_CAST")
	return NSString.stringWithFormat(this, *(args as Array<Any?>))
}