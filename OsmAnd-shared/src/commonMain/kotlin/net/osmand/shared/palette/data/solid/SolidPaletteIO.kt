package net.osmand.shared.palette.data.solid

import kotlinx.datetime.Clock
import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem
import net.osmand.shared.palette.domain.PaletteItemSource
import net.osmand.shared.ColorPalette
import net.osmand.shared.io.KFile
import net.osmand.shared.palette.data.PaletteRepository.Companion.DEFAULT_PALETTE_ID
import net.osmand.shared.palette.data.PaletteRepository.Companion.USER_PALETTE_PREFIX
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.Localization
import net.osmand.shared.util.LoggerFactory
import okio.IOException
import okio.buffer
import okio.use
import kotlin.random.Random

object SolidPaletteIO {

	private val LOG = LoggerFactory.getLogger("SolidPaletteIO")

	private const val HEADER = "# Index,R,G,B,A"

	fun read(file: KFile): Palette.SolidCollection {
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
							val uniqueId = generateUniqueId(usedIds)
							usedIds.add(uniqueId) // TODO: in the future we should read and write unique id as a 'value' part in the CVS table

							items.add(
								PaletteItem.Solid(
								id = uniqueId,
								displayName = ColorPalette.colorToHex(colorInt),
								source = PaletteItemSource.CollectionRecord(
									paletteId = paletteId,
									recordId = lineIndex.toString()
								),
								isEditable = true,
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

	fun write(collection: Palette.SolidCollection) {
		val originalOrder = collection.items
		val lastUsedOrder = originalOrder.sortedByDescending { it.lastUsedTime }

		// OPTIMIZATION: Create a map {Item -> Index} to avoid indexOf in the loop.
		// This makes writing instant even for large lists (O(N) instead of O(N^2)).
		val indexMap = originalOrder.withIndex().associate { it.value to (it.index + 1) }

		val file = collection.sourceFile
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

	fun generateUniqueId(existingIds: Set<String>): String {
		var newId: Int
		do {
			newId = Random.nextInt(100000, Int.MAX_VALUE)
		} while (existingIds.contains(newId.toString()))
		return newId.toString()
	}
}