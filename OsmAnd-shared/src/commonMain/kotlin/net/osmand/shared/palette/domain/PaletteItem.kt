package net.osmand.shared.palette.domain

import net.osmand.shared.ColorPalette
import net.osmand.shared.palette.domain.filetype.GradientFileType

sealed interface PaletteItemSource {
	val paletteId: String

	data class GradientFile(
		override val paletteId: String,
		val fileName: String             // e.g. "route_speed_default.txt"
	) : PaletteItemSource

	data class CollectionRecord(
		override val paletteId: String
	) : PaletteItemSource
}

data class GradientProperties(
	val fileType: GradientFileType,
	val rangeType: GradientRangeType,
	val name: String? = null,
	val comments: List<String> = emptyList(),
	val unrecognized: Map<String, String> = emptyMap()
)

data class GradientPoint(
	val value: Float,
	val color: Int
) {
	fun toColorValue() = ColorPalette.ColorValue(value.toDouble(), color)
}

sealed interface PaletteItem {
	val id: String
	val displayName: String
	val source: PaletteItemSource
	val isEditable: Boolean
	val historyIndex: Int
	val lastUsedTime: Long

	data class Solid(
		override val id: String,
		override val displayName: String,
		override val source: PaletteItemSource.CollectionRecord,
		override val isEditable: Boolean = true,
		override val historyIndex: Int,
		override val lastUsedTime: Long = 0,
		val colorInt: Int
	) : PaletteItem {
		fun getColorValue() = ColorPalette.ColorValue(historyIndex.toDouble(), colorInt)
	}

	data class Gradient(
		override val id: String,
		override val displayName: String,
		override val source: PaletteItemSource.GradientFile,
		val isDefault: Boolean,
		override val isEditable: Boolean = !isDefault,
		override val historyIndex: Int,
		override val lastUsedTime: Long = 0,
		val points: List<GradientPoint>,
		val properties: GradientProperties
	) : PaletteItem {

		fun getPaletteCategory() = properties.fileType.category

		fun getColorPalette(): ColorPalette {
			val palette = ColorPalette()
			points.forEach { palette.colors.add(it.toColorValue()) }
			return palette
		}

		fun isFixed() = properties.fileType.rangeType == GradientRangeType.FIXED_VALUES
	}
}