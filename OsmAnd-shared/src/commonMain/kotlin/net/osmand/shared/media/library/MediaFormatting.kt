package net.osmand.shared.media.library

object MediaFormatting {
	fun duration(ms: Long): String {
		val seconds = ms.coerceAtLeast(0) / 1000
		val minutes = seconds / 60
		val tail = (seconds % 60).toString().padStart(2, '0')
		return if (minutes < 60) "${minutes.toString().padStart(2, '0')}:$tail"
		else "${minutes / 60}:${(minutes % 60).toString().padStart(2, '0')}:$tail"
	}

	fun resolution(width: Int?, height: Int?): String? =
		if (width != null && height != null && width > 0 && height > 0) "$width × $height" else null

	fun formatLabel(fileName: String?, mime: String? = null): String? {
		val extension = fileName?.substringBefore('?')?.substringBefore('#')
			?.substringAfterLast('/')?.substringAfterLast('.', "")?.lowercase().orEmpty()
		val label = extension.ifEmpty {
			when (mime?.substringBefore(';')?.trim()?.lowercase()) {
				"image/jpeg" -> "jpeg"
				"image/png" -> "png"
				"image/heic" -> "heic"
				"video/mp4", "audio/mp4" -> "mp4"
				"video/3gpp", "audio/3gpp" -> "3gp"
				"audio/aac", "audio/aacp" -> "aac"
				"audio/mpeg" -> "mp3"
				else -> ""
			}
		}
		return when (label) {
			"" -> null
			"jpg" -> "JPEG"
			"3gpp", "3ga" -> "3GP"
			else -> label.uppercase()
		}
	}

	/** Composition is independent of Android date/size/distance localization. */
	fun secondLine(mode: MediaLibrarySortMode, date: String?, size: String?, duration: String?, distance: String? = null): String? {
		val parts = when (mode.group) {
			MediaLibrarySortMode.Group.SIZE -> listOf(size, date, duration)
			MediaLibrarySortMode.Group.DURATION -> listOf(duration, date, size)
			else -> listOf(date, size, duration)
		}.filterNot { it.isNullOrEmpty() }.joinToString(" • ")
		return listOfNotNull(distance?.takeIf { mode == MediaLibrarySortMode.NEAREST && it.isNotEmpty() },
			parts.takeIf { it.isNotEmpty() }).joinToString(" | ").takeIf { it.isNotEmpty() }
	}
}
