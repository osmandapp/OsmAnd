package net.osmand.shared.palette.domain.filetype

import net.osmand.shared.palette.domain.PaletteConstants
import net.osmand.shared.palette.domain.category.SolidPaletteCategory

enum class SolidFileType(
	override val filePrefix: String,
	override val category: SolidPaletteCategory
): PaletteFileType {

	// --- User solid color palettes (Prefix: user_palette_*) ---

	USER_PALETTE(
		filePrefix = PaletteConstants.SOLID_PALETTE_PREFIX,
		category = SolidPaletteCategory.SOLID_COLOR_PALETTE
	);

	companion object {
		fun fromFileName(fileName: String): PaletteFileType? {
			return entries.find { fileName.startsWith(it.filePrefix) }
		}
	}
}