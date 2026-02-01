package net.osmand.shared.palette.domain.filetype

import net.osmand.shared.palette.domain.category.SolidPaletteCategory

enum class SolidFileType(
	override val filePrefix: String = "user_palette_",
	override val category: SolidPaletteCategory = SolidPaletteCategory.SOLID_COLOR_PALETTE
): PaletteFileType {

	// --- User solid color palettes (Prefix: user_palette_*) ---

	USER_PALETTE;

	companion object {
		fun fromFileName(fileName: String): PaletteFileType? {
			return entries.find { fileName.startsWith(it.filePrefix) }
		}
	}
}