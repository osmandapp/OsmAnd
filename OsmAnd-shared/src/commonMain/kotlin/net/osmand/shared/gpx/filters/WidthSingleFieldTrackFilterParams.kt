package net.osmand.shared.gpx.filters

class WidthSingleFieldTrackFilterParams : SingleFieldTrackFilterParams() {

	override fun includeEmptyValues(): Boolean {
		return true
	}
}