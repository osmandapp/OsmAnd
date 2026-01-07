package net.osmand.shared.gpx.data

import net.osmand.shared.gpx.filters.TrackFolderAnalysis

interface ComparableTracksGroup {
	fun getFolderAnalysis(): TrackFolderAnalysis
	fun getDirName(includingSubdirs: Boolean): String
	fun lastModified(): Long
	fun getDefaultOrder(): Int = -1
	fun getSortValue(): Double = 0.0
}