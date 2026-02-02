package net.osmand.shared.palette.domain.category

enum class SolidPaletteCategory(
	override val id: String,
	override val nameResId: String,
	override val editable: Boolean = false
): PaletteCategory {

	// --- User solid color palettes (Prefix: user_palette_*) ---

	SOLID_COLOR_PALETTE(
		id = "user_palette",
		nameResId = "user_palette",
		editable = true
	),
}