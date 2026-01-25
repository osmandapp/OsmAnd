package net.osmand.shared.palette.data

import co.touchlab.stately.collections.ConcurrentMutableMap
import kotlinx.datetime.Clock
import net.osmand.shared.io.KFile
import net.osmand.shared.palette.data.solid.SolidPaletteFactory
import net.osmand.shared.palette.data.solid.SolidPaletteIO
import net.osmand.shared.palette.data.solid.SolidPaletteModifier
import net.osmand.shared.palette.domain.GradientPaletteCategory
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.util.LoggerFactory

interface PaletteDirectoryProvider {
	fun getPaletteDirectory(): KFile
}

class PaletteRepository(
	private val directoryProvider: PaletteDirectoryProvider
) {

	private val cachedPalettesForId = ConcurrentMutableMap<String, Palette>()

	// --- Read ---

	fun getPalette(id: String): Palette? {
		cachedPalettesForId[id]?.let { return it }

		val loadedPalette = readPalette(id)
		if (loadedPalette != null) {
			cachedPalettesForId[id] = loadedPalette
		}
		return loadedPalette
	}

	fun getPaletteItems(paletteId: String, sortMode: PaletteSortMode): List<PaletteItem> {
		getPalette(paletteId)?.let { palette ->
			return when(sortMode) {
				PaletteSortMode.LAST_USED_TIME -> palette.items.sortedByDescending { it.lastUsedTime }
				PaletteSortMode.ORIGINAL_ORDER -> palette.items
			}
		}
		return emptyList()
	}

	// --- CRUD ---

	fun updatePaletteItem(item: PaletteItem) {
		val paletteId = item.source.paletteId
		val currentPalette = getPalette(paletteId) ?: return

		val updatedPalette = when (currentPalette) {
			is Palette.SolidCollection -> SolidPaletteModifier.updateOrAdd(currentPalette, item)
			// is Palette.GradientCollection -> GradientPaletteModifier.updateOrAdd(...)
			else -> currentPalette
		}

		saveIfChanged(currentPalette, updatedPalette)
	}

	fun removePaletteItem(paletteId: String, itemId: String) {
		val currentPalette = getPalette(paletteId) ?: return

		val updatedPalette = when (currentPalette) {
			is Palette.SolidCollection -> SolidPaletteModifier.remove(currentPalette, itemId)
			// is Palette.GradientCollection -> GradientPaletteModifier.remove(...)
			else -> currentPalette
		}

		saveIfChanged(currentPalette, updatedPalette)
	}

	fun duplicatePaletteItem(paletteId: String, originalItemId: String): PaletteItem? {
		val currentPalette = getPalette(paletteId) ?: return null

		var resultItem: PaletteItem? = null

		val updatedPalette = when (currentPalette) {
			is Palette.SolidCollection -> {
				val result = SolidPaletteModifier.duplicate(
					collection = currentPalette,
					originalItemId = originalItemId,
					idGenerator = { ids -> SolidPaletteIO.generateUniqueId(ids) },
				)
				result?.let { (newCollection, newItem) ->
					resultItem = newItem
					newCollection
				} ?: currentPalette
			}
			// is Palette.GradientCollection -> ...
			else -> currentPalette
		}

		saveIfChanged(currentPalette, updatedPalette)
		return resultItem
	}

	/**
	 * Call this when a color is selected/applied by the user.
	 * It updates the timestamp so it appears first in the "Last Used" sort mode.
	 */
	fun markPaletteItemAsUsed(paletteId: String, itemId: String) {
		val currentPalette = getPalette(paletteId) ?: return

		val updatedPalette = when (currentPalette) {
			is Palette.SolidCollection -> SolidPaletteModifier.markAsUsed(
				collection = currentPalette,
				itemId = itemId,
				timeProvider = { Clock.System.now().toEpochMilliseconds() }
			)
			// is Palette.GradientCollection -> ...
			else -> currentPalette
		}

		saveIfChanged(currentPalette, updatedPalette)
	}

	fun getOrAddSolidColor(paletteId: String, colorInt: Int): PaletteItem? {
		val currentPalette = getPalette(paletteId)

		if (currentPalette is Palette.SolidCollection) {
			val existingItem = currentPalette.items.find { it.color == colorInt }

			if (existingItem != null) {
				markPaletteItemAsUsed(paletteId, existingItem.id)
				return getPalette(paletteId)?.let {
					(it as? Palette.SolidCollection)?.items?.find { item -> item.id == existingItem.id }
				}
			}
		}

		return addSolidColor(paletteId, colorInt, markAsRecentlyUsed = false)
	}

	fun addSolidColor(
		paletteId: String,
		colorInt: Int,
		markAsRecentlyUsed: Boolean = true
	): PaletteItem? {
		val currentPalette = getPalette(paletteId) ?: return null
		var resultItem: PaletteItem? = null

		val updatedPalette = when (currentPalette) {
			is Palette.SolidCollection -> {
				val (newCollection, newItem) = SolidPaletteModifier.addColor(
					collection = currentPalette,
					color = colorInt,
					markAsUsed = markAsRecentlyUsed,
					idGenerator = { ids -> SolidPaletteIO.generateUniqueId(ids) },
					timeProvider = { Clock.System.now().toEpochMilliseconds() }
				)
				resultItem = newItem
				newCollection
			}
			else -> currentPalette
		}

		saveIfChanged(currentPalette, updatedPalette)
		return resultItem
	}

	// --- Internal Helpers ---

	private fun saveIfChanged(old: Palette, new: Palette) {
		if (old !== new) {
			savePalette(new)
		}
	}

	private fun savePalette(palette: Palette) {
		// 1. Update Cache
		cachedPalettesForId[palette.id] = palette

		// 2. Write to Disk
		try {
			when (palette) {
				is Palette.SolidCollection -> SolidPaletteIO.write(palette)
				// is Palette.GradientCollection -> GradientPaletteIO.write(palette)
				else -> { /* No IO for unknown types */ }
			}
		} catch (e: Exception) {
			LOG.error("Failed to save palette ${palette.id}", e)
		}
	}

	private fun readPalette(id: String): Palette? {
		if (id.startsWith(USER_PALETTE_PREFIX)) {
			return readSolidPalette(id)
		}
		GradientPaletteCategory.fromFileName(id)?.let { category ->
			// TODO: read gradient palette using GradientPaletteIO
		}
		return null
	}

	private fun readSolidPalette(id: String): Palette.SolidCollection {
		val file = getFileForId(id)
		val isFirstCreation = !file.exists()

		// Returns an empty collection if the file doesn't exist
		var collection = SolidPaletteIO.read(file)

		if (isFirstCreation && id == DEFAULT_PALETTE_ID) {
			collection = SolidPaletteFactory.fillWithDefaults(collection)

			try {
				SolidPaletteIO.write(collection)
			} catch (e: Exception) {
				LOG.error("Failed to write default palette", e)
			}
		}
		return collection
	}

	private fun getFileForId(id: String): KFile {
		return KFile(directoryProvider.getPaletteDirectory(), "$id.txt")
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("PaletteRepository")
		const val DEFAULT_PALETTE_ID = "user_palette_default"
		const val USER_PALETTE_PREFIX = "user_palette_"
	}
}