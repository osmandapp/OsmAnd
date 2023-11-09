package net.osmand.plus.track

import net.osmand.plus.track.data.TrackFolderAnalysis

interface ComparableTracksGroup {
	fun getFolderAnalysis(): TrackFolderAnalysis
	fun getDirName(): String
	fun lastModified(): Long
}