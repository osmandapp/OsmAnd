@file:JvmName("TrackItemUtils")
package net.osmand.shared.gpx

import kotlin.jvm.JvmName

fun List<TrackItem>.filterByPaths(paths: List<String>?): List<TrackItem> {
	if (paths.isNullOrEmpty() || this.isEmpty()) return emptyList()
	val pathSet = paths.toSet()
	return this.filter { it.path in pathSet }
}