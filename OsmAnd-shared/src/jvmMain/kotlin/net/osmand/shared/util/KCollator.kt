package net.osmand.shared.util

import java.util.Locale

/**
 * `java.text.Collator` at primary strength, as `OsmAndCollator.primaryCollator()` builds it,
 * including its way around the locales that treat a diacritic as a letter of its own.
 */
actual fun primaryCollator(): KCollator {
	val language = Locale.getDefault().language
	val instance = if (language == "ro" || language == "cs" || language == "sk") {
		java.text.Collator.getInstance(Locale.US)
	} else {
		java.text.Collator.getInstance()
	}
	instance.strength = java.text.Collator.PRIMARY
	return JavaCollator(instance)
}

private class JavaCollator(private val instance: java.text.Collator) : KCollator {

	override fun compare(source: String, target: String): Int = instance.compare(source, target)

	override fun equals(source: String, target: String): Boolean = instance.equals(source, target)
}
