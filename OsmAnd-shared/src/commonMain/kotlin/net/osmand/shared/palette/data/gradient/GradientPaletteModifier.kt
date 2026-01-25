package net.osmand.shared.palette.data.gradient

import kotlinx.datetime.Clock
import net.osmand.shared.io.KFile
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.palette.domain.PaletteItemSource
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.NamingUtils

object GradientPaletteModifier {

	private val LOG = LoggerFactory.getLogger("GradientPaletteModifier")

	/**
	 * Updates an existing gradient item (e.g. points changed) or adds a new one.
	 * Automatically handles sorting of points by value before writing.
	 */
	fun updateOrAdd(
		collection: Palette.GradientCollection,
		item: PaletteItem,
		directory: KFile,
		settingsHelper: GradientSettingsHelper
	): Palette.GradientCollection {
		if (item !is PaletteItem.Gradient) {
			LOG.error("Wrong item type for GradientCollection: ${item::class.simpleName}")
			return collection
		}

		// 1. Ensure points are sorted by value (Safety check)
		// Gradients MUST be sorted by value to render correctly.
		val sortedItem = item.copy(points = item.points.sortedBy { it.value })

		// 2. Write physical file
		GradientPaletteIO.writeItem(directory, sortedItem)

		// 3. Update Memory Collection
		val currentItems = collection.items
		val index = currentItems.indexOfFirst { it.id == sortedItem.id }

		val newItems = if (index != -1) {
			val mutable = currentItems.toMutableList()
			mutable[index] = sortedItem
			mutable
		} else {
			// New item -> Add to end
			currentItems + sortedItem
		}

		// 4. Sync Settings (Persist existence and order)
		saveSettings(collection.category.key, newItems, settingsHelper)

		return collection.copy(items = newItems)
	}

	/**
	 * Removes the gradient file and its entry from settings.
	 */
	fun remove(
		collection: Palette.GradientCollection,
		itemId: String,
		directory: KFile,
		settingsHelper: GradientSettingsHelper
	): Palette.GradientCollection {
		val itemToRemove = collection.items.find { it.id == itemId } ?: return collection

		// 1. Delete physical file
		val file = KFile(directory, itemToRemove.source.fileName)
		if (file.exists()) {
			file.delete()
		}

		// 2. Update memory
		val newItems = collection.items.filter { it.id != itemId }

		// 3. Update settings (Remove the entry)
		saveSettings(collection.category.key, newItems, settingsHelper)

		return collection.copy(items = newItems)
	}

	fun duplicate(
		collection: Palette.GradientCollection,
		originalItemId: String,
		directory: KFile,
		settingsHelper: GradientSettingsHelper
	): Pair<Palette.GradientCollection, PaletteItem>? {
		val index = collection.items.indexOfFirst { it.id == originalItemId }
		if (index == -1) return null

		val originalItem = collection.items[index]

		// 1. Generate Unique ID / Name
		// In gradients, ID is usually the filename with extension
		val existingIds = collection.items.map { it.id }.toSet()
		val newFileName = NamingUtils.generateUniqueName(existingIds, originalItem.id)

		// 2. Create new Item
		val newItem = originalItem.copy(
			id = newFileName,
			paletteName = GradientPaletteIO.extractPaletteName(newFileName) ?: newFileName,
			// TODO: may be we should use pending name fetching to always get it up to date,
			//  but if we will implement rename logic, we should always also update name
			displayName = NamingUtils.getNextName(originalItem.displayName),
			source = PaletteItemSource.GradientFile(collection.id, newFileName),
			isDefault = false
		)

		// 3. Write physical file
		GradientPaletteIO.writeItem(directory, newItem)

		// 4. Insert into memory (after original)
		val newItems = collection.items.toMutableList()
		newItems.add(index + 1, newItem)

		// 5. Save settings
		saveSettings(collection.category.key, newItems, settingsHelper)

		return collection.copy(items = newItems) to newItem
	}

	/**
	 * Updates timestamp (virtual lastUsedTime) and reorders in Settings.
	 */
	fun markAsUsed(
		collection: Palette.GradientCollection,
		itemId: String,
		settingsHelper: GradientSettingsHelper,
		timeProvider: () -> Long = { Clock.System.now().toEpochMilliseconds() }
	): Palette.GradientCollection {
		val index = collection.items.indexOfFirst { it.id == itemId }
		if (index == -1) return collection

		val originalItem = collection.items[index]

		// 1. Update timestamp in memory
		val updatedItem = originalItem.copy(
			lastUsedTime = timeProvider()
		)

		val tempItems = collection.items.toMutableList()
		tempItems[index] = updatedItem

		// 2. Save new order to Settings
		saveSettings(collection.category.key, tempItems, settingsHelper)

		return collection.copy(items = tempItems)
	}

	// --- Helper to sync with Settings ---

	private fun saveSettings(
		categoryKey: String,
		originalOrder: List<PaletteItem.Gradient>,
		settingsHelper: GradientSettingsHelper
	) {
		val lastUsedOrder = originalOrder.sortedByDescending { it.lastUsedTime }
		val settingsItems = mutableListOf<GradientSettingsItem>()
		lastUsedOrder.forEach { item ->
			settingsItems.add(
				GradientSettingsItem(
					typeName = item.properties.fileType.category.key,
					paletteName = item.paletteName,
					index = originalOrder.indexOf(item) + 1
				)
			)
		}
		settingsHelper.saveItems(categoryKey, settingsItems)
	}
}