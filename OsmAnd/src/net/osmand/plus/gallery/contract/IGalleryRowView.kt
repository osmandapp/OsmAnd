package net.osmand.plus.gallery.contract

import net.osmand.plus.activities.MapActivity

interface IGalleryRowView {

	val mapActivity: MapActivity

	fun render()

	fun onLoadingImage(loading: Boolean)

	fun isNightMode(): Boolean
}