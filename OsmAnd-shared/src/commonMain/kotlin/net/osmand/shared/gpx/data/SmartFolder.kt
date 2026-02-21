package net.osmand.shared.gpx.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.enums.TracksSortScope
import net.osmand.shared.gpx.organization.TracksOrganizer
import net.osmand.shared.gpx.filters.BaseTrackFilter
import net.osmand.shared.gpx.filters.TrackFilterSerializer
import net.osmand.shared.gpx.filters.TrackFolderAnalysis
import net.osmand.shared.gpx.organization.OrganizeByParams
import net.osmand.shared.gpx.organization.OrganizeByParamsSerializer
import net.osmand.shared.gpx.organization.enums.OrganizeByType
import net.osmand.shared.util.KCollectionUtils

@Serializable
class SmartFolder(@Serializable var folderName: String) : TracksGroup, ComparableTracksGroup {
	companion object {
		const val ID_PREFIX = "SMART_FOLDER___"
	}

	@Transient
	private var trackItems: List<TrackItem>? = null

	@Transient
	private val tracksOrganizer = TracksOrganizer(this)

	@Serializable(with = OrganizeByParamsSerializer::class)
	var organizeByParams: OrganizeByParams? = null
		private set

	constructor() : this("")

	@Serializable
	var creationTime = 0L

	@Serializable(with = TrackFilterSerializer::class)
	var filters: List<BaseTrackFilter>? = null

	@Transient
	private var folderAnalysis: TrackFolderAnalysis? = null

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

	/**
	 * Ensures the track is present in the folder and forces a cache update.
	 * Since [TrackItem] is mutable, its properties (e.g., Activity) might have changed
	 * even if it's already in the list, requiring a re-evaluation of organized groups.
	 */
	fun addTrackItem(trackItem: TrackItem) {
		val currentItems = getTrackItems()
		if (!currentItems.contains(trackItem)) {
			trackItems = KCollectionUtils.addToList(currentItems, trackItem)
		}
		invalidateCache()
	}

	fun removeTrackItem(trackItem: TrackItem) {
		val currentItems = getTrackItems()
		if (currentItems.contains(trackItem)) {
			trackItems = KCollectionUtils.removeFromList(currentItems, trackItem)
			invalidateCache()
		}
	}

	override fun getSubgroupById(subgroupId: String): TracksGroup? {
		return getOrganizedTrackItems().find { it.getId() == subgroupId }
	}

	fun getOrganizedTrackItems(): List<OrganizedTracksGroup> {
		return tracksOrganizer.getOrganizedTrackItems()
	}

	fun setOrganizeByParams(organizeByParams: OrganizeByParams?) {
		this.organizeByParams = organizeByParams
		tracksOrganizer.setOrganizeByParams(organizeByParams)
	}

	override fun getTracksSortScope(): TracksSortScope {
		return tracksOrganizer.params?.type?.getTrackSortScope() ?: super.getTracksSortScope()
	}

	override fun getSupportedSortScopes(): List<TracksSortScope> {
		return listOf(TracksSortScope.TRACKS, TracksSortScope.ORGANIZED_BY_NAME, TracksSortScope.ORGANIZED_BY_VALUE)
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

	fun getOrganizeByType(): OrganizeByType? {
		return tracksOrganizer.params?.type
	}

	fun initTracksOrganizer() {
		tracksOrganizer.initParams(organizeByParams)
	}

	fun resetItems() {
		trackItems = ArrayList()
		invalidateCache()
	}

	fun invalidateCache() {
		tracksOrganizer.clearCache()
		folderAnalysis = null
	}
}