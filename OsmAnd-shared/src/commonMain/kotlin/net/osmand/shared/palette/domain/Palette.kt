package net.osmand.shared.palette.domain

import net.osmand.shared.io.KFile
import net.osmand.shared.palette.domain.category.GradientPaletteCategory

/**
 * Represents a grouped collection of palette items.
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
		override val isEditable: Boolean = true,
		val sourceFile: KFile
	) : Palette()

	data class GradientCollection(
		override val id: String,
		override val displayName: String,
		val category: GradientPaletteCategory,
		override val items: List<PaletteItem.Gradient>,
		override val isEditable: Boolean = true
	) : Palette()
}