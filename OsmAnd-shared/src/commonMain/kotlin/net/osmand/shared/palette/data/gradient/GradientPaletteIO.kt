package net.osmand.shared.palette.data.gradient

import kotlinx.datetime.Clock
import net.osmand.shared.ColorPalette
import net.osmand.shared.io.KFile
import net.osmand.shared.palette.data.PaletteIO
import net.osmand.shared.palette.data.PaletteUtils
import net.osmand.shared.palette.domain.*
import net.osmand.shared.palette.domain.category.GradientPaletteCategory
import net.osmand.shared.palette.domain.filetype.GradientFileType
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil
import okio.buffer
import okio.use

object GradientPaletteIO : PaletteIO<Palette.GradientCollection> {

	private val LOG = LoggerFactory.getLogger("GradientPaletteIO")
	private const val COMMENT_PREFIX = "#"
	private const val HEADER = "# Value,R,G,B,A"

	private val settingsHelper = GradientSettingsHelper()

	private val paletteDirectory: KFile
		get() = PlatformUtil.getOsmAndContext().getColorPaletteDir()

	override fun sync(
		oldPalette: Palette.GradientCollection?,
		newPalette: Palette.GradientCollection
	) {
		val oldItemsMap = oldPalette?.items?.associateBy { it.id } ?: emptyMap()
		val newItems = newPalette.items

		// 1. File System Sync (Write/Update)
		for (newItem in newItems) {
			val oldItem = oldItemsMap[newItem.id]

			if (oldItem == null || newItem != oldItem) {
				writeItem(newItem)
			}
		}

		// 2. File System Sync (Delete)
		if (oldPalette != null) {
			val newIds = newItems.map { it.id }.toSet()
			for (oldItem in oldPalette.items) {
				if (oldItem.id !in newIds) {
					val file = KFile(paletteDirectory, oldItem.source.fileName)
					if (file.exists()) {
						file.delete()
					}
				}
			}
		}

		// 3. Settings Sync (JSON)
		saveSettings(newPalette)
	}

	override fun read(paletteId: String): Palette.GradientCollection? {
		val category = GradientPaletteCategory.fromKey(paletteId)

		val targetCategory = category ?: GradientFileType.fromFileName(paletteId)?.category

		if (targetCategory == null) {
			return null
		}

		return readCollection(targetCategory)
	}

	private fun readCollection(category: GradientPaletteCategory): Palette.GradientCollection {

		val items = ArrayList<PaletteItem.Gradient>()

		// 1. Get metadata from settings (history, names)
		val settingsItems = settingsHelper.getItems(category.id)
		val settingsMap = settingsItems.associateBy { it.paletteName }

		val baseTime = Clock.System.now().toEpochMilliseconds()

		// 2. Scan directory
		val files = paletteDirectory.listFiles() ?: emptyList()

		for (file in files) {
			val fileName = file.name()

			// 3. Filter files by category
			val fileType = GradientFileType.fromFileName(fileName)
			if (fileType?.category == category && PaletteUtils.isPaletteFileExt(fileName)) {

				// Extract the ID used in settings (filename contains extension)
				val paletteName = PaletteUtils.extractPaletteName(fileName)
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
			id = category.id,
			displayName = category.getDisplayName(), // TODO: fix it
			category = category,
			items = items.sortedBy { it.historyIndex },
			isEditable = category.editable
		)
	}

	/**
	 * Reads a single gradient file and converts it into a PaletteItem.Gradient.
	 */
	private fun readItem(
		file: KFile,
		fileType: GradientFileType,
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
		val paletteId = fileType.category.id
		val paletteName = PaletteUtils.extractPaletteName(fileName) ?: return null
		val displayName = PaletteUtils.extractDisplayName(fileName) ?: return null

		// Use index from settings or last modified time (if new file)
		// TODO: don't use modification time as an history index
		val historyIndex = settingsItem?.index ?: file.lastModified().toInt()

		val properties = GradientProperties(
			fileType = fileType,
			rangeType = fileType.rangeType,
			// TODO: name to write in properties (implement in the future)
			name = null,
			comments = comments,
			unrecognized = unrecognized
		)

		return PaletteItem.Gradient(
			id = fileName,
			paletteName = paletteName,
			displayName = displayName,
			source = PaletteItemSource.GradientFile(paletteId, fileName),
			isDefault = PaletteUtils.isDefaultPalette(paletteName),
			historyIndex = historyIndex,
			lastUsedTime = lastUsedTime,
			points = points,
			properties = properties
		)
	}

	private fun writeItem(item: PaletteItem.Gradient) {
		// Ensure filename matches the convention: <type_prefix> + <palette_name> + .txt
		// e.g. "route_speed_" + "default" + ".txt"
		val fileName = item.source.fileName

		val file = KFile(paletteDirectory, fileName)

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

	private fun saveSettings(palette: Palette.GradientCollection) {
		val originalOrder = palette.items
		val lastUsedOrder = originalOrder.sortedByDescending { it.lastUsedTime }
		val settingsItems = mutableListOf<GradientSettingsItem>()
		lastUsedOrder.forEach { item ->
			settingsItems.add(
				GradientSettingsItem(
					typeName = item.properties.fileType.category.id,
					paletteName = item.paletteName,
					index = originalOrder.indexOf(item) + 1
				)
			)
		}
		settingsHelper.saveItems(palette.category.id, settingsItems)
	}
}