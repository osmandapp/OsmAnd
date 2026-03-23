package net.osmand.shared.palette.domain.filetype


object PaletteFileTypeRegistry {

	private val allTypes: List<PaletteFileType> by lazy {
		(GradientFileType.entries + SolidFileType.entries)
	}

	fun fromFileName(fileName: String): PaletteFileType? {
		return allTypes
			.sortedByDescending { it.filePrefix.length }
			.find { fileName.startsWith(it.filePrefix) }
	}
}