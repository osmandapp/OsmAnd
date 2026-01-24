package net.osmand.shared.palette.domain

import net.osmand.shared.ColorPalette

sealed interface PaletteItemSource {
	val paletteId: String

	data class GradientFile(
		override val paletteId: String,
		val fileName: String // e.g. "route_speed_default.txt"
	) : PaletteItemSource

	data class CollectionRecord(
		override val paletteId: String,
		val recordId: String // TODO: do we need this at all?
	) : PaletteItemSource
}

data class GradientProperties(
	val category: GradientPaletteCategory,
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
		val color: Int
	) : PaletteItem {
		fun getColorValue() = ColorPalette.ColorValue(historyIndex.toDouble(), color) // TODO: use color id instead of index
	}

	data class Gradient(
		override val id: String,
		override val displayName: String,
		override val source: PaletteItemSource.GradientFile,
		override val isEditable: Boolean,
		override val historyIndex: Int,
		override val lastUsedTime: Long = 0,
		val points: List<GradientPoint>,
		val properties: GradientProperties
	) : PaletteItem {

		fun getColorPalette(): ColorPalette {
			val palette = ColorPalette()
			points.forEach { palette.colors.add(it.toColorValue()) }
			return palette
		}
	}
}