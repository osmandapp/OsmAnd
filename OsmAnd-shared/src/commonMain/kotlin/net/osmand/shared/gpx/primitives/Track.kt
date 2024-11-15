package net.osmand.shared.gpx.primitives

class Track : GpxExtensions() {
	var name: String? = null
	var desc: String? = null
	var segments = mutableListOf<TrkSegment>()
	var generalTrack = false

	fun isGeneralTrack() = generalTrack
}