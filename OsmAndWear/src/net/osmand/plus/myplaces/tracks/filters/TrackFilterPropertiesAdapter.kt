package net.osmand.plus.myplaces.tracks.filters

interface TrackFilterPropertiesAdapter {
	fun filterCollection(query: String)
	fun setNewSelectedItems(newSelectedItems: List<String>)
}