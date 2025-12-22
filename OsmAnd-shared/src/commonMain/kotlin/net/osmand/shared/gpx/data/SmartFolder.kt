package net.osmand.shared.gpx.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.enums.OrganizeByType
import net.osmand.shared.gpx.filters.BaseTrackFilter
import net.osmand.shared.gpx.filters.TrackFilterSerializer
import net.osmand.shared.gpx.filters.TrackFiltersHelper
import net.osmand.shared.gpx.filters.TrackFolderAnalysis
import net.osmand.shared.util.KCollectionUtils

@Serializable
class SmartFolder(@Serializable var folderName: String) : TracksGroup, ComparableTracksGroup {
	companion object {
		const val ID_PREFIX = "SMART_FOLDER___"
	}

	@Transient
	private var trackItems: List<TrackItem>? = null

	@Transient
	private var organizedTrackItems: Map<OrganizedTrackGroup, MutableList<TrackItem>>? = null

	constructor() : this("")

	@Serializable
	var creationTime = 0L

	@Serializable(with = TrackFilterSerializer::class)
	var filters: List<BaseTrackFilter>? = null

	@Transient
	private var folderAnalysis: TrackFolderAnalysis? = null

	private var organizeByFilter: BaseTrackFilter? = null
	private var organizeByType: OrganizeByType? = null

	fun organizeByType(organizeByType: OrganizeByType) {
		this.organizeByType = organizeByType
		organizeByFilter = TrackFiltersHelper.createFilter(organizeByType.filterType, null)
		//todo check filter actual min max initialization
		organizeByFilter?.initFilter()
		organizeByFilter?.initOrganizedByGroups(organizeByType)

		val organizedTrackItems = HashMap<OrganizedTrackGroup, MutableList<TrackItem>>()
		trackItems?.let { items ->
			for (trackItem in items) {
				organizeByFilter?.let { filter ->
					val group: OrganizedTrackGroup? = filter.getOrganizedByGroup(trackItem)
					group?.let {
						val groupLIst = organizedTrackItems[it]
						if (groupLIst == null) {
							organizedTrackItems[it] = ArrayList()
						}
						groupLIst?.add(trackItem)
					}
				}
			}
		}
		this.organizedTrackItems = organizedTrackItems
	}

	override fun getId(): String {
		return ID_PREFIX + folderName
	}

	override fun getName() = folderName

	override fun getTrackItems(): List<TrackItem> {
		var trackItems = this.trackItems
		if (trackItems == null) {
			trackItems = ArrayList()
			this.trackItems = trackItems
		}
		return trackItems
	}

	fun addTrackItem(trackItem: TrackItem) {
		if (!getTrackItems().contains(trackItem)) {
			trackItems = KCollectionUtils.addToList(getTrackItems(), trackItem)
			folderAnalysis = null
		}
	}

	override fun getFolderAnalysis(): TrackFolderAnalysis {
		var analysis = folderAnalysis
		if (analysis == null) {
			analysis = TrackFolderAnalysis(this)
			folderAnalysis = analysis
		}
		return analysis
	}

	override fun getDirName(includingSubdirs: Boolean) = folderName

	override fun lastModified() = creationTime

	fun resetItems() {
		trackItems = ArrayList()
		folderAnalysis = null
	}
}