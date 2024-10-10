package net.osmand.shared.gpx.data

import net.osmand.shared.gpx.GpxHelper
import net.osmand.shared.gpx.TrackItem
import net.osmand.shared.gpx.filters.TrackFolderAnalysis
import net.osmand.shared.io.KFile
import net.osmand.shared.util.KAlgorithms
import net.osmand.shared.util.KCollectionUtils
import kotlin.math.max

class TrackFolder(dirFile: KFile, parentFolder: TrackFolder?) :
	TracksGroup, ComparableTracksGroup {
	private var dirFile: KFile
	private val parentFolder: TrackFolder?
	private var trackItems = mutableListOf<TrackItem>()
	private var subFolders = mutableListOf<TrackFolder>()
	private var flattenedTrackItems: List<TrackItem>? = null
	private var flattenedSubFolders: List<TrackFolder>? = null
	private var folderAnalysis: TrackFolderAnalysis? = null
	private var lastModified: Long = -1

	init {
		this.dirFile = dirFile
		this.parentFolder = parentFolder
	}

	constructor(trackFolder: TrackFolder) : this(trackFolder.dirFile, trackFolder.parentFolder) {
		update(trackFolder)
	}

	fun update(folder: TrackFolder) {
		trackItems = ArrayList(folder.trackItems)
		subFolders = ArrayList(folder.subFolders)
		flattenedTrackItems = folder.flattenedTrackItems?.toList()
		flattenedSubFolders = folder.flattenedSubFolders?.toList()
		folderAnalysis = folder.folderAnalysis
		lastModified = folder.lastModified
	}

	override fun getName(): String {
		return GpxHelper.getFolderName(dirFile, false)
	}

	fun getDirFile(): KFile {
		return dirFile
	}

	fun setDirFile(dirFile: KFile) {
		this.dirFile = dirFile
	}

	val relativePath: String
		get() {
			val dirName = getDirName()
			val parentFolder = getParentFolder()
			return if (parentFolder != null && !parentFolder.isRootFolder) parentFolder.relativePath + "/" + dirName else dirName
		}

	val isRootFolder: Boolean
		get() = getParentFolder() == null

	fun getParentFolder(): TrackFolder? {
		return parentFolder
	}

	fun getSubFolders(): List<TrackFolder> {
		return subFolders
	}

	override fun getTrackItems(): List<TrackItem> {
		return trackItems
	}

	fun setSubFolders(subFolders: List<TrackFolder>) {
		this.subFolders = ArrayList(subFolders)
	}

	fun addSubFolder(subFolder: TrackFolder) {
		this.subFolders.add(subFolder)
	}

	fun setTrackItems(trackItems: MutableList<TrackItem>) {
		this.trackItems = ArrayList(trackItems)
	}

	fun addTrackItem(trackItem: TrackItem) {
		this.trackItems.add(trackItem)
	}

	fun addTrackItems(trackItems: List<TrackItem>) {
		this.trackItems.addAll(trackItems)
	}

	val isEmpty: Boolean
		get() = KAlgorithms.isEmpty(trackItems) && KAlgorithms.isEmpty(getSubFolders())

	val color: Int
		get() = KAlgorithms.parseColor("#727272")
	val totalTracksCount: Int
		get() = getFlattenedTrackItems().size

	fun getFlattenedTrackItems(): List<TrackItem> {
		var flattenedTrackItems = this.flattenedTrackItems
		if (flattenedTrackItems == null) {
			flattenedTrackItems = mutableListOf()
			val stack: ArrayDeque<TrackFolder> = ArrayDeque()
			stack.add(this)
			while (!stack.isEmpty()) {
				val current: TrackFolder = stack.removeLast()
				flattenedTrackItems.addAll(current.trackItems)
				for (folder in current.subFolders) {
					stack.addLast(folder)
				}
			}
			this.flattenedTrackItems = flattenedTrackItems
		}
		return flattenedTrackItems
	}

	fun getFlattenedSubFolders(): List<TrackFolder> {
		var flattenedSubFolders = this.flattenedSubFolders
		if (flattenedSubFolders == null) {
			flattenedSubFolders = mutableListOf()
			val stack = ArrayDeque<TrackFolder>()
			stack.addLast(this)
			while (!stack.isEmpty()) {
				val current: TrackFolder = stack.removeLast()
				flattenedSubFolders.addAll(current.subFolders)
				for (folder in current.subFolders) {
					stack.addLast(folder)
				}
			}
			this.flattenedSubFolders = flattenedSubFolders
		}
		return flattenedSubFolders
	}

	override fun getFolderAnalysis(): TrackFolderAnalysis {
		var analysis: TrackFolderAnalysis? = folderAnalysis
		if (analysis == null) {
			analysis = TrackFolderAnalysis(this)
			folderAnalysis = analysis
		}
		return analysis
	}

	override fun getDirName(): String {
		return dirFile.name()
	}

	fun getLastModified(): Long {
		if (lastModified < 0) {
			lastModified = dirFile.lastModified()
			for (folder in getSubFolders()) {
				lastModified = max(lastModified, folder.getLastModified())
			}
			for (item in trackItems) {
				lastModified = max(lastModified, item.lastModified)
			}
		}
		return lastModified
	}

	fun clearData() {
		resetCachedData()
		trackItems = ArrayList()
		subFolders = ArrayList()
	}

	fun resetCachedData() {
		lastModified = -1
		flattenedTrackItems = null
		flattenedSubFolders = null
		folderAnalysis = null
	}

	override fun hashCode(): Int {
		return dirFile.hashCode()
	}

	override fun equals(other: Any?): Boolean {
		if (this === other) {
			return true
		}
		if (other == null) {
			return false
		}
		if (this::class != other::class) {
			return false
		}
		val trackFolder = other as TrackFolder
		return trackFolder.dirFile == dirFile
	}

	override fun toString(): String {
		return dirFile.absolutePath()
	}

	override fun lastModified(): Long {
		return getDirFile().lastModified()
	}
}
