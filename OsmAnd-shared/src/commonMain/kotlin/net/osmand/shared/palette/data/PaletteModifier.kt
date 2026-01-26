package net.osmand.shared.palette.data

import net.osmand.shared.palette.domain.Palette
import net.osmand.shared.palette.domain.PaletteItem

interface PaletteModifier<P : Palette> {

	fun updateOrAdd(palette: P, item: PaletteItem): P

	fun remove(palette: P, itemId: String): P

	fun duplicate(palette: P, originalItemId: String): Pair<P, PaletteItem>?

	fun markAsUsed(palette: P, itemId: String): P
}