package net.osmand.plus.palette.domain

/**
 * Represents a grouped collection of palette items.
 * Maps to a folder (for gradients) or a file (for solid colors).
 */
sealed class Palette {
	abstract val id: String
	abstract val displayName: String
	abstract val items: List<PaletteItem>
	abstract val isEditable: Boolean

	data class SolidCollection(
		override val id: String,
		override val displayName: String,
		override val items: List<PaletteItem.Solid>,
		override val isEditable: Boolean = true
	) : Palette()

	data class GradientCollection(
		override val id: String,
		override val displayName: String,
		val category: GradientPaletteCategory,
		override val items: List<PaletteItem.Gradient>,
		override val isEditable: Boolean = true
	) : Palette()
}