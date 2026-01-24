package net.osmand.shared.palette.data

import net.osmand.shared.ColorPalette
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.palette.domain.PaletteItemSource
import net.osmand.shared.util.LoggerFactory

object SolidPaletteModifier {

	private val LOG = LoggerFactory.getLogger("SolidPaletteModifier")

	fun updateOrAdd(collection: Palette.SolidCollection, item: PaletteItem): Palette.SolidCollection {
		if (item !is PaletteItem.Solid) {
			LOG.error("Wrong item type for SolidCollection: ${item::class.simpleName}")
			return collection
		}

		val currentItems = collection.items
		val index = currentItems.indexOfFirst { it.id == item.id }

		val newItems = if (index != -1) {
			// Update existing
			val mutable = currentItems.toMutableList()
			mutable[index] = item
			mutable
		} else {
			// Add new to end
			currentItems + item
		}

		return collection.copy(items = newItems)
	}

	fun remove(collection: Palette.SolidCollection, itemId: String): Palette.SolidCollection {
		val newItems = collection.items.filter { it.id != itemId }
		if (newItems.size == collection.items.size) return collection

		return collection.copy(items = newItems)
	}

	fun duplicate(
		collection: Palette.SolidCollection,
		originalItemId: String,
		idGenerator: (Set<String>) -> String,
	): Pair<Palette.SolidCollection, PaletteItem>? {
		val index = collection.items.indexOfFirst { it.id == originalItemId }
		if (index == -1) return null

		val originalItem = collection.items[index]

		val existingIds = collection.items.map { it.id }.toSet()
		val newId = idGenerator(existingIds)

		val newItem = originalItem.copy(id = newId)

		// Insert after original
		val newItems = collection.items.toMutableList()
		newItems.add(index + 1, newItem)

		return collection.copy(items = newItems) to newItem
	}

	/**
	 * Updates the `lastUsedTime` of the item to the current time.
	 * This effectively moves the item to the top when sorted by "Last Used".
	 */
	fun markAsUsed(
		collection: Palette.SolidCollection,
		itemId: String,
		timeProvider: () -> Long
	): Palette.SolidCollection {
		val index = collection.items.indexOfFirst { it.id == itemId }
		if (index == -1) return collection

		val originalItem = collection.items[index]

		// Update timestamp
		val updatedItem = originalItem.copy(
			lastUsedTime = timeProvider()
		)

		// Replace item in the list
		val newItems = collection.items.toMutableList()
		newItems[index] = updatedItem

		return collection.copy(items = newItems)
	}

	/**
	 * Adds a new color by value. Generates a new ID and sets the current time.
	 */
	fun addColor(
		collection: Palette.SolidCollection,
		color: Int,
		markAsUsed: Boolean,
		idGenerator: (Set<String>) -> String,
		timeProvider: () -> Long
	): Pair<Palette.SolidCollection, PaletteItem> {

		val existingIds = collection.items.map { it.id }.toSet()
		val newId = idGenerator(existingIds)

		val time = if (markAsUsed) timeProvider() else 0L

		val maxHistoryIndex = collection.items.maxOfOrNull { it.historyIndex } ?: 0

		val newItem = PaletteItem.Solid(
			id = newId,
			displayName = ColorPalette.colorToHex(color),
			source = PaletteItemSource.CollectionRecord(collection.id, newId),
			isEditable = true,
			color = color,
			historyIndex = maxHistoryIndex + 1,
			lastUsedTime = time
		)

		val newItems = collection.items + newItem
		return collection.copy(items = newItems) to newItem
	}
}