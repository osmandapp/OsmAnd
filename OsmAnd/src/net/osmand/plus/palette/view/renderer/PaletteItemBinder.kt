package net.osmand.plus.palette.view.renderer

import android.view.View
import android.view.ViewGroup
import net.osmand.shared.palette.domain.PaletteItem

interface PaletteItemBinder {

	fun createView(parent: ViewGroup): View

	fun bindView(itemView: View, item: PaletteItem, selected: Boolean)
}