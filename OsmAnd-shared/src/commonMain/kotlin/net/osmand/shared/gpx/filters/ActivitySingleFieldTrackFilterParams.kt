package net.osmand.shared.gpx.filters

class ActivitySingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {
	override fun includeEmptyValues(): Boolean {
		return true
	}
}