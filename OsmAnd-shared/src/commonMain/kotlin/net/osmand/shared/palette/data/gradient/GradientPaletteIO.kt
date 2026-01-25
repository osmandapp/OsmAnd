package net.osmand.shared.palette.data.gradient

import kotlinx.datetime.Clock
import net.osmand.shared.ColorPalette
import net.osmand.shared.io.KFile
import net.osmand.shared.palette.domain.*
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.LoggerFactory
import okio.buffer
import okio.use

object GradientPaletteIO {

	private val LOG = LoggerFactory.getLogger("GradientPaletteIO")
	private const val TXT_EXT = ".txt"
	private const val COMMENT_PREFIX = "#"
	private const val HEADER = "# Value,R,G,B,A"
	private const val ALTITUDE_DEFAULT_NAME = "altitude_default"
	private const val DEFAULT_NAME = "default"

	fun readCollection(
		directory: KFile,
		category: PaletteCategory,
		settingsHelper: GradientSettingsHelper
	): Palette.GradientCollection {

		val items = ArrayList<PaletteItem.Gradient>()

		// 1. Get metadata from settings (history, names)
		val settingsItems = settingsHelper.getItems(category.key)
		val settingsMap = settingsItems.associateBy { it.paletteName }

		val baseTime = Clock.System.now().toEpochMilliseconds()

		// 2. Scan directory
		val files = directory.listFiles() ?: emptyList()

		for (file in files) {
			val fileName = file.name()

			// 3. Filter files by category
			val fileType = PaletteFileType.fromFileName(fileName)
			if (fileType?.category == category && fileName.endsWith(TXT_EXT)) {

				// Extract the ID used in settings (filename contains extension)
				val paletteName = extractPaletteName(fileName)
				val metadata = settingsMap[paletteName]

				val indexInList = settingsItems.indexOf(metadata)
				val lastUsedTime = if (indexInList >= 0) baseTime - (indexInList * 1000L) else 0
				val item = readItem(file, fileType, metadata, lastUsedTime)
				if (item != null) {
					items.add(item)
				}
			}
		}

		return Palette.GradientCollection(
			id = category.key,
			displayName = category.displayName, // TODO: fix it
			category = category,
			items = items.sortedBy { it.historyIndex },
			isEditable = category.isEditable
		)
	}

	/**
	 * Reads a single gradient file and converts it into a PaletteItem.Gradient.
	 */
	fun readItem(
		file: KFile,
		fileType: PaletteFileType,
		settingsItem: GradientSettingsItem?,
		lastUsedTime: Long
	): PaletteItem.Gradient? {
		if (!file.exists()) return null

		// 1. Read gradient file
		val points = mutableListOf<GradientPoint>()
		val comments = mutableListOf<String>()
		val unrecognized = mutableMapOf<String, String>()

		try {
			file.source().buffer().use { source ->
				while (true) {
					val line = source.readUtf8Line() ?: break
					val trimmed = line.trim()

					if (trimmed.isEmpty()) continue

					// A) Handle Comments
					if (trimmed.startsWith(COMMENT_PREFIX)) {
						comments.add(trimmed)
						continue
					}

					// B) Handle Points using shared logic
					val colorValue = ColorPalette.parseColorValue(trimmed)

					if (colorValue != null) {
						points.add(GradientPoint(
							value = colorValue.value.toFloat(),
							color = colorValue.clr
						))
					} else {
						// C) Handle Unrecognized lines
						unrecognized[trimmed] = trimmed
					}
				}
			}
		} catch (e: Exception) {
			LOG.error("Failed to read gradient file: ${file.path}", e)
			return null
		}

		if (points.isEmpty()) {
			return null
		}

		// 2. Build a gradient item
		val fileName = file.name()
		val paletteId = fileType.category.key
		val paletteName = extractPaletteName(fileName) ?: return null
		val displayName = KAlgorithms.capitalizeFirstLetter(paletteName.replace("_", " "))

		// Use index from settings or last modified time (if new file)
		// TODO: don't use modification time as history index
		val historyIndex = settingsItem?.index ?: file.lastModified().toInt()

		val properties = GradientProperties(
			fileType = fileType,
			// TODO: improve it
			rangeType = if (fileType.useFixedValues) GradientRangeType.FIXED_VALUES else GradientRangeType.RELATIVE,
			// TODO: name to write in properties (implement in the future)
			name = null,
			comments = comments,
			unrecognized = unrecognized
		)

		return PaletteItem.Gradient(
			id = fileName,
			paletteName = paletteName,
			displayName = displayName ?: paletteName,
			source = PaletteItemSource.GradientFile(paletteId, fileName),
			isDefault = checkIsDefault(paletteName),
			historyIndex = historyIndex,
			lastUsedTime = lastUsedTime,
			points = points,
			properties = properties
		)
	}

	fun writeItem(
		directory: KFile,
		item: PaletteItem.Gradient
	) {
		// Ensure filename matches the convention: <type_prefix> + <palette_name> + .txt
		// e.g. "route_speed_" + "default" + ".txt"
		val fileName = item.source.fileName

		val file = KFile(directory, fileName)

		val content = buildString {
			// 1. Write Comments/Header
			if (item.properties.comments.isNotEmpty()) {
				item.properties.comments.forEach { comment ->
					append(comment)
					append("\n")
				}
			} else {
				append(HEADER)
				append("\n")
			}

			// 2. Write Points
			item.points.forEach { point ->
				val cv = point.toColorValue()
				append(ColorPalette.formatColorValue(cv))
				append("\n")
			}
		}

		try {
			val parent = file.getParentFile()
			if (parent != null && !parent.exists()) {
				parent.createDirectories()
			}
			file.writeText(content)
		} catch (e: Exception) {
			LOG.error("Failed to write gradient item: ${file.path}", e)
		}
	}

	// --- Public helpers ---

	fun extractPaletteName(fileName: String): String? {
		return PaletteFileType.fromFileName(fileName)?.let {
			fileName.replace(it.filePrefix, "").replace(TXT_EXT, "")
		}
	}

	// --- Internal helpers ---

	private fun checkIsDefault(paletteName: String): Boolean {
		// TODO: is it really enough
		return paletteName == ALTITUDE_DEFAULT_NAME || paletteName == DEFAULT_NAME
	}
}