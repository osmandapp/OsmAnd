package net.osmand.shared.util

import java.util.Locale

actual object Localization {

	fun initialize() {
	}

	actual fun getStringId(key: String): Int {
		return 0
	}

	actual fun getString(key: String): String = toHumanReadable(key)

	actual fun getString(key: String, vararg args: Any): String = toHumanReadable(key)

	private fun toHumanReadable(key: String) = key.replace('_', ' ')
		.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
}