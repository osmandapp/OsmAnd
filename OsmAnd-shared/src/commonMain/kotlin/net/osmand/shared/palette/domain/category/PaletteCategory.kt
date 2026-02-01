package net.osmand.shared.palette.domain.category

interface PaletteCategory {
	val id: String             // Internal ID & legacy key support
	val displayName: String    // Palette category name displayed on UI
	val editable: Boolean      // Indicates is this palette allow adding or removing new items
}