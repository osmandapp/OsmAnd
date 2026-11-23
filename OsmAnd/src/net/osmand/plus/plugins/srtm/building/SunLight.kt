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
import net.osmand.plus.OsmandApplication
import net.osmand.plus.R
import net.osmand.plus.plugins.srtm.SRTMPlugin
import net.osmand.plus.utils.OsmAndFormatter
import java.util.Date
import kotlin.math.roundToInt

/**
 * Direction of the light for the hillshade and the sun shadows.
 *
 * In [SunLightMode.MANUAL] it is the stored azimuth and altitude. In the other modes it is the
 * real sun above the centre of the map - now ([SunLightMode.LIVE]) or at the stored date and time
 * ([SunLightMode.DATE]). Those two follow the map, and live mode also the clock: the light is
 * recalculated every minute while the map is on screen. The renderer gets whole degrees and skips
 * a configuration that did not change.
 */
class SunLight(private val app: OsmandApplication, private val plugin: SRTMPlugin) {

	/** Azimuth clockwise from north and altitude above the horizon, in degrees. */
	data class Angles(val azimuth: Double, val altitude: Double)

	private val handler = Handler(Looper.getMainLooper())
	private var mapVisible = false

	private val liveUpdate = object : Runnable {
		override fun run() {
			plugin.updateElevationConfiguration()
			handler.postDelayed(this, LIVE_UPDATE_INTERVAL_MS)
		}
	}

	/** The light of the current mode, as the user sees it - the altitude may be negative at night. */
	fun getAngles(): Angles = when (plugin.SUN_LIGHT_MODE.get()) {
		SunLightMode.LIVE -> getSunAtMapCenter(System.currentTimeMillis())
		SunLightMode.DATE -> getSunAtMapCenter(getDateTime())
		else -> Angles(plugin.HILLSHADE_SUN_AZIMUTH.get().toDouble(), plugin.HILLSHADE_SUN_ANGLE.get().toDouble())
	}

	/** Azimuth for the renderer, whole degrees 0..359. */
	fun getLightAzimuth(): Int = Math.floorMod(getAngles().azimuth.roundToInt(), 360)

	/** Altitude for the renderer: the manual range 0..90, a sun below the horizon lights from 0. */
	fun getLightAltitude(): Int = getAngles().altitude.roundToInt().coerceIn(MIN_ALTITUDE, MAX_ALTITUDE)

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

	fun getSunAtMapCenter(timeMillis: Long): Angles {
		val mapView = app.osmandMap.mapView
		return getSun(mapView.latitude, mapView.longitude, timeMillis)
	}

	fun getSun(latitude: Double, longitude: Double, timeMillis: Long): Angles {
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
		if (mapOnScreen && plugin.SUN_LIGHT_MODE.get() != SunLightMode.MANUAL) {
			liveUpdate.run()
		}
	}

	/** Stops the minute updates when the plugin is turned off, whichever map is on screen. */
	fun stopLiveUpdates() {
		mapVisible = false
		handler.removeCallbacks(liveUpdate)
	}

	companion object {
		const val MIN_ALTITUDE = 0
		const val MAX_ALTITUDE = 90
		private const val LIVE_UPDATE_INTERVAL_MS = 60_000L
	}
}
