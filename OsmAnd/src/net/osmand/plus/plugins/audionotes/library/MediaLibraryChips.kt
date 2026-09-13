package net.osmand.plus.plugins.audionotes.library

import net.osmand.plus.R
import net.osmand.plus.gallery.model.GalleryDisplayMode
import net.osmand.plus.gallery.model.GallerySortMode
import net.osmand.plus.search.dialogs.ChipsLayout
import net.osmand.plus.widgets.popup.OsmAndDropdownMenuSelectionStyle

class MediaLibraryChips(private val view: ChipsLayout, private val controller: MediaLibraryController) {
	init {
		view.setOnChipClickListener {
			when (it) {
				"grid" -> controller.setDisplayMode(if (controller.getDisplayMode() == GalleryDisplayMode.GRID) GalleryDisplayMode.LIST else GalleryDisplayMode.GRID)
				"group" -> controller.toggleGrouping()
			}
		}
		view.setOnDropdownItemClickListener { _, id -> GallerySortMode.entries.getOrNull(id)?.let(controller::onSortModeSelected) }
	}

	fun update() {
		val mode = controller.sortMode
		fun chip(id: String, icon: Int, title: Int, selected: Boolean) = ChipsLayout.ChipData(id,
			if (selected) 0 else icon, view.context.getString(title), selected, true, true, false,
			ChipsLayout.TextColorStyle.PRIMARY, ChipsLayout.IconColorStyle.DEFAULT)
		view.updateContent(listOf(
			ChipsLayout.DropDownChipData("sort", mode.iconId, view.context.getString(mode.titleId), false, true, true,
				ChipsLayout.TextColorStyle.PRIMARY, ChipsLayout.IconColorStyle.ACTIVE,
				selectionStyle = OsmAndDropdownMenuSelectionStyle.CHECKMARK,
				dropdownItems = GallerySortMode.entries.mapIndexed { index, item ->
					ChipsLayout.DropdownItem(index, item.iconId, view.context.getString(item.titleId), selected = item == mode,
						showDividerBelow = GallerySortMode.entries.getOrNull(index + 1)?.let { it.group != item.group } == true)
				}),
			chip("grid", MediaLibraryIcons.GRID, R.string.shared_string_grid, controller.getDisplayMode() == GalleryDisplayMode.GRID),
			chip("group", MediaLibraryIcons.GROUP_BY, R.string.shared_string_group, controller.isGrouped())
		))
	}
}
