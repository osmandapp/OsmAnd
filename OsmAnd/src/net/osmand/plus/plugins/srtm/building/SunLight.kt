package net.osmand.plus.plugins.srtm.building

import android.text.format.DateFormat
import net.osmand.core.jni.Utilities
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.srtm.SRTMPlugin
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.plus.views.corenative.NativeCoreContext
import java.util.Date

class SunLight(private val app: OsmandApplication, private val plugin: SRTMPlugin) {

	/** Azimuth clockwise from north and altitude above the horizon, in degrees. */
	data class Angles(val azimuth: Double, val altitude: Double)

	fun getLiveAngles(): Angles? {
		if (!NativeCoreContext.isInit()) return null
		val renderer = app.osmandMap.mapView.mapRenderer ?: return null
		val sun = Utilities.getSunAnglesFromTarget31(renderer.state.target31)
		return Angles(azimuth = sun.y, altitude = sun.x)
	}

	/** Date and time of [SunLightMode.DATE]; until one is chosen, now. */
	fun getDateTime(): Long {
		val time = plugin.SUN_LIGHT_TIME.get()
		// Core reserves zero for Live and negative timestamps for manual angles.
		return if (time > 0) time else System.currentTimeMillis()
	}

	/** Short state for the Sun position rows: the mode, the date and time, or the manual angles. */
	fun getDescription(): String = when (plugin.SUN_LIGHT_MODE.get()) {
		SunLightMode.LIVE -> app.getString(R.string.sun_light_live)
		SunLightMode.DATE -> {
			val date = Date(getDateTime())
			DateFormat.getMediumDateFormat(app).format(date) + ", " + DateFormat.getTimeFormat(app).format(date)
		}
		else -> {
			val azimuth = plugin.HILLSHADE_SUN_AZIMUTH.get()
			"$azimuth°, ${OsmAndFormatter.getLocalizedCardinalDirection(app, azimuth.toDouble())}"
		}
	}
}
