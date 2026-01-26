package net.osmand.shared.palette.data.gradient

import kotlinx.datetime.Clock
import net.osmand.shared.io.KFile
import net.osmand.shared.palette.data.PaletteModifier
import net.osmand.shared.palette.data.PaletteUtils
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.palette.domain.PaletteItemSource
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil

object GradientPaletteModifier : PaletteModifier<Palette.GradientCollection> {

	private val LOG = LoggerFactory.getLogger("GradientPaletteModifier")

	private val settingsHelper = GradientSettingsHelper()

	private val paletteDirectory
		get() = PlatformUtil.getOsmAndContext().getColorPaletteDir()

	/**
	 * Updates an existing gradient item (e.g. points changed) or adds a new one.
	 * Automatically handles sorting of points by value before writing.
	 */
	override fun updateOrAdd(
		palette: Palette.GradientCollection,
		item: PaletteItem
	): Palette.GradientCollection {
		if (item !is PaletteItem.Gradient) {
			LOG.error("Wrong item type for GradientCollection: ${item::class.simpleName}")
			return palette
		}

		// 1. Ensure points are sorted by value (Safety check)
		// Gradients MUST be sorted by value to render correctly.
		val sortedItem = item.copy(points = item.points.sortedBy { it.value })

		// 2. Write physical file
		GradientPaletteIO.writeItem(paletteDirectory, sortedItem)

		// 3. Update Memory Collection
		val currentItems = palette.items
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
		saveSettings(palette.category.key, newItems, settingsHelper)

		return palette.copy(items = newItems)
	}

	/**
	 * Removes the gradient file and its entry from settings.
	 */
	override fun remove(
		palette: Palette.GradientCollection,
		itemId: String
	): Palette.GradientCollection {
		val itemToRemove = palette.items.find { it.id == itemId } ?: return palette

		// 1. Delete physical file
		val file = KFile(paletteDirectory, itemToRemove.source.fileName)
		if (file.exists()) {
			file.delete()
		}

		// 2. Update memory
		val newItems = palette.items.filter { it.id != itemId }

		// 3. Update settings (Remove the entry)
		saveSettings(palette.category.key, newItems, settingsHelper)

		return palette.copy(items = newItems)
	}

	override fun duplicate(
		palette: Palette.GradientCollection,
		originalItemId: String
	): Pair<Palette.GradientCollection, PaletteItem>? {
		val index = palette.items.indexOfFirst { it.id == originalItemId }
		if (index == -1) return null

		val originalItem = palette.items[index]

		// 1. Generate Unique ID / Name
		// In gradients, ID is usually the filename with extension
		val existingIds = palette.items.map { it.id }.toSet()
		val newFileName = PaletteUtils.generateUniqueFileName(existingIds, originalItem.id)
		val paletteName = PaletteUtils.extractPaletteName(newFileName) ?: return null
		val displayName = PaletteUtils.extractDisplayName(newFileName) ?: return null

		// 2. Create new Item
		val newItem = originalItem.copy(
			id = newFileName,
			paletteName = paletteName,
			displayName = displayName,
			source = PaletteItemSource.GradientFile(palette.id, newFileName),
			isDefault = false
		)

		// 3. Write physical file
		GradientPaletteIO.writeItem(paletteDirectory, newItem)

		// 4. Insert into memory (after original)
		val newItems = palette.items.toMutableList()
		newItems.add(index + 1, newItem)

		// 5. Save settings
		saveSettings(palette.category.key, newItems, settingsHelper)

		return palette.copy(items = newItems) to newItem
	}

	/**
	 * Updates timestamp (virtual lastUsedTime) and reorders in Settings.
	 */
	override fun markAsUsed(
		palette: Palette.GradientCollection,
		itemId: String
	): Palette.GradientCollection {
		val index = palette.items.indexOfFirst { it.id == itemId }
		if (index == -1) return palette

		val originalItem = palette.items[index]

		// 1. Update timestamp in memory
		val updatedItem = originalItem.copy(
			lastUsedTime = Clock.System.now().toEpochMilliseconds()
		)

		val tempItems = palette.items.toMutableList()
		tempItems[index] = updatedItem

		// 2. Save new order to Settings
		saveSettings(palette.category.key, tempItems, settingsHelper)

		return palette.copy(items = tempItems)
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