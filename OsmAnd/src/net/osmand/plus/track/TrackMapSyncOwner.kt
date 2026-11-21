package net.osmand.plus.track

interface TrackMapSyncOwner {

	fun isMapInSync(): Boolean

	fun setMapInSync(inSync: Boolean)
}
