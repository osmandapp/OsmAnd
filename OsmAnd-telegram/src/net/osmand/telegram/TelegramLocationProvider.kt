package net.osmand.telegram

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.*
import android.os.Build
import android.util.Log
import net.osmand.Location
import net.osmand.PlatformUtil
import net.osmand.data.LatLon
import net.osmand.telegram.helpers.location.LocationCallback
import net.osmand.telegram.helpers.location.LocationServiceHelper
import net.osmand.telegram.utils.AndroidUtils
import net.osmand.util.MapUtils
import java.util.*
import kotlin.math.atan2

class TelegramLocationProvider(private val app: TelegramApplication) : SensorEventListener {

	private var locationServiceHelper: LocationServiceHelper = app.createLocationServiceHelper()
	private var lastTimeGPSLocationFixed: Long = 0

	private var sensorRegistered = false
	private val mGravs = FloatArray(3)
	private val mGeoMags = FloatArray(3)
	private var previousCorrectionValue = 360f

	internal var avgValSin = 0f
	internal var avgValCos = 0f
	internal var lastValSin = 0f
	internal var lastValCos = 0f
	private val previousCompassValuesA = FloatArray(50)
	private val previousCompassValuesB = FloatArray(50)
	private var previousCompassIndA = 0
	private var previousCompassIndB = 0
	private var inUpdateValue = false

	@get:Synchronized
	var heading: Float? = null
		private set

	// Current screen orientation
	private var currentScreenOrientation: Int = 0

	var lastKnownLocation: Location? = null
		private set

	private val locationListeners = ArrayList<TelegramLocationListener>()
	private val compassListeners = ArrayList<TelegramCompassListener>()
	private val mRotationM = FloatArray(9)
	private val useMagneticFieldSensorCompass = false

	val lastKnownLocationLatLon: LatLon?
		get() = if (lastKnownLocation != null) {
			LatLon(lastKnownLocation!!.latitude, lastKnownLocation!!.longitude)
		} else {
			null
		}

	interface TelegramLocationListener {
		fun updateLocation(location: Location?)
	}

	interface TelegramCompassListener {
		fun updateCompassValue(value: Float)
	}

	fun updateLocationSource() {
		pauseAllUpdates()
		locationServiceHelper = app.createLocationServiceHelper()
		resumeAllUpdates()
	}

	@SuppressLint("MissingPermission")
	fun resumeAllUpdates() {
		registerOrUnregisterCompassListener(true)

		if (AndroidUtils.isLocationPermissionAvailable(app)) {
			try {
				locationServiceHelper.requestLocationUpdates(
					object : LocationCallback() {
						override fun onLocationResult(locations: List<Location>) {
							var location: Location? = null
							if (locations.isNotEmpty()) {
								location = locations[locations.size - 1]
								lastTimeGPSLocationFixed = System.currentTimeMillis()
							}
							setLocation(location)
						}
					}
				)
			} catch (e : SecurityException) {
				// Location service permission not granted
			} catch (e : IllegalArgumentException) {
				// GPS location provider not available
			}
			// try to always ask for network provide : it is faster way to find location
			if (locationServiceHelper.isNetworkLocationUpdatesSupported()) {
				locationServiceHelper.requestNetworkLocationUpdates(
					object : LocationCallback() {
						override fun onLocationResult(locations: List<Location>) {
							if (locations.isNotEmpty() && !useOnlyGPS()) {
								setLocation(locations[locations.size - 1])
							}
						}
					}
				)
			}
		}
	}

	fun pauseAllUpdates() {
		stopLocationRequests()
		registerOrUnregisterCompassListener(false)
	}

	private fun stopLocationRequests() {
		try {
			locationServiceHelper.removeLocationUpdates()
		} catch (unlikely: SecurityException) {
			// Location service permission not granted
		}
	}

	private fun setLocation(location: Location?) {
		this.lastKnownLocation = location
		updateLocation(this.lastKnownLocation)
	}

	private fun updateLocation(loc: Location?) {
		for (l in locationListeners) {
			l.updateLocation(loc)
		}
	}

	fun checkIfLastKnownLocationIsValid() {
		val loc = lastKnownLocation
		if (loc != null && System.currentTimeMillis() - loc.time > INTERVAL_TO_CLEAR_SET_LOCATION) {
			setLocation(null)
		}
	}

	private fun useOnlyGPS(): Boolean {
		val now = System.currentTimeMillis()
		val timePassed = now - lastTimeGPSLocationFixed < NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS
		return timePassed || isRunningOnEmulator()
	}

	private fun isRunningOnEmulator(): Boolean = Build.DEVICE == "generic"

	fun updateScreenOrientation(orientation: Int) {
		currentScreenOrientation = orientation
	}

	fun addLocationListener(listener: TelegramLocationListener) {
		if (!locationListeners.contains(listener)) {
			locationListeners.add(listener)
		}
	}

	fun removeLocationListener(listener: TelegramLocationListener) {
		locationListeners.remove(listener)
	}

	fun addCompassListener(listener: TelegramCompassListener) {
		if (!compassListeners.contains(listener)) {
			compassListeners.add(listener)
		}
	}

	fun removeCompassListener(listener: TelegramCompassListener) {
		compassListeners.remove(listener)
	}

