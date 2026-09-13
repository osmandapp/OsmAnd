package net.osmand.plus.plugins.audionotes.library.data

import net.osmand.data.LatLon
import net.osmand.shared.gpx.primitives.Linkable
import net.osmand.shared.gpx.primitives.Link

data class MediaAttachment(
	val target: Linkable,
	val link: Link,
	val kind: Kind,
	val name: String,
	val latLon: LatLon,
	val trackFile: String? = null
) {
	enum class Kind { FAVORITE, TRACK_POINT }
}
