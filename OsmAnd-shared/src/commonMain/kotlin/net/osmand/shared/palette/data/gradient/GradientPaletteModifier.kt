package net.osmand.shared.palette.data.gradient

import kotlinx.datetime.Clock
import net.osmand.shared.palette.data.PaletteModifier
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.util.LoggerFactory

object GradientPaletteModifier : PaletteModifier<Palette.GradientCollection> {

	private val LOG = LoggerFactory.getLogger("GradientPaletteModifier")

	/**
	 * Updates an existing item in the collection.
	 * If the item ID is not found, the collection is returned unchanged.
	 */
	override fun update(
		palette: Palette.GradientCollection,
		item: PaletteItem
	): Palette.GradientCollection {
		if (item !is PaletteItem.Gradient) {
			LOG.error("Wrong item type for GradientCollection: ${item::class.simpleName}")
			return palette
		}

		val currentItems = palette.items
		val index = currentItems.indexOfFirst { it.id == item.id }

		if (index == -1) return palette

		val newItems = currentItems.toMutableList()
		newItems[index] = item

		return palette.copy(items = newItems)
	}

	/**
	 * Adds a NEW item to the end of the collection.
	 * If an item with the same ID already exists, the collection is returned unchanged.
	 */
	override fun add(
		palette: Palette.GradientCollection,
		item: PaletteItem
	): Palette.GradientCollection {
		if (item !is PaletteItem.Gradient) return palette

		if (palette.items.any { it.id == item.id }) {
			return palette
		}

		return palette.copy(items = palette.items + item)
	}

	/**
	 * Inserts a NEW item immediately after a specific anchor item.
	 * Useful for duplication logic or drag-and-drop operations where order matters.
	 */
	override fun insertAfter(
		palette: Palette.GradientCollection,
		anchorId: String,
		item: PaletteItem
	): Palette.GradientCollection {
		if (item !is PaletteItem.Gradient) return palette

		if (palette.items.any { it.id == item.id }) {
			return palette
		}

		val items = palette.items.toMutableList()
		val anchorIndex = items.indexOfFirst { it.id == anchorId }

		if (anchorIndex != -1) {
			items.add(anchorIndex + 1, item)
		} else {
			items.add(item)
		}

		return palette.copy(items = items)
	}

	/**
	 * Removes an item from the collection by its ID.
	 */
	override fun remove(
		palette: Palette.GradientCollection,
		itemId: String
	): Palette.GradientCollection {
		val newItems = palette.items.filter { it.id != itemId }
		if (newItems.size == palette.items.size) return palette
		return palette.copy(items = newItems)
	}

	/**
	 * Updates the `lastUsedTime` of a specific item without changing its other properties.
	 * Note: This does NOT reorder the list in memory. Reordering happens at the UI level
	 * or during the next save/load cycle via the Repository/IO logic.
	 */
	override fun markAsUsed(
		palette: Palette.GradientCollection,
		itemId: String
	): Palette.GradientCollection {
		val index = palette.items.indexOfFirst { it.id == itemId }
		if (index == -1) return palette

		val updatedItem = palette.items[index].copy(
			lastUsedTime = Clock.System.now().toEpochMilliseconds()
		)

		val newItems = palette.items.toMutableList()
		newItems[index] = updatedItem

		return palette.copy(items = newItems)
	}
}