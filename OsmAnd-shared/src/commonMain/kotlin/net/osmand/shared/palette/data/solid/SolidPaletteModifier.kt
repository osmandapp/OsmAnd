package net.osmand.shared.palette.data.solid

import kotlinx.datetime.Clock
import net.osmand.shared.palette.data.PaletteModifier
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.util.LoggerFactory

object SolidPaletteModifier : PaletteModifier<Palette.SolidCollection> {

	private val LOG = LoggerFactory.getLogger("SolidPaletteModifier")

	override fun update(
		palette: Palette.SolidCollection,
		item: PaletteItem
	): Palette.SolidCollection {
		if (item !is PaletteItem.Solid) {
			LOG.error("Wrong item type for SolidCollection: ${item::class.simpleName}")
			return palette
		}

		val currentItems = palette.items
		val index = currentItems.indexOfFirst { it.id == item.id }

		if (index == -1) {
			return palette
		}

		val newItems = currentItems.toMutableList()
		newItems[index] = item

		return palette.copy(items = newItems)
	}

	/**
	 * Adds a NEW item to the end of the list.
	 * Prevents duplicates by ID.
	 */
	override fun add(
		palette: Palette.SolidCollection,
		item: PaletteItem
	): Palette.SolidCollection {
		if (item !is PaletteItem.Solid) return palette

		if (palette.items.any { it.id == item.id }) {
			return palette
		}

		return palette.copy(items = palette.items + item)
	}

	/**
	 * Inserts a NEW item immediately after the anchor item.
	 * Used for duplicating items or explicit ordering.
	 */
	override fun insertAfter(
		palette: Palette.SolidCollection,
		anchorId: String,
		item: PaletteItem
	): Palette.SolidCollection {
		if (item !is PaletteItem.Solid) return palette

		if (palette.items.any { it.id == item.id }) {
			return palette
		}

		val items = palette.items.toMutableList()
		val anchorIndex = items.indexOfFirst { it.id == anchorId }

		if (anchorIndex != -1) {
			items.add(anchorIndex + 1, item)
		} else {
			// Fallback: append to end if anchor not found
			items.add(item)
		}

		return palette.copy(items = items)
	}

	/**
	 * Replaces an existing item with a new one at the same position.
	 */
	override fun replace(
		palette: Palette.SolidCollection,
		oldItemId: String,
		newItem: PaletteItem
	): Palette.SolidCollection {
		if (newItem !is PaletteItem.Solid) return palette

		// Replace the item with matching ID to preserve the list order
		val newItems = palette.items.map { item ->
			if (item.id == oldItemId) newItem else item
		}

		return palette.copy(items = newItems)
	}

	override fun remove(
		palette: Palette.SolidCollection,
		itemId: String
	): Palette.SolidCollection {
		val newItems = palette.items.filter { it.id != itemId }
		if (newItems.size == palette.items.size) return palette
		return palette.copy(items = newItems)
	}

	/**
	 * Updates the `lastUsedTime` of the item to the current time.
	 * This effectively moves the item to the top when sorted by "Last Used".
	 */
	override fun markAsUsed(
		palette: Palette.SolidCollection,
		itemId: String
	): Palette.SolidCollection {
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