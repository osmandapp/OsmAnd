package net.osmand.plus.plugins.srtm.building

import android.os.Handler
import android.text.format.DateFormat
import android.os.Looper
import io.github.cosinekitty.astronomy.Aberration
import io.github.cosinekitty.astronomy.Body
import io.github.cosinekitty.astronomy.EquatorEpoch
import io.github.cosinekitty.astronomy.Observer
import io.github.cosinekitty.astronomy.Refraction
import io.github.cosinekitty.astronomy.Time
import io.github.cosinekitty.astronomy.equator
import io.github.cosinekitty.astronomy.horizon
import net.osmand.core.jni.Utilities
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.srtm.SRTMPlugin
import net.osmand.plus.utils.OsmAndFormatter
import net.osmand.util.MapUtils
import java.util.Date

/**
 * Direction of the light for the hillshade and the sun shadows.
 *
 * In [SunLightMode.MANUAL] it is the stored azimuth and altitude. In the other modes the renderer
 * computes the real sun above the target of the map - now ([SunLightMode.LIVE]) or at the stored
 * date and time ([SunLightMode.DATE]) - and follows the map by itself. Live mode gets the current
 * time every minute while the map is on screen.
 */
class SunLight(private val app: OsmandApplication, private val plugin: SRTMPlugin) {

	/** Azimuth clockwise from north and altitude above the horizon, in degrees. */
	data class Angles(val azimuth: Double, val altitude: Double)

	private val handler = Handler(Looper.getMainLooper())
	private var mapVisible = false

	private val liveUpdate = object : Runnable {
		override fun run() {
			plugin.updateSunPosition()
			handler.postDelayed(this, LIVE_UPDATE_INTERVAL_MS)
		}
	}

	/**
	 * The light of the current mode as the renderer draws it - the altitude may be negative at night.
	 * Null while the map has no OpenGL renderer.
	 */
	fun getAngles(): Angles? = when (plugin.SUN_LIGHT_MODE.get()) {
		SunLightMode.MANUAL -> Angles(plugin.HILLSHADE_SUN_AZIMUTH.get().toDouble(), plugin.HILLSHADE_SUN_ANGLE.get().toDouble())
		else -> getSunAtMapTarget(getSunTime())
	}

	/** The moment the renderer computes the sun for: now in Live, the stored date and time in Date. */
	fun getSunTime(): Long = if (plugin.SUN_LIGHT_MODE.get() == SunLightMode.LIVE) System.currentTimeMillis() else getDateTime()

	/** Date and time of [SunLightMode.DATE]; until one is chosen, now. */
	fun getDateTime(): Long {
		val time = plugin.SUN_LIGHT_TIME.get()
		return if (time != 0L) time else System.currentTimeMillis()
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

	/** The sun above the target of the map, with the formula of the renderer. */
	fun getSunAtMapTarget(timeMillis: Long): Angles? {
		val target31 = app.osmandMap.mapView.mapRenderer?.state?.target31 ?: return null
		if (timeMillis <= 0) {
			// The renderer takes a time before 1970 for "keep the angles": they come from here
			val latitude = MapUtils.get31LatitudeY(target31.y)
			val longitude = MapUtils.get31LongitudeX(target31.x)
			return getSun(latitude, longitude, timeMillis)
		}
		val sun = Utilities.getSunAnglesFromTarget31(target31, timeMillis)
		return Angles(azimuth = sun.y, altitude = sun.x)
	}

	private fun getSun(latitude: Double, longitude: Double, timeMillis: Long): Angles {
		val time = Time.fromMillisecondsSince1970(timeMillis)
		val observer = Observer(latitude, longitude, 0.0)
		val equatorial = equator(Body.Sun, time, observer, EquatorEpoch.OfDate, Aberration.Corrected)
		val horizontal = horizon(time, observer, equatorial.ra, equatorial.dec, Refraction.Normal)
		return Angles(horizontal.azimuth, horizontal.altitude)
	}

	/** The map of MapActivity is on the phone screen. */
	fun setMapVisible(visible: Boolean) {
		mapVisible = visible
		updateLiveUpdates()
	}

	/** Starts or stops the minute updates after the mode or the visibility of the map changed. */
	fun updateLiveUpdates() {
		handler.removeCallbacks(liveUpdate)
		// The Android Auto map stays on the car screen also while the phone is locked
		val mapOnScreen = mapVisible || app.isAppInForegroundOnAndroidAuto
		// Date keeps its moment: the renderer follows the map by itself
		if (mapOnScreen && plugin.SUN_LIGHT_MODE.get() == SunLightMode.LIVE) {
			liveUpdate.run()
		}
	}

	/** Stops the minute updates when the plugin is turned off, whichever map is on screen. */
	fun stopLiveUpdates() {
		mapVisible = false
		handler.removeCallbacks(liveUpdate)
	}

	companion object {
		private const val LIVE_UPDATE_INTERVAL_MS = 60_000L
	}
}
