package net.osmand.plus.configmap.tracks

import net.osmand.plus.OsmandApplication
import net.osmand.plus.track.helpers.GpxSelectionHelper.CURRENT_TRACK
import net.osmand.shared.gpx.TrackItem

class SelectTracksTabsHelper(
	app: OsmandApplication,
	paths: Collection<String>
) : TrackTabsHelper(app) {

	private val initialPaths: Set<String> = paths.toSet()
	private val visibleTrackItems = mutableSetOf<TrackItem>()
	private var selectedPaths: Set<String>? = null

	override fun processVisibleTracks() {
		val paths = selectedPaths?.let { getSelectedPaths().toSet() } ?: initialPaths
		selectedPaths = paths
		super.processVisibleTracks()

		val helper = itemsSelectionHelper
		visibleTrackItems.clear()
		visibleTrackItems.addAll(helper.selectedItems)

		helper.setOriginalSelectedItems(getTrackItems(initialPaths))
		helper.setSelectedItems(getTrackItems(paths))
	}

	override fun getVisibleTrackItems(): Set<TrackItem> = visibleTrackItems

	fun getSelectedPaths(): List<String> {
		val helper = itemsSelectionHelper
		val paths = helper.selectedItems.mapTo(mutableListOf()) { getTrackPath(it) }
		// Keep tracks the list can't show, e.g. the current track while recording is off
		val shownPaths = helper.allItems.mapTo(mutableSetOf()) { getTrackPath(it) }
		initialPaths.filterTo(paths) { it !in shownPaths }
		return paths
	}

	private fun getTrackItems(paths: Set<String>): Set<TrackItem> =
		itemsSelectionHelper.allItems.filterTo(mutableSetOf()) { getTrackPath(it) in paths }

	private fun getTrackPath(item: TrackItem): String =
		if (item.isShowCurrentTrack) CURRENT_TRACK else item.path
}
