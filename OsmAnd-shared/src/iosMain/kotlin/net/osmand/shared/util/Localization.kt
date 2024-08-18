package net.osmand.shared.util

import net.osmand.shared.extensions.format
import platform.Foundation.NSBundle
import platform.Foundation.NSString
import platform.Foundation.stringWithFormat

actual object Localization {

	private var enBundle: NSBundle? = null

	init {
		val path = NSBundle.mainBundle.pathForResource("en", "lproj")
		enBundle = if (path != null) NSBundle(path = path) else null
	}

	actual fun getStringId(key: String): Int {
		return 0
	}

	actual fun getString(key: String): String {
		return localizedString(key)
	}

	actual fun getString(key: String, vararg args: Any): String {
		return localizedString(key, args)
	}

	fun localizedString(defaultValue: String): String {
		return _localizedString(false, defaultValue)
	}

	fun localizedString(defaultValue: String, vararg args: Any?): String {
		return _localizedString(false, defaultValue, args)
	}

	private fun _localizedString(upperCase: Boolean, defaultValue: String, vararg args: Any?): String {
		val words = defaultValue.split(" ")
		val key = words.firstOrNull { it.isNotEmpty() && it[0].isLetter() }

		val result: String
		if (key != null) {
			var localizedString = NSBundle.mainBundle.localizedStringForKey(key, "!!!", null)
			if (localizedString == "!!!" || localizedString.isEmpty()) {
				localizedString = enBundle?.localizedStringForKey(key, "", null) ?: key
			}

			val newValue = defaultValue
				.replace(key, localizedString)
				.replace("%1\$s", "%@")
				.replace("%1\$d", "%d")
				.replace("%2\$s", "%@")
				.replace("%2\$d", "%d")

			result = if (defaultValue == key) {
				if (upperCase) newValue.uppercase() else newValue
			} else {
				if (upperCase) {
					newValue.format(args).uppercase()
				} else {
					newValue.format(args)
				}
			}
		} else {
			result = defaultValue.format(args)
		}

		return result
	}
}