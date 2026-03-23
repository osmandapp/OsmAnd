package net.osmand.shared.palette.data.solid

import kotlinx.datetime.Clock
import net.osmand.shared.palette.domain.DefaultPaletteColors
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.palette.domain.PaletteItemSource

object SolidPaletteFactory {

	/**
	 * Populates an empty collection with the default set of colors.
	 * Used when initializing the palette file for the first time.
	 */
	fun fillWithDefaults(emptyCollection: Palette.SolidCollection): Palette.SolidCollection {
		val defaultColors = DefaultPaletteColors.values()
		val newItems = ArrayList<PaletteItem.Solid>()

		// Use current time to ensure correct sorting in "Last Used" mode
		val baseTime = Clock.System.now().toEpochMilliseconds()

		defaultColors.forEachIndexed { index, colorInt ->
			val stableId = index.toString()

			newItems.add(
				PaletteItem.Solid(
					id = stableId,
					displayName = net.osmand.shared.ColorPalette.colorToHex(colorInt),
					source = PaletteItemSource.CollectionRecord(emptyCollection.id),
					isEditable = true,
					colorInt = colorInt,
					historyIndex = index + 1,
					lastUsedTime = baseTime - (index * 1000L)
				)
			)
		}

		return emptyCollection.copy(items = newItems)
	}
}