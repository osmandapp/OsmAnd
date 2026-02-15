package net.osmand.shared.palette.data

import kotlinx.datetime.Clock
import net.osmand.shared.ColorPalette
import net.osmand.shared.palette.domain.GradientPoint
import net.osmand.shared.palette.domain.GradientProperties
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteConstants.ALTITUDE_DEFAULT_NAME
import net.osmand.shared.palette.domain.PaletteConstants.DEFAULT_NAME
import net.osmand.shared.palette.domain.filetype.PaletteFileType
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.palette.domain.PaletteItemSource
import net.osmand.shared.palette.domain.filetype.GradientFileType
import net.osmand.shared.palette.domain.filetype.PaletteFileTypeRegistry
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.NamingUtils
import kotlin.random.Random

object PaletteUtils {

	private const val TXT_EXT = ".txt"
	private const val CUSTOM_NAME = "custom"

	fun buildFileName(paletteName: String, fileType: PaletteFileType): String {
		return "${fileType.filePrefix}${paletteName}${TXT_EXT}"
	}

	fun buildDisplayName(paletteName: String): String {
		return KAlgorithms.capitalizeFirstLetter(paletteName.replace("_", " "))!!
	}

	fun getPaletteNameFromDisplayName(displayName: String): String {
		return displayName.replace(" ", "_")
	}

	fun extractPaletteName(fileName: String): String? {
		val fileType = PaletteFileTypeRegistry.fromFileName(fileName) ?: return null
		return fileName.replace(fileType.filePrefix, "").replace(TXT_EXT, "")
	}

	fun isDefaultPalette(paletteName: String): Boolean {
		return paletteName in setOf(ALTITUDE_DEFAULT_NAME, DEFAULT_NAME)
	}

	private fun generateUniquePaletteName(existingIds: Set<String>, baseId: String): String {
		return NamingUtils.generateUniqueName(existingIds, baseId, "_")
	}

	fun generateSolidUniqueId(existingIds: Set<String>): String {
		var newId: Int
		do {
			newId = Random.nextInt(100000, Int.MAX_VALUE)
		} while (existingIds.contains(newId.toString()))
		return newId.toString()
	}

	fun isPaletteFileExt(fileName: String): Boolean = fileName.endsWith(TXT_EXT)

	fun renameGradientPalette(item: PaletteItem.Gradient, newName: String): PaletteItem.Gradient {
		val fileType = item.properties.fileType
		val paletteName = getPaletteNameFromDisplayName(newName)

		return item.copy(
			id = paletteName,
			displayName = newName,
			source = item.source.copy(
				fileName = buildFileName(paletteName, fileType)
			)
		)
	}

	fun createGradientColor(
		palette: Palette.GradientCollection,
		fileType: GradientFileType,
		points: List<GradientPoint>
	): PaletteItem.Gradient {

		val existingIds = palette.items.map { it.id }.toSet()
		val paletteName = generateUniquePaletteName(existingIds, CUSTOM_NAME)
		val displayName = buildDisplayName(paletteName)
		val newFileName = buildFileName(paletteName, fileType)

		return PaletteItem.Gradient(
			id = paletteName,
			displayName = displayName,
			source = PaletteItemSource.GradientFile(palette.id, newFileName),
			isDefault = false,
			isEditable = true,
			historyIndex = 0,
			lastUsedTime = 0,
			points = points,
			properties = GradientProperties(
				fileType = fileType,
				rangeType = fileType.rangeType
			)
		)
	}

	fun createGradientDuplicate(
		palette: Palette.GradientCollection,
		originalItemId: String
	): PaletteItem.Gradient? {
		val index = palette.items.indexOfFirst { it.id == originalItemId }
		if (index == -1) return null

		val originalItem = palette.items[index]
		val fileType = originalItem.properties.fileType

		// 1. Generate Unique ID / Name
		// In gradients, ID is the filename with extension
		val existingIds = palette.items.map { it.id }.toSet()
		val paletteName = generateUniquePaletteName(existingIds, originalItem.id)
		val newFileName = buildFileName(paletteName, fileType)
		val displayName = buildDisplayName(paletteName)

		// 2. Create new Item
		return originalItem.copy(
			id = paletteName,
			displayName = displayName,
			source = PaletteItemSource.GradientFile(palette.id, newFileName),
			isDefault = false,
			isEditable = true
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
			source = PaletteItemSource.CollectionRecord(palette.id),
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
			source = PaletteItemSource.CollectionRecord(palette.id),
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