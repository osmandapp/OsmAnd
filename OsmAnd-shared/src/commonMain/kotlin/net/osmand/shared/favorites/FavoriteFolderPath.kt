package net.osmand.shared.favorites

import kotlin.jvm.JvmStatic

object FavoriteFolderPath {

	const val DELIMITER = "/"
	const val SUBFOLDER_PLACEHOLDER = "_%_"

	private const val DELIMITER_CHAR = '/'

	@JvmStatic
	fun split(fullPath: String?): List<String> {
		if (fullPath.isNullOrEmpty()) {
			return emptyList()
		}
		return fullPath.split(DELIMITER_CHAR).filter { it.isNotEmpty() }
	}

	@JvmStatic
	fun join(segments: List<String>): String {
		return segments.filter { it.isNotEmpty() }.joinToString(DELIMITER)
	}

	@JvmStatic
	fun parentPath(fullPath: String?): String {
		if (fullPath.isNullOrEmpty()) {
			return ""
		}
		val end = trimTrailingDelimiters(fullPath)
		if (end <= 0) {
			return ""
		}
		val delimiterIndex = fullPath.lastIndexOf(DELIMITER_CHAR, end - 1)
		if (delimiterIndex < 0) {
			return ""
		}
		val parentPath = fullPath.substring(0, delimiterIndex)
		return if (isNormalizedPath(parentPath)) parentPath else join(split(parentPath))
	}

	@JvmStatic
	fun lastSegment(fullPath: String?): String {
		if (fullPath.isNullOrEmpty()) {
			return ""
		}
		val end = trimTrailingDelimiters(fullPath)
		if (end <= 0) {
			return ""
		}
		val delimiterIndex = fullPath.lastIndexOf(DELIMITER_CHAR, end - 1)
		return fullPath.substring(delimiterIndex + 1, end)
	}

	@JvmStatic
	fun isDescendantOrSelf(path: String?, ancestorPath: String?): Boolean {
		val normalizedPath = path ?: ""
		val normalizedAncestor = ancestorPath ?: ""
		return normalizedAncestor.isEmpty()
				|| normalizedPath == normalizedAncestor
				|| normalizedPath.startsWith(normalizedAncestor + DELIMITER)
	}

	@JvmStatic
	fun replacePathPrefix(path: String, oldPrefix: String, newPrefix: String): String {
		if (!isDescendantOrSelf(path, oldPrefix)) {
			return path
		}
		if (path == oldPrefix) {
			return newPrefix
		}
		val suffix = if (oldPrefix.isEmpty()) path else path.substring(oldPrefix.length + DELIMITER.length)
		if (newPrefix.isEmpty()) {
			return suffix
		}
		return newPrefix + DELIMITER + suffix
	}

	@JvmStatic
	fun isValidFullPath(fullPath: String?): Boolean {
		if (fullPath == null) {
			return false
		}
		if (fullPath.isEmpty()) {
			return true
		}
		return fullPath.split(DELIMITER_CHAR).all { isValidSegment(it) }
	}

	@JvmStatic
	fun isValidSegment(segment: String?): Boolean {
		return !segment.isNullOrEmpty()
				&& !segment.contains(DELIMITER)
				&& !segment.contains(SUBFOLDER_PLACEHOLDER)
	}

	@JvmStatic
	fun requireValidFullPath(fullPath: String) {
		require(isValidFullPath(fullPath)) { "Invalid favorite folder path: $fullPath" }
	}

	private fun trimTrailingDelimiters(fullPath: String): Int {
		var end = fullPath.length
		while (end > 0 && fullPath[end - 1] == DELIMITER_CHAR) {
			end--
		}
		return end
	}

	private fun isNormalizedPath(fullPath: String): Boolean {
		return fullPath.isEmpty()
				|| (!fullPath.startsWith(DELIMITER)
				&& !fullPath.endsWith(DELIMITER)
				&& !fullPath.contains(DELIMITER + DELIMITER))
	}
}
