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
	private const val SHORT_LINK_ZOOM = 15
	private const val SHORT_LINK_LENGTH = 10

	@OptIn(FormatStringsInDatetimeFormats::class)
	private val DATE_TIME_FORMATTER: DateTimeFormat<LocalDateTime> = LocalDateTime.Format {
		byUnicodePattern(DATE_TIME_PATTERN)
	}

	@JvmStatic
	fun createUniqueMediaFileName(
		extension: String,
		exists: (String) -> Boolean
	): String {
		return createUniqueMediaFileName(Double.NaN, Double.NaN, extension, exists)
	}

	@JvmStatic
	fun createUniqueMediaFileName(
		lat: Double,
		lon: Double,
		extension: String,
		exists: (String) -> Boolean
	): String {
		val normalizedExtension = MediaType.normalizeExtension(extension)
		val typeName = MediaType.fromExtension(normalizedExtension).typeName
		val dateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
		var baseName = typeName + "_" + DATE_TIME_FORMATTER.format(dateTime)
		if (KMapUtils.isValidLatLon(lat, lon)) {
			baseName += "_" + KMapUtils.createShortLinkString(lat, lon, SHORT_LINK_ZOOM)
		}
		return createUniqueFileName(baseName, normalizedExtension, '_', 0, exists)
	}

	private fun createUniqueFileName(
		baseName: String,
		extension: String,
		separator: Char,
		startSuffix: Int,
		exists: (String) -> Boolean
	): String {
		var suffix = startSuffix
		var fileName: String
		do {
			fileName = if (suffix == 0) {
				"$baseName.$extension"
			} else {
				"$baseName$separator$suffix.$extension"
			}
			suffix++
		} while (exists(fileName))
		return fileName
	}

	@JvmStatic
	fun createUniqueGeneratedMediaFileName(fileName: String, exists: (String) -> Boolean): String {
		if (!exists(fileName)) {
			return fileName
		}
		val parsedName = parseMediaFileName(fileName) ?: return fileName
		val extensionSeparator = fileName.lastIndexOf('.')
		val extension = fileName.substring(extensionSeparator + 1)
		val baseNameEnd = if (parsedName.suffix > 0) {
			fileName.lastIndexOf(parsedName.suffixSeparator, extensionSeparator - 1)
		} else {
			extensionSeparator
		}
		val baseName = fileName.substring(0, baseNameEnd)
		return createUniqueFileName(
			baseName, extension, parsedName.suffixSeparator, parsedName.suffix + 1, exists
		)
	}

	@JvmStatic
	fun isManagedMediaFileName(fileName: String): Boolean {
		return parseMediaFileName(fileName) != null
	}

	@JvmStatic
	fun getShortLink(fileName: String): String? {
		return parseMediaFileName(fileName)?.shortLink
	}

	@JvmStatic
	fun getDescription(fileName: String): String? {
		val parsedName = parseMediaFileName(fileName)
		if (parsedName == null || parsedName.descriptionEnd < 0) {
			return null
		}
		return fileName.substring(0, parsedName.descriptionEnd)
	}

	private fun parseMediaFileName(fileName: String): ParsedMediaFileName? {
		val mediaType = MediaType.fromFileName(fileName)
		if (mediaType == MediaType.UNKNOWN) {
			return null
		}
		val generatedName = parseGeneratedMediaFileName(fileName, mediaType)
		if (generatedName != null) {
			return generatedName
		}
		return parseLegacyMediaFileName(fileName)
	}

	private fun parseGeneratedMediaFileName(
		fileName: String,
		mediaType: MediaType
	): ParsedMediaFileName? {
		val prefix = "${mediaType.typeName}_"
		val prefixStart = fileName.lastIndexOf(prefix)
		if (prefixStart < 0 || (prefixStart > 0 && fileName[prefixStart - 1] != ' ')) {
			return null
		}
		val extensionSeparator = fileName.lastIndexOf('.')
		val dateStart = prefixStart + prefix.length
		val dateEnd = dateStart + DATE_TIME_PATTERN.length
		if (extensionSeparator < dateEnd) {
			return null
		}
		try {
			DATE_TIME_FORMATTER.parse(fileName.substring(dateStart, dateEnd))
		} catch (e: IllegalArgumentException) {
			return null
		}
		val descriptionEnd = prefixStart - 1
		val suffixSeparator = fileName.lastIndexOf('_', extensionSeparator - 1)
		val suffix = parseSuffix(fileName, suffixSeparator, extensionSeparator)
		val shortLinkEnd = if (suffix > 0) suffixSeparator else extensionSeparator
		if (shortLinkEnd == dateEnd) {
			return ParsedMediaFileName(null, descriptionEnd, suffix, '_')
		}
		val shortLinkStart = shortLinkEnd - SHORT_LINK_LENGTH
		if (shortLinkStart != dateEnd + 1 || fileName[dateEnd] != '_') {
			return null
		}
		val shortLink = fileName.substring(shortLinkStart, shortLinkEnd)
		return if (isShortLink(shortLink)) ParsedMediaFileName(shortLink, descriptionEnd, suffix, '_') else null
	}

	private fun parseLegacyMediaFileName(fileName: String): ParsedMediaFileName? {
		val extensionSeparator = fileName.lastIndexOf('.')
		var suffixSeparator = fileName.lastIndexOf('.', extensionSeparator - 1)
		var suffix = parseSuffix(fileName, suffixSeparator, extensionSeparator)
		if (suffix == 0) {
			suffixSeparator = fileName.lastIndexOf('-', extensionSeparator - 1)
			suffix = parseSuffix(fileName, suffixSeparator, extensionSeparator)
		}
		if (suffix == 0 || suffixSeparator < SHORT_LINK_LENGTH) {
			return null
		}
		val shortLinkStart = suffixSeparator - SHORT_LINK_LENGTH
		val shortLink = fileName.substring(shortLinkStart, suffixSeparator)
		val descriptionEnd = shortLinkStart - 1
		if (descriptionEnd >= 0) {
			val separator = fileName[descriptionEnd]
			if (separator != ' ' && separator != '_') {
				return null
			}
		}
		return if (isShortLink(shortLink)) {
			ParsedMediaFileName(shortLink, descriptionEnd, suffix, fileName[suffixSeparator])
		} else {
			null
		}
	}

	private fun parseSuffix(fileName: String, start: Int, end: Int): Int {
		if (start < 0) {
			return 0
		}
		val number = fileName.substring(start + 1, end).toIntOrNull()
		return if (number != null && number > 0) number else 0
	}

	private fun isShortLink(shortLink: String): Boolean {
		return shortLink.length == SHORT_LINK_LENGTH && shortLink.endsWith("--")
	}

	private class ParsedMediaFileName(
		val shortLink: String?,
		val descriptionEnd: Int,
		val suffix: Int,
		val suffixSeparator: Char
	)
}