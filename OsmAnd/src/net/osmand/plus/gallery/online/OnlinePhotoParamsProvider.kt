package net.osmand.plus.gallery.online

import net.osmand.data.LatLon

interface OnlinePhotoParamsProvider {

	fun getLatLon(): LatLon

	fun getAdditionalImageParams(): Map<String, String>
}