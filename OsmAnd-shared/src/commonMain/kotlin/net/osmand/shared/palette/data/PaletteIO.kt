package net.osmand.shared.palette.data

import net.osmand.shared.palette.domain.Palette

interface PaletteIO<P : Palette> {

	fun read(paletteId: String): P?

	fun sync(oldPalette: P?, newPalette: P)

	fun createDefault(paletteId: String): P? = null
}