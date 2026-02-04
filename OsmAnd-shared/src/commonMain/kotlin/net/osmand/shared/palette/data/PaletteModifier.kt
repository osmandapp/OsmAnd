package net.osmand.shared.palette.data

import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem

interface PaletteModifier<P : Palette> {

	fun update(palette: P, item: PaletteItem): P

	fun add(palette: P, item: PaletteItem): P

	fun insertAfter(palette: P, anchorId: String, item: PaletteItem): P

	fun remove(palette: P, itemId: String): P

	fun markAsUsed(palette: P, itemId: String): P
}