package net.osmand.shared.media

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import net.osmand.shared.media.domain.MediaType
import net.osmand.shared.util.KMapUtils
import kotlin.jvm.JvmStatic

object MediaFileNameFormat {

	const val IMG_EXTENSION = "jpg"
	const val MPEG4_EXTENSION = "mp4"
	const val THREEGP_EXTENSION = "3gp"

	private const val DATE_TIME_PATTERN = "yyyy-MM-dd_HH-mm-ss"

	@OptIn(FormatStringsInDatetimeFormats::class)
	private val DATE_TIME_FORMATTER: DateTimeFormat<LocalDateTime> = LocalDateTime.Format {
		byUnicodePattern(DATE_TIME_PATTERN)
	}

	@JvmStatic
	fun createUniqueMediaFileName(
		extension: String,
		exists: (String) -> Boolean
	): String {
		val normalizedExtension = MediaType.normalizeExtension(extension)
		val typeName = MediaType.fromExtension(normalizedExtension).typeName
		val dateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
		return createUniqueDateTimeMediaFileName(typeName, dateTime, normalizedExtension, 0, exists)
	}

	private fun createUniqueDateTimeMediaFileName(typeName: String, dateTime: LocalDateTime, extension: String,
	                                              startSuffix: Int, exists: (String) -> Boolean): String {
		val normalizedExtension = MediaType.normalizeExtension(extension)
		val baseName = "${typeName}_${DATE_TIME_FORMATTER.format(dateTime)}"
		var suffix = startSuffix
		var fileName: String
		do {
			fileName = if (suffix == 0) {
				"$baseName.$normalizedExtension"
			} else {
				"${baseName}_$suffix.$normalizedExtension"
			}
			suffix++
		} while (exists(fileName))
		return fileName
	}

	@JvmStatic
	fun createUniqueLegacyMediaFileName(
		lat: Double,
		lon: Double,
		extension: String,
		exists: (String) -> Boolean
	): String {
		val basename = KMapUtils.createShortLinkString(lat, lon, 15)
		val normalizedExtension = MediaType.normalizeExtension(extension)
		var index = 1
		var fileName: String
		do {
			fileName = "$basename.${index++}.$normalizedExtension"
		} while (exists(fileName))
		return fileName
	}

	@JvmStatic
	fun createUniqueGeneratedMediaFileName(fileName: String, exists: (String) -> Boolean): String {
		if (!exists(fileName) || !isManagedMediaFileName(fileName)) {
			return fileName
		}
		val extension = getFileNameExtension(fileName)
		val mediaType = MediaType.fromExtension(extension)
		val mediaName = parseNewGeneratedMediaFileName(fileName, mediaType)
		if (mediaName != null) {
			val (dateTime, suffix) = mediaName
			return createUniqueDateTimeMediaFileName(mediaType.typeName, dateTime, extension, suffix, exists)
		}
		val extensionSeparator = fileName.lastIndexOf('.')
		val indexSeparator = fileName.lastIndexOf('.', extensionSeparator - 1)
		val prefix = fileName.substring(0, indexSeparator)
		var index = fileName.substring(indexSeparator + 1, extensionSeparator).toIntOrNull() ?: 1
		var candidate: String
		do {
			candidate = "$prefix.${++index}.$extension"
		} while (exists(candidate))
		return candidate
	}

	@JvmStatic
	fun isManagedMediaFileName(fileName: String): Boolean {
		val extension = getFileNameExtension(fileName)
		val mediaType = MediaType.fromExtension(extension)
		val extensionSeparator = fileName.lastIndexOf('.')

		if (extensionSeparator <= 0 || mediaType == MediaType.UNKNOWN) {
			return false
		}
		return isNewGeneratedMediaFileName(fileName) || isLegacyGeneratedMediaFileName(fileName, extensionSeparator)
	}

	@JvmStatic
	fun isNewGeneratedMediaFileName(fileName: String): Boolean {
		val extension = getFileNameExtension(fileName)
		val mediaType = MediaType.fromExtension(extension)
		return mediaType != MediaType.UNKNOWN && parseNewGeneratedMediaFileName(fileName, mediaType) != null
	}

	private fun parseNewGeneratedMediaFileName(fileName: String, mediaType: MediaType): Pair<LocalDateTime, Int>? {
		val prefix = "${mediaType.typeName}_"
		val extensionSeparator = fileName.lastIndexOf('.')
		if (extensionSeparator < prefix.length + DATE_TIME_PATTERN.length || !fileName.startsWith(prefix)) {
			return null
		}
		val name = fileName.substring(prefix.length, extensionSeparator)
		val suffix = name.substring(DATE_TIME_PATTERN.length)
		val suffixIndex = when {
			suffix.isEmpty() -> 0
			suffix.startsWith("_") -> suffix.substring(1).toIntOrNull()?.takeIf { it > 0 } ?: return null
			else -> return null
		}
		return runCatching {
			DATE_TIME_FORMATTER.parse(name.substring(0, DATE_TIME_PATTERN.length)) to suffixIndex
		}.getOrNull()
	}

	private fun isLegacyGeneratedMediaFileName(fileName: String, extensionSeparator: Int): Boolean {
		val indexSeparator = fileName.lastIndexOf('.', extensionSeparator - 1)
		if (indexSeparator <= 0) {
			return false
		}
		val index = fileName.substring(indexSeparator + 1, extensionSeparator)
		if ((index.toIntOrNull() ?: -1) < 1) {
			return false
		}
		var shortLink = fileName.substring(0, indexSeparator)
		val nameSeparator = shortLink.lastIndexOf(' ')
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
		val index = fileName.lastIndexOf('.')
		return if (index >= 0 && index + 1 < fileName.length) {
			fileName.substring(index + 1)
		} else {
			fileName
		}
	}
}