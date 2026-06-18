package net.osmand.shared.media

import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.util.KMapUtils
import kotlin.jvm.JvmStatic

object MediaFileNameFormat {

	const val IMG_EXTENSION = "jpg"
	const val MPEG4_EXTENSION = "mp4"
	const val THREEGP_EXTENSION = "3gp"

	private const val PART_SEPARATOR = '.'
	private const val DISPLAY_NAME_SEPARATOR = ' '

	@JvmStatic
	fun createUniqueMediaFileName(
		lat: Double,
		lon: Double,
		extension: String,
		exists: (String) -> Boolean
	): String {
		val basename = KMapUtils.createShortLinkString(lat, lon, 15)
		var index = 1
		var fileName: String
		do {
			fileName = buildFileName(basename, index++, extension)
		} while (exists(fileName))
		return fileName
	}

	@JvmStatic
	fun createUniqueGeneratedMediaFileName(fileName: String, exists: (String) -> Boolean): String {
		if (!exists(fileName) || !isGeneratedMediaFileName(fileName)) {
			return fileName
		}
		val extensionSeparator = fileName.lastIndexOf(PART_SEPARATOR)
		val indexSeparator = fileName.lastIndexOf(PART_SEPARATOR, extensionSeparator - 1)
		val prefix = fileName.substring(0, indexSeparator)
		val extension = fileName.substring(extensionSeparator + 1)
		var index = fileName.substring(indexSeparator + 1, extensionSeparator).toIntOrNull() ?: 1
		var candidate: String
		do {
			candidate = buildFileName(prefix, ++index, extension)
		} while (exists(candidate))
		return candidate
	}

	@JvmStatic
	fun isGeneratedMediaFileName(fileName: String): Boolean {
		val extensionSeparator = fileName.lastIndexOf(PART_SEPARATOR)
		val extension = getFileNameExtension(fileName)
		if (extensionSeparator <= 0 || !MediaType.isSupportedExtension(extension)) {
			return false
		}
		val indexSeparator = fileName.lastIndexOf(PART_SEPARATOR, extensionSeparator - 1)
		if (indexSeparator <= 0) {
			return false
		}
		val index = fileName.substring(indexSeparator + 1, extensionSeparator)
		if ((index.toIntOrNull() ?: -1) < 1) {
			return false
		}
		var shortLink = fileName.substring(0, indexSeparator)
		val nameSeparator = shortLink.lastIndexOf(DISPLAY_NAME_SEPARATOR)
		if (nameSeparator >= 0) {
			shortLink = shortLink.substring(nameSeparator + 1)
		}
		return isShortLinkString(shortLink)
	}

	@JvmStatic
	fun isShortLinkString(shortLink: String): Boolean {
		if (shortLink.isEmpty()) {
			return false
		}
		return shortLink.all { c ->
			c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '_' || c == '~' || c == '-'
		}
	}

	private fun getFileNameExtension(fileName: String): String {
		val index = fileName.lastIndexOf(PART_SEPARATOR)
		return if (index >= 0 && index + 1 < fileName.length) {
			fileName.substring(index + 1)
		} else {
			fileName
		}
	}

	private fun buildFileName(prefix: String, index: Int, extension: String): String {
		return prefix + PART_SEPARATOR + index + PART_SEPARATOR + extension
	}
}