package net.osmand.shared.palette.data

import co.touchlab.stately.collections.ConcurrentMutableList
import co.touchlab.stately.collections.ConcurrentMutableMap
import net.osmand.shared.palette.data.gradient.GradientPaletteIO
import net.osmand.shared.palette.data.gradient.GradientPaletteModifier
import net.osmand.shared.palette.data.solid.SolidPaletteIO
import net.osmand.shared.palette.data.solid.SolidPaletteModifier
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.category.GradientPaletteCategory
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.palette.domain.filetype.GradientFileType
import net.osmand.shared.palette.domain.filetype.PaletteFileTypeRegistry
import net.osmand.shared.palette.domain.filetype.SolidFileType
import net.osmand.shared.util.LoggerFactory
import kotlin.jvm.JvmOverloads

class PaletteRepository {

	private val cachedPalettesForId = ConcurrentMutableMap<String, Palette>()

	private val listeners = ConcurrentMutableList<PaletteRepositoryListener>()

	@Suppress("UNCHECKED_CAST")
	private val Palette.modifier: PaletteModifier<Palette>
		get() = when (this) {
			is Palette.SolidCollection -> SolidPaletteModifier as PaletteModifier<Palette>
			is Palette.GradientCollection -> GradientPaletteModifier as PaletteModifier<Palette>
		}

	@Suppress("UNCHECKED_CAST")
	private val Palette.io: PaletteIO<Palette>
		get() = when (this) {
			is Palette.SolidCollection -> SolidPaletteIO as PaletteIO<Palette>
			is Palette.GradientCollection -> GradientPaletteIO as PaletteIO<Palette>
		}

	// --- Listeners Management ---

	fun addListener(listener: PaletteRepositoryListener) {
		listeners.add(listener)
	}

	fun removeListener(listener: PaletteRepositoryListener) {
		listeners.remove(listener)
	}

	private fun notifyListeners(event: PaletteChangeEvent) {
		listeners.forEach { it.onPaletteChanged(event) }
	}

	// --- Read ---

	fun findPaletteItem(paletteId: String, itemId: String): PaletteItem? {
		val palette = getPalette(paletteId)
		return palette?.items?.firstOrNull { it.id == itemId }
	}

	fun getPalette(id: String): Palette? {
		cachedPalettesForId[id]?.let { return it }

		val loadedPalette = readPalette(id)
		if (loadedPalette != null) {
			cachedPalettesForId[id] = loadedPalette
		}
		return loadedPalette
	}

	@JvmOverloads
	fun getPaletteItems(
		paletteId: String,
		sortMode: PaletteSortMode = PaletteSortMode.ORIGINAL_ORDER
	): List<PaletteItem> {
		getPalette(paletteId)?.let { palette ->
			return when(sortMode) {
				PaletteSortMode.LAST_USED_TIME -> palette.items.sortedByDescending { it.lastUsedTime }
				PaletteSortMode.ORIGINAL_ORDER -> palette.items.sortedBy { it.historyIndex }
			}
		}
		return emptyList()
	}

	// --- CRUD ---

	fun updatePaletteItem(item: PaletteItem) {
		val currentPalette = getPalette(item.source.paletteId) ?: return
		val updatedPalette = currentPalette.modifier.update(currentPalette, item)

		applyChange(currentPalette, updatedPalette) {
			PaletteChangeEvent.Updated(item)
		}
	}

	fun addPaletteItem(paletteId: String, newItem: PaletteItem) {
		val currentPalette = getPalette(paletteId) ?: return
		val updatedPalette = currentPalette.modifier.add(currentPalette, newItem)

		applyChange(currentPalette, updatedPalette) {
			PaletteChangeEvent.Added(newItem)
		}
	}

	fun replacePaletteItem(paletteId: String, oldItemId: String, newItem: PaletteItem) {
		val currentPalette = getPalette(paletteId) ?: return
		val updatedPalette = currentPalette.modifier.replace(currentPalette, oldItemId, newItem)

		applyChange(currentPalette, updatedPalette) {
			PaletteChangeEvent.Replaced(oldItemId, newItem)
		}
	}

	fun insertPaletteItemAfter(paletteId: String, anchorId: String, newItem: PaletteItem) {
		val currentPalette = getPalette(paletteId) ?: return
		val updatedPalette = currentPalette.modifier.insertAfter(currentPalette, anchorId, newItem)

		applyChange(currentPalette, updatedPalette) {
			PaletteChangeEvent.Added(newItem)
		}
	}

	fun removePaletteItem(paletteId: String, itemId: String) {
		val currentPalette = getPalette(paletteId) ?: return
		val updatedPalette = currentPalette.modifier.remove(currentPalette, itemId)

		applyChange(currentPalette, updatedPalette) {
			PaletteChangeEvent.Removed(itemId, paletteId)
		}
	}

	fun markPaletteItemAsUsed(paletteId: String, itemId: String) {
		val currentPalette = getPalette(paletteId) ?: return
		val updatedPalette = currentPalette.modifier.markAsUsed(currentPalette, itemId)

		applyChange(currentPalette, updatedPalette) {
			// We need to find the updated item to pass it in the event
			val updatedItem = updatedPalette.items.find { it.id == itemId }
			if (updatedItem != null) {
				PaletteChangeEvent.Updated(updatedItem)
			} else {
				// Should not happen, but as a fallback/dummy
				// In reality, if item is not found, updatedPalette == currentPalette, so this block won't run
				PaletteChangeEvent.Updated(currentPalette.items.first())
			}
		}
	}

	// --- Internal Helpers ---

	/**
	 * Checks if changes occurred, saves the palette, and notifies listeners.
	 * @param oldPalette The state before modification.
	 * @param newPalette The state after modification.
	 * @param createEvent A lambda that creates the event object. It is only called if changes actually happened.
	 */
	private inline fun applyChange(
		oldPalette: Palette,
		newPalette: Palette,
		createEvent: () -> PaletteChangeEvent
	) {
		if (oldPalette !== newPalette) {
			savePalette(oldPalette, newPalette)
			notifyListeners(createEvent())
		}
	}

	private fun savePalette(old: Palette?, new: Palette) {
		// 1. Update Cache
		cachedPalettesForId[new.id] = new

		// 2. Sync on disk and settings
		try {
			new.io.sync(old, new)
		} catch (e: Exception) {
			LOG.error("Failed to save palette ${new.id}", e)
		}
	}

	private fun readPalette(id: String): Palette? {
		val io = findPaletteIO(id) ?: return null
		var palette = io.read(id)

		if (palette == null) {
			palette = io.createDefault(id)
			if (palette != null) {
				savePalette(null, palette)
			}
		}
		return palette
	}

	private fun findPaletteIO(paletteId: String): PaletteIO<out Palette>? {
		val fileType = PaletteFileTypeRegistry.fromFileName(paletteId)
		if (fileType is SolidFileType) {
			return SolidPaletteIO
		}
		if (GradientPaletteCategory.fromKey(paletteId) != null || fileType is GradientFileType) {
			return GradientPaletteIO
		}
		return null
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("PaletteRepository")
	}
}