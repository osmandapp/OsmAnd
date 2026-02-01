package net.osmand.shared.palette.data

import kotlinx.datetime.Clock
import net.osmand.shared.ColorPalette
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.filetype.PaletteFileType
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.palette.domain.PaletteItemSource
import net.osmand.shared.palette.domain.filetype.PaletteFileTypeRegistry
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.NamingUtils
import kotlin.random.Random

object PaletteUtils {

	private const val TXT_EXT = ".txt"
	private const val ALTITUDE_DEFAULT_NAME = "altitude_default"
	const val DEFAULT_NAME = "default"

	fun buildFileName(paletteName: String, fileType: PaletteFileType): String {
		return "${fileType.filePrefix}${paletteName}${TXT_EXT}"
	}

	fun extractDisplayName(fileName: String): String? {
		val paletteName = extractPaletteName(fileName) ?: return null
		return KAlgorithms.capitalizeFirstLetter(paletteName.replace("_", " "))
	}

	fun extractPaletteName(fileName: String): String? {
		val fileType = PaletteFileTypeRegistry.fromFileName(fileName) ?: return null
		return fileName.replace(fileType.filePrefix, "").replace(TXT_EXT, "")
	}

	fun isDefaultPalette(paletteName: String): Boolean {
		return paletteName in setOf(ALTITUDE_DEFAULT_NAME, DEFAULT_NAME)
	}

	private fun generateGradientUniqueFileName(existing: Set<String>, baseName: String): String {
		return NamingUtils.generateUniqueName(existing, baseName, "_")
	}

	fun generateSolidUniqueId(existingIds: Set<String>): String {
		var newId: Int
		do {
			newId = Random.nextInt(100000, Int.MAX_VALUE)
		} while (existingIds.contains(newId.toString()))
		return newId.toString()
	}

	fun isPaletteFileExt(fileName: String): Boolean = fileName.endsWith(TXT_EXT)

	// TODO: candidates to extraction (temporally placed methods)

	fun createGradientDuplicate(
		palette: Palette.GradientCollection,
		originalItemId: String
	): PaletteItem.Gradient? {
		val index = palette.items.indexOfFirst { it.id == originalItemId }
		if (index == -1) return null

		val originalItem = palette.items[index]

		// 1. Generate Unique ID / Name
		// In gradients, ID is the filename with extension
		val existingIds = palette.items.map { it.id }.toSet()
		val newFileName = generateGradientUniqueFileName(existingIds, originalItem.id)
		val paletteName = extractPaletteName(newFileName) ?: return null
		val displayName = extractDisplayName(newFileName) ?: return null

		// 2. Create new Item
		return originalItem.copy(
			id = newFileName,
			paletteName = paletteName,
			displayName = displayName,
			source = PaletteItemSource.GradientFile(palette.id, newFileName),
			isDefault = false
		)
	}

	fun createSolidDuplicate(
		palette: Palette.SolidCollection,
		originalItemId: String,
		markAsUsed: Boolean = false,
	): PaletteItem.Solid? {
		val index = palette.items.indexOfFirst { it.id == originalItemId }
		if (index == -1) return null

		val originalItem = palette.items[index]

		val existingIds = palette.items.map { it.id }.toSet()
		val newId = generateSolidUniqueId(existingIds)
		val lastUsedTime =
			if (markAsUsed) Clock.System.now().toEpochMilliseconds() else originalItem.lastUsedTime

		return originalItem.copy(
			id = newId,
			source = PaletteItemSource.CollectionRecord(palette.id, newId),
			lastUsedTime = lastUsedTime
		)
	}

	fun createSolidColor(
		palette: Palette.SolidCollection,
		colorInt: Int,
		markAsUsed: Boolean
	): PaletteItem.Solid {
		val existingIds = palette.items.map { it.id }.toSet()
		val newId = generateSolidUniqueId(existingIds)

		val lastUsedTime = if (markAsUsed) Clock.System.now().toEpochMilliseconds() else 0L
		val maxHistoryIndex = palette.items.maxOfOrNull { it.historyIndex } ?: 0

		return PaletteItem.Solid(
			id = newId,
			displayName = ColorPalette.colorToHex(colorInt),
			source = PaletteItemSource.CollectionRecord(palette.id, newId),
			colorInt = colorInt,
			historyIndex = maxHistoryIndex + 1,
			lastUsedTime = lastUsedTime
		)
	}

	fun updateSolidColor(
		originalItem: PaletteItem.Solid,
		newColorInt: Int
	): PaletteItem.Solid {
		return originalItem.copy(
			colorInt = newColorInt,
			displayName = ColorPalette.colorToHex(newColorInt)
		)
	}
}