package net.osmand.shared.util

object NamingUtils {

	/**
	 * Generates a new name based on the pattern "Name (N)".
	 * Examples:
	 * "MyRoute" -> "MyRoute (2)"
	 * "MyRoute (2)" -> "MyRoute (3)"
	 * "My.File.txt" -> "My.File (2).txt" (preserves extension if present)
	 */
	private fun getNextName(
		fileName: String,
		delimiter: String
	): String {
		val extensionIndex = fileName.lastIndexOf('.')
		val name: String
		val extension: String

		if (extensionIndex != -1) {
			name = fileName.substring(0, extensionIndex)
			extension = fileName.substring(extensionIndex)
		} else {
			name = fileName
			extension = ""
		}

		// Regex to find " (digits)" at the end of the name
		val regex = Regex("""$delimiter\((\d+)\)$""")
		val match = regex.find(name)

		val newBaseName = if (match != null) {
			// Found existing counter (e.g., "Name (2)")
			val numberStr = match.groupValues[1]
			val number = numberStr.toIntOrNull() ?: 1
			// Replace the old number with incremented one
			name.replaceRange(match.range, "$delimiter(${number + 1})")
		} else {
			// No counter found, append " (2)"
			"$name$delimiter(2)"
		}

		return newBaseName + extension
	}

	/**
	 * Generates a unique name ensuring it doesn't exist in the provided set.
	 * Starts by incrementing the original.
	 */
	fun generateUniqueName(
		existingNames: Set<String>,
		originalName: String,
		delimiter: String = " "
	): String {
		var candidate = originalName

		// Loop until we find a free name
		// e.g., if "Name (2)" exists, try "Name (3)"
		while (existingNames.contains(candidate)) {
			candidate = getNextName(candidate, delimiter)
		}
		return candidate
	}
}