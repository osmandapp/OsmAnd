package net.osmand.plus.palette.domain

import net.osmand.shared.ColorPalette

sealed interface PaletteItemSource {
	val paletteId: String

	data class GradientFile(
		override val paletteId: String,
		val fileName: String // e.g. "route_speed_default.txt"
	) : PaletteItemSource

	data class CollectionRecord(
		override val paletteId: String,
		val recordId: String
	) : PaletteItemSource
}

data class GradientProperties(
	val category: GradientPaletteCategory,
	val rangeType: GradientRangeType,
	val name: String? = null,

	val otherProperties: Map<String, String> = emptyMap(),
	val comments: List<String> = emptyList()
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
	val lastUsedTime: Long

	data class Solid(
		override val id: String,
		override val displayName: String,
		override val source: PaletteItemSource.CollectionRecord,
		override val isEditable: Boolean = true,
		override val lastUsedTime: Long = 0,
		val color: Int
	) : PaletteItem

	data class Gradient(
		override val id: String,
		override val displayName: String,
		override val source: PaletteItemSource.GradientFile,
		override val isEditable: Boolean,
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