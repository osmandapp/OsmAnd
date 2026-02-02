package net.osmand.shared.palette.domain.category

import net.osmand.shared.util.Localization

interface PaletteCategory {
	val id: String             // Internal ID & legacy key support
	val nameResId: String      // Palette category name displayed on UI
	val editable: Boolean      // Indicates is this palette allow adding or removing new items

	fun getDisplayName(): String = Localization.getString(nameResId)
}