package net.osmand.shared.filters

interface ComparableTracksGroup {
	fun getFolderAnalysis(): TrackFolderAnalysis
	fun getDirName(): String
	fun lastModified(): Long
}