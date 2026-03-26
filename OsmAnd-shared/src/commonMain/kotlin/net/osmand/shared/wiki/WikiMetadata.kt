package net.osmand.shared.wiki

object WikiMetadata {

	private const val UNKNOWN = "Unknown"
	const val ENGLISH_LANGUAGE = "en"

	class Metadata {
		var date: String? = null
		var author: String? = null
		var license: String? = null

		private var descriptionsMap: LinkedHashMap<String, String>? = null

		val descriptions: Map<String, String>
			get() = descriptionsMap ?: emptyMap()

		fun getDescription(): String? {
			return getDescription(ENGLISH_LANGUAGE)
		}

		fun setDescription(description: String?) {
			putDescription(ENGLISH_LANGUAGE, description)
		}

		fun putDescription(language: String, description: String?) {
			val normalizedLanguage = normalizeLanguageKey(language) ?: return

			if (description == null) {
				descriptionsMap?.remove(normalizedLanguage)
				return
			}
			if (description.isBlank()) {
				return
			}
			if (descriptionsMap == null) {
				descriptionsMap = LinkedHashMap()
			}
			descriptionsMap?.put(normalizedLanguage, description)
		}

		fun getDescription(preferredLanguage: String?): String? {
			val localizedDescriptions = descriptionsMap ?: return null
			val normalizedLanguage = normalizeLanguageKey(preferredLanguage)
			if (normalizedLanguage != null) {
				localizedDescriptions[normalizedLanguage]?.takeIf { it.isNotBlank() }?.let { return it }
			}
			return localizedDescriptions[ENGLISH_LANGUAGE]?.takeIf { it.isNotBlank() }
				?: localizedDescriptions.values.firstOrNull { it.isNotBlank() }
		}
	}

	fun updateMetadata(metadataMap: Map<String, Map<String, String>>, metadata: Metadata) {
		metadataMap.forEach { (language, details) ->
			details["date"]?.takeIf { it.isNotEmpty() && (metadata.date.isNullOrEmpty() || metadata.date == UNKNOWN) }?.let {
				metadata.date = it
			}
			details["license"]?.takeIf { it.isNotEmpty() && (metadata.license.isNullOrEmpty() || metadata.license == UNKNOWN) }?.let {
				metadata.license = it
			}
			details["author"]?.takeIf { it.isNotEmpty() && (metadata.author.isNullOrEmpty() || metadata.author == UNKNOWN) }?.let {
				metadata.author = it
			}
			details["description"]?.takeIf { it.isNotEmpty() }?.let {
				metadata.putDescription(language, it)
			}
		}
	}

	private fun normalizeLanguageKey(language: String?): String? {
		return language?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
	}

}