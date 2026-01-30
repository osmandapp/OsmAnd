package net.osmand.shared.palette.data

import co.touchlab.stately.collections.ConcurrentMutableMap
import net.osmand.shared.palette.data.gradient.GradientPaletteIO
import net.osmand.shared.palette.data.gradient.GradientPaletteModifier
import net.osmand.shared.palette.data.solid.SolidPaletteIO
import net.osmand.shared.palette.data.solid.SolidPaletteModifier
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteCategory
import net.osmand.shared.palette.domain.PaletteFileType
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.util.LoggerFactory
import kotlin.jvm.JvmOverloads

class PaletteRepository {

	private val cachedPalettesForId = ConcurrentMutableMap<String, Palette>()

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

	// --- Read ---

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
		saveIfChanged(currentPalette, updatedPalette)
	}

	fun addPaletteItem(paletteId: String, newItem: PaletteItem) {
		val currentPalette = getPalette(paletteId) ?: return
		val updatedPalette = currentPalette.modifier.add(currentPalette, newItem)
		saveIfChanged(currentPalette, updatedPalette)
	}

	fun insertPaletteItemAfter(paletteId: String, anchorId: String, newItem: PaletteItem) {
		val currentPalette = getPalette(paletteId) ?: return
		val updatedPalette = currentPalette.modifier.insertAfter(currentPalette, anchorId, newItem)
		saveIfChanged(currentPalette, updatedPalette)
	}

	fun removePaletteItem(paletteId: String, itemId: String) {
		val currentPalette = getPalette(paletteId) ?: return
		val updatedPalette = currentPalette.modifier.remove(currentPalette, itemId)
		saveIfChanged(currentPalette, updatedPalette)
	}

	fun markPaletteItemAsUsed(paletteId: String, itemId: String) {
		val currentPalette = getPalette(paletteId) ?: return
		val updatedPalette = currentPalette.modifier.markAsUsed(currentPalette, itemId)
		saveIfChanged(currentPalette, updatedPalette)
	}

	// --- Internal Helpers ---

	private fun saveIfChanged(old: Palette, new: Palette) {
		if (old !== new) {
			savePalette(old, new)
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
		val fileType = PaletteFileType.fromFileName(paletteId)
		if (fileType != null && fileType.category == PaletteCategory.SOLID_PALETTE) {
			return SolidPaletteIO
		}
		val paletteCategory = PaletteCategory.fromKey(paletteId)
		if (paletteCategory != null) {
			return GradientPaletteIO
		}
		return null
	}

	companion object {
		private val LOG = LoggerFactory.getLogger("PaletteRepository")
	}
}