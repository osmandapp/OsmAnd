package net.osmand.plus.mapcontextmenu.gallery

import net.osmand.plus.gallery.online.OnlinePhotosHolder

interface OnlinePhotosFlowListener {

	fun onPhotosLoadStarted()

	fun onPhotosLoadFinished(holder: OnlinePhotosHolder)
}