package net.osmand.plus.shared

import net.osmand.shared.gpx.helper.Kml2Gpx
import okio.source
import java.io.InputStream

object GpxImport {
	@JvmStatic
	fun kml2Gpx(inputStream: InputStream): String? {
		return Kml2Gpx.toGpx(inputStream.source())
	}
}