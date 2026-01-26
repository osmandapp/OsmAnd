package net.osmand.shared.palette.data.solid

import kotlinx.datetime.Clock
import net.osmand.shared.ColorPalette
import net.osmand.shared.palette.data.PaletteModifier
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.palette.domain.PaletteItemSource
import net.osmand.shared.util.LoggerFactory

object SolidPaletteModifier : PaletteModifier<Palette.SolidCollection> {

	private val LOG = LoggerFactory.getLogger("SolidPaletteModifier")

	override fun updateOrAdd(
		palette: Palette.SolidCollection,
		item: PaletteItem
	): Palette.SolidCollection {
		if (item !is PaletteItem.Solid) {
			LOG.error("Wrong item type for SolidCollection: ${item::class.simpleName}")
			return palette
		}

		val currentItems = palette.items
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

	override fun duplicate(
		palette: Palette.SolidCollection,
		originalItemId: String
	): Pair<Palette.SolidCollection, PaletteItem>? {
		val index = palette.items.indexOfFirst { it.id == originalItemId }
		if (index == -1) return null

		val originalItem = palette.items[index]

		val existingIds = palette.items.map { it.id }.toSet()
		val newId = SolidPaletteIO.generateUniqueId(existingIds)

		val newItem = originalItem.copy(id = newId)

		// Insert after original
		val newItems = palette.items.toMutableList()
		newItems.add(index + 1, newItem)

		return palette.copy(items = newItems) to newItem
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

		val originalItem = palette.items[index]

		// Update timestamp
		val updatedItem = originalItem.copy(
			lastUsedTime = Clock.System.now().toEpochMilliseconds()
		)

		// Replace item in the list
		val newItems = palette.items.toMutableList()
		newItems[index] = updatedItem

		return palette.copy(items = newItems)
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
			isDefault = true,
			color = color,
			historyIndex = maxHistoryIndex + 1,
			lastUsedTime = time
		)

		val newItems = collection.items + newItem
		return collection.copy(items = newItems) to newItem
	}
}