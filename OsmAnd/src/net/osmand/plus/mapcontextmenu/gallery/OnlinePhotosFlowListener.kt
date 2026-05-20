package net.osmand.plus.mapcontextmenu.gallery

import net.osmand.data.LatLon
import net.osmand.plus.gallery.online.OnlinePhotosHolder

interface OnlinePhotosFlowListener {

	fun onPhotosLoadStarted()

	fun onPhotosLoadFinished(holder: OnlinePhotosHolder)

	// TODO: this parameters should be in constructor of OnlinePhotosFlow?
	fun getLatLon(): LatLon
	fun getAdditionalImageParams(): Map<String, String>
}