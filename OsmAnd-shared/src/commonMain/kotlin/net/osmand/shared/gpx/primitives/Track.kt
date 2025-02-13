package net.osmand.shared.gpx.primitives

class Track : GpxExtensions {
	var name: String? = null
	var desc: String? = null
	var segments = mutableListOf<TrkSegment>()
	var generalTrack = false

	constructor()

	constructor(other: Track) : this() {
		this.name = other.name
		this.desc = other.desc
		this.segments.addAll(other.segments.map { TrkSegment(it) })
		this.generalTrack = other.generalTrack
	}

	fun isGeneralTrack() = generalTrack
}