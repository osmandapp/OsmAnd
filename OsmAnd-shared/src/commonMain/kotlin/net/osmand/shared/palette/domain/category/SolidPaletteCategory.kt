package net.osmand.shared.palette.domain.category

enum class SolidPaletteCategory(
	override val id: String,
	override val displayName: String,
	override val editable: Boolean = false
): PaletteCategory {

	// --- User solid color palettes (Prefix: user_palette_*) ---

	SOLID_COLOR_PALETTE(
		id = "user_palette",
		displayName = "User palette",
		editable = true
	),
}