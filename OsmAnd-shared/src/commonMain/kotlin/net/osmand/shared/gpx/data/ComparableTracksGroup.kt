package net.osmand.shared.gpx.data

import net.osmand.shared.gpx.filters.TrackFolderAnalysis

interface ComparableTracksGroup {
	fun getFolderAnalysis(): TrackFolderAnalysis
	fun getDirName(): String
	fun lastModified(): Long
}