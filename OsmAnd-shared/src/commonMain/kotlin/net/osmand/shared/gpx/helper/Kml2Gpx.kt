package net.osmand.shared.gpx.helper

import okio.Source

expect object Kml2Gpx {
	fun toGpx(kml: Source): String?
}
