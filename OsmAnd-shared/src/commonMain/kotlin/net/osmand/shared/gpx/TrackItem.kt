package net.osmand.shared.gpx

import net.osmand.shared.io.KFile
import net.osmand.shared.util.Localization

class TrackItem {
	val name: String

	val path: String

	private val file: KFile?
	val lastModified: Long

	var dataItem: GpxDataItem? = null

	constructor(file: KFile) {
		this.file = file
		path = file.absolutePath()
		name = GpxHelper.getGpxTitle(file.name())
		lastModified = file.lastModified()
	}

	constructor(gpxFile: GpxFile) {
		if (gpxFile.showCurrentTrack) {
			file = null
			path = gpxFile.path
			name = Localization.getString("shared_string_currently_recording_track")
			lastModified = gpxFile.modifiedTime
		} else {
			file = KFile(gpxFile.path)
			path = file.absolutePath()
			name = GpxHelper.getGpxTitle(file.name())
			lastModified = file.lastModified()
		}
	}

	fun getFile(): KFile? {
		return file
	}

	val isShowCurrentTrack: Boolean
		get() = file == null

	override fun toString(): String {
		return name
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
		val trackItem = other as TrackItem
		return file?.equals(trackItem.file) == true
	}

	override fun hashCode(): Int {
		return file?.hashCode() ?: super.hashCode()
	}
}
