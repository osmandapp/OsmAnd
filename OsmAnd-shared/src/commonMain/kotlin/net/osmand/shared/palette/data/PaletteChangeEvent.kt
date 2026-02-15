package net.osmand.shared.palette.data

import net.osmand.shared.palette.domain.PaletteItem

/**
 * Represents the type of change that occurred in the PaletteRepository.
 */
sealed class PaletteChangeEvent {

	/**
	 * An existing item has been modified (e.g., colors changed).
	 * The ID remains the same.
	 *
	 * @property item The updated palette item.
	 */
	data class Updated(val item: PaletteItem) : PaletteChangeEvent()

	/**
	 * A new item has been added to the repository.
	 *
	 * @property item The newly added palette item.
	 */
	data class Added(val item: PaletteItem) : PaletteChangeEvent()

	/**
	 * An item has been removed from the repository.
	 *
	 * @property id The ID of the removed item.
	 * @property paletteId The ID of the palette collection (category) it belonged to.
	 */
	data class Removed(val id: String, val paletteId: String) : PaletteChangeEvent()

	/**
	 * An item has been replaced (e.g., renamed).
	 * The old item is removed, and a new item with a different ID/Name is inserted at the same position.
	 *
	 * @property oldId The ID of the item that was replaced.
	 * @property newItem The new item that replaced the old one.
	 */
	data class Replaced(val oldId: String, val newItem: PaletteItem) : PaletteChangeEvent()
}