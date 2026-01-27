package net.osmand.shared.palette.data.solid

import kotlinx.datetime.Clock
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.palette.domain.PaletteItemSource
import net.osmand.shared.ColorPalette
import net.osmand.shared.io.KFile
import net.osmand.shared.palette.data.PaletteIO
import net.osmand.shared.palette.data.PaletteUtils
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.Localization
import net.osmand.shared.util.LoggerFactory
import net.osmand.shared.util.PlatformUtil
import okio.IOException
import okio.buffer
import okio.use

object SolidPaletteIO : PaletteIO<Palette.SolidCollection> {

	private val LOG = LoggerFactory.getLogger("SolidPaletteIO")

	private const val DEFAULT_PALETTE_ID = "user_palette_default"
	private const val USER_PALETTE_PREFIX = "user_palette_"
	private const val HEADER = "# Index,R,G,B,A"

	private val paletteDirectory: KFile
		get() = PlatformUtil.getOsmAndContext().getColorPaletteDir()

	override fun sync(
		oldPalette: Palette.SolidCollection?,
		newPalette: Palette.SolidCollection
	) {
		if (oldPalette != newPalette) {
			write(newPalette)
		}
	}

	override fun createDefault(paletteId: String): Palette.SolidCollection? {
		if (paletteId != DEFAULT_PALETTE_ID) return null
		val emptyCollection = createSolidCollection()
		return SolidPaletteFactory.fillWithDefaults(emptyCollection)
	}

	override fun read(paletteId: String): Palette.SolidCollection? {
		val file = getFileForId(paletteId)
		if (!file.exists()) return null

		return readInternal(file)
	}

	private fun readInternal(file: KFile): Palette.SolidCollection {
		val paletteId = file.getFileNameWithoutExtension() ?: ""
		val items = mutableListOf<PaletteItem.Solid>()

		val usedIds = HashSet<String>()

		if (file.exists()) {
			try {
				file.source().buffer().use { source ->
					val baseTime = Clock.System.now().toEpochMilliseconds()
					var lineIndex = 0

					while (true) {
						val line = source.readUtf8Line() ?: break

						val trimmed = line.trim()
						if (trimmed.isEmpty() || trimmed.startsWith("#")) {
							continue
						}

						val colorValue = ColorPalette.parseColorValue(trimmed)
						if (colorValue != null) {
							val colorInt = colorValue.clr
							val uniqueId = PaletteUtils.generateSolidUniqueId(usedIds)
							usedIds.add(uniqueId) // TODO: in the future we should read and write unique id as a 'value' part in the CVS table

							items.add(
								PaletteItem.Solid(
								id = uniqueId,
								displayName = ColorPalette.colorToHex(colorInt),
								source = PaletteItemSource.CollectionRecord(
									paletteId = paletteId,
									recordId = lineIndex.toString()
								),
								color = colorInt,
								historyIndex = colorValue.value.toInt(), // TODO: don't use it
								lastUsedTime = baseTime - (lineIndex * 1000L)
							))
						}
						lineIndex++
					}
				}
			} catch (e: Exception) {
				LOG.error(e.message, e)
			}
		}

		return createSolidCollection(file, items)
	}

	private fun createSolidCollection(
		file: KFile = getFileForId(DEFAULT_PALETTE_ID),
		items: List<PaletteItem.Solid> = emptyList()
	): Palette.SolidCollection {
		val paletteId = file.getFileNameWithoutExtension() ?: ""

		// TODO: store and use palette name from property
		val displayName = if (paletteId == DEFAULT_PALETTE_ID) {
			Localization.getString("user_palette")
		} else {
			val name = paletteId.replace(USER_PALETTE_PREFIX, "").replace("_", " ")
			KAlgorithms.capitalizeFirstLetter(name)
		}

		return Palette.SolidCollection(
			id = paletteId,
			displayName = displayName.toString(),
			items = items.sortedBy { it.historyIndex },
			isEditable = true,
			sourceFile = file
		)
	}

	private fun write(palette: Palette.SolidCollection) {
		val originalOrder = palette.items
		val lastUsedOrder = originalOrder.sortedByDescending { it.lastUsedTime }

		// OPTIMIZATION: Create a map {Item -> Index} to avoid indexOf in the loop.
		// This makes writing instant even for large lists (O(N) instead of O(N^2)).
		val indexMap = originalOrder.withIndex().associate { it.value to (it.index + 1) }

		val file = palette.sourceFile
		val content = buildString {
			append(HEADER)
			append("\n")
			lastUsedOrder.forEach { item ->
				val index = indexMap[item] ?: 0

				val copy = item.copy(historyIndex = index)
				val cv = copy.getColorValue()

				append(ColorPalette.formatColorValue(cv))
				append("\n")
			}
		}

		try {
			val dir = file.getParentFile()
			if (dir != null && !dir.exists()) {
				dir.createDirectories()
			}
			file.writeText(content)
		} catch (e: IOException) {
			LOG.error(e.message, e)
		}
	}

	private fun getFileForId(id: String): KFile {
		return KFile(paletteDirectory, "$id.txt")
	}
}