	@Synchronized
	fun registerOrUnregisterCompassListener(register: Boolean) {
		if (sensorRegistered && !register) {
			Log.d(PlatformUtil.TAG, "Disable sensor")
			(app.getSystemService(Context.SENSOR_SERVICE) as SensorManager).unregisterListener(this)
			sensorRegistered = false
			heading = null
		} else if (!sensorRegistered && register) {
			Log.d(PlatformUtil.TAG, "Enable sensor")
			val sensorMgr = app.getSystemService(Context.SENSOR_SERVICE) as SensorManager
			if (useMagneticFieldSensorCompass) {
				var s: Sensor? = sensorMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
				if (s == null || !sensorMgr.registerListener(
						this,
						s,
						SensorManager.SENSOR_DELAY_UI
					)
				) {
					Log.e(PlatformUtil.TAG, "Sensor accelerometer could not be enabled")
				}
				s = sensorMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
				if (s == null || !sensorMgr.registerListener(
						this,
						s,
						SensorManager.SENSOR_DELAY_UI
					)
				) {
					Log.e(PlatformUtil.TAG, "Sensor magnetic field could not be enabled")
				}
			} else {
				val s = sensorMgr.getDefaultSensor(Sensor.TYPE_ORIENTATION)
				if (s == null || !sensorMgr.registerListener(
						this,
						s,
						SensorManager.SENSOR_DELAY_UI
					)
				) {
					Log.e(PlatformUtil.TAG, "Sensor orientation could not be enabled")
				}
			}
			sensorRegistered = true
		}
	}

	override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}

	override fun onSensorChanged(event: SensorEvent) {
		// Attention : sensor produces a lot of events & can hang the system
		if (inUpdateValue) {
			return
		}
		synchronized(this) {
			if (!sensorRegistered) {
				return
			}
			inUpdateValue = true
			try {
				var `val` = 0f
				when (event.sensor.type) {
					Sensor.TYPE_ACCELEROMETER -> System.arraycopy(event.values, 0, mGravs, 0, 3)
					Sensor.TYPE_MAGNETIC_FIELD -> System.arraycopy(event.values, 0, mGeoMags, 0, 3)
					Sensor.TYPE_ORIENTATION -> `val` = event.values[0]
					else -> return
				}
				if (useMagneticFieldSensorCompass) {
					if (mGravs != null && mGeoMags != null) {
						val success =
							SensorManager.getRotationMatrix(mRotationM, null, mGravs, mGeoMags)
						if (!success) {
							return
						}
						val orientation = SensorManager.getOrientation(mRotationM, FloatArray(3))
						`val` = Math.toDegrees(orientation[0].toDouble()).toFloat()
					} else {
						return
					}
				}
				`val` = calcScreenOrientationCorrection(`val`)
				`val` = calcGeoMagneticCorrection(`val`)

				val valRad = (`val` / 180f * Math.PI).toFloat()
				lastValSin = Math.sin(valRad.toDouble()).toFloat()
				lastValCos = Math.cos(valRad.toDouble()).toFloat()

				avgValSin = lastValSin
				avgValCos = lastValCos

				updateCompassVal()
			} finally {
				inUpdateValue = false
			}
		}
	}

	private fun calcGeoMagneticCorrection(value: Float): Float {
		var res = value
		if (previousCorrectionValue == 360f && lastKnownLocation != null) {
			val l = lastKnownLocation
			val gf = GeomagneticField(
				l!!.latitude.toFloat(), l.longitude.toFloat(), l.altitude.toFloat(),
				System.currentTimeMillis()
			)
			previousCorrectionValue = gf.declination
		}
		if (previousCorrectionValue != 360f) {
			res += previousCorrectionValue
		}
		return res
	}

	private fun calcScreenOrientationCorrection(value: Float): Float {
		var res = value
		when (currentScreenOrientation) {
			1 -> res += 90f
			2 -> res += 180f
			3 -> res -= 90f
		}
		return res
	}

	private fun filterCompassValue() {
		if (heading == null && previousCompassIndA == 0) {
			Arrays.fill(previousCompassValuesA, lastValSin)
			Arrays.fill(previousCompassValuesB, lastValCos)
			avgValSin = lastValSin
			avgValCos = lastValCos
		} else {
			val l = previousCompassValuesA.size
			previousCompassIndA = (previousCompassIndA + 1) % l
			previousCompassIndB = (previousCompassIndB + 1) % l
			// update average
			avgValSin += (-previousCompassValuesA[previousCompassIndA] + lastValSin) / l
			previousCompassValuesA[previousCompassIndA] = lastValSin
			avgValCos += (-previousCompassValuesB[previousCompassIndB] + lastValCos) / l
			previousCompassValuesB[previousCompassIndB] = lastValCos
		}
	}

	private fun updateCompassVal() {
		heading = getAngle(avgValSin, avgValCos)
		for (c in compassListeners) {
			c.updateCompassValue(heading!!)
		}
	}

	private fun getAngle(sinA: Float, cosA: Float) = MapUtils.unifyRotationTo360(
		(atan2(sinA.toDouble(), cosA.toDouble()) * 180 / Math.PI).toFloat()
	)

	companion object {
		private const val INTERVAL_TO_CLEAR_SET_LOCATION = 30 * 1000
		private const val NOT_SWITCH_TO_NETWORK_WHEN_GPS_LOST_MS = 12000
	}
}
