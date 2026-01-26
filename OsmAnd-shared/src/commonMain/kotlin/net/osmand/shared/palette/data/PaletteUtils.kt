package net.osmand.shared.palette.data

import net.osmand.shared.palette.domain.PaletteFileType
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.NamingUtils

object PaletteUtils {

	private const val TXT_EXT = ".txt"
	private const val ALTITUDE_DEFAULT_NAME = "altitude_default"
	private const val DEFAULT_NAME = "default"

	fun buildFileName(paletteName: String, fileType: PaletteFileType): String {
		return "${fileType.filePrefix}${paletteName}${TXT_EXT}"
	}

	fun extractDisplayName(fileName: String): String? {
		val paletteName = extractPaletteName(fileName) ?: return null
		return KAlgorithms.capitalizeFirstLetter(paletteName.replace("_", " "))
	}

	fun extractPaletteName(fileName: String): String? {
		val fileType = PaletteFileType.fromFileName(fileName) ?: return null
		return fileName.replace(fileType.filePrefix, "").replace(TXT_EXT, "")
	}

	fun isDefaultPalette(paletteName: String): Boolean {
		return paletteName in setOf(ALTITUDE_DEFAULT_NAME, DEFAULT_NAME)
	}

	fun generateUniqueFileName(existingFileNames: Set<String>, baseName: String): String {
		return NamingUtils.generateUniqueName(existingFileNames, baseName)
	}

	fun isPaletteFileExt(fileName: String): Boolean = fileName.endsWith(TXT_EXT)
}