package net.osmand.shared.palette.domain.filetype

import net.osmand.shared.palette.domain.category.PaletteCategory

interface PaletteFileType {
	val category: PaletteCategory         // Related palette category
	val filePrefix: String                // Exact file prefix on disk
}